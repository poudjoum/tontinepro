#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════
#  TontinePro — Configuration initiale du serveur Ubuntu 22.04 / 24.04
#  Serveur : 192.168.1.209
#
#  Usage (depuis le serveur, en tant que votre utilisateur avec sudo) :
#    chmod +x setup-server.sh
#    sudo ./setup-server.sh <votre_nom_utilisateur>
#
#  Exemple : sudo ./setup-server.sh rodrigue
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

# ── Paramètre obligatoire : nom d'utilisateur déploiement ────────────
DEPLOY_USER="${1:-}"
if [[ -z "$DEPLOY_USER" ]]; then
  echo "❌  Usage: sudo $0 <nom_utilisateur>"
  echo "    Exemple: sudo $0 rodrigue"
  exit 1
fi

DEPLOY_DIR="/opt/tontinepro"

echo ""
echo "═══════════════════════════════════════════════════"
echo "  TontinePro — Configuration serveur 192.168.1.209"
echo "═══════════════════════════════════════════════════"
echo "  Utilisateur déploiement : $DEPLOY_USER"
echo "  Répertoire              : $DEPLOY_DIR"
echo ""

# ── 1. Mise à jour du système ────────────────────────────────────────
echo "▶  [1/7] Mise à jour des paquets..."
apt-get update -qq
apt-get upgrade -y -qq

# ── 2. Paquets utilitaires ───────────────────────────────────────────
echo "▶  [2/7] Installation des utilitaires..."
apt-get install -y -qq \
  curl wget git ufw \
  ca-certificates gnupg lsb-release \
  apt-transport-https software-properties-common

# ── 3. Installation Docker CE ────────────────────────────────────────
echo "▶  [3/7] Installation de Docker CE..."
if ! command -v docker &>/dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list

  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin
  systemctl enable docker
  systemctl start docker
  echo "   ✓ Docker $(docker --version)"
else
  echo "   ✓ Docker déjà installé — $(docker --version)"
fi

# ── 4. Ajout de l'utilisateur au groupe docker ───────────────────────
echo "▶  [4/7] Configuration utilisateur $DEPLOY_USER..."
if id "$DEPLOY_USER" &>/dev/null; then
  usermod -aG docker "$DEPLOY_USER"
  echo "   ✓ $DEPLOY_USER ajouté au groupe docker"
else
  echo "   ⚠  Utilisateur $DEPLOY_USER introuvable — création..."
  useradd -m -s /bin/bash "$DEPLOY_USER"
  usermod -aG docker,sudo "$DEPLOY_USER"
  echo "   ✓ Utilisateur $DEPLOY_USER créé"
fi

# ── 5. Répertoire de déploiement ─────────────────────────────────────
echo "▶  [5/7] Création du répertoire de déploiement..."
mkdir -p "$DEPLOY_DIR"
chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_DIR"
chmod 750 "$DEPLOY_DIR"
echo "   ✓ $DEPLOY_DIR créé"

# ── 6. Pare-feu UFW ──────────────────────────────────────────────────
echo "▶  [6/7] Configuration du pare-feu UFW..."
ufw --force reset > /dev/null
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp    comment 'SSH'
ufw allow 80/tcp    comment 'HTTP TontinePro'
ufw allow 443/tcp   comment 'HTTPS (futur)'
ufw --force enable
echo "   ✓ UFW activé — ports 22, 80, 443 ouverts"

# ── 7. Service systemd pour auto-démarrage ───────────────────────────
echo "▶  [7/7] Service systemd tontinepro..."
cat > /etc/systemd/system/tontinepro.service << SERVICE
[Unit]
Description=TontinePro Stack (Docker Compose)
Requires=docker.service
After=docker.service network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$DEPLOY_DIR
ExecStart=/usr/bin/docker compose -f docker-compose.prod.yml up -d --remove-orphans
ExecStop=/usr/bin/docker compose -f docker-compose.prod.yml down
TimeoutStartSec=300
User=$DEPLOY_USER

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable tontinepro.service
echo "   ✓ Service tontinepro activé au démarrage"

# ── Résumé ───────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════"
echo "  ✅  Serveur configuré avec succès !"
echo "═══════════════════════════════════════════════════"
echo ""
echo "  Prochaines étapes :"
echo ""
echo "  1. Reconnectez-vous pour activer le groupe docker :"
echo "     exit && ssh $DEPLOY_USER@192.168.1.209"
echo ""
echo "  2. Clonez le projet (ou copiez docker-compose.prod.yml) :"
echo "     cd $DEPLOY_DIR"
echo "     git clone https://github.com/poudjoum/tontinepro.git ."
echo ""
echo "  3. Créez le fichier .env :"
echo "     cp .env.example .env && nano .env"
echo ""
echo "  4. Lancez la stack :"
echo "     ./scripts/deploy.sh"
echo ""
