import { randomInt } from "crypto";
import { HttpsError, CallableRequest } from "firebase-functions/v2/https";

// Crockford-style alphabet without the look-alikes 0/O and 1/I, so a code
// read aloud or copied by hand can't be mistyped into a different valid code.
const INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
export const INVITE_CODE_LENGTH = 8;
const INVITE_CODE_RE = new RegExp(`^[${INVITE_ALPHABET}]{${INVITE_CODE_LENGTH}}$`);

export const SIX_DIGIT_RE = /^\d{6}$/;
export const FAMILY_ID_RE = /^[A-Za-z0-9_-]{1,64}$/;

/**
 * A uniformly random [len]-digit code from a CSPRNG. Math.random() is not
 * used: its xorshift128+ state can be recovered from a handful of outputs,
 * and createTvPairing hands outputs to any caller.
 */
export function numericCode(len: number): string {
  return randomInt(0, 10 ** len).toString().padStart(len, "0");
}

/** 8 chars from a 32-symbol alphabet ≈ 1.1 × 10^12 combinations. */
export function inviteCode(): string {
  let out = "";
  for (let i = 0; i < INVITE_CODE_LENGTH; i++) {
    out += INVITE_ALPHABET[randomInt(0, INVITE_ALPHABET.length)];
  }
  return out;
}

/** Uppercases and strips the cosmetic separators the UI shows ("ABCD-EFGH"). */
export function normalizeInviteCode(input: string): string {
  return input.toUpperCase().replace(/[\s-]/g, "");
}

export function isInviteCode(code: string): boolean {
  return INVITE_CODE_RE.test(code);
}

/**
 * Reads data[field] as a string matching [re]. Anything else — missing,
 * non-string, wrong shape — is an invalid-argument rather than reaching
 * Firestore (where e.g. a "/" in a doc id throws and surfaces as INTERNAL).
 */
export function requireString(
  req: CallableRequest,
  field: string,
  re: RegExp,
): string {
  const value = (req.data as Record<string, unknown> | undefined)?.[field];
  if (typeof value !== "string" || !re.test(value)) {
    throw new HttpsError("invalid-argument", `${field} is missing or malformed.`);
  }
  return value;
}

/**
 * Parents always sign in with Google; only the TV signs in anonymously.
 * Anonymous uids are free to mint, which would make any per-uid rate limit
 * meaningless, so guess-able endpoints refuse them outright.
 */
export function requireNonAnonymous(req: CallableRequest): void {
  const provider = req.auth?.token?.firebase?.sign_in_provider;
  if (!provider || provider === "anonymous") {
    throw new HttpsError("permission-denied", "A signed-in parent account is required.", {
      reason: "anonymous_not_allowed",
    });
  }
}
