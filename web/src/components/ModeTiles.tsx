import type { ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumSizes } from '../theme/tokens';

export type TvMode = 'lock' | 'limits' | 'allow';

const ICONS: Record<TvMode, ReactElement> = {
  lock: (
    <>
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </>
  ),
  limits: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  allow: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1" />
    </>
  ),
};

/**
 * Lock · Limits · Allow tile row (design/i6c-peach-plum tokens.json#sizes.modeTile).
 * `disabled` is the "no TV paired" state from README §4 ("tiles disabled") —
 * there's nothing to switch modes on, so the tiles go inert rather than
 * silently no-op when tapped.
 */
export function ModeTiles({ active, onSelect, disabled = false }: { active: TvMode; onSelect: (mode: TvMode) => void; disabled?: boolean }) {
  const { t } = useTranslation();
  const labels: Record<TvMode, string> = {
    lock: t('today.modeLock'),
    limits: t('today.modeLimits'),
    allow: t('today.modeAllow'),
  };

  return (
    <div role="group" aria-label={t('today.modeGroupAria')} style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 10, opacity: disabled ? 0.5 : 1 }}>
      {(['lock', 'limits', 'allow'] as TvMode[]).map((mode) => {
        const selected = active === mode;
        return (
          <button
            key={mode}
            aria-pressed={selected}
            disabled={disabled}
            onClick={() => onSelect(mode)}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'flex-start',
              justifyContent: 'space-between',
              height: peachPlumSizes.modeTile.height,
              padding: 14,
              borderRadius: peachPlumSizes.modeTile.radius,
              border: `1.5px solid ${selected ? peachPlumColor.ink : peachPlumColor.hairline}`,
              background: selected ? peachPlumColor.ink : peachPlumColor.surface,
              color: selected ? peachPlumColor.surface : peachPlumColor.ink,
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 14,
              fontWeight: 600,
              cursor: disabled ? 'default' : 'pointer',
            }}
          >
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke={selected ? peachPlumColor.peach : peachPlumColor.muted}
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              {ICONS[mode]}
            </svg>
            {labels[mode]}
          </button>
        );
      })}
    </div>
  );
}
