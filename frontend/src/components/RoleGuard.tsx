import type { ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import type { CurrentUserDto } from '../models/UserDto';

interface RoleGuardProps {
  when: (user: CurrentUserDto) => boolean;
  children: ReactNode;
}

/** Hides UI the current user isn't permitted to use — a UX convenience only; the backend enforces the real rule via @PreAuthorize. */
export default function RoleGuard({ when, children }: RoleGuardProps) {
  const { user } = useAuth();
  if (!user || !when(user)) {
    return null;
  }
  return <>{children}</>;
}
