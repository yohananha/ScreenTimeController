import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import type { DayOfWeek, TimeFrameSchedule, TimeFrameWindow } from '../models/TimeFrameSchedule';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../models/TimeFrameSchedule';

export interface TimeFrameUiState {
  schedule: TimeFrameSchedule;
  pendingChanges: boolean;
  saving: boolean;
}

const WEEKDAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const WEEKEND: DayOfWeek[] = ['SATURDAY', 'SUNDAY'];

export function useTimeFrame(familyId: string | null) {
  const [state, setState] = useState<TimeFrameUiState>({
    schedule: DEFAULT_TIME_FRAME_SCHEDULE,
    pendingChanges: false,
    saving: false,
  });

  useEffect(() => {
    if (!familyId) return;
    return repo.subscribeTimeFrame(familyId, (schedule) => {
      setState((prev) => (prev.pendingChanges ? prev : { schedule, pendingChanges: false, saving: false }));
    });
  }, [familyId]);

  const setEnabled = useCallback((enabled: boolean) => {
    setState((prev) => ({ ...prev, schedule: { ...prev.schedule, enabled }, pendingChanges: true }));
  }, []);

  const setWindows = useCallback((day: DayOfWeek, windows: TimeFrameWindow[]) => {
    setState((prev) => ({
      ...prev,
      schedule: { ...prev.schedule, windowsByDay: { ...prev.schedule.windowsByDay, [day]: windows } },
      pendingChanges: true,
    }));
  }, []);

  const copyToWeekdays = useCallback((day: DayOfWeek) => {
    setState((prev) => {
      const windows = prev.schedule.windowsByDay[day] ?? [];
      const updated = { ...prev.schedule.windowsByDay };
      for (const d of WEEKDAYS) updated[d] = windows;
      return { ...prev, schedule: { ...prev.schedule, windowsByDay: updated }, pendingChanges: true };
    });
  }, []);

  const copyToWeekend = useCallback((day: DayOfWeek) => {
    setState((prev) => {
      const windows = prev.schedule.windowsByDay[day] ?? [];
      const updated = { ...prev.schedule.windowsByDay };
      for (const d of WEEKEND) updated[d] = windows;
      return { ...prev, schedule: { ...prev.schedule, windowsByDay: updated }, pendingChanges: true };
    });
  }, []);

  const save = useCallback(() => {
    if (!familyId) return;
    setState((prev) => ({ ...prev, saving: true }));
    repo.setTimeFrame(familyId, state.schedule).finally(() => {
      setState((prev) => ({ ...prev, saving: false, pendingChanges: false }));
    });
  }, [familyId, state.schedule]);

  return { state, setEnabled, setWindows, copyToWeekdays, copyToWeekend, save };
}
