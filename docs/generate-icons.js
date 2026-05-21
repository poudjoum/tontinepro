const sharp  = require('sharp');
const toIco  = require('to-ico');
const fs     = require('fs');
const path   = require('path');

const OUT = path.join(__dirname, '../tontinepro-frontend/public');

// ── SVG source — carré arrondi dégradé indigo + T blanc ─────────────────────
function makeSvg(size, radiusFactor = 0.22, padding = 0.12) {
  const r   = Math.round(size * radiusFactor);
  const fs2 = Math.round(size * 0.52);           // taille fonte
  const cy  = Math.round(size * 0.685);           // baseline texte
  const pad = Math.round(size * padding);

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <defs>
    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%"   stop-color="#312E81"/>
      <stop offset="100%" stop-color="#4F46E5"/>
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="${size}" height="${size}" rx="${r}" ry="${r}" fill="url(#g)"/>
  <text
    x="${size / 2}" y="${cy}"
    font-family="Arial, Helvetica, sans-serif"
    font-size="${fs2}" font-weight="900"
    fill="white" text-anchor="middle" dominant-baseline="auto"
    letter-spacing="-2">T</text>
</svg>`;
}

// SVG maskable : fond plein bord-à-bord, T plus petit (safe zone 80 %)
function makeSvgMaskable(size) {
  const fs2 = Math.round(size * 0.42);
  const cy  = Math.round(size * 0.64);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <defs>
    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%"   stop-color="#312E81"/>
      <stop offset="100%" stop-color="#4F46E5"/>
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="${size}" height="${size}" fill="url(#g)"/>
  <text
    x="${size / 2}" y="${cy}"
    font-family="Arial, Helvetica, sans-serif"
    font-size="${fs2}" font-weight="900"
    fill="white" text-anchor="middle" dominant-baseline="auto">T</text>
</svg>`;
}

async function toPng(svgStr, size) {
  return sharp(Buffer.from(svgStr))
    .resize(size, size)
    .png()
    .toBuffer();
}

async function run() {
  console.log('Génération des icônes TontinePro…');

  // ── 1. PNG classiques ────────────────────────────────────────────────────
  const sizes = [16, 32, 48, 72, 96, 128, 144, 152, 192, 384, 512];
  const pngBuffers = {};

  for (const s of sizes) {
    pngBuffers[s] = await toPng(makeSvg(s), s);
    fs.writeFileSync(path.join(OUT, `icon-${s}x${s}.png`), pngBuffers[s]);
    console.log(`  ✅  icon-${s}x${s}.png`);
  }

  // ── 2. Apple touch icon 180×180 ──────────────────────────────────────────
  const apple = await toPng(makeSvg(180), 180);
  fs.writeFileSync(path.join(OUT, 'apple-touch-icon.png'), apple);
  console.log('  ✅  apple-touch-icon.png');

  // ── 3. Maskable 512×512 ──────────────────────────────────────────────────
  const maskable = await toPng(makeSvgMaskable(512), 512);
  fs.writeFileSync(path.join(OUT, 'icon-maskable-512x512.png'), maskable);
  console.log('  ✅  icon-maskable-512x512.png');

  // ── 4. favicon.ico (multi-taille : 16, 32, 48) ──────────────────────────
  const icoBuffer = await toIco([pngBuffers[16], pngBuffers[32], pngBuffers[48]]);
  fs.writeFileSync(path.join(OUT, 'favicon.ico'), icoBuffer);
  console.log('  ✅  favicon.ico');

  // ── 5. manifest.webmanifest ──────────────────────────────────────────────
  const manifest = {
    name: 'TontinePro',
    short_name: 'TontinePro',
    description: 'Gérez votre tontine — cotisations, épargne, prêts, aides.',
    start_url: '/',
    display: 'standalone',
    background_color: '#312E81',
    theme_color: '#4F46E5',
    orientation: 'portrait',
    icons: [
      { src: 'icon-72x72.png',           sizes: '72x72',   type: 'image/png' },
      { src: 'icon-96x96.png',           sizes: '96x96',   type: 'image/png' },
      { src: 'icon-128x128.png',         sizes: '128x128', type: 'image/png' },
      { src: 'icon-144x144.png',         sizes: '144x144', type: 'image/png' },
      { src: 'icon-152x152.png',         sizes: '152x152', type: 'image/png' },
      { src: 'icon-192x192.png',         sizes: '192x192', type: 'image/png', purpose: 'any' },
      { src: 'icon-384x384.png',         sizes: '384x384', type: 'image/png' },
      { src: 'icon-512x512.png',         sizes: '512x512', type: 'image/png', purpose: 'any' },
      { src: 'icon-maskable-512x512.png',sizes: '512x512', type: 'image/png', purpose: 'maskable' },
    ],
  };
  fs.writeFileSync(
    path.join(OUT, 'manifest.webmanifest'),
    JSON.stringify(manifest, null, 2)
  );
  console.log('  ✅  manifest.webmanifest');

  console.log('\n🎉  Toutes les icônes générées dans public/');
}

run().catch(e => { console.error('❌', e.message); process.exit(1); });
