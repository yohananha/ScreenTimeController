import { peachPlumColor } from '../theme/tokens';

/** Unlock code display tiles (design/i6c-peach-plum README §3 "Code tiles"). Always LTR. */
export function CodeTilesRow({ code }: { code: string }) {
  return (
    // dir="ltr" keeps digit order stable under RTL — a Row would otherwise reverse it.
    <div dir="ltr" style={{ display: 'flex', gap: 10, width: '100%' }}>
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          style={{
            flexGrow: 1,
            height: 84,
            borderRadius: 18,
            background: peachPlumColor.surface,
            border: `1.5px solid ${peachPlumColor.hairline}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: 'Rubik, system-ui, sans-serif',
            fontWeight: 600,
            fontSize: 40,
            letterSpacing: '-0.02em',
            color: peachPlumColor.ink,
          }}
        >
          {code[i] ?? '-'}
        </div>
      ))}
    </div>
  );
}
