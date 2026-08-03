import type { ButtonHTMLAttributes, CSSProperties, ReactNode } from 'react';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  leading?: ReactNode;
  shadow?: boolean;
};

export function SproutPrimaryButton({ leading, shadow, style, children, disabled, ...rest }: ButtonProps) {
  const bg = disabled ? colors.outline : colors.primary;
  const merged: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    border: 'none',
    borderRadius: 999,
    background: bg,
    color: disabled ? colors.inkFaint : colors.onPrimary,
    padding: '13px 22px',
    boxShadow: shadow && !disabled ? '0 8px 18px rgba(255,107,94,.30)' : undefined,
    ...typography.label,
    ...style,
  };
  return (
    <button style={merged} disabled={disabled} {...rest}>
      {leading}
      {children}
    </button>
  );
}

export function SproutGhostButton({ leading, style, children, ...rest }: ButtonProps) {
  const merged: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    background: 'transparent',
    border: `1.5px solid ${colors.outline}`,
    borderRadius: 999,
    color: colors.ink,
    padding: '13px 22px',
    ...typography.label,
    ...style,
  };
  return (
    <button style={merged} {...rest}>
      {leading}
      {children}
    </button>
  );
}

export function SproutDangerButton({ style, children, ...rest }: ButtonProps) {
  const merged: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: 'none',
    borderRadius: 999,
    background: colors.overDisplay,
    color: colors.surface,
    padding: '11px 18px',
    ...typography.label,
    ...style,
  };
  return (
    <button style={merged} {...rest}>
      {children}
    </button>
  );
}
