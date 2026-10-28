import { useCallback, useEffect, useState } from 'react';

/* global __BUILD_ID__ */

async function fetchLatestBuildId(): Promise<string | null> {
  try {
    const res = await fetch(`/version.json?cb=${Date.now()}`, { cache: 'no-store' });
    if (!res.ok) return null;
    const data = (await res.json()) as { buildId?: unknown };
    return typeof data.buildId === 'string' ? data.buildId : null;
  } catch {
    return null;
  }
}

/**
 * True when a newer build has been deployed than the one this tab is running.
 * Checked on load and whenever the tab becomes visible again. Silent offline.
 * `dismiss` hides the prompt until the next load.
 */
export function useVersionCheck() {
  const [outdated, setOutdated] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const check = async () => {
      const latest = await fetchLatestBuildId();
      if (!cancelled && latest !== null && latest !== __BUILD_ID__) setOutdated(true);
    };
    void check();
    const onVisible = () => {
      if (document.visibilityState === 'visible') void check();
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, []);

  const dismiss = useCallback(() => setOutdated(false), []);
  return { outdated, dismiss };
}
