import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';

/** Selectable chip row used in Requests amount chooser and Codes settings. */
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
    <div style={{ display: 'flex', gap: 8, width: '100%' }}>
      {options.map((opt) => {
        const isSel = opt === selected;
        return (
          <button
            key={opt}
            onClick={() => onSelect(opt)}
            style={{
              flex: 1,
              background: isSel ? colors.ink : colors.surface,
              color: isSel ? colors.background : colors.ink,
              border: `1px solid ${isSel ? colors.ink : colors.outline}`,
              borderRadius: radius.input,
              padding: '11px 0',
              ...typography.label,
            }}
          >
            {label(opt)}
          </button>
        );
      })}
    </div>
  );
}
