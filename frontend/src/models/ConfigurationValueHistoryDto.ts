import type { ChangeStamp } from './ChangeStamp';
import type { HistoryAction } from './HistoryAction';

export interface ConfigurationValueHistoryDto {
  id: number;
  configurationId: number | null;
  systemId: number | null;
  environmentId: number | null;
  configurationName: string;
  systemName: string;
  environmentName: string;
  action: HistoryAction;
  changed: ChangeStamp;
}

