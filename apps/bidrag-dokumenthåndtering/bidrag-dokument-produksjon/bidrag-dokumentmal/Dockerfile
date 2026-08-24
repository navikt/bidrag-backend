# syntax=docker/dockerfile:1.7

FROM node:22-alpine3.22 AS build
ENV NODE_ENV=production
LABEL maintainer="Team Bidrag"
WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    --mount=type=secret,id=reader_token \
    TOKEN="$(cat /run/secrets/reader_token)" && \
    printf '%s\n' \
      'registry=https://registry.npmjs.org/' \
      '@navikt:registry=https://npm.pkg.github.com' \
      "//npm.pkg.github.com/:_authToken=${TOKEN}" > /root/.npmrc && \
    npm ci --include=dev && \
    rm -f /root/.npmrc


COPY . .
RUN npm run build
RUN npm prune --omit=dev && npm cache clean --force

FROM node:22-alpine3.22 AS runtime
ENV NODE_ENV=production
ENV PORT=8080

# Patch OS packages from the base image. node:22-alpine3.22 currently ships
# openssl 3.5.6-r0, which has known CVEs fixed in 3.5.7-r0. The base image is
# only rebuilt periodically, so upgrade explicitly rather than waiting for it.
RUN apk upgrade --no-cache

# Drop the bundled npm/corepack CLIs from the runtime image. They are never used
# here - the container starts react-router-serve directly - but their vendored
# dependencies dominate the image's CVE report (npm bundles its own copies of
# brace-expansion, picomatch, ip-address, tar and friends, which are flagged even
# though no code path can reach them). Removing them eliminates that whole class
# of findings instead of suppressing them one by one.
RUN rm -rf /usr/local/lib/node_modules/npm \
           /usr/local/lib/node_modules/corepack \
           /usr/local/bin/npm /usr/local/bin/npx \
           /usr/local/bin/corepack /opt/yarn* \
           /usr/local/bin/yarn /usr/local/bin/yarnpkg

WORKDIR /app
COPY --from=build --chown=node:node /app/package*.json ./
COPY --from=build --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/build ./build
COPY --from=build --chown=node:node /app/app/style ./app/style
COPY --from=build --chown=node:node /app/public ./public

USER node

EXPOSE 8080

CMD ["./node_modules/.bin/react-router-serve", "./build/server/index.js"]
