import type { Role } from './Role';

export interface UserDto {
  id: number;
  email: string;
  displayName: string | null;
  role: Role;
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface CurrentUserDto {
  id: number;
  email: string;
  displayName: string | null;
  role: Role;
  canWrite: boolean;
  canRevealConfigurationValue: boolean;
  canDelete: boolean;
  canManageUsers: boolean;
}

