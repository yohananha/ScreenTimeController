import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { colors } from '../theme/colors';
import { radius } from '../theme/radius';
import { typography } from '../theme/typography';
import { subscribeForegroundMessages } from '../firebase/push';

/** In-app banner for foreground FCM messages — web doesn't auto-show a system notification while the tab is focused. */
export function ForegroundToast() {
  const [message, setMessage] = useState<{ title: string; body: string } | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    let unsub: (() => void) | undefined;
    subscribeForegroundMessages((payload) => setMessage(payload)).then((u) => {
      if (u) unsub = u;
    });
    return () => unsub?.();
  }, []);

  if (!message) return null;

  return (
    <div
      onClick={() => {
        setMessage(null);
        navigate('/requests');
      }}
      style={{
        position: 'fixed',
        top: 12,
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 200,
        background: colors.ink,
        color: colors.background,
        borderRadius: radius.input,
        padding: '12px 16px',
        maxWidth: 360,
        cursor: 'pointer',
        boxShadow: '0 10px 24px rgba(0,0,0,.2)',
      }}
    >
      <div style={{ ...typography.bodyStrong, color: colors.background }}>{message.title}</div>
      <div style={{ ...typography.caption, color: colors.darkMutedText }}>{message.body}</div>
    </div>
  );
}
