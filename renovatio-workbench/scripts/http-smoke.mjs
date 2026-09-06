import { spawn } from 'node:child_process';
import process from 'node:process';

const endpoint = process.env.RENOVATIO_SMOKE_URL ?? 'http://127.0.0.1:3000/';
const timeoutMs = Number(process.env.RENOVATIO_SMOKE_TIMEOUT_MS ?? 90000);
const child = spawn('npm', ['run', 'start'], {
    cwd: new URL('..', import.meta.url),
    env: {
        ...process.env,
        RENOVATIO_TELEMETRY_ENABLED: process.env.RENOVATIO_TELEMETRY_ENABLED ?? 'false'
    },
    stdio: ['ignore', 'pipe', 'pipe']
});

let output = '';
child.stdout.on('data', chunk => { output = `${output}${chunk}`.slice(-12000); });
child.stderr.on('data', chunk => { output = `${output}${chunk}`.slice(-12000); });

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
    child.kill('SIGTERM');
}
