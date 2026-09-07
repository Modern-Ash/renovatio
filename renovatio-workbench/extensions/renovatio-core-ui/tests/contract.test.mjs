import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contribution = await readFile(new URL('../src/browser/renovatio-workbench-contribution.ts', import.meta.url), 'utf8');
const widget = await readFile(new URL('../src/browser/renovatio-workbench-widget.tsx', import.meta.url), 'utf8');
const shell = await readFile(new URL('../src/browser/renovatio-shell-widget.tsx', import.meta.url), 'utf8');
const styles = await readFile(new URL('../src/browser/style/renovatio-workbench.css', import.meta.url), 'utf8');

test('registers the stable command and menu contribution', () => {
    assert.match(contribution, /renovatio\.workbench\.open/);
    assert.match(contribution, /registerCommand/);
    assert.match(contribution, /registerMenuAction/);
});

test('renders a named accessible view and environment contract', () => {
    assert.match(widget, /aria-labelledby='renovatio-workbench-heading'/);
    assert.match(widget, /aria-live='polite'/);
    assert.match(widget, /RENOVATIO_BACKEND_URL/);
    assert.match(widget, /RENOVATIO_AUTH_MODE/);
});

test('provides focus, responsive, and reduced-motion styles', () => {
    assert.match(styles, /focus-visible/);
    assert.match(styles, /@media \(max-width: 860px\)/);
    assert.match(styles, /prefers-reduced-motion: no-preference/);
});

test('registers six activity areas with command palette keybindings', () => {
    for (const area of ['project', 'analysis', 'domain', 'architecture', 'ai', 'equivalence']) {
        assert.match(contribution, new RegExp(`renovatio\\.shell\\.${area}`));
    }
    assert.match(contribution, /registerKeybindings/);
    assert.match(contribution, /ctrlcmd\+alt\+1/);
    assert.match(contribution, /ctrlcmd\+alt\+6/);
});

test('provides accessible project navigation over all required asset classes', () => {
    for (const group of ['COBOL sources', 'Copybooks', 'JCL', 'Models', 'Runs', 'Evidence']) {
        assert.match(shell, new RegExp(group));
    }
    assert.match(shell, /aria-label='Renovatio activity areas'/);
    assert.match(shell, /aria-label='Project assets'/);
    assert.match(shell, /aria-current=/);
});

test('persists the active project and area while exposing failure states', () => {
    assert.match(shell, /renovatio\.workbench\.active-area/);
    assert.match(shell, /renovatio\.workbench\.selected-project/);
    assert.match(shell, /window\.localStorage\.setItem/);
    assert.match(shell, /'permission-denied'/);
    assert.match(shell, /'error'/);
});

test('keeps dashboard continuity at a configuration boundary', () => {
    assert.match(shell, /RENOVATIO_DASHBOARD_URL/);
    assert.match(shell, /Open administrative dashboard/);
    assert.doesNotMatch(shell, /renovatio-ui\/src/);
    assert.doesNotMatch(shell, /Wizard/);
});

test('loads live projects through the Spring Boot adapter with explicit failure states', () => {
    assert.match(shell, /\/api\/workbench\/projects/);
    assert.match(shell, /\/workbench\/assets/);
    assert.match(shell, /encodeURIComponent\(this\.selectedProject\)/);
    assert.match(shell, /response\.status === 401 \|\| response\.status === 403/);
    assert.match(shell, /this\.shellState = 'empty'/);
});

test('reads assets and limits saves to development-approved generated targets', () => {
    assert.match(shell, /assetContentState/);
    assert.match(shell, /method: 'PUT'/);
    assert.match(shell, /selectedAssetWritable/);
    assert.match(shell, /Legacy source and evidence are read-only/);
    assert.match(shell, /Temporary development mode/);
});

test('restores only project-scoped non-sensitive context through the adapter', () => {
    assert.match(shell, /\/workbench\/context/);
    assert.match(shell, /activeArea: this\.activeArea/);
    assert.match(shell, /selectedAssetId: this\.selectedAssetId/);
    assert.match(shell, /loadContext\(\)/);
    assert.match(shell, /persistContext\(\)/);
});

test('shows read-only inventory and persisted run summaries in Analysis', () => {
    assert.match(shell, /\/workbench\/analysis/);
    assert.match(shell, /analysisState/);
    assert.match(shell, /Analysis data is unavailable/);
    assert.match(shell, /No inventory or persisted runs/);
    assert.doesNotMatch(shell, /method: 'POST'.*workbench\/analysis/);
});

