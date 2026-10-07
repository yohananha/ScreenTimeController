import { beforeEach, describe, expect, it, vi } from 'vitest';

const order: string[] = [];

vi.mock('./app', () => ({ db: {}, auth: { currentUser: { uid: 'uid-1' } } }));
vi.mock('./push', () => ({
  disableNotifications: vi.fn(async () => {
    order.push('disable');
  }),
}));
vi.mock('firebase/auth', () => ({
  GoogleAuthProvider: vi.fn(),
  onAuthStateChanged: vi.fn(),
  signInWithPopup: vi.fn(),
  signOut: vi.fn(async () => {
    order.push('signOut');
  }),
}));
vi.mock('firebase/firestore', () => ({ doc: vi.fn(), setDoc: vi.fn() }));
vi.mock('./firestoreRepository', () => ({
  deleteAccount: vi.fn(async () => {
    order.push('deleteAccount');
  }),
}));

import { signOut as fbSignOut } from 'firebase/auth';
import { disableNotifications } from './push';
import { deleteAccount } from './firestoreRepository';
import { deleteAccountAndSignOut, signOut } from './authRepository';

describe('signOut', () => {
  beforeEach(() => {
    order.length = 0;
    vi.clearAllMocks();
  });

  it('unregisters the push token while still signed in, then signs out', async () => {
    await signOut();
    expect(disableNotifications).toHaveBeenCalledWith('uid-1');
    expect(order).toEqual(['disable', 'signOut']);
  });

  it('still signs out when unregistering the token fails', async () => {
    vi.mocked(disableNotifications).mockRejectedValueOnce(new Error('offline'));
    await signOut();
    expect(fbSignOut).toHaveBeenCalled();
  });

  it('deleteAccountAndSignOut: unregisters push, deletes server-side, then signs out', async () => {
    await deleteAccountAndSignOut();
    expect(order).toEqual(['disable', 'deleteAccount', 'signOut']);
  });

  it('deleteAccountAndSignOut stays signed in if the server delete fails (so the user can retry)', async () => {
    vi.mocked(deleteAccount).mockRejectedValueOnce(new Error('offline'));
    await expect(deleteAccountAndSignOut()).rejects.toThrow('offline');
    expect(fbSignOut).not.toHaveBeenCalled();
  });
});
