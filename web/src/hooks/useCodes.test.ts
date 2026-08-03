import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useCodes } from './useCodes';
import * as repo from '../firebase/firestoreRepository';

vi.mock('../firebase/firestoreRepository');

describe('useCodes', () => {
  beforeEach(() => vi.resetAllMocks());

  it('generate emits isGenerating then active code on success', async () => {
    const code = { code: '123456', extraMinutes: 30, expiresAt: new Date(Date.now() + 3_600_000) };
    vi.mocked(repo.createCode).mockResolvedValue(code);

    const { result } = renderHook(() => useCodes('fam-1'));
    expect(result.current.state).toEqual({ isGenerating: false, active: null, error: null });

    act(() => result.current.generate(30));
    expect(result.current.state.isGenerating).toBe(true);

    await waitFor(() => expect(result.current.state.isGenerating).toBe(false));
    expect(result.current.state.active).toEqual(code);
    expect(result.current.state.error).toBeNull();
  });

  it('generate exposes error message on failure', async () => {
    vi.mocked(repo.createCode).mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useCodes('fam-1'));

    act(() => result.current.generate(15));
    await waitFor(() => expect(result.current.state.error).toBe('boom'));
    expect(result.current.state.active).toBeNull();
  });

  it('generate without familyId reports error and does not call firestore', () => {
    const { result } = renderHook(() => useCodes(null));
    act(() => result.current.generate(15));
    expect(result.current.state.error).toContain('No family');
    expect(repo.createCode).not.toHaveBeenCalled();
  });

  it('dismiss resets the state', async () => {
    vi.mocked(repo.createCode).mockResolvedValue({
      code: '000000',
      extraMinutes: 5,
      expiresAt: new Date(Date.now() + 60_000),
    });
    const { result } = renderHook(() => useCodes('fam-1'));
    act(() => result.current.generate(5));
    await waitFor(() => expect(result.current.state.active).not.toBeNull());

    act(() => result.current.dismiss());
    expect(result.current.state).toEqual({ isGenerating: false, active: null, error: null });
  });
});
