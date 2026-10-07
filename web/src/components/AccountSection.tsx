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

  const buttonStyle = {
    height: 40,
    padding: '0 16px',
    borderRadius: 999,
    border: `1.5px solid ${peachPlumColor.over}`,
    background: 'transparent',
    color: peachPlumColor.over,
    fontFamily: 'Rubik, system-ui, sans-serif',
    fontSize: 13,
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
      <div>
        <SecondaryButton onClick={() => void signOut()}>{t('account.signOut')}</SecondaryButton>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {isOwner && (
          <button style={buttonStyle} onClick={() => setPending('family')}>
            {t('account.deleteFamily')}
          </button>
        )}
        <button style={buttonStyle} onClick={() => setPending('account')}>
          {t('account.deleteAccount')}
        </button>
      </div>

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
