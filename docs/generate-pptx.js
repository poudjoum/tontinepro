const PptxGenJS = require('pptxgenjs');

const pptx = new PptxGenJS();

// ── Thème global ────────────────────────────────────────────────────────────
pptx.layout = 'LAYOUT_WIDE';
pptx.author  = 'Poudjoum Leutche Gedeon Rodrigue';
pptx.company = 'JumpyTech';
pptx.subject = 'TontinePro — Présentation';
pptx.title   = 'TontinePro v1.0';

const PRIMARY = '4F46E5';
const DARK    = '1E293B';
const GRAY    = '64748B';
const WHITE   = 'FFFFFF';
const GREEN   = '22C55E';
const AMBER   = 'F59E0B';
const RED     = 'EF4444';

function addFooter(slide, num) {
  slide.addText(`TontinePro v1.0  ·  JumpyTech  ·  © 2026`, {
    x: 0.3, y: 6.9, w: 12, h: 0.3,
    fontSize: 8, color: '94A3B8', align: 'center',
  });
  slide.addText(String(num), {
    x: 12.2, y: 6.9, w: 0.5, h: 0.3,
    fontSize: 8, color: '94A3B8', align: 'right',
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 1 — Cover
// ═══════════════════════════════════════════════════════════════════════════
let sl = pptx.addSlide();
sl.background = { color: '312E81' };

sl.addShape(pptx.ShapeType.roundRect, {
  x: 5.8, y: 0.5, w: 1.2, h: 1.2,
  fill: { color: WHITE }, line: { color: WHITE, width: 0 },
});
sl.addText('T', {
  x: 5.8, y: 0.5, w: 1.2, h: 1.2,
  fontSize: 40, bold: true, color: PRIMARY, align: 'center', valign: 'middle',
});

sl.addText('TontinePro', {
  x: 1, y: 1.9, w: 10.8, h: 1.0,
  fontSize: 52, bold: true, color: WHITE, align: 'center',
});
sl.addText('La plateforme digitale pour gérer votre tontine\navec simplicité, transparence et sécurité', {
  x: 1, y: 3.0, w: 10.8, h: 1.0,
  fontSize: 18, color: 'C7D2FE', align: 'center',
});
sl.addText('Version 1.0  ·  Mai 2026  ·  JumpyTech', {
  x: 1, y: 4.2, w: 10.8, h: 0.5,
  fontSize: 13, color: '818CF8', align: 'center',
});
sl.addText('📞 697 853 398 / 678 026 373', {
  x: 1, y: 5.5, w: 10.8, h: 0.5,
  fontSize: 13, color: 'C7D2FE', align: 'center',
});

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 2 — Le problème
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: 'FFFFFF' };

sl.addText('LE PROBLÈME', { x: 0.5, y: 0.3, w: 12, h: 0.4, fontSize: 11, bold: true, color: PRIMARY, charSpacing: 3 });
sl.addText('Gérer une tontine manuellement, c\'est risquer des pertes', {
  x: 0.5, y: 0.7, w: 12, h: 0.6, fontSize: 24, bold: true, color: DARK,
});

const problems = [
  '❌  Cahier physique perdu ou abîmé',
  '❌  Calculs d\'intérêts erronés',
  '❌  Retards de cotisation non détectés',
  '❌  Pas d\'historique des absences/sanctions',
  '❌  Secrétaire seul détient l\'information',
  '❌  Conflits lors des réunions',
];
const solutions = [
  '✅  Données sécurisées dans le cloud',
  '✅  Calculs automatiques (taux, échéancier)',
  '✅  Alertes en temps réel pour chaque membre',
  '✅  Absences tracées avec sanctions auto.',
  '✅  Transparence totale pour tous les membres',
  '✅  Zéro conflit — tout est documenté',
];

sl.addShape(pptx.ShapeType.roundRect, { x: 0.3, y: 1.5, w: 6.0, h: 5.0, fill: { color: 'FEF2F2' }, line: { color: 'FECACA', width: 1 } });
sl.addText('Avant TontinePro', { x: 0.5, y: 1.6, w: 5.6, h: 0.5, fontSize: 14, bold: true, color: 'DC2626' });
problems.forEach((p, i) => {
  sl.addText(p, { x: 0.5, y: 2.2 + i * 0.65, w: 5.8, h: 0.55, fontSize: 12, color: '7F1D1D' });
});

sl.addShape(pptx.ShapeType.roundRect, { x: 6.6, y: 1.5, w: 6.0, h: 5.0, fill: { color: 'F0FDF4' }, line: { color: '86EFAC', width: 1 } });
sl.addText('Avec TontinePro', { x: 6.8, y: 1.6, w: 5.6, h: 0.5, fontSize: 14, bold: true, color: '16A34A' });
solutions.forEach((s, i) => {
  sl.addText(s, { x: 6.8, y: 2.2 + i * 0.65, w: 5.8, h: 0.55, fontSize: 12, color: '166534' });
});

addFooter(sl, 2);

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 3 — Fonctionnalités (9 features)
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: 'F8FAFC' };

sl.addText('FONCTIONNALITÉS', { x: 0.5, y: 0.2, w: 12, h: 0.4, fontSize: 11, bold: true, color: PRIMARY, charSpacing: 3 });
sl.addText('Tout ce dont votre tontine a besoin', { x: 0.5, y: 0.6, w: 12, h: 0.5, fontSize: 22, bold: true, color: DARK });

const features = [
  { icon: '💳', title: 'Cotisations', desc: 'Génération auto, saisie de séance, statut en temps réel' },
  { icon: '🔄', title: 'Sessions', desc: 'Tours de bénéfice, calendrier, validation du pot mensuel' },
  { icon: '🏦', title: 'Épargne', desc: 'Compte individuel, capital prêtable, intérêts distribués' },
  { icon: '📋', title: 'Prêts', desc: 'Demande + documents, validation, échéancier automatique' },
  { icon: '⚠️', title: 'Absences & Sanctions', desc: 'Appel de présence, sanctions auto, suivi des amendes' },
  { icon: '🤝', title: 'Fond de solidarité', desc: 'Mensuel ou par session, gestion des dettes, projets' },
  { icon: '📊', title: 'Suivi mensuel', desc: 'Vue synthétique par mois : cotis., épargne, prêts' },
  { icon: '📤', title: 'Rattrapage historique', desc: 'Import Excel pour tontines déjà en cours' },
  { icon: '🔗', title: 'Invitations par lien', desc: 'SMS/WhatsApp, compte créé en 1 minute' },
];

const cols = 3, rows = 3;
features.forEach((f, i) => {
  const col = i % cols;
  const row = Math.floor(i / cols);
  const x = 0.3 + col * 4.3;
  const y = 1.3 + row * 1.8;

  sl.addShape(pptx.ShapeType.roundRect, { x, y, w: 4.0, h: 1.6, fill: { color: WHITE }, line: { color: 'E2E8F0', width: 1 } });
  sl.addText(f.icon, { x: x + 0.15, y: y + 0.15, w: 0.7, h: 0.7, fontSize: 22 });
  sl.addText(f.title, { x: x + 0.9, y: y + 0.15, w: 2.9, h: 0.4, fontSize: 12, bold: true, color: DARK });
  sl.addText(f.desc, { x: x + 0.9, y: y + 0.55, w: 2.9, h: 0.8, fontSize: 9.5, color: GRAY });
});

addFooter(sl, 3);

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 4 — Membre : vue quotidienne
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: 'FFFFFF' };

