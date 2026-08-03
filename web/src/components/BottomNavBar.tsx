import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

export interface NavTabDef {
  route: string;
  label: string;
  icon: string;
}

export const NAV_TABS: NavTabDef[] = [
  { route: 'limits', label: 'Limits', icon: '🕐' },
  { route: 'requests', label: 'Requests', icon: '🔔' },
  { route: 'codes', label: 'Codes', icon: '⌨' },
  { route: 'family', label: 'Family', icon: '👪' },
];

export function SproutBottomNavBar({
  selectedRoute,
  pendingCount,
  onTabClick,
}: {
  selectedRoute: string;
  pendingCount: number;
  onTabClick: (route: string) => void;
}) {
  return (
    <div style={{ width: '100%', borderTop: `1px solid ${colors.outline}`, background: colors.surface }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-around',
          alignItems: 'center',
          padding: '8px 6px 10px',
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
              <div style={{ position: 'relative' }}>
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
                {tab.route === 'requests' && pendingCount > 0 && (
                  <span
                    style={{
                      position: 'absolute',
                      top: -4,
                      right: 4,
                      minWidth: 16,
                      minHeight: 16,
                      borderRadius: 999,
                      background: colors.overDisplay,
                      color: '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      padding: '1px 4px',
                      fontSize: 10,
                      fontWeight: 800,
                    }}
                  >
                    {pendingCount}
                  </span>
                )}
              </div>
              <span
                style={{
                  ...typography.caption,
                  fontWeight: selected ? 800 : 700,
                  color: selected ? colors.ink : colors.inkMuted,
                }}
              >
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
