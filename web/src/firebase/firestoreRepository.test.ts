import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./app', () => ({ db: {}, functions: {} }));
vi.mock('firebase/firestore', async (importOriginal) => ({
  ...(await importOriginal<typeof import('firebase/firestore')>()),
  doc: vi.fn((_db: unknown, ...path: string[]) => path.join('/')),
  setDoc: vi.fn(async () => undefined),
  deleteField: vi.fn(() => 'DELETE'),
  onSnapshot: vi.fn(),
}));

import { onSnapshot, setDoc } from 'firebase/firestore';
import { setInstantLock, subscribeInstantLock } from './firestoreRepository';

describe('instant lock (web ↔ TV contract)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 7, 0, 30)); // 00:30 local — UTC may still be Oct 6
  });
  afterEach(() => vi.useRealTimers());

  it('locking stamps today’s LOCAL date, which the TV requires', async () => {
    await setInstantLock('fam', true);
    expect(setDoc).toHaveBeenCalledWith('families/fam/limits/instantLock', { locked: true, date: '2026-10-07' });
  });

  it('unlocking clears the date', async () => {
    await setInstantLock('fam', false);
    expect(setDoc).toHaveBeenCalledWith(
      'families/fam/limits/instantLock',
      { locked: false, date: 'DELETE' },
      { merge: true },
    );
  });

  it('only a lock dated today reads as active', () => {
    const cb = vi.fn();
    subscribeInstantLock('fam', cb);
    const onNext = vi.mocked(onSnapshot).mock.calls[0]![1] as unknown as (snap: { data: () => unknown }) => void;
    onNext({ data: () => ({ locked: true, date: '2026-10-07' }) });
    onNext({ data: () => ({ locked: true }) });
    onNext({ data: () => ({ locked: true, date: '2026-10-06' }) });
    expect(cb.mock.calls.map((c) => c[0])).toEqual([true, false, false]);
  });
});

describe('secureSixDigits', () => {
  it('returns 6 digits from crypto.getRandomValues, never Math.random', async () => {
    const { secureSixDigits } = await import('./firestoreRepository');
    const mathRandom = vi.spyOn(Math, 'random');
    for (let i = 0; i < 200; i++) expect(secureSixDigits()).toMatch(/^\d{6}$/);
    expect(mathRandom).not.toHaveBeenCalled();
  });
});
