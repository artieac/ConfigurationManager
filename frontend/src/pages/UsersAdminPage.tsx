import { useEffect, useState } from 'react';
import type { UserDto } from '../models/UserDto';
import { ROLES, type Role } from '../models/Role';
import { listUsers, updateUserRole } from '../api/UserRepository';
import DataTable from '../components/DataTable';
import LoadingSpinner from '../components/LoadingSpinner';

export default function UsersAdminPage() {
  const [users, setUsers] = useState<UserDto[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = () => {
    listUsers()
      .then(setUsers)
      .catch(() => setError('Failed to load users.'));
  };

  useEffect(refresh, []);

  if (error) {
    return <p style={{ color: 'var(--danger)' }}>{error}</p>;
  }

  if (!users) {
    return <LoadingSpinner />;
  }

  const handleRoleChange = async (user: UserDto, role: Role) => {
    await updateUserRole(user.id, role);
    refresh();
  };

  return (
    <div>
      <h1>Users</h1>
      <DataTable
        rows={users}
        rowKey={(u) => u.id}
        columns={[
          { header: 'Name', render: (u) => u.displayName ?? '—' },
          { header: 'Email', render: (u) => u.email },
          {
            header: 'Role',
            render: (u) => (
              <select value={u.role} onChange={(e) => void handleRoleChange(u, e.target.value as Role)}>
                {ROLES.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            ),
          },
          { header: 'Last login', render: (u) => (u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : '—') },
        ]}
      />
    </div>
  );
}
