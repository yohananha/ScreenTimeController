import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';

/** Disclosure chevron that always points toward the trailing edge, LTR or RTL. */
export function Chevron({ style }: { style?: CSSProperties }) {
  const { i18n } = useTranslation();
  return <span style={style}>{i18n.dir() === 'rtl' ? '‹' : '›'}</span>;
}

/** Back-navigation chevron — points toward the leading edge, LTR or RTL. */
export function BackChevron({ style }: { style?: CSSProperties }) {
  const { i18n } = useTranslation();
  return <span style={style}>{i18n.dir() === 'rtl' ? '→' : '←'}</span>;
}
