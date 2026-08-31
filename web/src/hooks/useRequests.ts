import { useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import { isActiveGrant, type TimeRequest } from '../models/TimeRequest';

export interface RequestsUiState {
  pending: TimeRequest[];
  active: TimeRequest[];
}

/** Ticks once a second so grant-expiry state (isActiveGrant) re-evaluates live, mirroring Ticker.kt. */
function useTick(periodMs = 1000) {
  const [, setTick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), periodMs);
    return () => clearInterval(id);
  }, [periodMs]);
}

export function useRequests(familyId: string | null) {
  const [all, setAll] = useState<TimeRequest[]>([]);
  useTick();

  useEffect(() => {
    if (!familyId) {
      setAll([]);
      return;
    }
    return repo.subscribeRecentRequests(familyId, setAll);
  }, [familyId]);

  const now = new Date();
  const state: RequestsUiState = {
    pending: all.filter((r) => r.status === 'Pending'),
    // Denied requests and expired grants are dropped entirely — nothing left to act on or show.
    active: all.filter((r) => isActiveGrant(r, now)),
  };

  const approve = (request: TimeRequest, minutes: number = request.requestedMinutes) => {
    if (!familyId) return;
    void repo.respondToRequest(familyId, request.id, minutes);
  };

  const deny = (request: TimeRequest) => {
    if (!familyId) return;
    void repo.respondToRequest(familyId, request.id, null);
  };

  return { state, approve, deny };
}
