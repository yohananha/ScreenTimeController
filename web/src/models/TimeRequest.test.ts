import { describe, expect, it } from 'vitest';
import { grantExpiresAt, isActiveGrant, type TimeRequest } from './TimeRequest';

function baseRequest(overrides: Partial<TimeRequest> = {}): TimeRequest {
  return {
    id: 'a',
    appPackage: 'p',
    requestedMinutes: 10,
    status: 'Pending',
    approvedMinutes: null,
    createdAt: new Date(),
    respondedAt: null,
    ...overrides,
  };
}

describe('TimeRequest', () => {
  it('pending request is not an active grant', () => {
    expect(isActiveGrant(baseRequest())).toBe(false);
  });

  it('approved grant within window is active', () => {
    const req = baseRequest({
      status: 'Approved',
      approvedMinutes: 10,
      respondedAt: new Date(Date.now() - 60_000),
    });
    expect(isActiveGrant(req)).toBe(true);
  });

  it('approved grant past expiry is no longer active', () => {
    const req = baseRequest({
      status: 'Approved',
      approvedMinutes: 1,
      respondedAt: new Date(Date.now() - 120_000),
    });
    expect(isActiveGrant(req)).toBe(false);
  });

  it('denied grant is never active', () => {
    const req = baseRequest({ status: 'Denied', respondedAt: new Date() });
    expect(isActiveGrant(req)).toBe(false);
  });

  it('approvedMinutes overrides requestedMinutes when computing expiry', () => {
    const req = baseRequest({
      requestedMinutes: 30,
      status: 'Approved',
      approvedMinutes: 5,
      respondedAt: new Date('2026-01-01T00:00:00Z'),
    });
    expect(grantExpiresAt(req)).toEqual(new Date('2026-01-01T00:05:00Z'));
  });
});
