import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./app', () => ({ db: {}, auth: { currentUser: null } }));
vi.mock('./messaging', () => ({ getMessagingIfSupported: vi.fn(async () => ({})) }));
vi.mock('firebase/messaging', () => ({
  getToken: vi.fn(async () => 'tok-1'),
  deleteToken: vi.fn(async () => true),
  onMessage: vi.fn(),
}));
vi.mock('firebase/firestore', () => ({
  doc: vi.fn((_db: unknown, ...path: string[]) => path.join('/')),
  setDoc: vi.fn(async () => undefined),
  arrayUnion: vi.fn((t: string) => ({ union: t })),
  arrayRemove: vi.fn((t: string) => ({ remove: t })),
}));

import { setDoc } from 'firebase/firestore';
import { deleteToken } from 'firebase/messaging';
import { disableNotifications, enableNotifications, syncNotificationToken } from './push';

const g = globalThis as { Notification?: unknown };
const original = g.Notification;

function setPermission(permission: string) {
  g.Notification = { permission, requestPermission: vi.fn(async () => permission) };
}

describe('push token storage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(navigator, 'serviceWorker', {
      value: { register: vi.fn(async () => ({})) },
      configurable: true,
    });
  });
  afterEach(() => {
    g.Notification = original;
  });

  it('enableNotifications saves the token to the user’s private push doc, never the family doc', async () => {
    setPermission('granted');
    expect(await enableNotifications('uid-1')).toBe(true);
    expect(setDoc).toHaveBeenCalledWith('users/uid-1/private/push', { tokens: { union: 'tok-1' } }, { merge: true });
  });

  it('syncNotificationToken never prompts and does nothing without a prior grant', async () => {
    setPermission('default');
    await syncNotificationToken('uid-1');
    expect(setDoc).not.toHaveBeenCalled();
  });

  it('disableNotifications removes the token then invalidates it with FCM', async () => {
    setPermission('granted');
    await disableNotifications('uid-1');
    expect(setDoc).toHaveBeenCalledWith('users/uid-1/private/push', { tokens: { remove: 'tok-1' } }, { merge: true });
    expect(deleteToken).toHaveBeenCalled();
  });
});
