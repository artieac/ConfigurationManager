import type { ChangeStamp } from './ChangeStamp';
import type { HistoryAction } from './HistoryAction';

export interface SystemHistoryDto {
  id: number;
  systemId: number | null;
  systemName: string;
  externalId: string | null;
  description: string | null;
  action: HistoryAction;
  changed: ChangeStamp;
}
