# ── Stage 1 : Build Angular (Node 20) ───────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Dépendances d'abord (layer cache)
COPY package*.json ./
RUN npm ci --ignore-scripts

COPY . .
RUN npm run build -- --configuration production

# ── Stage 2 : Serve avec Nginx ───────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Copier la configuration Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier le build Angular (Angular 19 → dist/.../browser/)
COPY --from=builder /app/dist/tontinepro-frontend/browser /usr/share/nginx/html

# Utilisateur non-root (nginx écoute sur 80 mais tourne en worker sans root)
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx \
    && chmod -R 755 /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
