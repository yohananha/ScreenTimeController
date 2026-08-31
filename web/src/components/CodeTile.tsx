import { colors } from '../theme/colors';

/** 6 cream-on-dark digit tiles used in the active unlock-code hero card. */
export function CodeTilesRow({ code }: { code: string }) {
  return (
    // dir="ltr" keeps digit order stable under RTL — a Row would otherwise reverse it.
    <div dir="ltr" style={{ display: 'flex', gap: 9, width: '100%' }}>
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          style={{
            flex: 1,
            height: 78,
            borderRadius: 18,
            background: colors.background,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: 'Fredoka, sans-serif',
            fontWeight: 600,
            fontSize: 44,
            color: colors.ink,
          }}
        >
          {code[i] ?? '-'}
        </div>
      ))}
    </div>
  );
}
