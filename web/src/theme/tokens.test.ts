import { describe, expect, it } from 'vitest';
import { peachPlumColor, peachPlumRadius, peachPlumSpacing } from './tokens';

/** Guards against drift from design/i6c-peach-plum/tokens.json. */
describe('Peach & plum design tokens', () => {
  it('color roles match tokens.json', () => {
    expect(peachPlumColor.ground).toBe('#FFF6EE');
    expect(peachPlumColor.ink).toBe('#2A1E2E');
    expect(peachPlumColor.muted).toBe('#6E5F66');
    expect(peachPlumColor.hairline).toBe('#F0E3D8');
    expect(peachPlumColor.tint).toBe('#FFEDE0');
    expect(peachPlumColor.peach).toBe('#FFB088');
    expect(peachPlumColor.rose).toBe('#F79AC0');
    expect(peachPlumColor.ok).toBe('#12A87A');
    expect(peachPlumColor.over).toBe('#B3261E');
    expect(peachPlumColor.tv.ground).toBe('#1E1622');
    expect(peachPlumColor.tv.ink).toBe('#FFF6EE');
  });

  it('spacing and radius scales match tokens.json', () => {
    expect(peachPlumSpacing.scale).toEqual([4, 8, 12, 16, 22, 26, 36]);
    expect(peachPlumSpacing.phoneGutter).toBe(22);
    expect(peachPlumSpacing.cardPadding).toBe(24);
    expect(peachPlumRadius.card).toBe(22);
    expect(peachPlumRadius.sheet).toBe(28);
    expect(peachPlumRadius.pill).toBe(999);
  });
});
