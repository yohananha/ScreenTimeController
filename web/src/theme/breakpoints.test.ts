import { describe, expect, it } from 'vitest';
import { screenPaddingForWidth } from './breakpoints';

describe('screenPaddingForWidth', () => {
  it('compact width (360dp) -> 16px padding', () => {
    expect(screenPaddingForWidth(360)).toBe(16);
  });

  it('medium width (600dp) -> 32px padding', () => {
    expect(screenPaddingForWidth(600)).toBe(32);
  });

  it('expanded width (840dp) -> 64px padding', () => {
    expect(screenPaddingForWidth(840)).toBe(64);
  });

  it('padding is always at least 16px', () => {
    for (const width of [0, 100, 359, 599, 839, 2000]) {
      expect(screenPaddingForWidth(width)).toBeGreaterThanOrEqual(16);
    }
  });
});
