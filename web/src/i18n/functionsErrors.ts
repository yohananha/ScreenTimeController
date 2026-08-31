import i18n from './i18n';

/**
 * Maps a caught error to a localized message instead of showing the server's
 * English-only Error#message straight in the UI. Mirrors
 * shared/.../FunctionsErrors.kt on the Android side.
 *
 * Prefers the machine-readable `details.reason` tag that functions/src/index.ts
 * attaches to each HttpsError — the gRPC status code alone is ambiguous
 * (NOT_FOUND alone covers five distinct conditions across the callable
 * functions: invalid code, used code, expired code, missing family, missing
 * pairing). The Functions SDK's error class isn't part of the public
 * `firebase/functions` type exports, so this checks structurally instead of
 * with `instanceof`.
 */
interface FunctionsErrorLike {
  code?: string;
  details?: unknown;
}

function isFunctionsErrorLike(error: unknown): error is FunctionsErrorLike {
  return typeof error === 'object' && error !== null && 'code' in error;
}

export function describeFunctionsError(error: unknown): string {
  if (!isFunctionsErrorLike(error)) return i18n.t('errors.generic');
  const reason = (error.details as { reason?: string } | undefined)?.reason;
  switch (reason) {
    case 'family_not_found':
      return i18n.t('errors.familyNotFound');
    case 'not_admin':
      return i18n.t('errors.notAdmin');
    case 'not_owner':
      return i18n.t('errors.notOwner');
    case 'code_invalid':
      return i18n.t('errors.codeInvalid');
    case 'code_used':
      return i18n.t('errors.codeUsed');
    case 'code_expired':
      return i18n.t('errors.codeExpired');
    case 'code_invalid_or_expired':
      return i18n.t('errors.codeInvalidOrExpired');
    case 'tv_already_paired':
      return i18n.t('errors.tvAlreadyPaired');
    case 'device_not_paired':
      return i18n.t('errors.deviceNotPaired');
    case 'locked_out':
      return i18n.t('errors.lockedOut');
    default:
      switch (error.code) {
        case 'functions/unauthenticated':
          return i18n.t('errors.signInRequired');
        case 'functions/permission-denied':
          return i18n.t('errors.permissionDenied');
        case 'functions/unavailable':
        case 'functions/deadline-exceeded':
          return i18n.t('errors.network');
        default:
          return i18n.t('errors.generic');
      }
  }
}
