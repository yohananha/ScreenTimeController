export type TimeRequestStatus = 'Pending' | 'Approved' | 'Denied';

export interface TimeRequest {
  id: string;
  appPackage: string;
  requestedMinutes: number;
  status: TimeRequestStatus;
  approvedMinutes: number | null;
  createdAt: Date;
  respondedAt: Date | null;
}

export function grantExpiresAt(request: TimeRequest): Date | null {
  if (request.status !== 'Approved') return null;
  const grantedMinutes = request.approvedMinutes ?? request.requestedMinutes;
  if (!request.respondedAt) return null;
  return new Date(request.respondedAt.getTime() + grantedMinutes * 60_000);
}

/** True while an approved grant's window (respondedAt + granted minutes) hasn't elapsed yet. */
export function isActiveGrant(request: TimeRequest, now: Date = new Date()): boolean {
  const expiry = grantExpiresAt(request);
  if (!expiry) return false;
  return now.getTime() < expiry.getTime();
}
