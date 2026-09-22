import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

export interface NavTabDef {
  route: string;
  labelKey: string;
  icon: string;
}

export const NAV_TABS: NavTabDef[] = [
  { route: 'limits', labelKey: 'nav.limits', icon: '🕐' },
  { route: 'settings', labelKey: 'nav.settings', icon: '⚙️' },
];

export function SproutBottomNavBar({
  selectedRoute,
  onTabClick,
}: {
  selectedRoute: string;
  onTabClick: (route: string) => void;
}) {
  const { t } = useTranslation();
  return (
    <div
      style={{
        width: '100%',
        borderTop: `1px solid ${colors.outline}`,
        background: colors.surface,
        position: 'sticky',
        bottom: 0,
        flexShrink: 0,
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-around',
          alignItems: 'center',
          padding: '8px 6px calc(10px + env(safe-area-inset-bottom))',
        }}
      >
        {NAV_TABS.map((tab) => {
          const selected = selectedRoute === tab.route || (tab.route === 'limits' && selectedRoute === 'history');
          return (
            <button
              key={tab.route}
              onClick={() => onTabClick(tab.route)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 4,
                width: 72,
                background: 'none',
                border: 'none',
              }}
            >
              <div
                style={{
                  width: 60,
                  height: 32,
                  borderRadius: 999,
                  background: selected ? colors.accentContainer : 'transparent',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 18,
                }}
              >
                {tab.icon}
              </div>
              <span
                style={{
                  ...typography.caption,
                  fontWeight: selected ? 800 : 700,
                  color: selected ? colors.ink : colors.inkMuted,
                }}
              >
                {t(tab.labelKey)}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
