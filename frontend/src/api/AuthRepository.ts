import RestClient from './RestClient';
import type { CurrentUserDto } from '../models/UserDto';

/** Full-page navigations (not XHR) — the backend redirects the browser through Auth0 and back. */
export function loginUrl(apiBaseUrl: string): string {
  return `${apiBaseUrl}/api/auth/login`;
}

export async function fetchCurrentUser(): Promise<CurrentUserDto> {
  const response = await RestClient.get<CurrentUserDto>('/api/auth/me');
  return response.data;
}

export async function logout(): Promise<void> {
  await RestClient.post('/api/auth/logout');
}
