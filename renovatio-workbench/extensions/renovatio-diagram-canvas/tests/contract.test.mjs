import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const protocol = await readFile(new URL('../src/common/diagram-protocol.ts', import.meta.url), 'utf8');
const canvas = await readFile(new URL('../src/browser/diagram-canvas.tsx', import.meta.url), 'utf8');
const index = await readFile(new URL('../src/browser/index.ts', import.meta.url), 'utf8');
const pkg = JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8'));

test('defines the view-model contract independent of the real domain model', () => {
    assert.match(protocol, /export interface DiagramNodeVM/);
    assert.match(protocol, /export interface DiagramEdgeVM/);
    assert.match(protocol, /export interface DiagramModel/);
    assert.match(protocol, /export type DiagramEvent/);
    for (const event of ["'nodeMoved'", "'nodeSelected'", "'edgeCreated'", "'nodesPruned'"]) {
        assert.match(protocol, new RegExp(event));
    }
    // The canvas package must stay ignorant of Renovatio's real domain types —
    // those mappers live in @renovatio/core-ui (issues #265/#267), not here.
    // (Doc comments may *mention* those type names for context; only an actual
    // reference — an import or a type annotation — would be a boundary leak.)
    assert.doesNotMatch(protocol, /:\s*(DomainModel|DomainNode|ArchitectureProfileDraft|ArchitectureCanvasNode)\b/);
    assert.doesNotMatch(protocol, /^import .*(DomainModel|ArchitectureProfileDraft)/m);
});

test('the canvas component never fetches or knows about a backend URL', () => {
    assert.doesNotMatch(canvas, /fetch\(/);
    assert.doesNotMatch(canvas, /backendUrl/i);
    assert.doesNotMatch(canvas, /DomainModel|ArchitectureProfileDraft/);
});

test('DiagramCanvas renders a React Flow surface and reports every interaction upward', () => {
    assert.match(canvas, /export function DiagramCanvas/);
    assert.match(canvas, /<ReactFlow/);
    assert.match(canvas, /onNodesChange=\{handleNodesChange\}/);
    assert.match(canvas, /onConnect=\{handleConnect\}/);
    assert.match(canvas, /onPaneClick=\{handlePaneClick\}/);
    // Every event kind from the protocol must actually be emitted somewhere.
    for (const event of ["type: 'nodeMoved'", "type: 'nodeSelected'", "type: 'edgeCreated'"]) {
        assert.match(canvas, new RegExp(event.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
    }
    // Host-extensibility hooks required by #265 (UML), #266 (DER) and #267 (Architecture)
    // to register their own node renderers without forking this component.
    assert.match(canvas, /nodeTypes\?:/);
    assert.match(canvas, /nodeTypeFor\?:/);
});

test('the public entry point re-exports the component and the protocol types', () => {
    assert.match(index, /export \{ DiagramCanvas \}/);
    assert.match(index, /DiagramModel, DiagramNodeVM, DiagramEdgeVM, DiagramEvent/);
});

test('package metadata declares react as a peer dependency, not a bundled one', () => {
    assert.ok(pkg.peerDependencies?.react, 'react must be a peer dependency');
    assert.ok(pkg.peerDependencies?.['react-dom'], 'react-dom must be a peer dependency');
    assert.ok(pkg.dependencies?.['@xyflow/react'], '@xyflow/react must be declared as a real dependency');
    assert.equal(pkg.dependencies?.['@theia/core'], undefined, 'this package must not depend on @theia/core');
});
