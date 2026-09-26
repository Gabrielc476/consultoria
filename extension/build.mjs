import * as esbuild from 'esbuild';
import { cpSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const common = {
  bundle: true,
  format: 'iife',
  platform: 'browser',
  target: 'chrome120',
  logLevel: 'info'
};

await esbuild.build({
  ...common,
  entryPoints: ['src/background/service-worker.ts'],
  outfile: 'dist/background/service-worker.js'
});

await esbuild.build({
  ...common,
  entryPoints: ['src/content/content-script.ts'],
  outfile: 'dist/content/content-script.js'
});

await esbuild.build({
  ...common,
  entryPoints: ['src/popup/popup.ts'],
  outfile: 'dist/popup/popup.js'
});

mkdirSync('dist/popup', { recursive: true });
cpSync('src/popup/popup.html', 'dist/popup/popup.html');

const manifest = JSON.parse(readFileSync('manifest.json', 'utf8'));
writeFileSync('dist/manifest.json', JSON.stringify(manifest, null, 2));
