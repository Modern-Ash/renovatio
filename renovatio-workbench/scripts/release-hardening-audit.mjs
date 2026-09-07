import { createHash } from 'node:crypto';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const root = new URL('..', import.meta.url);
const repoRoot = new URL('..', root);
const failures = [];

async function readText(relativePath, base = root) {
    return readFile(new URL(relativePath, base), 'utf8');
}

async function readJson(relativePath, base = root) {
    return JSON.parse(await readText(relativePath, base));
}

function assertCondition(condition, message) {
    if (!condition) {
        failures.push(message);
    }
}

function assertIncludes(text, expected, message) {
    assertCondition(text.includes(expected), message);
}

async function sha256(relativePath) {
    const bytes = await readFile(new URL(relativePath, root));
    return createHash('sha256').update(bytes).digest('hex');
}

const hardening = await readJson('config/release-hardening.json');
const workspacePackage = await readJson('package.json');
const browserPackage = await readJson('applications/browser/package.json');
const dockerfile = await readText('Dockerfile');
const workflow = await readText('.github/workflows/theia-platform-spike.yml', repoRoot);
const readme = await readText('README.md');
const securityRunbook = await readText('docs/security-and-operations-runbook.md');
const handoffGuide = await readText('docs/llm-handoff-guide.md');
const pilotReport = await readText('docs/demo-pilot-report.md');
const contribution = await readText('extensions/renovatio-core-ui/src/browser/renovatio-workbench-contribution.ts');

assertCondition(workspacePackage.engines.node === hardening.runtime.node, 'package.json Node engine must match release hardening config');
assertCondition(workspacePackage.packageManager === `npm@${hardening.runtime.npmMajor}.19.0`, 'packageManager must stay pinned to the approved npm line');
assertCondition(dockerfile.includes(`FROM ${hardening.distribution.dockerBaseImage} AS build`), 'Docker build image must match release hardening config');
assertCondition(dockerfile.includes(`FROM ${hardening.distribution.dockerBaseImage} AS runtime`), 'Docker runtime image must match release hardening config');
assertIncludes(dockerfile, 'npm prune --omit=dev', 'Docker build must prune development dependencies before runtime copy');
assertIncludes(dockerfile, `USER ${hardening.distribution.runtimeUser}`, 'Docker runtime must not run as root');
assertIncludes(dockerfile, `RENOVATIO_TELEMETRY_ENABLED=${hardening.security.telemetryEnvDefault}`, 'Docker telemetry default must be opt-out/off');
assertIncludes(dockerfile, `RENOVATIO_WORKSPACE_ROOT=${hardening.distribution.workspaceRoot}`, 'Docker workspace root must be explicit');

for (const [name, version] of Object.entries(browserPackage.dependencies)) {
    if (name.startsWith('@theia/')) {
        assertCondition(version === hardening.runtime.theia, `${name} must be pinned to Theia ${hardening.runtime.theia}`);
    }
}
assertCondition(!Object.keys(browserPackage.dependencies).some(name => name.includes('vsx') || name.includes('plugin-ext')),
    'Open VSX/plugin runtime dependencies must remain disabled until security unblock');
assertCondition(browserPackage.theia.frontend.config.preferences['telemetry.telemetryLevel'] === hardening.security.telemetryDefault,
    'Theia telemetry preference must default to off');
assertCondition(browserPackage.theia.frontend.config.preferences['security.workspace.trust.enabled'] === hardening.security.workspaceTrust,
    'Workspace trust must stay enabled');
assertCondition(browserPackage.theia.frontend.config.preferences['files.enableTrash'] === hardening.security.trashEnabled,
    'Trash behavior must match workspace safety policy');

for (const command of hardening.security.allowedCommands) {
    assertIncludes(contribution, command, `Allowed command ${command} must be registered in the contribution`);
}
const registeredCommandIds = [...contribution.matchAll(/\bid:\s*'([^']+)'/g)]
    .map(match => match[1])
    .filter(command => command.startsWith('renovatio.'));
const allowedCommands = new Set(hardening.security.allowedCommands);
for (const command of registeredCommandIds) {
    assertCondition(allowedCommands.has(command), `Registered command ${command} is missing from the release allowlist`);
}
for (const command of hardening.security.allowedCommands) {
    assertCondition(registeredCommandIds.includes(command), `Allowlisted command ${command} is not registered`);
}

for (const scriptName of ['hardening:audit', 'performance:budget', 'pilot:demo']) {
    assertCondition(Boolean(workspacePackage.scripts[scriptName]), `package.json must expose ${scriptName}`);
    assertIncludes(workflow, `npm run ${scriptName}`, `CI must run ${scriptName}`);
}
assertIncludes(workflow, 'mvn -q -pl renovatio-api -am', 'CI must run API contract regressions');
assertCondition(!workflow.includes('paths:'), 'Release gate workflow must not use path filters; backend contract changes must always trigger it');
assertIncludes(workflow, 'npm test', 'CI must run UI/extension contract tests');
assertIncludes(workflow, 'npm run smoke', 'CI must run the web E2E smoke check');
assertIncludes(workflow, 'docker build --target build', 'CI must verify the reproducible Docker build stage');

for (const docNeedle of ['Install from zero', 'Security audit gate', 'Performance budgets', 'Independent LLM continuation guide']) {
    assertIncludes(readme, docNeedle, `README must document ${docNeedle}`);
}
for (const docNeedle of ['Workspace isolation', 'Command allowlist', 'MCP allowlist', 'CSP gateway policy', 'Telemetry opt-in']) {
    assertIncludes(securityRunbook, docNeedle, `Security runbook must document ${docNeedle}`);
}
for (const docNeedle of ['Context to preserve', 'Verification commands', 'Known release debt']) {
    assertIncludes(handoffGuide, docNeedle, `LLM handoff guide must document ${docNeedle}`);
}
assertIncludes(pilotReport, hardening.pilot.name, 'Pilot report must name the configured demo pilot');

for (const fixture of hardening.pilot.fixtures) {
    await stat(new URL(fixture.path, root));
    const actualHash = await sha256(fixture.path);
    assertCondition(actualHash === fixture.sha256, `${fixture.path} hash drifted: expected ${fixture.sha256}, got ${actualHash}`);
}

if (failures.length > 0) {
    console.error('Release hardening audit failed:');
    for (const failure of failures) {
        console.error(`- ${failure}`);
    }
    process.exit(1);
}

console.log(`Release hardening audit passed: ${hardening.security.allowedCommands.length} commands, ${hardening.pilot.fixtures.length} pilot fixtures, Node ${hardening.runtime.node}, Theia ${hardening.runtime.theia}.`);