sl.addText('ESPACE MEMBRE', { x: 0.5, y: 0.2, w: 12, h: 0.4, fontSize: 11, bold: true, color: PRIMARY, charSpacing: 3 });
sl.addText('Chaque membre suit ses données en totale autonomie', { x: 0.5, y: 0.6, w: 12, h: 0.5, fontSize: 22, bold: true, color: DARK });

const memberFeatures = [
  { icon: '💳', title: 'Mes cotisations', desc: 'Historique complet, statut PAYEE/RETARD, mois par mois' },
  { icon: '🏦', title: 'Mon épargne', desc: 'Solde en temps réel, mouvements tracés, intérêts visibles' },
  { icon: '🔄', title: 'Mon tour', desc: 'Position dans la session, date prévue de bénéfice, countdown' },
  { icon: '📊', title: 'Mon suivi mensuel', desc: 'Vue synthétique annuelle : tout résumé mois par mois' },
  { icon: '📋', title: 'Mes prêts', desc: 'Demande, échéancier, remboursement chaque mensualité' },
  { icon: '⚠️', title: 'Mes sanctions', desc: 'Amendes, statut payé/impayé, historique complet' },
];

memberFeatures.forEach((f, i) => {
  const col = i % 2;
  const row = Math.floor(i / 2);
  const x = 0.3 + col * 6.3;
  const y = 1.3 + row * 1.75;

  sl.addShape(pptx.ShapeType.roundRect, { x, y, w: 6.0, h: 1.6, fill: { color: 'F8FAFC' }, line: { color: 'E2E8F0', width: 1 } });
  sl.addText(f.icon, { x: x + 0.2, y: y + 0.4, w: 0.8, h: 0.8, fontSize: 26 });
  sl.addText(f.title, { x: x + 1.1, y: y + 0.2, w: 4.7, h: 0.5, fontSize: 13, bold: true, color: DARK });
  sl.addText(f.desc, { x: x + 1.1, y: y + 0.7, w: 4.7, h: 0.7, fontSize: 10.5, color: GRAY });
});

