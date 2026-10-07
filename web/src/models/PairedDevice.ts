export const DEFAULT_DEVICE_NAME = 'Android TV';

/** Three missed 5-minute heartbeats (see TvHeartbeat on the TV). */
export const HEARTBEAT_STALE_AFTER_MS = 15 * 60 * 1000;

export interface PairedDevice {
  id: string;
  name: string;
  /**
   * The TV's last heartbeat (server time). Its enforcement service stamps it
   * every few minutes while running, so a stale value means protection is
   * off — service disabled, app stopped/uninstalled, or the TV offline.
   */
  lastSeen: Date | null;
}

export type Heartbeat = 'ACTIVE' | 'NOT_RESPONDING' | 'NEVER_SEEN';

/** Mirrors PairedDevice.heartbeatAt in shared/.../model/PairedDevice.kt. */
export function heartbeatOf(device: PairedDevice, now: Date): Heartbeat {
  if (!device.lastSeen) return 'NEVER_SEEN';
  return now.getTime() - device.lastSeen.getTime() > HEARTBEAT_STALE_AFTER_MS ? 'NOT_RESPONDING' : 'ACTIVE';
}

/** The device to surface in a one-line summary: any not-responding TV beats a healthy one. */
export function worstHeartbeatDevice(devices: PairedDevice[], now: Date): PairedDevice | null {
  const rank: Record<Heartbeat, number> = { NOT_RESPONDING: 2, NEVER_SEEN: 1, ACTIVE: 0 };
  return devices.reduce<PairedDevice | null>(
    (worst, d) => (worst === null || rank[heartbeatOf(d, now)] > rank[heartbeatOf(worst, now)] ? d : worst),
    null,
  );
}
