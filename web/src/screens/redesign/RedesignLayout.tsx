import { useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../../theme/tokens';
import { useIsWideLayout } from '../../theme/breakpoints';
import { NavPill, type TodayRoute } from '../../components/NavPill';
import { SideRail } from '../../components/SideRail';
import { UnlockSheet } from '../../components/UnlockSheet';

/**
 * Shared chrome for Today/Rules/Family (design/i6c-peach-plum): the glow
 * background, the NavPill (<840px) or SideRail (≥840px), and the Unlock
 * sheet, which any of the three screens can open via `openUnlock`.
 */
export function RedesignLayout({
  active,
  familyId,
  children,
}: {
  active: TodayRoute;
  familyId: string;
  children: (ctx: { isWide: boolean; openUnlock: () => void }) => ReactNode;
}) {
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const isWide = useIsWideLayout();
  const [unlockOpen, setUnlockOpen] = useState(false);

  const goTo = (route: TodayRoute) => navigate(`/${route}`);

  return (
    <div
      dir={i18n.dir()}
      style={{
        position: 'relative',
        minHeight: '100vh',
        display: 'flex',
        flexDirection: isWide ? 'row' : 'column',
        background: peachPlumColor.ground,
        color: peachPlumColor.ink,
        fontFamily: 'Rubik, system-ui, sans-serif',
        overflow: 'hidden',
      }}
    >
      {/* Decorative glows are pinned to physical corners in both languages —
          screens/Today-HE.html keeps the same left/right as the EN version,
          it does not mirror them like the rest of the layout. Physical
          `left`/`right` (not insetInlineStart/End) is deliberate here. */}
      <div
        aria-hidden="true"
        style={{
          position: 'absolute',
          top: -220,
          left: -160,
          width: 560,
          height: 560,
          borderRadius: '50%',
          background: `radial-gradient(closest-side, ${peachPlumColor.glow.peach}, rgba(255,255,255,0))`,
          pointerEvents: 'none',
        }}
      />
      <div
        aria-hidden="true"
        style={{
          position: 'absolute',
          top: -120,
          right: -220,
          width: 520,
          height: 520,
          borderRadius: '50%',
          background: `radial-gradient(closest-side, ${peachPlumColor.glow.rose}, rgba(255,255,255,0))`,
          pointerEvents: 'none',
        }}
      />

      {isWide && <SideRail active={active} onNavigate={goTo} onUnlock={() => setUnlockOpen(true)} />}

      <div style={{ position: 'relative', flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflowY: 'auto' }}>
        {children({ isWide, openUnlock: () => setUnlockOpen(true) })}
      </div>

      {!isWide && <NavPill active={active} onNavigate={goTo} onUnlock={() => setUnlockOpen(true)} />}

      {unlockOpen && <UnlockSheet familyId={familyId} onClose={() => setUnlockOpen(false)} />}
    </div>
  );
}
