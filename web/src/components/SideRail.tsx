import type { ReactElement } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumSizes } from '../theme/tokens';
import type { TodayRoute } from './NavPill';

const ICONS: Record<TodayRoute, ReactElement> = {
  today: <path d="M3 11l9-8 9 8v9a2 2 0 0 1-2 2h-4v-6H9v6H5a2 2 0 0 1-2-2z" />,
  rules: <path d="M4 6h16M4 12h10M4 18h7" />,
  family: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <circle cx="17" cy="9" r="2.5" />
      <path d="M15.5 14a5 5 0 0 1 6 5" />
    </>
  ),
};

/** ≥840px side rail — the wide-viewport counterpart to NavPill (design/i6c-peach-plum README §3 "Web rail"). */
export function SideRail({
  active,
  onNavigate,
  onUnlock,
}: {
  active: TodayRoute;
  onNavigate: (route: TodayRoute) => void;
  onUnlock: () => void;
}) {
  const { t } = useTranslation();
  const items: { route: TodayRoute; label: string }[] = [
    { route: 'today', label: t('today.navToday') },
    { route: 'rules', label: t('today.navRules') },
    { route: 'family', label: t('today.navFamily') },
  ];

  return (
    <nav
      style={{
        width: peachPlumSizes.webRail.width,
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        padding: '28px 0',
        boxSizing: 'border-box',
        background: peachPlumColor.ink,
        color: peachPlumColor.surface,
      }}
    >
      <span style={{ width: 40, height: 40, borderRadius: 14, background: peachPlumColor.peach, marginBottom: 20 }} />
      {items.map((item) => {
        const selected = active === item.route;
        return (
          <button
            key={item.route}
            aria-current={selected ? 'page' : undefined}
            onClick={() => onNavigate(item.route)}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 6,
              width: 76,
              height: 68,
              borderRadius: 18,
              border: 0,
              background: selected ? 'rgba(255,255,255,0.12)' : 'transparent',
              color: selected ? peachPlumColor.surface : peachPlumColor.navInactive,
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 12,
              fontWeight: selected ? 600 : 500,
            }}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              {ICONS[item.route]}
            </svg>
            {item.label}
          </button>
        );
      })}
      <div style={{ flexGrow: 1 }} />
      <button
        aria-label={t('today.unlockAria')}
        onClick={onUnlock}
        style={{
          width: 56,
          height: 56,
          borderRadius: '50%',
          border: 0,
          background: peachPlumColor.peach,
          color: peachPlumColor.ink,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <rect x="4" y="11" width="16" height="10" rx="2" />
          <path d="M8 11V7a4 4 0 0 1 7.5-2" />
        </svg>
      </button>
    </nav>
  );
}
