export interface OneTimeCode {
  code: string;
  extraMinutes: number;
  expiresAt: Date;
}

export function isExpired(code: OneTimeCode, now: Date = new Date()): boolean {
  return now.getTime() > code.expiresAt.getTime();
}
