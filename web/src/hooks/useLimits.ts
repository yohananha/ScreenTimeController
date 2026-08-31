import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import type { AppLimit } from '../models/Limits';
import { DEFAULT_OVERALL_MINUTES } from '../models/Limits';
import type { InstalledApp } from '../models/InstalledApp';
import type { LockoutMode, LockoutSettings } from '../models/LockoutSettings';
import { defaultLockoutSettings } from '../models/LockoutSettings';
import type { TimeFrameSchedule } from '../models/TimeFrameSchedule';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../models/TimeFrameSchedule';
import { totalMillis } from '../models/UsageSnapshot';

export interface LimitsUiState {
  limits: AppLimit[];
  overallDailyMinutes: number;
  availableApps: InstalledApp[];
  lockout: LockoutSettings;
  timeFrame: TimeFrameSchedule;
  allowAllDayActive: boolean;
  instantLocked: boolean;
  usagePerApp: Record<string, number>;
  totalUsageMillis: number;
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

const initialState: LimitsUiState = {
  limits: [],
  overallDailyMinutes: DEFAULT_OVERALL_MINUTES,
  availableApps: [],
  lockout: defaultLockoutSettings(),
  timeFrame: DEFAULT_TIME_FRAME_SCHEDULE,
  allowAllDayActive: false,
  instantLocked: false,
  usagePerApp: {},
  totalUsageMillis: 0,
};

export function useLimits(familyId: string | null) {
  const [state, setState] = useState<LimitsUiState>(initialState);
  const [writeError, setWriteError] = useState<string | null>(null);

  useEffect(() => {
    if (!familyId) {
      setState(initialState);
      return;
    }
    const partial: Partial<LimitsUiState> = {};
    const commit = () => setState((prev) => ({ ...prev, ...partial }));
    const unsubs = [
      repo.subscribePerAppLimits(familyId, (perApp) => {
        partial.limits = Object.values(perApp).sort((a, b) => a.packageName.localeCompare(b.packageName));
        commit();
      }),
      repo.subscribeOverallLimit(familyId, (minutes) => {
        partial.overallDailyMinutes = minutes;
        commit();
      }),
      repo.subscribeInstalledApps(familyId, (apps) => {
        partial.availableApps = apps;
        commit();
      }),
      repo.subscribeLockout(familyId, (lockout) => {
        partial.lockout = lockout;
        commit();
      }),
      repo.subscribeTimeFrame(familyId, (timeFrame) => {
        partial.timeFrame = timeFrame;
        commit();
      }),
      repo.subscribeAllowAllDay(familyId, (date) => {
        partial.allowAllDayActive = date === todayIso();
        commit();
      }),
      repo.subscribeInstantLock(familyId, (locked) => {
        partial.instantLocked = locked;
        commit();
      }),
      repo.subscribeUsage(familyId, todayIso(), (usage) => {
        partial.usagePerApp = usage.perAppMillis;
        partial.totalUsageMillis = totalMillis(usage);
        commit();
      }),
    ];
    return () => unsubs.forEach((u) => u());
  }, [familyId]);

  const write = useCallback(
    (fn: () => Promise<void>) => {
      fn().catch((e: unknown) => setWriteError(e instanceof Error ? e.message : 'Something went wrong.'));
    },
    [],
  );

  const clearWriteError = useCallback(() => setWriteError(null), []);

  const setLimit = useCallback(
    (packageName: string, minutes: number) => familyId && write(() => repo.setLimit(familyId, packageName, minutes)),
    [familyId, write],
  );
  const removeLimit = useCallback(
    (packageName: string) => familyId && write(() => repo.removeLimit(familyId, packageName)),
    [familyId, write],
  );
  const setOverallLimit = useCallback(
    (minutes: number) => familyId && write(() => repo.setOverallLimit(familyId, minutes)),
    [familyId, write],
  );
  const setLockoutConfig = useCallback(
    (durationMinutes: number, mode: LockoutMode) =>
      familyId && write(() => repo.setLockoutConfig(familyId, durationMinutes, mode)),
    [familyId, write],
  );
  const unlockNow = useCallback(() => familyId && write(() => repo.clearLockout(familyId)), [familyId, write]);

  // Compound writes: selecting a mode clears the others, same order as LimitsViewModel.kt.
  const selectInstantLock = useCallback(
    () =>
      familyId &&
      write(async () => {
        await repo.setAllowAllDay(familyId, null);
        await repo.setInstantLock(familyId, true);
      }),
    [familyId, write],
  );
  const selectAllowAllDay = useCallback(
    () =>
      familyId &&
      write(async () => {
        await repo.setInstantLock(familyId, false);
        await repo.setAllowAllDay(familyId, todayIso());
      }),
    [familyId, write],
  );
  const selectDefaultLimits = useCallback(
    () =>
      familyId &&
      write(async () => {
        await repo.setInstantLock(familyId, false);
        await repo.setAllowAllDay(familyId, null);
      }),
    [familyId, write],
  );

  return {
    state,
    writeError,
    clearWriteError,
    setLimit,
    removeLimit,
    setOverallLimit,
    setLockoutConfig,
    unlockNow,
    selectInstantLock,
    selectAllowAllDay,
    selectDefaultLimits,
  };
}
