import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';

/** Settings row used on Rules and Family (design/i6c-peach-plum README §3 "Settings row"). */
export function SettingsRow({
  label,
  value,
  onClick,
  showTopBorder = false,
}: {
  label: string;
  value?: string;
  onClick?: () => void;
  showTopBorder?: boolean;
}) {
  const { i18n } = useTranslation();
  return (
    <button
      onClick={onClick}
      disabled={!onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        width: '100%',
        minHeight: 60,
        padding: '10px 0',
        border: 0,
        borderTop: showTopBorder ? `1px solid ${peachPlumColor.hairline}` : 0,
        background: 'transparent',
        color: peachPlumColor.ink,
        textAlign: 'start',
        fontFamily: 'Rubik, system-ui, sans-serif',
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      <span style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <span style={{ fontSize: 16, fontWeight: 600 }}>{label}</span>
        {value && <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{value}</span>}
      </span>
      {onClick && (
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke={peachPlumColor.navInactive}
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
          style={{ flexShrink: 0 }}
        >
          <path d={i18n.dir() === 'rtl' ? 'M15 6l-6 6 6 6' : 'M9 6l6 6-6 6'} />
        </svg>
      )}
    </button>
  );
}
