export interface UsageSnapshot {
  date: string;
  perAppMillis: Record<string, number>;
}

export function millisFor(snapshot: UsageSnapshot, packageName: string): number {
  return snapshot.perAppMillis[packageName] ?? 0;
}

export function totalMillis(snapshot: UsageSnapshot): number {
  return Object.values(snapshot.perAppMillis).reduce((sum, v) => sum + v, 0);
}
