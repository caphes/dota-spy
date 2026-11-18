import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { access, copyFile, cp, mkdir, readFile, readdir, realpath, rm, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { iconPng } from './icon.mjs';

const root = await realpath(fileURLToPath(new URL('.', import.meta.url)));
const out = resolve(root, 'dist');
const art = resolve(process.argv[2] || fileURLToPath(new URL('../app/build/generated/cardAssets/', import.meta.url)));
const catalog = JSON.parse(await readFile(new URL('../app/src/main/assets/deck.json', import.meta.url), 'utf8'));

const exists = path => access(path).then(() => true, error => {
  if (error.code === 'ENOENT') return false;
  throw error;
});

assert.equal(catalog.filter(card => card.category === 'hero').length, 127);
assert.equal(catalog.filter(card => card.category === 'item').length, 50);
assert.equal(new Set(catalog.map(card => card.id)).size, catalog.length);

const cards = await Promise.all(catalog.map(async card => {
  const { id, category, name, description } = card;
  assert.match(id, /^[a-z0-9_]+$/);
  assert.ok(['hero', 'item'].includes(category) && name && description);
  assert.equal(card.image, `./assets/cards/${category}/${id}.png`);
  if (card.localImage) assert.equal(card.localImage, './assets/cards/hero/pudge_photo.jpg');
  const local = card.localImage && await exists(join(art, card.localImage.slice('./assets/'.length)));
  const image = local ? card.localImage : card.image;
  assert.ok(await exists(join(art, image.slice('./assets/'.length))), `Нет изображения ${image}`);
  return { id, category, name, description, image, ...(local ? { portrait: true } : {}) };
}));

assert.equal(dirname(out), root);
await rm(out, { recursive: true, force: true });
await cp(join(root, 'src'), out, { recursive: true });
await writeFile(join(out, 'deck.json'), JSON.stringify(cards, null, 2) + '\n');
await Promise.all([192, 512].map(size => writeFile(join(out, `icon-${size}.png`), iconPng(size))));
await Promise.all(cards.map(async ({ image }) => {
  const dest = resolve(out, image);
  await mkdir(dirname(dest), { recursive: true });
  await copyFile(join(art, image.slice('./assets/'.length)), dest);
}));

const worker = await readFile(join(out, 'sw.js'), 'utf8');
const shell = (await readdir(out, { withFileTypes: true }))
  .filter(entry => entry.isFile() && entry.name !== 'sw.js')
  .map(entry => './' + entry.name);
const files = [...shell, ...cards.map(card => card.image)].sort();
const hash = createHash('sha256').update(worker);
for (const file of files) hash.update(file).update(await readFile(resolve(out, file)));
const version = hash.digest('hex').slice(0, 16);
await writeFile(join(out, 'sw.js'), worker.replace("'__VERSION__'", JSON.stringify(version))
  .replace("'__PATHS__'", JSON.stringify(files)));
console.log(`Веб-версия готова. ${cards.length} карт, ${files.length} ресурсов в офлайн-кеше.`);
