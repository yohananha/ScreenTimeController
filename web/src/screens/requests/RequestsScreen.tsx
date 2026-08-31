import { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { formatRelativeTime } from '../../i18n/format';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { appAccentFor } from '../../theme/appAccent';
import { TopHeader } from '../../components/TopHeader';
import { EmptyState } from '../../components/EmptyState';
import { ChipGroup } from '../../components/Chips';
import { SproutPrimaryButton, SproutGhostButton } from '../../components/SproutButton';
import { useRequests } from '../../hooks/useRequests';
import { grantExpiresAt, type TimeRequest } from '../../models/TimeRequest';

function appLabelFor(pkg: string): string {
  const last = pkg.split('.').pop() ?? pkg;
  return last.charAt(0).toUpperCase() + last.slice(1);
}

export function RequestsScreen({ familyId }: { familyId: string }) {
  const { t } = useTranslation();
  const { state, approve, deny } = useRequests(familyId);
  const [selectedAmounts, setSelectedAmounts] = useState<Record<string, number>>({});
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 24px`, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <TopHeader familyName={t('topHeader.family')} parentInitial="P" />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, color: colors.ink, margin: 0 }}>{t('requests.title')}</h1>
          <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>{t('requests.subtitle')}</p>
        </div>

        {state.pending.length === 0 && state.active.length === 0 ? (
          <EmptyState
            icon={<span style={{ fontSize: 36, color: colors.positiveText }}>✓</span>}
            iconBg={colors.positiveContainer}
            title={t('requests.caughtUpTitle')}
            subtitle={t('requests.caughtUpSubtitle')}
          />
        ) : (
          <>
            {state.pending.map((req) => (
              <PendingCard
                key={req.id}
                request={req}
                selectedAmount={selectedAmounts[req.id] ?? req.requestedMinutes}
                onAmountChange={(v) => setSelectedAmounts((prev) => ({ ...prev, [req.id]: v }))}
                onApprove={() => approve(req, selectedAmounts[req.id] ?? req.requestedMinutes)}
                onDeny={() => deny(req)}
              />
            ))}
            {state.active.length > 0 && (
              <>
                <div style={{ ...typography.title, color: colors.ink, paddingTop: 12 }}>{t('requests.active')}</div>
                {state.active.map((req) => (
                  <ActiveGrantRow key={req.id} request={req} />
                ))}
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function PendingCard({
  request,
  selectedAmount,
  onAmountChange,
  onApprove,
  onDeny,
}: {
  request: TimeRequest;
  selectedAmount: number;
  onAmountChange: (v: number) => void;
  onApprove: () => void;
  onDeny: () => void;
}) {
  const { t } = useTranslation();
  const appLabel = appLabelFor(request.appPackage);
  const clampedSelected = Math.min(60, Math.max(15, selectedAmount));

  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 44, height: 44, borderRadius: 14, background: colors.ink, display: 'flex', alignItems: 'center', justifyContent: 'center', color: colors.background }}>
          📺
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ ...typography.headline, color: colors.ink }}>{t('requests.livingRoomTv')}</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>{formatRelativeTime(request.createdAt)}</div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, background: colors.accentContainer, borderRadius: radius.pill, padding: '6px 10px' }}>
          <div style={{ width: 20, height: 20, borderRadius: 6, background: appAccentFor(request.appPackage), display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 11 }}>
            {appLabel.charAt(0)}
          </div>
          <span style={{ ...typography.label, color: colors.ink }}>{appLabel}</span>
        </div>
      </div>
      <p style={{ ...typography.title, margin: 0 }}>
        <Trans
          i18nKey="requests.wantsMore"
          values={{ minutes: request.requestedMinutes }}
          components={[<span key="0" style={{ color: colors.overDisplay }} />]}
        />
      </p>
      <span style={{ ...typography.label, color: colors.inkFaint }}>{t('requests.grantHowLong')}</span>
      <ChipGroup options={[15, 30, 60]} selected={clampedSelected} onSelect={onAmountChange} label={(m) => `${m}m`} />
      <div style={{ display: 'flex', gap: 10 }}>
        <SproutPrimaryButton onClick={onApprove} style={{ flex: 1 }}>
          {t('requests.approve', { minutes: selectedAmount })}
        </SproutPrimaryButton>
        <SproutGhostButton onClick={onDeny} style={{ flex: 1 }}>
          {t('requests.deny')}
        </SproutGhostButton>
      </div>
    </div>
  );
}

function ActiveGrantRow({ request }: { request: TimeRequest }) {
  const { t, i18n } = useTranslation();
  const granted = request.approvedMinutes ?? request.requestedMinutes;
  const appLabel = appLabelFor(request.appPackage);
  const expiry = grantExpiresAt(request);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 11, background: colors.surfaceSunken, borderRadius: radius.input, padding: 14 }}>
      <div style={{ width: 36, height: 36, borderRadius: 11, background: colors.outline, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        📺
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ ...typography.bodyStrong, color: colors.ink }}>
          {t('requests.approvedOn', { minutes: granted, app: appLabel })}
        </div>
        {expiry && (
          <div style={{ ...typography.caption, color: colors.inkMuted }}>
            {t('requests.activeUntil', {
              time: expiry.toLocaleTimeString(i18n.language, { hour: 'numeric', minute: '2-digit' }),
            })}
          </div>
        )}
      </div>
    </div>
  );
}
