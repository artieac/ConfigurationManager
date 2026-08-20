import RestClient from './RestClient';
import type { UserDto } from '../models/UserDto';
import type { Role } from '../models/Role';

export async function listUsers(): Promise<UserDto[]> {
  const response = await RestClient.get<UserDto[]>('/api/users');
  return response.data;
}

export async function updateUserRole(id: number, role: Role): Promise<UserDto> {
  const response = await RestClient.put<UserDto>(`/api/users/${id}/role`, { role });
  return response.data;
}