addFooter(sl, 4);

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 5 — Rattrapage historique
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: 'F8FAFC' };

sl.addText('RATTRAPAGE HISTORIQUE', { x: 0.5, y: 0.2, w: 12, h: 0.4, fontSize: 11, bold: true, color: PRIMARY, charSpacing: 3 });
sl.addText('Vous utilisez TontinePro en cours de session ? Aucun problème.', { x: 0.5, y: 0.6, w: 12, h: 0.5, fontSize: 20, bold: true, color: DARK });

sl.addShape(pptx.ShapeType.roundRect, { x: 0.3, y: 1.3, w: 7.5, h: 5.3, fill: { color: WHITE }, line: { color: 'E2E8F0', width: 1 } });
sl.addText('Comment ça marche ?', { x: 0.5, y: 1.4, w: 7.0, h: 0.5, fontSize: 14, bold: true, color: PRIMARY });

const steps = [
  { n: '1', t: 'Télécharger le modèle Excel', d: 'Admin → Rattrapage → Télécharger' },
  { n: '2', t: 'Remplir les 6 onglets', d: 'Tours passés · Cotisations · Absences · Épargne · Prêts' },
  { n: '3', t: 'Charger le fichier dans l\'app', d: 'Sélectionner la session à renseigner' },
  { n: '4', t: 'Lancer l\'import', d: 'Bilan détaillé + avertissements pour les lignes ignorées' },
];
steps.forEach((s, i) => {
  const y = 2.0 + i * 1.1;
  sl.addShape(pptx.ShapeType.ellipse, { x: 0.5, y, w: 0.5, h: 0.5, fill: { color: PRIMARY }, line: { color: PRIMARY, width: 0 } });
  sl.addText(s.n, { x: 0.5, y, w: 0.5, h: 0.5, fontSize: 13, bold: true, color: WHITE, align: 'center', valign: 'middle' });
  sl.addText(s.t, { x: 1.2, y: y + 0.0, w: 6.3, h: 0.3, fontSize: 12, bold: true, color: DARK });
  sl.addText(s.d, { x: 1.2, y: y + 0.28, w: 6.3, h: 0.3, fontSize: 10, color: GRAY });
});

sl.addShape(pptx.ShapeType.roundRect, { x: 8.1, y: 1.3, w: 4.5, h: 5.3, fill: { color: 'EFF6FF' }, line: { color: 'BFDBFE', width: 1 } });
sl.addText('Ce qui est importé', { x: 8.3, y: 1.4, w: 4.1, h: 0.5, fontSize: 13, bold: true, color: '1D4ED8' });
const imported = ['✅  Tours passés (bénéficiaires)', '✅  Cotisations par mois', '✅  Absences et sanctions', '✅  Épargne initiale / complémentaire', '✅  Fond antérieur (caisse)', '✅  Prêts en cours + remboursements'];
imported.forEach((item, i) => {
  sl.addText(item, { x: 8.3, y: 2.1 + i * 0.7, w: 4.1, h: 0.6, fontSize: 11, color: '1E3A8A' });
});

addFooter(sl, 5);

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 6 — Multi-tontine & Sécurité
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: 'FFFFFF' };

sl.addText('SÉCURITÉ & MULTI-TONTINE', { x: 0.5, y: 0.2, w: 12, h: 0.4, fontSize: 11, bold: true, color: PRIMARY, charSpacing: 3 });
sl.addText('Vos données protégées, vos tontines bien séparées', { x: 0.5, y: 0.6, w: 12, h: 0.5, fontSize: 22, bold: true, color: DARK });

