import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';

/** Per-app row on Rules' "App limits" card (design/i6c-peach-plum README §3 "App row"). */
export function AppRow({
  name,
  swatchColor,
  value,
  onClick,
  showTopBorder = false,
}: {
  name: string;
  swatchColor: string;
  value: string;
  onClick: () => void;
  showTopBorder?: boolean;
}) {
  const { i18n } = useTranslation();
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        width: '100%',
        height: 52,
        padding: 0,
        border: 0,
        borderTop: showTopBorder ? `1px solid ${peachPlumColor.hairline}` : 0,
        background: 'transparent',
        color: peachPlumColor.ink,
        textAlign: 'start',
        fontFamily: 'Rubik, system-ui, sans-serif',
        cursor: 'pointer',
      }}
    >
      <span style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 15, fontWeight: 600 }}>
        <span style={{ width: 10, height: 10, borderRadius: 3, background: swatchColor, display: 'inline-block', flexShrink: 0 }} />
        {name}
      </span>
      <span style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>
        {value}
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={peachPlumColor.navInactive} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ flexShrink: 0 }}>
          <path d={i18n.dir() === 'rtl' ? 'M15 6l-6 6 6 6' : 'M9 6l6 6-6 6'} />
        </svg>
      </span>
    </button>
  );
}
