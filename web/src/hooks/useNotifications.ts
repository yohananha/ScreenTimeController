import { useCallback, useState } from 'react';
import { enableNotifications } from '../firebase/push';

export type NotificationStatus = 'unsupported' | 'default' | 'granted' | 'denied';

function readPermission(): NotificationStatus {
  if (typeof Notification === 'undefined') return 'unsupported';
  return Notification.permission;
}

/**
 * Settings-screen counterpart to NotificationsPrompt (the dismissible banner):
 * a persistent way to check status and (re-)enable push notifications for new
 * time requests, since a dismissed banner doesn't come back within a session.
 */
export function useNotificationStatus(familyId: string) {
  const [status, setStatus] = useState<NotificationStatus>(readPermission);
  const [enabling, setEnabling] = useState(false);

  const enable = useCallback(() => {
    setEnabling(true);
    enableNotifications(familyId)
      .catch(() => false)
      .then(() => setStatus(readPermission()))
      .finally(() => setEnabling(false));
  }, [familyId]);

  return { status, enabling, enable };
}
