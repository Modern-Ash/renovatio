import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contribution = await readFile(new URL('../src/browser/renovatio-workbench-contribution.ts', import.meta.url), 'utf8');
const widget = await readFile(new URL('../src/browser/renovatio-workbench-widget.tsx', import.meta.url), 'utf8');
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
