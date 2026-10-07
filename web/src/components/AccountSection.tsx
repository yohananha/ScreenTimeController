import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';
import { PrimaryButton, SecondaryButton } from './Button';
import { Modal } from './Modal';
import { useAccountDeletion } from '../hooks/useAccountDeletion';
import { signOut } from '../firebase/authRepository';

type Pending = 'account' | 'family' | null;

/**
 * The Account section: "Sign out", plus "Delete my account" (everyone) and
 * "Delete family" (owner only), each behind a confirm dialog that spells out
 * what is lost and for whom.
 */
export function AccountSection({ familyId, isOwner }: { familyId: string; isOwner: boolean }) {
  const { t } = useTranslation();
  const { busy, error, deleteAccount, deleteFamily } = useAccountDeletion(familyId);
  const [pending, setPending] = useState<Pending>(null);

  // Full-width and stacked (not side by side): the Hebrew labels are long
  // enough to wrap in a shared row on a phone, and a co-parent (no "Delete
  // family") would get a lopsided row. Same size for all three; the delete
  // actions are red so they never look like Sign out. Mirrors the Android
  // AccountButtons.
  const fullWidth = { width: '100%', height: 48, fontSize: 15 } as const;
  const destructiveStyle = {
    ...fullWidth,
    padding: '0 22px',
    borderRadius: 999,
    border: `1.5px solid ${peachPlumColor.over}`,
    background: 'transparent',
    color: peachPlumColor.over,
    fontFamily: 'Rubik, system-ui, sans-serif',
    fontWeight: 600,
    cursor: 'pointer',
  } as const;

  const title = pending === 'family' ? t('account.deleteFamilyTitle') : t('account.deleteAccountTitle');
  const body =
    pending === 'family'
      ? t('account.deleteFamilyBody')
      : isOwner
        ? t('account.deleteAccountBodyOwner')
        : t('account.deleteAccountBodyMember');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <span style={{ fontWeight: 600, fontSize: 18, letterSpacing: '-0.01em' }}>{t('account.title')}</span>
      <SecondaryButton style={fullWidth} onClick={() => void signOut()}>
        {t('account.signOut')}
      </SecondaryButton>
      {isOwner && (
        <button style={destructiveStyle} onClick={() => setPending('family')}>
          {t('account.deleteFamily')}
        </button>
      )}
      <button style={destructiveStyle} onClick={() => setPending('account')}>
        {t('account.deleteAccount')}
      </button>

      {pending && (
        <Modal onClose={() => !busy && setPending(null)}>
          <h2 style={{ margin: '0 0 8px', fontSize: 18, fontWeight: 600, color: peachPlumColor.ink }}>{title}</h2>
          <p style={{ margin: 0, fontSize: 14, color: peachPlumColor.muted }}>{body}</p>
          {error && <p role="alert" style={{ margin: '12px 0 0', fontSize: 13, color: peachPlumColor.over }}>{error}</p>}
          <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
            <SecondaryButton disabled={busy} onClick={() => setPending(null)}>
              {t('common.cancel')}
            </SecondaryButton>
            <PrimaryButton
              disabled={busy}
              style={{ background: peachPlumColor.over }}
              onClick={() => void (pending === 'family' ? deleteFamily() : deleteAccount())}
            >
              {busy ? t('account.deleting') : t('account.confirmDelete')}
            </PrimaryButton>
          </div>
        </Modal>
      )}
    </div>
  );
}
