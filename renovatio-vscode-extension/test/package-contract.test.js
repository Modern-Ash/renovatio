const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const pkg = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'package.json'), 'utf8'));

test('extension exposes npm test and core evaluator/workbench commands', () => {
  assert.equal(pkg.scripts.test, 'npm run test:compile && node --test test/*.test.js');
  const commands = new Set(pkg.contributes.commands.map(command => command.command));
  for (const command of [
    'renovatio.openEvaluatorGuide',
    'renovatio.installDemoWorkspace',
    'renovatio.openMigrationMap',
    'renovatio.previewMigrationDiff',
    'renovatio.syncStatus',
    'renovatio.exportEvidenceBundle'
  ]) {
    assert.equal(commands.has(command), true, `${command} should be contributed`);
    assert.equal(pkg.activationEvents.includes(`onCommand:${command}`), true, `${command} should activate extension`);
  }
});

test('fixture workspace contains source, target and evidence handoff files', () => {
  const fixtureRoot = path.join(__dirname, 'fixtures', 'workspace-basic');
  for (const relative of [
    '.renovatio/workspace.renovatio.json',
    '.renovatio/migration-map.renovatio.json',
    '.renovatio/diagrams/sample.renovatio-domain.json',
    '.renovatio/diagrams/sample.renovatio-arch.json',
    'src/mainframe/CARDDEMO.cbl',
    'src/mainframe/CARDJOB.jcl',
    'copybooks/CARDREC.cpy',
    'generated/java/src/main/java/com/example/CardDemoService.java',
    '.renovatio/evidence/evaluator-summary.md',
    '.renovatio/evidence-bundles/bundle-001/manifest.json'
  ]) {
    assert.equal(fs.existsSync(path.join(fixtureRoot, relative)), true, relative);
  }
});

test('evidence bundle manifest fixture links core workbench artifacts', () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(
    __dirname,
    'fixtures',
    'workspace-basic',
    '.renovatio',
    'evidence-bundles',
    'bundle-001',
    'manifest.json'
  ), 'utf8'));
  assert.equal(manifest.version, '1');
  assert.equal(manifest.summary.status, 'needs-review');
  assert.equal(manifest.artifacts.workspace, '.renovatio/workspace.renovatio.json');
  assert.equal(manifest.artifacts.migrationMap, '.renovatio/migration-map.renovatio.json');
  assert.equal(manifest.llm.promptProfile, 'renovatio-cobol-reverse-engineering-v1');
});
