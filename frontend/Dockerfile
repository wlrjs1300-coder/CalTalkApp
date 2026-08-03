FROM node:24-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime

ENV PORT=8080 \
    BACKEND_ORIGIN=http://backend:8080 \
    NGINX_ENVSUBST_FILTER=^(PORT|BACKEND_ORIGIN)$

COPY --chown=101:101 nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --chown=101:101 --chmod=755 docker-entrypoint.sh /usr/local/bin/caltalk-entrypoint.sh
COPY --from=build --chown=101:101 /workspace/dist /usr/share/nginx/html

USER 101
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:$${PORT:-8080}/healthz" || exit 1

ENTRYPOINT ["/usr/local/bin/caltalk-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
