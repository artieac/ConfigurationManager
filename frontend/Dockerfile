# ---- Build stage ----
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first so this layer is cached unless package*.json changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Vite bakes VITE_API_BASE_URL into the built JS at this step, not at container
# runtime. Left unset (the default), the build falls through to .env.production
# (committed, not a secret — see src/config.ts). Pass --build-arg to override:
#   docker compose build --build-arg VITE_API_BASE_URL=https://api.example.com frontend
ARG VITE_API_BASE_URL
RUN if [ -n "$VITE_API_BASE_URL" ]; then export VITE_API_BASE_URL; fi; npm run build

# ---- Runtime stage ----
FROM node:22-alpine
WORKDIR /app

# 'serve' handles the SPA fallback itself (-s rewrites unmatched paths to
# index.html) — react-router uses BrowserRouter, not #hash routes, so that's
# required for direct/refreshed navigation to a route like /systems/1 to work.
RUN npm install -g serve@14.2.5

COPY --from=build /app/dist ./dist

EXPOSE 80
CMD ["serve", "-s", "dist", "-l", "80"]