const secItems = [
  { icon: '🔐', t: 'Double authentification', d: '2FA avec Google Authenticator ou Authy' },
  { icon: '🔒', t: 'Tokens JWT sécurisés', d: 'Sessions à durée limitée, déconnexion immédiate' },
  { icon: '🌐', t: 'HTTPS + CORS strict', d: 'Communication chiffrée TLS' },
  { icon: '🏛️', t: 'Rôles et permissions', d: 'SUPER_ADMIN · Président · Secrétaire · Membre' },
  { icon: '💾', t: 'Hébergement local', d: 'Vos données ne transitent pas par des tiers' },
  { icon: '📧', t: 'Notifications email', d: 'Alerte pour chaque action importante' },
];

secItems.forEach((s, i) => {
  const col = i % 3;
  const row = Math.floor(i / 3);
  const x = 0.3 + col * 4.3;
  const y = 1.4 + row * 1.8;
  sl.addShape(pptx.ShapeType.roundRect, { x, y, w: 4.0, h: 1.5, fill: { color: 'EFF6FF' }, line: { color: 'BFDBFE', width: 1 } });
  sl.addText(s.icon, { x: x + 0.2, y: y + 0.15, w: 0.7, h: 0.7, fontSize: 22 });
  sl.addText(s.t, { x: x + 0.9, y: y + 0.15, w: 2.9, h: 0.35, fontSize: 11, bold: true, color: '1D4ED8' });
  sl.addText(s.d, { x: x + 0.9, y: y + 0.5, w: 2.9, h: 0.7, fontSize: 10, color: '3730A3' });
});

// Multi-tontine box
sl.addShape(pptx.ShapeType.roundRect, { x: 0.3, y: 5.0, w: 12.3, h: 1.6, fill: { color: 'F0FDF4' }, line: { color: '86EFAC', width: 1 } });
sl.addText('Multi-tontine :', { x: 0.6, y: 5.1, w: 2.5, h: 0.4, fontSize: 12, bold: true, color: '15803D' });
sl.addText('Un même opérateur peut gérer plusieurs tontines avec des comptes distincts. Chaque membre ne voit que les données de SA tontine. Isolation totale garantie.', {
  x: 0.6, y: 5.5, w: 12.0, h: 0.8, fontSize: 11, color: '166534',
});

addFooter(sl, 6);

// ═══════════════════════════════════════════════════════════════════════════
// SLIDE 7 — Contact & CTA
// ═══════════════════════════════════════════════════════════════════════════
sl = pptx.addSlide();
sl.background = { color: '1E1B4B' };

sl.addShape(pptx.ShapeType.roundRect, {
  x: 4.3, y: 0.4, w: 4.2, h: 4.2,
  fill: { color: '312E81' }, line: { color: '4F46E5', width: 2 },
});
sl.addText('T', { x: 4.3, y: 0.4, w: 4.2, h: 1.2, fontSize: 60, bold: true, color: WHITE, align: 'center' });
sl.addText('TontinePro', { x: 4.3, y: 1.5, w: 4.2, h: 0.7, fontSize: 24, bold: true, color: WHITE, align: 'center' });
sl.addText('tontinepro.tontinepro.uk', { x: 4.3, y: 2.2, w: 4.2, h: 0.5, fontSize: 13, color: 'C7D2FE', align: 'center', underline: true });

sl.addText('Contact & Support', { x: 1, y: 4.8, w: 10.8, h: 0.5, fontSize: 16, bold: true, color: WHITE, align: 'center' });
sl.addText('Poudjoum Leutche Gedeon Rodrigue', { x: 1, y: 5.3, w: 10.8, h: 0.45, fontSize: 14, color: 'C7D2FE', align: 'center' });
sl.addText('📞  697 853 398  /  678 026 373', { x: 1, y: 5.75, w: 10.8, h: 0.45, fontSize: 14, color: 'A5B4FC', align: 'center' });
sl.addText('JumpyTech  ·  © 2026  ·  Tous droits réservés', { x: 1, y: 6.6, w: 10.8, h: 0.35, fontSize: 10, color: '6366F1', align: 'center' });

// ── Écriture du fichier ─────────────────────────────────────────────────────
pptx.writeFile({ fileName: 'TontinePro-Presentation.pptx' })
  .then(() => console.log('✅  TontinePro-Presentation.pptx généré'))
  .catch(e  => console.error('❌ Erreur:', e));