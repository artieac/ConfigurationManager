import type { ChangeStamp } from './ChangeStamp';

/** Created-only — see SystemHistoryDto for who changed what since. */
export interface SystemDto {
  id: number;
  name: string;
  externalId: string;
  description: string | null;
  created: ChangeStamp;
}
