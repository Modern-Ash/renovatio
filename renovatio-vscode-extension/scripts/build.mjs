import { existsSync, rmSync } from 'node:fs';
import { build, context } from 'esbuild';

const watch = process.argv.includes('--watch');

if (!watch && existsSync('dist')) {
  rmSync('dist', { recursive: true, force: true });
}

const extensionConfig = {
  entryPoints: ['src/extension.ts'],
  bundle: true,
  external: ['vscode'],
  format: 'cjs',
  platform: 'node',
  sourcemap: true,
  outfile: 'dist/extension.js'
};

const webviewConfig = {
  entryPoints: ['src/webview.tsx'],
  bundle: true,
  format: 'iife',
  platform: 'browser',
  sourcemap: true,
  outfile: 'dist/webview.js',
  loader: {
    '.css': 'css'
  }
};

if (watch) {
  const extension = await context(extensionConfig);
  const webview = await context(webviewConfig);
  await extension.watch();
  await webview.watch();
  console.log('Watching Renovatio VS Code extension sources...');
} else {
  await build(extensionConfig);
  await build(webviewConfig);
}
