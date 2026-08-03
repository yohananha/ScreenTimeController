import { useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { isActiveGrant, type TimeRequest } from '../models/TimeRequest';

/**
 * Always-on listener that drives the bottom-nav "Requests" badge — so a new
 * pending request or a freshly-granted bonus is visible from any tab, in
 * real time. Mirrors RequestsBadgeViewModel.kt.
 */
export function useRequestsBadge(familyId: string | null) {
  const [all, setAll] = useState<TimeRequest[]>([]);
  const [, setTick] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (!familyId) {
      setAll([]);
      return;
    }
    return repo.subscribeRecentRequests(familyId, setAll);
  }, [familyId]);

  const now = new Date();
  const pendingCount = all.filter((r) => r.status === 'Pending').length;
  const hasUpdates = all.some((r) => r.status === 'Pending' || isActiveGrant(r, now));

  return { pendingCount, hasUpdates };
}
