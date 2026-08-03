import type { TimeFrameSchedule } from './TimeFrameSchedule';
import { DEFAULT_TIME_FRAME_SCHEDULE } from './TimeFrameSchedule';

export const DEFAULT_OVERALL_MINUTES = 120;
export const UNLIMITED = -1;

export interface AppLimit {
  packageName: string;
  dailyLimitMinutes: number;
}

export interface Limits {
  overallDailyMinutes: number;
  resetTime: string;
  perApp: Record<string, AppLimit>;
  timeFrame: TimeFrameSchedule;
  /** yyyy-MM-dd date string. When it matches today, all limits and schedule are bypassed. */
  allowAllDayDate: string | null;
  /** When true the TV is immediately blocked regardless of schedule, limits, or bonus time. */
  instantLocked: boolean;
}

export function defaultLimits(): Limits {
  return {
    overallDailyMinutes: DEFAULT_OVERALL_MINUTES,
    resetTime: '00:00',
    perApp: {},
    timeFrame: DEFAULT_TIME_FRAME_SCHEDULE,
    allowAllDayDate: null,
    instantLocked: false,
  };
}
