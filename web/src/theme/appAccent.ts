/** Port of AppAccent.kt's hash-based per-app accent color picker. */
const appAccents = [
  '#E5483A',
  '#5B6B7B',
  '#2A2730',
  '#4FA98C',
  '#8E86D9',
  '#F2A93B',
  '#B9A8F0',
];

function javaStringHashCode(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) {
    h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  }
  return h;
}

export function appAccentFor(packageName: string): string {
  const hash = javaStringHashCode(packageName);
  const index = (hash < 0 ? -hash : hash) % appAccents.length;
  return appAccents[index]!;
}
