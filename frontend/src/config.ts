/**
 * Backend API origin. Comes from VITE_API_BASE_URL, which Vite resolves per
 * mode: `.env.development` (dev server / `npm run dev`) points at the shared
 * dev backend, `.env.production` (`npm run build`) points at the production
 * backend. An untracked `.env.local` can still override this on a single
 * machine — e.g. to point at a backend running on localhost instead of the
 * shared dev environment.
 */
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'https://local.api.configurationmanager.alwaysmoveforward.com';

