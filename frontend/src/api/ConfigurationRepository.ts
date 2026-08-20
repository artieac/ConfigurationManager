import RestClient from './RestClient';
import type { ConfigurationDto, ConfigurationValueDto } from '../models/ConfigurationDto';
import type { ConfigurationValueHistoryDto } from '../models/ConfigurationValueHistoryDto';

export async function listConfigurations(systemId: number): Promise<ConfigurationDto[]> {
  const response = await RestClient.get<ConfigurationDto[]>(`/api/systems/${systemId}/secrets`);
  return response.data;
}

/** Creates a secret NAME only — use setConfigurationValue to give it a value in a specific environment. */
export async function createConfiguration(systemId: number, name: string): Promise<ConfigurationDto> {
  const response = await RestClient.post<ConfigurationDto>(`/api/systems/${systemId}/secrets`, { name });
  return response.data;
}

export async function renameConfiguration(id: number, name: string): Promise<ConfigurationDto> {
  const response = await RestClient.put<ConfigurationDto>(`/api/secrets/${id}`, { name });
  return response.data;
}

/** Deletes the secret and its value in every environment. */
export async function deleteConfiguration(id: number): Promise<void> {
  await RestClient.delete(`/api/secrets/${id}`);
}

/** Only called when the user explicitly clicks "Reveal" — never prefetched alongside the list. */
export async function revealConfigurationValue(configurationId: number, environmentId: number): Promise<ConfigurationValueDto> {
  const response = await RestClient.get<ConfigurationValueDto>(`/api/secrets/${configurationId}/environments/${environmentId}/value`);
  return response.data;
}

/** Upsert — sets the value whether or not one was already present for this (secret, environment) pair. */
export async function setConfigurationValue(configurationId: number, environmentId: number, value: string): Promise<void> {
  await RestClient.put(`/api/secrets/${configurationId}/environments/${environmentId}/value`, { value });
}

export async function deleteConfigurationValue(configurationId: number, environmentId: number): Promise<void> {
  await RestClient.delete(`/api/secrets/${configurationId}/environments/${environmentId}/value`);
}

/** Omit environmentId for history across every environment; pass it to scope to just one. */
export async function getConfigurationValueHistory(configurationId: number, environmentId?: number): Promise<ConfigurationValueHistoryDto[]> {
  const response = await RestClient.get<ConfigurationValueHistoryDto[]>(`/api/secrets/${configurationId}/history`, {
    params: environmentId !== undefined ? { environmentId } : undefined,
  });
  return response.data;
}

/** Only called when the user explicitly clicks "Reveal" on a specific history row — never prefetched. */
export async function revealHistoricConfigurationValue(configurationId: number, historyId: number): Promise<{ historyId: number; value: string }> {
  const response = await RestClient.get<{ historyId: number; value: string }>(`/api/secrets/${configurationId}/history/${historyId}/value`);
  return response.data;
}

