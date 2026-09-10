import { copyFile, mkdir } from 'node:fs/promises';

const source = new URL('../src/browser/style/renovatio-workbench.css', import.meta.url);
const targetDirectory = new URL('../lib/browser/style/', import.meta.url);
const target = new URL('renovatio-workbench.css', targetDirectory);

await mkdir(targetDirectory, { recursive: true });
await copyFile(source, target);
