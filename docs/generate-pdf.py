#!/usr/bin/env python3
"""
Génère les PDF de la documentation TontinePro à partir des sources HTML.

Rendu via Playwright (Chromium) pour une fidélité identique au navigateur
(gradients, flexbox, maquettes téléphone).

Installation (une seule fois) :
    pip install -r requirements.txt
    playwright install chromium

Utilisation :
    python generate-pdf.py            # génère tous les documents
    python generate-pdf.py guide      # génère seulement le guide utilisateur
"""

import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

# Console Windows en cp1252 : forcer UTF-8 pour les symboles (✓, ⚠…)
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

DOCS_DIR = Path(__file__).resolve().parent

SANS_MARGE = {"top": "0", "right": "0", "bottom": "0", "left": "0"}
MARGE_DOC = {"top": "12mm", "right": "10mm", "bottom": "12mm", "left": "10mm"}

# clé CLI -> (source HTML, PDF de sortie, marges de page)
# Le guide est concu en pleine page (couverture a fond perdu) : marges nulles.
# La presentation est un document de lecture : marges reelles, sinon les titres
# de section sont rognes en haut de page.
DOCUMENTS = {
    "guide": ("guide-utilisateur.html", "TontinePro-Guide-Utilisateur.pdf", SANS_MARGE),
    # Nom de sortie aligne sur le fichier servi par l'app (public/docs/)
    "promotion": ("document-promotion.html", "TontinePro-Presentation.pdf", MARGE_DOC),
}


def generer(page, source: str, sortie: str, marges: dict) -> None:
    src_path = DOCS_DIR / source
    out_path = DOCS_DIR / sortie
    if not src_path.exists():
        print(f"  ⚠ source introuvable, ignorée : {source}")
        return
    page.goto(src_path.as_uri(), wait_until="networkidle")
    page.pdf(
        path=str(out_path),
        format="A4",
        print_background=True,
        display_header_footer=False,
        prefer_css_page_size=True,
        margin=marges,
    )
    taille_ko = round(out_path.stat().st_size / 1024)
    print(f"  ✓ {sortie} ({taille_ko} Ko)")


def main() -> None:
    cles = sys.argv[1:] or list(DOCUMENTS)
    inconnues = [c for c in cles if c not in DOCUMENTS]
    if inconnues:
        print(f"Document(s) inconnu(s) : {', '.join(inconnues)}")
        print(f"Valeurs possibles : {', '.join(DOCUMENTS)}")
        sys.exit(1)

    print("Génération des PDF TontinePro…")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        for cle in cles:
            source, sortie, marges = DOCUMENTS[cle]
            generer(page, source, sortie, marges)
        browser.close()
    print("Terminé.")


if __name__ == "__main__":
    main()
