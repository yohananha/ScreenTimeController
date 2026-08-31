import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';
import { SproutPrimaryButton, SproutGhostButton } from './SproutButton';
import { enableNotifications } from '../firebase/push';

/**
 * Web needs an explicit user gesture to request Notification permission —
 * unlike Android, which grants POST_NOTIFICATIONS at install time. Shown
 * once per session until dismissed or granted.
 */
export function NotificationsPrompt({ familyId }: { familyId: string }) {
  const { t } = useTranslation();
  const [dismissed, setDismissed] = useState(
    () => typeof Notification !== 'undefined' && Notification.permission !== 'default',
  );

  if (dismissed || typeof Notification === 'undefined') return null;

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
        background: colors.accentContainer,
        borderRadius: radius.input,
        padding: '10px 14px',
        margin: '0 16px',
      }}
    >
      <span style={{ ...typography.caption, color: colors.ink }}>{t('notifications.prompt')}</span>
      <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
        <SproutGhostButton onClick={() => setDismissed(true)} style={{ padding: '8px 12px' }}>
          {t('notifications.notNow')}
        </SproutGhostButton>
        <SproutPrimaryButton
          onClick={() => {
            void enableNotifications(familyId).finally(() => setDismissed(true));
          }}
          style={{ padding: '8px 14px' }}
        >
          {t('notifications.enable')}
        </SproutPrimaryButton>
      </div>
    </div>
  );
}
