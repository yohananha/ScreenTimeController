import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { heartbeatOf, type PairedDevice } from '../models/PairedDevice';
import { formatRelativeTime } from '../i18n/format';
import { peachPlumColor } from '../theme/tokens';

/** Re-renders once a minute so "last seen" ages and flips to not-responding without new data. */
function useNow(intervalMs = 60_000): Date {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}

/**
 * "Protection on" / "Not responding · last seen 2 hours ago" / "Waiting for
 * the TV to check in" — from the TV's heartbeat (devices/{id}.lastSeen).
 * Replaces a hardcoded "Online" that showed even when enforcement was off.
 */
export function DeviceHeartbeat({
  device,
  okColor,
  dotSize = 7,
  fontSize = 13,
}: {
  device: PairedDevice;
  okColor: string;
  dotSize?: number;
  fontSize?: number;
}) {
  const { t } = useTranslation();
  const now = useNow();
  const status = heartbeatOf(device, now);
  const color = status === 'ACTIVE' ? okColor : status === 'NOT_RESPONDING' ? '#FFB4A8' : peachPlumColor.navInactive;
  const dot = status === 'ACTIVE' ? peachPlumColor.ok : status === 'NOT_RESPONDING' ? peachPlumColor.over : peachPlumColor.navInactive;
  const label =
    status === 'ACTIVE'
      ? t('settings.protectionOn')
      : status === 'NOT_RESPONDING'
        ? t('settings.notResponding', { ago: formatRelativeTime(device.lastSeen!) })
        : t('settings.waitingForCheckIn');

  return (
    <span role="status" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize, fontWeight: 500, color }}>
      <span style={{ width: dotSize, height: dotSize, borderRadius: '50%', background: dot, flexShrink: 0 }} />
      {label}
    </span>
  );
}
