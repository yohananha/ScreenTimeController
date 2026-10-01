import { useTranslation } from 'react-i18next';
import { Modal } from './Modal';
import { SproutGhostButton, SproutPrimaryButton } from './SproutButton';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { colors } from '../theme/colors';
import { typography } from '../theme/typography';

/**
 * Developer → users message (config/announcement). The copy is authored in
 * the console in English only, so it renders LTR even in the Hebrew UI.
 */
export function AnnouncementModal() {
  const { t } = useTranslation();
  const { announcement, dismiss } = useAnnouncement();
  if (!announcement) return null;

  const share = () => {
    const subject = announcement.emailSubject ? `?subject=${encodeURIComponent(announcement.emailSubject)}` : '';
    window.location.href = `mailto:${announcement.ctaEmail}${subject}`;
    dismiss();
  };

  return (
    <Modal onClose={dismiss}>
      <div dir="ltr" lang="en">
        {announcement.title && (
          <h2 style={{ ...typography.title, color: colors.ink, margin: '0 0 12px' }}>{announcement.title}</h2>
        )}
        <p style={{ ...typography.body, color: colors.ink, margin: 0, whiteSpace: 'pre-line' }}>{announcement.body}</p>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, justifyContent: 'flex-end', marginTop: 20 }}>
        <SproutGhostButton onClick={dismiss}>{t('announcement.later')}</SproutGhostButton>
        {announcement.ctaEmail && (
          <SproutPrimaryButton onClick={share}>{t('announcement.share')}</SproutPrimaryButton>
        )}
      </div>
    </Modal>
  );
}
