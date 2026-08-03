import { describe, expect, it } from 'vitest';
import { isAdmin, isOwner, roleOf, type Family } from './Family';

describe('Family', () => {
  const family: Family = {
    id: 'fam-1',
    ownerUid: 'owner-uid',
    members: { 'owner-uid': 'ADMIN', 'member-uid': 'USER' },
    deviceIds: [],
  };

  it('roleOf returns the stored role', () => {
    expect(roleOf(family, 'member-uid')).toBe('USER');
    expect(roleOf(family, 'unknown')).toBeUndefined();
  });

  it('isOwner is true only for ownerUid', () => {
    expect(isOwner(family, 'owner-uid')).toBe(true);
    expect(isOwner(family, 'member-uid')).toBe(false);
  });

  it('isAdmin reflects the roles map, independent of ownership', () => {
    expect(isAdmin(family, 'owner-uid')).toBe(true);
    expect(isAdmin(family, 'member-uid')).toBe(false);
  });
});
