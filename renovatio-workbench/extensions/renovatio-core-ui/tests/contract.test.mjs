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

test('registers five activity areas with command palette keybindings', () => {
    for (const area of ['project', 'analysis', 'architecture', 'ai', 'equivalence']) {
        assert.match(contribution, new RegExp(`renovatio\\.shell\\.${area}`));
    }
    assert.match(contribution, /registerKeybindings/);
    assert.match(contribution, /ctrlcmd\+alt\+1/);
    assert.match(contribution, /ctrlcmd\+alt\+5/);
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
