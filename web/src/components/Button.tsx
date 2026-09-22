import type { ButtonHTMLAttributes } from 'react';
import { peachPlumColor } from '../theme/tokens';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement>;

/** Primary pill button — ink fill, white text (design/i6c-peach-plum README §3). */
export function PrimaryButton({ style, ...props }: ButtonProps) {
  return (
    <button
      {...props}
      style={{
        height: 52,
        padding: '0 22px',
        borderRadius: 999,
        border: 0,
        background: peachPlumColor.ink,
        color: peachPlumColor.surface,
        fontFamily: 'Rubik, system-ui, sans-serif',
        fontSize: 16,
        fontWeight: 600,
        opacity: props.disabled ? 0.6 : 1,
        cursor: props.disabled ? 'default' : 'pointer',
        ...style,
      }}
    />
  );
}

/** Secondary pill button — transparent fill, 1.5px ink border. */
export function SecondaryButton({ style, ...props }: ButtonProps) {
  return (
    <button
      {...props}
      style={{
        height: 52,
        padding: '0 22px',
        borderRadius: 999,
        border: `1.5px solid ${peachPlumColor.ink}`,
        background: 'transparent',
        color: peachPlumColor.ink,
        fontFamily: 'Rubik, system-ui, sans-serif',
        fontSize: 16,
        fontWeight: 600,
        opacity: props.disabled ? 0.6 : 1,
        cursor: props.disabled ? 'default' : 'pointer',
        ...style,
      }}
    />
  );
}
