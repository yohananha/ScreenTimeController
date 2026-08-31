/**
 * Port of SproutTypography.kt's SproutTypeScale.
 * Rubik -> display/title/headline; Nunito Sans stands in for Varela Round on
 * body/label/caption (Varela Round is display-only and reads poorly at body
 * sizes on web; Nunito Sans matches the design-handoff doc's own body font).
 */
export const typography = {
  display: { fontFamily: 'Fredoka, sans-serif', fontWeight: 600, fontSize: '34px', lineHeight: '36px' },
  title: { fontFamily: 'Fredoka, sans-serif', fontWeight: 600, fontSize: '24px', lineHeight: '28px' },
  headline: { fontFamily: 'Fredoka, sans-serif', fontWeight: 500, fontSize: '20px', lineHeight: '26px' },
  bodyL: { fontFamily: '"Nunito Sans", sans-serif', fontWeight: 600, fontSize: '16px', lineHeight: '24px' },
  body: { fontFamily: '"Nunito Sans", sans-serif', fontWeight: 400, fontSize: '15px', lineHeight: '22px' },
  bodyStrong: { fontFamily: '"Nunito Sans", sans-serif', fontWeight: 600, fontSize: '15px', lineHeight: '22px' },
  label: { fontFamily: '"Nunito Sans", sans-serif', fontWeight: 800, fontSize: '13px', lineHeight: '14px' },
  caption: { fontFamily: '"Nunito Sans", sans-serif', fontWeight: 600, fontSize: '12px', lineHeight: '16px' },
} as const;

export type TypographyToken = keyof typeof typography;
