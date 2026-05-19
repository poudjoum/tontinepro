#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════
#  TontinePro — Script de déploiement / mise à jour
#  À exécuter sur le serveur 192.168.1.209 en tant que : jumpy
#
#  Usage :
#    cd /opt/tontinepro
#    ./scripts/deploy.sh [--build]
#
#  Options :
#    --build   Rebuild les images Docker localement (sans registry)
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

DEPLOY_DIR="/opt/tontinepro"
COMPOSE_FILE="docker-compose.prod.yml"
BUILD_LOCAL=false

[[ "${1:-}" == "--build" ]] && BUILD_LOCAL=true

echo ""
echo "═══════════════════════════════════════════════════"
echo "  TontinePro — Déploiement $(date '+%Y-%m-%d %H:%M')"
echo "═══════════════════════════════════════════════════"

cd "$DEPLOY_DIR"

# Vérification du .env
if [[ ! -f ".env" ]]; then
  echo "❌  Fichier .env manquant !"
  echo "    Créez-le depuis .env.example : cp .env.example .env && nano .env"
  exit 1
fi

if $BUILD_LOCAL; then
  echo "▶  Build local des images Docker..."
  docker compose -f "$COMPOSE_FILE" build --no-cache
else
  echo "▶  Pull des dernières images depuis ghcr.io..."
  docker compose -f "$COMPOSE_FILE" pull || true
fi

echo "▶  Démarrage de la stack..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

echo "▶  Vérification des conteneurs..."
sleep 5
docker compose -f "$COMPOSE_FILE" ps

echo "▶  Nettoyage des anciennes images..."
docker image prune -f > /dev/null

echo ""
echo "═══════════════════════════════════════════════════"
echo "  ✅  Déploiement terminé !"
echo "  🌐  Frontend : http://192.168.1.209"
echo "  🔧  Backend  : http://192.168.1.209/api/v1"
echo "═══════════════════════════════════════════════════"
