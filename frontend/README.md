# ConfigurationManager Frontend

React + TypeScript (Vite) UI for the Secret Manager application.

## Requirements

- Node 20+

## Configuration

The backend API origin (`VITE_API_BASE_URL`) is set per Vite mode, not by hand:

| Mode | File | Backend origin |
|---|---|---|
| development (`npm run dev`) | `.env.development` | `https://local.api.configurationmanager.alwaysmoveforward.com` |
| production (`npm run build`) | `.env.production` | `https://api.configurationmanager.alwaysmoveforward.com` |

Both files are committed — these are hostnames, not secrets. To point your own machine at a different backend (e.g. one running on `localhost`), copy `.env.example` to `.env.local` (gitignored) and set `VITE_API_BASE_URL` there; Vite applies it on top of the mode file.

## Running

```bash
npm install
npm run dev
```

## Building

```bash
npm run build
```

## How auth works here

There is no login form and no token handling in this app's code. Clicking "Log in" does a full-page navigation to the backend's `/api/auth/login`, which redirects to Auth0 and back; the backend sets an `HttpOnly` session cookie the browser then sends automatically on every API call (`RestClient.ts` sets `withCredentials: true`). `AuthContext` just asks the backend "who am I" (`GET /api/auth/me`) on load and reacts to whether that succeeds.

State-changing requests (`POST`/`PUT`/`DELETE`) require the `X-XSRF-TOKEN` header, which `RestClient.ts` reads from the `XSRF-TOKEN` cookie Spring Security sets automatically — no manual wiring needed elsewhere.

## Out of scope for this pass

This pass does not stand up a real Auth0 tenant or a running backend — see `../backend/README.md`.
