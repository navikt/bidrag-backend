# ────────────────────────────────
# Dockerfile – optimized for Cloud Run
# ────────────────────────────────
FROM node:24-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:24-alpine

WORKDIR /app

COPY --from=builder /app/node_modules ./node_modules

# Copy source code
COPY . .

ENV PORT=8080
EXPOSE 8080

CMD ["npx", "functions-framework", "--target=gcpAuditToSlack", "--signature-type=http"]