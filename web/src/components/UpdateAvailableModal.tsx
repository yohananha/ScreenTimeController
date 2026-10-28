import { useTranslation } from 'react-i18next';
import { Modal } from './Modal';
import { SproutGhostButton, SproutPrimaryButton } from './SproutButton';
import { useVersionCheck } from '../hooks/useVersionCheck';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

/** Shown when a newer build has been deployed than the one this tab loaded. */
export function UpdateAvailableModal() {
  const { t } = useTranslation();
  const { outdated, dismiss } = useVersionCheck();
  if (!outdated) return null;

  return (
    <Modal onClose={dismiss}>
      <h2 style={{ ...typography.title, color: colors.ink, margin: '0 0 12px' }}>{t('updatePrompt.title')}</h2>
      <p style={{ ...typography.body, color: colors.ink, margin: 0 }}>{t('updatePrompt.body')}</p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, justifyContent: 'flex-end', marginTop: 20 }}>
        <SproutGhostButton onClick={dismiss}>{t('updatePrompt.later')}</SproutGhostButton>
        <SproutPrimaryButton onClick={() => window.location.reload()}>{t('updatePrompt.reload')}</SproutPrimaryButton>
      </div>
    </Modal>
  );
}
