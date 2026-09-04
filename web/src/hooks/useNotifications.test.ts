import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { useNotificationStatus } from './useNotifications';
import * as push from '../firebase/push';

vi.mock('../firebase/push');

describe('useNotificationStatus', () => {
  const originalNotification = (globalThis as { Notification?: unknown }).Notification;

  beforeEach(() => {
    vi.resetAllMocks();
    (globalThis as { Notification?: unknown }).Notification = { permission: 'default' };
  });

  afterEach(() => {
    (globalThis as { Notification?: unknown }).Notification = originalNotification;
  });

  it('reads the current browser permission as the initial status', () => {
    const { result } = renderHook(() => useNotificationStatus('fam-1'));
    expect(result.current.status).toBe('default');
    expect(result.current.enabling).toBe(false);
  });

  it('reports unsupported when the Notification API is unavailable', () => {
    (globalThis as { Notification?: unknown }).Notification = undefined;
    const { result } = renderHook(() => useNotificationStatus('fam-1'));
    expect(result.current.status).toBe('unsupported');
  });

  it('enable() calls enableNotifications and refreshes status from the (now granted) permission', async () => {
    vi.mocked(push.enableNotifications).mockImplementation(async () => {
      (globalThis as { Notification?: unknown }).Notification = { permission: 'granted' };
      return true;
    });
    const { result } = renderHook(() => useNotificationStatus('fam-1'));

    act(() => result.current.enable());
    expect(result.current.enabling).toBe(true);

    await waitFor(() => expect(result.current.enabling).toBe(false));
    expect(push.enableNotifications).toHaveBeenCalledWith('fam-1');
    expect(result.current.status).toBe('granted');
  });

  it('enable() still resolves status on failure instead of leaving enabling stuck', async () => {
    vi.mocked(push.enableNotifications).mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useNotificationStatus('fam-1'));

    act(() => result.current.enable());
    await waitFor(() => expect(result.current.enabling).toBe(false));
    expect(result.current.status).toBe('default');
  });
});
