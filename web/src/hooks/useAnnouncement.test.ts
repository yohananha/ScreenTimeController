import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useAnnouncement } from './useAnnouncement';
import * as repo from '../firebase/firestoreRepository';
import type { Announcement } from '../models/Announcement';

vi.mock('../firebase/firestoreRepository');

function announcement(id: string): Announcement {
  return { id, title: 'Hi', body: 'Tell us what you think', ctaEmail: 'dev@example.com', emailSubject: null };
}

function emit(value: Announcement | null) {
  vi.mocked(repo.subscribeAnnouncement).mockImplementation((cb) => {
    cb(value);
    return () => {};
  });
}

describe('useAnnouncement', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
  });

  it('shows nothing when there is no active announcement', () => {
    emit(null);
    const { result } = renderHook(() => useAnnouncement());
    expect(result.current.announcement).toBeNull();
  });

  it('shows a new announcement', () => {
    emit(announcement('a1'));
    const { result } = renderHook(() => useAnnouncement());
    expect(result.current.announcement?.id).toBe('a1');
  });

  it('hides an announcement this browser already dismissed', () => {
    localStorage.setItem('announcement_seen_id', 'a1');
    emit(announcement('a1'));
    const { result } = renderHook(() => useAnnouncement());
    expect(result.current.announcement).toBeNull();
  });

  it('dismiss hides it and persists the id', () => {
    emit(announcement('a1'));
    const { result } = renderHook(() => useAnnouncement());
    act(() => result.current.dismiss());
    expect(result.current.announcement).toBeNull();
    expect(localStorage.getItem('announcement_seen_id')).toBe('a1');
  });

  it('a new id shows again after an older one was dismissed', () => {
    localStorage.setItem('announcement_seen_id', 'a1');
    emit(announcement('a2'));
    const { result } = renderHook(() => useAnnouncement());
    expect(result.current.announcement?.id).toBe('a2');
  });
});
