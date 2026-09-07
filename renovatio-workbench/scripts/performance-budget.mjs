import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const root = new URL('..', import.meta.url);
const hardening = JSON.parse(await readFile(new URL('config/release-hardening.json', root), 'utf8'));
const metricsFile = process.env.RENOVATIO_SMOKE_METRICS_FILE ?? '.theia-smoke-metrics.json';
const failures = [];

function assertBudget(condition, message) {
    if (!condition) {
        failures.push(message);
    }
}

async function walk(directory) {
    const entries = await readdir(directory, { withFileTypes: true });
    const files = [];
    for (const entry of entries) {
        const resolved = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            files.push(...await walk(resolved));
        } else {
            files.push(resolved);
        }
    }
    return files;
}

let measured = false;
try {
    const metrics = JSON.parse(await readFile(new URL(metricsFile, root), 'utf8'));
    measured = true;
    assertBudget(metrics.startupMs <= hardening.performanceBudgets.smokeStartupMs,
        `Measured smoke startup ${metrics.startupMs}ms exceeds budget ${hardening.performanceBudgets.smokeStartupMs}ms`);
    assertBudget(metrics.openP95Ms <= hardening.performanceBudgets.openP95Ms,
        `Measured open p95 ${metrics.openP95Ms}ms exceeds budget ${hardening.performanceBudgets.openP95Ms}ms`);
    assertBudget(metrics.rssMb > 0, 'Measured RSS must be greater than zero');
    assertBudget(metrics.rssMb <= hardening.performanceBudgets.rssMb,
        `Measured RSS ${metrics.rssMb}MB exceeds budget ${hardening.performanceBudgets.rssMb}MB`);
    assertBudget(metrics.openSamples >= 3, 'At least three HTTP open samples are required for p95');
} catch (error) {
    if (error.code === 'ENOENT' && process.env.RENOVATIO_PERFORMANCE_ACCEPT_STATIC === '1') {
        measured = false;
    } else if (error.code === 'ENOENT') {
        failures.push(`Missing smoke metrics file ${metricsFile}; run npm run smoke before npm run performance:budget`);
    } else {
        throw error;
    }
}

const frontendCandidates = [
    new URL('applications/browser/lib/frontend', root),
    new URL('browser-app/lib/frontend', root)
];

let checkedBundle = false;
for (const candidate of frontendCandidates) {
    try {
        const files = await walk(candidate.pathname);
        const jsFiles = files.filter(file => file.endsWith('.js'));
        if (jsFiles.length === 0) {
            continue;
        }
        checkedBundle = true;
        let totalBytes = 0;
        for (const file of jsFiles) {
            totalBytes += (await stat(file)).size;
        }
        const totalKb = Math.ceil(totalBytes / 1024);
        assertBudget(totalKb <= hardening.performanceBudgets.frontendBundleKb,
            `Frontend JS bundle ${totalKb}KiB exceeds budget ${hardening.performanceBudgets.frontendBundleKb}KiB`);
    } catch (error) {
        if (error.code !== 'ENOENT') {
            throw error;
        }
    }
}

assertBudget(hardening.performanceBudgets.openP95Ms <= 5000, 'Open P95 budget must remain at or below 5000ms for continuous use');
assertBudget(checkedBundle, 'No built frontend JS bundle found; run npm run build before performance:budget');

if (failures.length > 0) {
    console.error('Performance budget check failed:');
    for (const failure of failures) {
        console.error(`- ${failure}`);
    }
    process.exit(1);
}

console.log(`Performance budgets passed (${measured ? 'measured' : 'static-only'}): open p95 <= ${hardening.performanceBudgets.openP95Ms}ms, smoke <= ${hardening.performanceBudgets.smokeStartupMs}ms, RSS <= ${hardening.performanceBudgets.rssMb}MB, frontend JS <= ${hardening.performanceBudgets.frontendBundleKb}KiB.`);
