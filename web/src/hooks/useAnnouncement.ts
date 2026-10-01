import { useCallback, useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import type { Announcement } from '../models/Announcement';

const SEEN_KEY = 'announcement_seen_id';

function readSeenId(): string | null {
  try {
    return localStorage.getItem(SEEN_KEY);
  } catch {
    return null;
  }
}

function writeSeenId(id: string) {
  try {
    localStorage.setItem(SEEN_KEY, id);
  } catch {
    // Storage blocked (private mode etc.) — the message may show again next visit.
  }
}

/** The current developer announcement, unless this browser already dismissed that id. */
export function useAnnouncement() {
  const [announcement, setAnnouncement] = useState<Announcement | null>(null);
  const [seenId, setSeenId] = useState<string | null>(readSeenId);

  useEffect(() => repo.subscribeAnnouncement(setAnnouncement), []);

  const dismiss = useCallback(() => {
    if (!announcement) return;
    writeSeenId(announcement.id);
    setSeenId(announcement.id);
  }, [announcement]);

  return {
    announcement: announcement && announcement.id !== seenId ? announcement : null,
    dismiss,
  };
}
