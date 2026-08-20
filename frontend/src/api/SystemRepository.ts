import RestClient from './RestClient';
import type { SystemDto } from '../models/SystemDto';
import type { SystemHistoryDto } from '../models/SystemHistoryDto';

export async function listSystems(): Promise<SystemDto[]> {
  const response = await RestClient.get<SystemDto[]>('/api/systems');
  return response.data;
}

export async function getSystem(id: number): Promise<SystemDto> {
  const response = await RestClient.get<SystemDto>(`/api/systems/${id}`);
  return response.data;
}

export async function createSystem(name: string, description: string): Promise<SystemDto> {
  const response = await RestClient.post<SystemDto>('/api/systems', { name, description });
  return response.data;
}

export async function updateSystem(id: number, name: string, externalId: string, description: string): Promise<SystemDto> {
  const response = await RestClient.put<SystemDto>(`/api/systems/${id}`, { name, externalId, description });
  return response.data;
}

export async function deleteSystem(id: number): Promise<void> {
  await RestClient.delete(`/api/systems/${id}`);
}

export async function getSystemHistory(id: number): Promise<SystemHistoryDto[]> {
  const response = await RestClient.get<SystemHistoryDto[]>(`/api/systems/${id}/history`);
  return response.data;
}
