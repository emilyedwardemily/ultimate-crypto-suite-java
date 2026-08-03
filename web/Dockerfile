FROM node:20-alpine AS deps

RUN apk add --no-cache libc6-compat

WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ARG NEXT_PUBLIC_PYTHON_URL=https://ultimate-crypto-python.onrender.com
ARG NEXT_PUBLIC_NODE_URL=https://ultimate-crypto-node-gateway.onrender.com
ARG NEXT_PUBLIC_API_KEY=Emily_Crypto_Secure_2026_KIU
ENV NEXT_PUBLIC_PYTHON_URL=$NEXT_PUBLIC_PYTHON_URL
ENV NEXT_PUBLIC_NODE_URL=$NEXT_PUBLIC_NODE_URL
ENV NEXT_PUBLIC_API_KEY=$NEXT_PUBLIC_API_KEY
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
