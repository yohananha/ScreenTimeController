import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useRequests } from './useRequests';
import * as repo from '../firebase/firestoreRepository';
import type { TimeRequest } from '../models/TimeRequest';

vi.mock('../firebase/firestoreRepository');

function pendingRequest(id: string, minutes: number): TimeRequest {
  return {
    id,
    appPackage: 'com.x',
    requestedMinutes: minutes,
    status: 'Pending',
    approvedMinutes: null,
    createdAt: new Date(),
    respondedAt: null,
  };
}

describe('useRequests', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(repo.subscribeRecentRequests).mockImplementation((_familyId, cb) => {
      cb([]);
      return () => {};
    });
  });

  it('approve calls respondToRequest with requested minutes by default', () => {
    const { result } = renderHook(() => useRequests('fam-1'));
    act(() => result.current.approve(pendingRequest('r1', 20)));
    expect(repo.respondToRequest).toHaveBeenCalledWith('fam-1', 'r1', 20);
  });

  it('approve with override minutes uses the override', () => {
    const { result } = renderHook(() => useRequests('fam-1'));
    act(() => result.current.approve(pendingRequest('r1', 20), 5));
    expect(repo.respondToRequest).toHaveBeenCalledWith('fam-1', 'r1', 5);
  });

  it('deny calls respondToRequest with null minutes', () => {
    const { result } = renderHook(() => useRequests('fam-1'));
    act(() => result.current.deny(pendingRequest('r1', 20)));
    expect(repo.respondToRequest).toHaveBeenCalledWith('fam-1', 'r1', null);
  });

  it('actions are no-ops without a family id', () => {
    const { result } = renderHook(() => useRequests(null));
    act(() => {
      result.current.approve(pendingRequest('r1', 20));
      result.current.deny(pendingRequest('r1', 20));
    });
    expect(repo.respondToRequest).not.toHaveBeenCalled();
  });
});
