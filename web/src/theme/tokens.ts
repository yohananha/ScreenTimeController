/**
 * Generated from design/i6c-peach-plum/tokens.json ("Peach & plum", direction I6c).
 * This is the source of truth for the redesign — do not hand-type hex/number
 * values elsewhere; add to or edit tokens.json first, then mirror the change
 * here and in tv/.../ui/theme/PeachPlumTokens.kt so the two stacks can't drift.
 *
 * These tokens are additive during the migration (see README §7): existing
 * screens keep using colors.ts/typography.ts/spacing.ts/radius.ts's original
 * Sprout-era exports until each one is rebuilt against this palette. Once
 * nothing references the old exports they get removed and this file's values
 * move into colors.ts/typography.ts/spacing.ts/radius.ts directly.
 */

export const peachPlumFont = {
  family: 'Rubik',
  weights: [400, 500, 600] as const,
  googleFonts: 'https://fonts.googleapis.com/css2?family=Rubik:wght@400;500;600;700&display=swap',
} as const;

export const peachPlumColor = {
  ground: '#FFF6EE',
  ink: '#2A1E2E',
  muted: '#6E5F66',
  hairline: '#F0E3D8',
  tint: '#FFEDE0',
  surface: '#FFFFFF',
  peach: '#FFB088',
  rose: '#F79AC0',
  ok: '#12A87A',
  over: '#B3261E',
  navInactive: '#B9A9B5',
  okContainer: '#DFF6EC',
  okText: '#0F6B4E',
  overContainer: '#FBE2DF',
  tv: {
    ground: '#1E1622',
    ink: '#FFF6EE',
    muted: '#B9A9B5',
    surface: 'rgba(255,255,255,0.08)',
    line: 'rgba(255,255,255,0.16)',
    okText: '#9FE9CE',
    errorContainer: 'rgba(179,38,30,0.22)',
    errorLine: '#D9534F',
    errorText: '#FFD3CD',
  },
  glow: {
    peach: 'rgba(255,176,136,0.24)',
    rose: 'rgba(247,154,192,0.20)',
    tvPeach: 'rgba(255,176,136,0.16)',
    tvRose: 'rgba(247,154,192,0.14)',
  },
} as const;

export const peachPlumType = {
  phone: {
    gauge: { size: 56, weight: 600, letterSpacing: '-0.03em' },
    gaugeWord: { size: 16, weight: 500 },
    title: { size: 30, weight: 600, letterSpacing: '-0.03em' },
    heading: { size: 18, weight: 600, letterSpacing: '-0.01em' },
    bodyStrong: { size: 16, weight: 600 },
    body: { size: 15, weight: 500 },
    label: { size: 14, weight: 600 },
    caption: { size: 13, weight: 500 },
    nav: { size: 13, weight: 600 },
    sheetTitle: { size: 24, weight: 600, letterSpacing: '-0.02em' },
    codeTile: { size: 40, weight: 600 },
  },
  web: {
    title: { size: 34, weight: 600, letterSpacing: '-0.03em' },
    sectionLabel: { size: 15, weight: 600 },
  },
} as const;

export const peachPlumSpacing = {
  scale: [4, 8, 12, 16, 22, 26, 36],
  phoneGutter: 22,
  cardPadding: 24,
  phoneTopInset: 56,
  webGutter: 40,
} as const;

export const peachPlumRadius = {
  dot: 3,
  iconTile: 12,
  tile: 14,
  input: 18,
  strip: 20,
  card: 22,
  sheet: 28,
  pill: 999,
} as const;

export const peachPlumShadow = {
  card: '0 1px 2px rgba(42,30,46,0.04), 0 10px 30px rgba(42,30,46,0.06)',
  pillLight: '0 1px 2px rgba(42,30,46,0.05), 0 6px 18px rgba(42,30,46,0.07)',
  navPill: '0 14px 32px rgba(42,30,46,0.25)',
  sheet: '0 -12px 40px rgba(42,30,46,0.22)',
} as const;

export const peachPlumSizes = {
  touchTargetMin: 44,
  navPillHeight: 58,
  navItemHeight: 46,
  modeTile: { height: 84, radius: 22 },
  requestStrip: { minHeight: 68, radius: 20 },
  arc: { width: 300, height: 170, radius: 120, stroke: 16, cx: 150, cy: 150 },
  webRail: { width: 96 },
} as const;

export const peachPlumMotion = {
  livePulse: { duration: '2s', easing: 'ease-out' },
  waitPulse: { duration: '1.8s', easing: 'ease-in-out' },
  arcFill: { duration: '600ms', easing: 'ease-out' },
} as const;
