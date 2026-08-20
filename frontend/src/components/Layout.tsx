import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import RoleGuard from './RoleGuard';

export default function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.75rem 1.5rem',
          borderBottom: '1px solid var(--border)',
        }}
      >
        <nav style={{ display: 'flex', gap: '1.25rem', alignItems: 'center' }}>
          <Link to="/" style={{ fontWeight: 600, textDecoration: 'none' }}>
            Configuration Manager
          </Link>
          <RoleGuard when={(u) => u.canManageUsers}>
            <Link to="/admin/users">Users</Link>
          </RoleGuard>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {user && (
            <span style={{ color: 'var(--text-muted)', fontSize: 14 }}>
              {user.displayName ?? user.email} · {user.role}
            </span>
          )}
          <button type="button" onClick={() => void logout()}>
            Log out
          </button>
        </div>
      </header>

      <main style={{ flex: 1, padding: '1.5rem' }}>{children}</main>
    </div>
  );
}

