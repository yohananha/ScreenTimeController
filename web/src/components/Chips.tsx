import { peachPlumColor } from '../theme/tokens';

/** Selectable chip row — Unlock's "How much time" / "Applies to" (design/i6c-peach-plum README §3 "Chip"). */
export function ChipGroup<T extends string | number>({
  options,
  selected,
  onSelect,
  label,
}: {
  options: T[];
  selected: T;
  onSelect: (value: T) => void;
  label: (value: T) => string;
}) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {options.map((opt) => {
        const isSel = opt === selected;
        return (
          <button
            key={opt}
            aria-pressed={isSel}
            onClick={() => onSelect(opt)}
            style={{
              height: 44,
              padding: '0 18px',
              borderRadius: 999,
              border: `1.5px solid ${isSel ? peachPlumColor.ink : peachPlumColor.hairline}`,
              background: isSel ? peachPlumColor.ink : peachPlumColor.surface,
              color: isSel ? peachPlumColor.surface : peachPlumColor.ink,
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 14,
              fontWeight: isSel ? 600 : 500,
              whiteSpace: 'nowrap',
            }}
          >
            {label(opt)}
          </button>
        );
      })}
    </div>
  );
}
