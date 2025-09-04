# 1) Build the Angular app
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm config set registry https://registry.npmjs.org/ \
 && rm -f package-lock.json \
 && npm i --package-lock-only \
 && npm ci

COPY . .
ARG APP_BASE_PATH=/
RUN BASE="$(printf "%s" "${APP_BASE_PATH%/}/")" \
 && npm run build -- --configuration production --base-href "$BASE" --deploy-url "$BASE"

# 2) Serve with NGINX
FROM nginx:1.27-alpine

ENV NGINX_ENTRYPOINT_QUIET_LOGS=1

# Copy configs first
COPY ./ops/nginx.conf /etc/nginx/nginx.conf
COPY ./ops/nginx.static.conf /etc/nginx/conf.d/default.conf

# Clear default welcome files (optional but tidy)
RUN rm -rf /usr/share/nginx/html/*

# COPY THE RIGHT BUILD OUTPUT (note /browser)
# If your project name differs, adjust both occurrences below.
COPY --from=build /app/dist/workbench-ui/browser /usr/share/nginx/html

# Runtime bits + ensure writable temp dirs exist for nginx
RUN mkdir -p /usr/share/nginx/html/assets \
 && printf "window.__env={apiBaseUrl:\"\"};" > /usr/share/nginx/html/assets/env.js \
 && mkdir -p /tmp/nginx/{client_temp,proxy_temp,fastcgi_temp,uwsgi_temp,scgi_temp} \
 && chown -R 101:101 /tmp/nginx

USER 101
EXPOSE 8080

# Healthcheck: hit the right port and your /healthz location
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget -qO- http://127.0.0.1:8080/healthz >/dev/null || exit 1
