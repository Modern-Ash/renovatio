FROM node:24.20.0-bookworm-slim AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 make g++ pkg-config libsecret-1-dev libudev-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY package.json package-lock.json .npmrc ./
COPY applications/browser/package.json applications/browser/package.json
COPY extensions/renovatio-core-ui/package.json extensions/renovatio-core-ui/package.json
RUN npm ci

COPY tsconfig.json ./
COPY applications applications
COPY extensions extensions
COPY config config
COPY scripts scripts
RUN npm run build && npm prune --omit=dev

FROM node:24.20.0-bookworm-slim AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends libsecret-1-0 libudev1 \
    && rm -rf /var/lib/apt/lists/*
ENV NODE_ENV=production \
    RENOVATIO_BACKEND_URL=http://host.docker.internal:8080 \
    RENOVATIO_TELEMETRY_ENABLED=false \
    RENOVATIO_WORKSPACE_ROOT=/workspace-data
WORKDIR /workspace
COPY --from=build /workspace /workspace
RUN mkdir -p /workspace-data
EXPOSE 3000
USER node
CMD ["node", "applications/browser/lib/backend/main.js", "/workspace-data", "--hostname", "0.0.0.0", "--port", "3000"]
