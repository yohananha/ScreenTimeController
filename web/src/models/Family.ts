export type FamilyRole = 'ADMIN' | 'USER';

/**
 * A family and its members. `ownerUid` is the single "main admin" — the only
 * account allowed to pair/unpair the TV and delete the family.
 */
export interface Family {
  id: string;
  ownerUid: string;
  members: Record<string, FamilyRole>;
  deviceIds: string[];
}

export function roleOf(family: Family, uid: string): FamilyRole | undefined {
  return family.members[uid];
}

export function isOwner(family: Family, uid: string): boolean {
  return uid === family.ownerUid;
}

export function isAdmin(family: Family, uid: string): boolean {
  return family.members[uid] === 'ADMIN';
}

/**
 * Shows an 8-char invite as "ABCD-EFGH" for easier reading. Display only —
 * the server and the code input both strip the dash.
 */
export function formatInviteCode(code: string): string {
  return code.length === 8 ? `${code.slice(0, 4)}-${code.slice(4)}` : code;
}
