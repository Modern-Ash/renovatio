import { spawn } from 'node:child_process';
import process from 'node:process';

const endpoint = process.env.RENOVATIO_SMOKE_URL ?? 'http://127.0.0.1:3000/';
const timeoutMs = Number(process.env.RENOVATIO_SMOKE_TIMEOUT_MS ?? 90000);
const runsInOwnProcessGroup = process.platform !== 'win32';
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

const deadline = Date.now() + timeoutMs;
let lastError;

try {
    while (Date.now() < deadline) {
        if (child.exitCode !== null) {
            throw new Error(`Theia exited before readiness with code ${child.exitCode}\n${output}`);
        }
        try {
            const response = await fetch(endpoint);
            if (response.ok) {
                const body = await response.text();
                if (!body.toLowerCase().includes('<html')) {
                    throw new Error('Root response was not HTML');
                }
                console.log(`HTTP smoke passed: ${response.status} ${endpoint}`);
                process.exitCode = 0;
                break;
            }
            lastError = new Error(`Unexpected HTTP status ${response.status}`);
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
