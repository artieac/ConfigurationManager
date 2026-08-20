import type { ChangeStamp } from './ChangeStamp';

export interface ApiKeyDto {
  id: number;
  systemId: number;
  name: string;
  created: ChangeStamp;
  lastUsedAt: string | null;
}

export interface IssuedApiKeyDto {
  id: number;
  systemId: number;
  name: string;
  token: string;
}
