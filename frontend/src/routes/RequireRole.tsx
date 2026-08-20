import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { CurrentUserDto } from '../models/UserDto';

interface RequireRoleProps {
  when: (user: CurrentUserDto) => boolean;
}

export default function RequireRole({ when }: RequireRoleProps) {
  const { user } = useAuth();

  if (!user || !when(user)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
