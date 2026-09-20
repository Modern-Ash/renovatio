const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const {
  applyDiagramEvent,
  formatDiagramDocument,
  parseDiagramDocument
} = require('../dist-test/src/model');

const fixtureRoot = path.join(__dirname, 'fixtures', 'workspace-basic', '.renovatio', 'diagrams');

test('domain diagram fixture parses into nodes and relations', () => {
  const uri = path.join(fixtureRoot, 'sample.renovatio-domain.json');
  const parsed = parseDiagramDocument(fs.readFileSync(uri, 'utf8'), uri);
  assert.equal(parsed.kind, 'domain');
  assert.equal(parsed.model.nodes.length, 2);
  assert.equal(parsed.model.edges.length, 1);
});

test('domain diagram layout changes are persisted in raw layout', () => {
  const uri = path.join(fixtureRoot, 'sample.renovatio-domain.json');
  const parsed = parseDiagramDocument(fs.readFileSync(uri, 'utf8'), uri);
  const next = applyDiagramEvent(parsed, {
    type: 'nodeMoved',
    id: 'usecase:card-demo',
    x: 240,
    y: 180
  });
  assert.deepEqual(next.raw.layout['usecase:card-demo'], { x: 240, y: 180 });
  assert.match(formatDiagramDocument(next.raw), /"usecase:card-demo"/);
});

test('architecture diagram fixture parses and preserves profile layout changes', () => {
  const uri = path.join(fixtureRoot, 'sample.renovatio-arch.json');
  const parsed = parseDiagramDocument(fs.readFileSync(uri, 'utf8'), uri);
  assert.equal(parsed.kind, 'architecture');
  assert.ok(parsed.model.nodes.some(node => node.id === 'architecture-layer:service'));
  assert.ok(parsed.model.edges.some(edge =>
    edge.id === 'architecture-rule:controller:service:0' &&
    edge.source === 'architecture-layer:controller' &&
    edge.target === 'architecture-layer:service'
  ));
  const next = applyDiagramEvent(parsed, {
    type: 'layoutChanged',
    positions: {
      'architecture-layer:service': { x: 420, y: 120 }
    }
  });
  assert.deepEqual(next.raw.profile.layout['architecture-layer:service'], { x: 420, y: 120 });
});
