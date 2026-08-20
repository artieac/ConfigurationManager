import RestClient from './RestClient';
import type { ApiKeyDto, IssuedApiKeyDto } from '../models/ApiKeyDto';

export async function listApiKeys(systemId: number): Promise<ApiKeyDto[]> {
  const response = await RestClient.get<ApiKeyDto[]>(`/api/systems/${systemId}/api-keys`);
  return response.data;
}

/** The response's `token` field is the only time the raw key is ever returned — it can't be retrieved again. */
export async function createApiKey(systemId: number, name: string): Promise<IssuedApiKeyDto> {
  const response = await RestClient.post<IssuedApiKeyDto>(`/api/systems/${systemId}/api-keys`, { name });
  return response.data;
}

export async function renameApiKey(systemId: number, id: number, name: string): Promise<ApiKeyDto> {
  const response = await RestClient.put<ApiKeyDto>(`/api/systems/${systemId}/api-keys/${id}`, { name });
  return response.data;
}

export async function revokeApiKey(systemId: number, id: number): Promise<void> {
  await RestClient.delete(`/api/systems/${systemId}/api-keys/${id}`);
}
