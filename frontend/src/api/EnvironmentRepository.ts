import RestClient from './RestClient';
import type { EnvironmentDto } from '../models/EnvironmentDto';

export async function listEnvironments(systemId: number): Promise<EnvironmentDto[]> {
  const response = await RestClient.get<EnvironmentDto[]>(`/api/systems/${systemId}/environments`);
  return response.data;
}

export async function createEnvironment(systemId: number, name: string): Promise<EnvironmentDto> {
  const response = await RestClient.post<EnvironmentDto>(`/api/systems/${systemId}/environments`, { name });
  return response.data;
}

export async function updateEnvironment(id: number, name: string, externalId: string): Promise<EnvironmentDto> {
  const response = await RestClient.put<EnvironmentDto>(`/api/environments/${id}`, { name, externalId });
  return response.data;
}

export async function deleteEnvironment(id: number): Promise<void> {
  await RestClient.delete(`/api/environments/${id}`);
}
