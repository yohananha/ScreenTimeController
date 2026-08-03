import { describe, expect, it } from 'vitest';
import { colors } from './colors';

/** Guards against silent token drift from the design-doc README's Color Roles table. */
describe('Sprout color tokens', () => {
  it('brand tokens match the design system', () => {
    expect(colors.primary).toBe('#FF6B5E');
    expect(colors.background).toBe('#FCF6F0');
    expect(colors.ink).toBe('#3A2A4D');
    expect(colors.accentContainer).toBe('#ECE5FB');
    expect(colors.positiveDisplay).toBe('#4FCFA1');
    expect(colors.warningDisplay).toBe('#F2A93B');
    expect(colors.overDisplay).toBe('#E5483A');
    expect(colors.outline).toBe('#EADFD4');
  });
});
