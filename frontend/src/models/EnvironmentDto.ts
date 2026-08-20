import type { ChangeStamp } from './ChangeStamp';

export interface EnvironmentDto {
  id: number;
  systemId: number;
  name: string;
  externalId: string;
  created: ChangeStamp;
  updated: ChangeStamp;
}
