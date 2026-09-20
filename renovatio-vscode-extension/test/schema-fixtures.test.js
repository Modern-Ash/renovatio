const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const Ajv2020 = require('ajv/dist/2020');

const root = path.join(__dirname, '..');
const fixtures = path.join(__dirname, 'fixtures', 'workspace-basic');

function readJson(...parts) {
  return JSON.parse(fs.readFileSync(path.join(...parts), 'utf8'));
}

test('workspace, migration map and diagram fixtures validate against contributed schemas', () => {
  const ajv = new Ajv2020({ strict: false, validateFormats: false });
  const cases = [
    ['schemas/workspace-manifest.schema.json', ['.renovatio', 'workspace.renovatio.json']],
    ['schemas/migration-map.renovatio.schema.json', ['.renovatio', 'migration-map.renovatio.json']],
    ['schemas/domain-model.schema.json', ['.renovatio', 'diagrams', 'sample.renovatio-domain.json']],
    ['schemas/architecture.schema.json', ['.renovatio', 'diagrams', 'sample.renovatio-arch.json']]
  ];

  for (const [schemaPath, fixturePath] of cases) {
    const validate = ajv.compile(readJson(root, schemaPath));
    const valid = validate(readJson(fixtures, ...fixturePath));
    assert.equal(valid, true, `${schemaPath}: ${JSON.stringify(validate.errors)}`);
  }
});
