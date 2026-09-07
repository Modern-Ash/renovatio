import { execFile, spawn } from 'node:child_process';
import { writeFile } from 'node:fs/promises';
import process from 'node:process';
import { promisify } from 'node:util';

const endpoint = process.env.RENOVATIO_SMOKE_URL ?? 'http://127.0.0.1:3000/';
const timeoutMs = Number(process.env.RENOVATIO_SMOKE_TIMEOUT_MS ?? 90000);
const metricsFile = process.env.RENOVATIO_SMOKE_METRICS_FILE ?? '.theia-smoke-metrics.json';
const openSamples = Number(process.env.RENOVATIO_SMOKE_OPEN_SAMPLES ?? 5);
const runsInOwnProcessGroup = process.platform !== 'win32';
const execFileAsync = promisify(execFile);
const startedAt = Date.now();
const child = spawn('npm', ['run', 'start'], {
    cwd: new URL('..', import.meta.url),
    detached: runsInOwnProcessGroup,
    env: {
        ...process.env,
        RENOVATIO_TELEMETRY_ENABLED: process.env.RENOVATIO_TELEMETRY_ENABLED ?? 'false'
    },
    stdio: ['ignore', 'pipe', 'pipe']
});

let output = '';
child.stdout.on('data', chunk => { output = `${output}${chunk}`.slice(-12000); });
child.stderr.on('data', chunk => { output = `${output}${chunk}`.slice(-12000); });
const childClosed = new Promise(resolve => child.once('close', resolve));
const openDurationsMs = [];
let maxRssMb = 0;

function signalProcessTree(signal) {
    if (child.pid === undefined) {
        return;
    }

    try {
        if (runsInOwnProcessGroup) {
            process.kill(-child.pid, signal);
        } else {
            child.kill(signal);
        }
    } catch (error) {
        if (error.code !== 'ESRCH') {
            throw error;
        }
    }
}

async function waitForChildClose(timeout) {
    let timeoutId;
    const closed = await Promise.race([
        childClosed.then(() => true),
        new Promise(resolve => {
            timeoutId = setTimeout(() => resolve(false), timeout);
        })
    ]);
    clearTimeout(timeoutId);
    return closed;
}

async function stopProcessTree() {
    signalProcessTree('SIGTERM');
    if (await waitForChildClose(5000)) {
        return;
    }

    signalProcessTree('SIGKILL');
    await childClosed;
}

async function sampleRssMb() {
    if (!runsInOwnProcessGroup || child.pid === undefined) {
        return 0;
    }

    try {
        const { stdout } = await execFileAsync('ps', ['-o', 'rss=', '-g', String(child.pid)]);
        return stdout
            .trim()
            .split(/\s+/)
            .filter(Boolean)
            .map(value => Number(value))
            .filter(value => Number.isFinite(value))
            .reduce((total, rssKb) => total + rssKb, 0) / 1024;
    } catch {
        return 0;
    }
}

async function fetchHtml() {
    const sampleStart = Date.now();
    const response = await fetch(endpoint);
    const durationMs = Date.now() - sampleStart;
    if (!response.ok) {
        throw new Error(`Unexpected HTTP status ${response.status}`);
    }
    const body = await response.text();
    if (!body.toLowerCase().includes('<html')) {
        throw new Error('Root response was not HTML');
    }
    openDurationsMs.push(durationMs);
}

function percentile95(values) {
    const ordered = [...values].sort((a, b) => a - b);
    const index = Math.ceil(ordered.length * 0.95) - 1;
    return ordered[Math.max(0, index)] ?? 0;
}

async function writeMetrics(startupMs) {
    maxRssMb = Math.max(maxRssMb, await sampleRssMb());
    const metrics = {
        schemaVersion: 1,
        endpoint,
        startupMs,
        openSamples: openDurationsMs.length,
        openDurationsMs,
        openP95Ms: percentile95(openDurationsMs),
        rssMb: Math.ceil(maxRssMb),
        node: process.version
    };
    await writeFile(metricsFile, `${JSON.stringify(metrics, null, 2)}\n`);
}

const deadline = Date.now() + timeoutMs;
let lastError;

try {
    while (Date.now() < deadline) {
        if (child.exitCode !== null) {
            throw new Error(`Theia exited before readiness with code ${child.exitCode}\n${output}`);
        }
        try {
            maxRssMb = Math.max(maxRssMb, await sampleRssMb());
            await fetchHtml();
            for (let index = 1; index < openSamples; index += 1) {
                await fetchHtml();
            }
            const startupMs = Date.now() - startedAt;
            await writeMetrics(startupMs);
            console.log(`HTTP smoke passed: ${endpoint} startup=${startupMs}ms openP95=${percentile95(openDurationsMs)}ms rss=${Math.ceil(maxRssMb)}MB`);
            process.exitCode = 0;
            break;
        } catch (error) {
            lastError = error;
        }
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    if (Date.now() >= deadline) {
        throw new Error(`Timed out waiting for ${endpoint}: ${lastError?.message ?? 'no response'}\n${output}`);
    }
} finally {
    await stopProcessTree();
}