test('exposes a read-only Source Explorer over the governed adapter (issue #178)', () => {
    assert.match(shell, /\/workbench\/source-explorer/);
    assert.match(shell, /sourceExplorerState/);
    for (const state of ["'loading'", "'ready'", "'empty'", "'permission-denied'", "'error'"]) {
        assert.match(shell, new RegExp(`sourceExplorerState = ${state}`));
    }
    // navigable tree + outline + search + Problems + IR linkage
    assert.match(shell, /aria-label='Source files'/);
    assert.match(shell, /aria-label=\{`Outline of \$\{file\.name\}`\}/);
    assert.match(shell, /aria-label='Search symbols and references'/);
    assert.match(shell, /aria-label='Problems'/);
    assert.match(shell, /symbol\.irCoordinate/);
    assert.match(shell, /aria-current=\{this\.selectedSourceFileId === entry\.id \? 'true' : undefined\}/);
    // file metadata surfaced
    assert.match(shell, /entry\.hash/);
    assert.match(shell, /entry\.encoding/);
    assert.match(shell, /entry\.analysisStatus/);
    // strictly read-only: never mutates the endpoint
    assert.doesNotMatch(shell, /source-explorer`, \{\s*method/);
    assert.doesNotMatch(shell, /method: '(POST|PUT|DELETE|PATCH)'[^;]*source-explorer/);
});

test('provides the governed DomainModel editor and immutable version boundary (issue #179)', () => {
    assert.match(shell, /\/workbench\/domain-model/);
    assert.match(shell, /method: 'PUT'/);
    assert.match(shell, /expectedRevision: this\.domain\.revision/);
    assert.match(shell, /\/domain-model\/versions/);
    assert.match(shell, /\/domain-model\/compare/);
    assert.match(shell, /\/domain-model\/suggestions/);
    for (const state of ['loading', 'ready', 'empty', 'permission-denied', 'conflict', 'error']) {
        assert.match(shell, new RegExp(`'${state}'`));
    }
    assert.match(shell, /aria-label='Business DomainModel editor'/);
    assert.match(shell, /aria-label='Selected domain element editor'/);
    assert.match(shell, /aria-label='Domain provenance and history'/);
    assert.match(shell, /aria-live='polite'/);
    assert.match(shell, /DOMAIN_KINDS/);
    assert.match(shell, /CARDINALITIES/);
    assert.match(shell, /Stable ids and evidence references remain immutable/);
});

test('links source evidence and symbols in both directions without triggering analysis', () => {
    assert.match(shell, /navigateToSource\(evidence\.sourceRef\)/);
    assert.match(shell, /navigateToDomainFromSource\(file, symbol\)/);
    assert.match(shell, /aria-label='Related DomainModel elements'/);
    assert.match(shell, /sourceRef\.split\('#'\)/);
    assert.doesNotMatch(shell, /navigateToDomainFromSource[\s\S]{0,700}loadAnalysis\(/);
});

test('keeps DomainModel work neutral and outside generation or architecture mutation', () => {
    assert.doesNotMatch(shell, /domain-model[^\n]+method: '(DELETE|PATCH)'/);
    assert.doesNotMatch(shell, /saveDomainModel[\s\S]{0,1200}(generate|architecture)/i);
    assert.match(styles, /renovatio-domain-grid/);
    assert.match(styles, /state-conflict/);
});

test('provides the editable Architecture Canvas with profile history (issue #180)', () => {
    assert.match(shell, /\/workbench\/architecture\/canvas/);
    assert.match(shell, /saveArchitectureProfile/);
    assert.match(shell, /expectedRevision: this\.architecture\.revision/);
    assert.match(shell, /\/architecture\/canvas\/versions/);
    assert.match(shell, /\/architecture\/canvas\/compare/);
    for (const style of ['LAYERED_MVC', 'HEXAGONAL', 'CLEAN', 'LAYERED', 'TRANSACTION_SCRIPT']) {
        assert.match(shell, new RegExp(style));
    }
    for (const state of ['loading', 'ready', 'empty', 'permission-denied', 'conflict', 'error']) {
        assert.match(shell, new RegExp(`'${state}'`));
    }
    assert.match(shell, /aria-label='Architecture Canvas editor'/);
    assert.match(shell, /aria-label='Artifact and package manifest preview'/);
    assert.match(shell, /illegal dependencies visible before generate/);
    assert.match(styles, /renovatio-architecture-grid/);
    assert.match(styles, /renovatio-segmented/);
});

test('keeps Architecture Canvas outside DomainModel and generation execution', () => {
    assert.doesNotMatch(shell, /saveArchitectureProfile[\s\S]{0,1200}domain-model/i);
    assert.doesNotMatch(shell, /saveArchitectureProfile[\s\S]{0,1200}generate/i);
    assert.doesNotMatch(shell, /architecture\/canvas[^\n]+method: '(DELETE|PATCH)'/);
});
