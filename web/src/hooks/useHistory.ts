import { useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import type { UsageSnapshot } from '../models/UsageSnapshot';

function isoDaysAgo(daysAgo: number): string {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}

export function useHistory(familyId: string | null) {
  const [snapshots, setSnapshots] = useState<UsageSnapshot[]>([]);

  useEffect(() => {
    if (!familyId) {
      setSnapshots([]);
      return;
    }
    const byDate = new Map<string, UsageSnapshot>();
    const dates = Array.from({ length: 7 }, (_, i) => isoDaysAgo(i));
    const unsubs = dates.map((date) =>
      repo.subscribeUsage(familyId, date, (snap) => {
        byDate.set(date, snap);
        setSnapshots(dates.map((d) => byDate.get(d)).filter((s): s is UsageSnapshot => s !== undefined).sort((a, b) => (a.date < b.date ? 1 : -1)));
      }),
    );
    return () => unsubs.forEach((u) => u());
  }, [familyId]);

  return snapshots;
}
