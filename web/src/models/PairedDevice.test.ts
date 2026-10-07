import { describe, expect, it } from 'vitest';
import { heartbeatOf, worstHeartbeatDevice, type PairedDevice } from './PairedDevice';

const now = new Date('2026-10-07T18:00:00Z');
const minsAgo = (m: number) => new Date(now.getTime() - m * 60_000);
const tv = (id: string, lastSeen: Date | null): PairedDevice => ({ id, name: id, lastSeen });

describe('PairedDevice heartbeat', () => {
  it('classifies by lastSeen age (15-minute threshold)', () => {
    expect(heartbeatOf(tv('a', null), now)).toBe('NEVER_SEEN');
    expect(heartbeatOf(tv('a', minsAgo(4)), now)).toBe('ACTIVE');
    expect(heartbeatOf(tv('a', minsAgo(15)), now)).toBe('ACTIVE');
    expect(heartbeatOf(tv('a', minsAgo(16)), now)).toBe('NOT_RESPONDING');
  });

  it('worstHeartbeatDevice surfaces a not-responding TV over healthy ones', () => {
    const devices = [tv('ok', minsAgo(1)), tv('dead', minsAgo(90)), tv('new', null)];
    expect(worstHeartbeatDevice(devices, now)?.id).toBe('dead');
    expect(worstHeartbeatDevice([], now)).toBeNull();
  });
});
