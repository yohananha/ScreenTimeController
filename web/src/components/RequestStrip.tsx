import { peachPlumColor, peachPlumSizes } from '../theme/tokens';

/** Pending time request, inline on Today (design/i6c-peach-plum tokens.json#sizes.requestStrip). */
export function RequestStrip({
  title,
  subtitle,
  onApprove,
  onDeny,
  approveAria,
  denyAria,
}: {
  title: string;
  subtitle: string;
  onApprove: () => void;
  onDeny: () => void;
  approveAria: string;
  denyAria: string;
}) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '12px 12px 12px 16px',
        borderRadius: peachPlumSizes.requestStrip.radius,
        minHeight: peachPlumSizes.requestStrip.minHeight,
        background: peachPlumColor.ink,
        color: peachPlumColor.surface,
      }}
    >
      <span style={{ display: 'flex', flexDirection: 'column', gap: 2, flexGrow: 1 }}>
        <span style={{ fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 15, fontWeight: 600 }}>{title}</span>
        <span style={{ fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 12, fontWeight: 500, color: 'rgba(255,255,255,0.78)' }}>{subtitle}</span>
      </span>
      <button
        aria-label={approveAria}
        onClick={onApprove}
        style={{
          width: 44,
          height: 44,
          borderRadius: '50%',
          border: 0,
          background: peachPlumColor.surface,
          color: peachPlumColor.ink,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M5 12l5 5L20 7" />
        </svg>
      </button>
      <button
        aria-label={denyAria}
        onClick={onDeny}
        style={{
          width: 44,
          height: 44,
          borderRadius: '50%',
          border: '1.5px solid rgba(255,255,255,0.5)',
          background: 'transparent',
          color: peachPlumColor.surface,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      </button>
    </div>
  );
}
