import { loginUrl } from '../api/AuthRepository';
import { API_BASE_URL } from '../config';

/**
 * Login is a full-page redirect, not an XHR: clicking this button sends the
 * browser to the backend's /api/auth/login, which redirects to Auth0, which
 * redirects back to the backend's callback, which redirects here with the
 * session cookie already set.
 */
export default function LoginLandingPage() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', flex: 1, gap: '1rem' }}>
      <h1>Configuration Manager</h1>
      <p style={{ color: 'var(--text-muted)' }}>Sign in to view and manage configuration settings.</p>
      <button
        type="button"
        onClick={() => {
          window.location.href = loginUrl(API_BASE_URL);
        }}
        style={{ padding: '0.6rem 1.2rem', background: 'var(--accent)', color: 'var(--accent-contrast)', border: 'none', borderRadius: 6 }}
      >
        Log in
      </button>
    </div>
  );
}

