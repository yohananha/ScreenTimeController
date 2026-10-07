import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../theme/tokens';
import { PrimaryButton, SecondaryButton } from './Button';
import { Modal } from './Modal';
import { WhiteCard } from './WhiteCard';
import { useAccountDeletion } from '../hooks/useAccountDeletion';
import { signOut } from '../firebase/authRepository';

type Pending = 'signOut' | 'account' | 'family' | null;

/**
 * The Account section: Sign out, Delete family (owner only) and Delete my
 * account, as plain text rows — regular weight, no outlines — with the
 * delete actions in red text so they never read as Sign out. Every action,
 * Sign out included, goes through a confirm dialog. Mirrors the Android
 * AccountActions.
 */
export function AccountSection({ familyId, isOwner }: { familyId: string; isOwner: boolean }) {
  const { t } = useTranslation();
  const { busy, error, deleteAccount, deleteFamily } = useAccountDeletion(familyId);
  const [pending, setPending] = useState<Pending>(null);
  const [signingOut, setSigningOut] = useState(false);
  const working = busy || signingOut;

  const title =
    pending === 'signOut'
      ? t('account.signOutTitle')
      : pending === 'family'
        ? t('account.deleteFamilyTitle')
        : t('account.deleteAccountTitle');
  const body =
    pending === 'signOut'
      ? t('account.signOutBody')
      : pending === 'family'
        ? t('account.deleteFamilyBody')
        : isOwner
          ? t('account.deleteAccountBodyOwner')
          : t('account.deleteAccountBodyMember');

  const confirm = () => {
    if (pending === 'signOut') {
      setSigningOut(true);
      void signOut().finally(() => setSigningOut(false));
    } else if (pending === 'family') {
      void deleteFamily();
    } else {
      void deleteAccount();
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <span style={{ fontWeight: 600, fontSize: 18, letterSpacing: '-0.01em', paddingBottom: 6 }}>{t('account.title')}</span>
      <WhiteCard>
        <ActionRow label={t('account.signOut')} onClick={() => setPending('signOut')} />
        {isOwner && <ActionRow label={t('account.deleteFamily')} destructive showTopBorder onClick={() => setPending('family')} />}
        <ActionRow label={t('account.deleteAccount')} destructive showTopBorder onClick={() => setPending('account')} />
      </WhiteCard>

      {pending && (
        <Modal onClose={() => !working && setPending(null)}>
          <h2 style={{ margin: '0 0 8px', fontSize: 18, fontWeight: 600, color: peachPlumColor.ink }}>{title}</h2>
          <p style={{ margin: 0, fontSize: 14, color: peachPlumColor.muted }}>{body}</p>
          {error && pending !== 'signOut' && (
            <p role="alert" style={{ margin: '12px 0 0', fontSize: 13, color: peachPlumColor.over }}>
              {error}
            </p>
          )}
          <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
            <SecondaryButton disabled={working} onClick={() => setPending(null)}>
              {t('common.cancel')}
            </SecondaryButton>
            <PrimaryButton
              disabled={working}
              style={pending === 'signOut' ? undefined : { background: peachPlumColor.over }}
              onClick={confirm}
            >
              {pending === 'signOut'
                ? t('account.signOut')
                : busy
                  ? t('account.deleting')
                  : t('account.confirmDelete')}
            </PrimaryButton>
          </div>
        </Modal>
      )}
    </div>
  );
}

/** A plain, full-width text row — regular weight, no outline — like the other settings rows. */
function ActionRow({
  label,
  onClick,
  destructive = false,
  showTopBorder = false,
}: {
  label: string;
  onClick: () => void;
  destructive?: boolean;
  showTopBorder?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        width: '100%',
        minHeight: 52,
        padding: '10px 0',
        border: 0,
        borderTop: showTopBorder ? `1px solid ${peachPlumColor.hairline}` : 0,
        background: 'transparent',
        color: destructive ? peachPlumColor.over : peachPlumColor.ink,
        textAlign: 'start',
        fontFamily: 'Rubik, system-ui, sans-serif',
        fontSize: 16,
        fontWeight: 400,
        cursor: 'pointer',
      }}
    >
      {label}
    </button>
  );
}
