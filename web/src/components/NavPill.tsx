import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumShadow, peachPlumSizes } from '../theme/tokens';

export type TodayRoute = 'today' | 'rules' | 'family';

/** Bottom nav pill: Today · Rules · Family + a round peach Unlock button. */
export function NavPill({
  active,
  onNavigate,
  onUnlock,
}: {
  active: TodayRoute;
  onNavigate: (route: TodayRoute) => void;
  onUnlock: () => void;
}) {
  const { t } = useTranslation();
  const tabs: { route: TodayRoute; label: string }[] = [
    { route: 'today', label: t('today.navToday') },
    { route: 'rules', label: t('today.navRules') },
    { route: 'family', label: t('today.navFamily') },
  ];

  return (
    <nav
      style={{
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        margin: '0 22px 24px',
        padding: 6,
        borderRadius: 999,
        background: peachPlumColor.ink,
        boxShadow: peachPlumShadow.navPill,
      }}
    >
      {tabs.map((tab) => {
        const selected = active === tab.route;
        return (
          <button
            key={tab.route}
            aria-current={selected ? 'page' : undefined}
            onClick={() => onNavigate(tab.route)}
            style={{
              flexGrow: 1,
              height: peachPlumSizes.navItemHeight,
              borderRadius: 999,
              border: 0,
              background: selected ? 'rgba(255,255,255,0.12)' : 'transparent',
              color: selected ? peachPlumColor.surface : peachPlumColor.navInactive,
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 13,
              fontWeight: selected ? 600 : 500,
            }}
          >
            {tab.label}
          </button>
        );
      })}
      <button
        aria-label={t('today.unlockAria')}
        onClick={onUnlock}
        style={{
          width: peachPlumSizes.navItemHeight,
          height: peachPlumSizes.navItemHeight,
          borderRadius: '50%',
          border: 0,
          background: peachPlumColor.peach,
          color: peachPlumColor.ink,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <rect x="4" y="11" width="16" height="10" rx="2" />
          <path d="M8 11V7a4 4 0 0 1 7.5-2" />
        </svg>
      </button>
    </nav>
  );
}
