import type { ChangeStamp } from './ChangeStamp';

/** Metadata only — the API never returns a decrypted value from this shape. */
export interface ConfigurationDto {
  id: number;
  systemId: number;
  name: string;
  /** Environment ids that currently have a value set for this secret. */
  valuesSetInEnvironmentIds: number[];
  created: ChangeStamp;
  updated: ChangeStamp;
}

export interface ConfigurationValueDto {
  configurationId: number;
  environmentId: number;
  configurationName: string;
  environmentName: string | null;
  value: string;
}

