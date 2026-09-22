import { useEffect, useState } from 'react';

/** Port of SproutTokens.kt's rememberScreenPadding(): 16 compact / 32 medium / 64 expanded. */
export function screenPaddingForWidth(widthPx: number): number {
  if (widthPx >= 840) return 64;
  if (widthPx >= 600) return 32;
  return 16;
}

export function useResponsivePadding(): number {
  const [width, setWidth] = useState(() => (typeof window === 'undefined' ? 375 : window.innerWidth));
  useEffect(() => {
    const onResize = () => setWidth(window.innerWidth);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);
  return screenPaddingForWidth(width);
}

/** The redesign's side-rail breakpoint (design/i6c-peach-plum README §4 "Web"). */
export const WIDE_LAYOUT_MIN_WIDTH = 840;

export function useIsWideLayout(): boolean {
  const [isWide, setIsWide] = useState(() => (typeof window === 'undefined' ? false : window.innerWidth >= WIDE_LAYOUT_MIN_WIDTH));
  useEffect(() => {
    const onResize = () => setIsWide(window.innerWidth >= WIDE_LAYOUT_MIN_WIDTH);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);
  return isWide;
}
