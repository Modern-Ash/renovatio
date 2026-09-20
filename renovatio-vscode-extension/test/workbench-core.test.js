const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const {
  classifyHashState,
  entriesForMigrationPath,
  formatMigrationMap,
  migrationHoverMarkdown,
  migrationRangeContains,
  sameHash,
  sha256Text,
  validateMigrationMapArtifact,
  workspaceRelativePath
} = require('../dist-test/src/workbenchCore');

const fixtureRoot = path.join(__dirname, 'fixtures', 'workspace-basic');
const migrationMap = JSON.parse(fs.readFileSync(path.join(fixtureRoot, '.renovatio', 'migration-map.renovatio.json'), 'utf8'));

test('migration map fixture validates and indexes source and target paths', () => {
  assert.deepEqual(validateMigrationMapArtifact(migrationMap), []);
  assert.equal(entriesForMigrationPath(migrationMap, 'src/mainframe/CARDDEMO.cbl', 'source').length, 1);
  assert.equal(entriesForMigrationPath(migrationMap, 'generated/java/src/main/java/com/example/CardDemoService.java', 'target').length, 1);
});

test('migration map formatter sorts entries by id', () => {
  const text = formatMigrationMap({
    ...migrationMap,
    entries: [
      { ...migrationMap.entries[0], id: 'program:ZLAST' },
      { ...migrationMap.entries[0], id: 'program:AFIRST' }
    ]
  });
  assert.ok(text.indexOf('program:AFIRST') < text.indexOf('program:ZLAST'));
  assert.ok(text.endsWith('\n'));
});

test('hover markdown is concise and includes commands and stale review state', () => {
  const markdown = migrationHoverMarkdown(migrationMap.entries[0], 'source');
  assert.match(markdown, /Renovatio migration/);
  assert.match(markdown, /Status: `needs-review`/);
  assert.match(markdown, /Open target/);
  assert.match(markdown, /Warnings: needs review/);
});

test('hash and stale-state helpers normalize sha256 prefixes', async () => {
  assert.equal(sameHash('sha256:ABC', 'abc'), true);
  assert.equal(classifyHashState('sha256:old', 'sha256:new', 'source'), 'stale-source');
  assert.equal(classifyHashState('sha256:new', 'new', 'target'), 'clean');
  assert.equal(await sha256Text('Renovatio'), 'sha256:46c907c5f0f4afad39e7eeea0a097552c568ee42a27b1b2f2e5994d982aaeb04');
});

test('path and range helpers handle workspace-relative editor cases', () => {
  assert.equal(
    workspaceRelativePath('/repo/workspace', '/repo/workspace/src/mainframe/CARDDEMO.cbl'),
    'src/mainframe/CARDDEMO.cbl'
  );
  assert.equal(migrationRangeContains(migrationMap.entries[0].source.range, 0, 0), true);
  assert.equal(migrationRangeContains(migrationMap.entries[0].source.range, 20, 0), false);
});
