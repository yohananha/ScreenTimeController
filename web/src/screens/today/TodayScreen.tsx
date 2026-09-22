import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumSpacing } from '../../theme/tokens';
import { appAccentFor } from '../../theme/appAccent';
import { TimeGauge } from '../../components/TimeGauge';
import { ModeTiles, type TvMode } from '../../components/ModeTiles';
import { RequestStrip } from '../../components/RequestStrip';
import { UsageRow } from '../../components/UsageRow';
import { SecondaryButton } from '../../components/Button';
import { wideCardStyle } from '../../components/WhiteCard';
import { RedesignLayout } from '../redesign/RedesignLayout';
import { useLimits } from '../../hooks/useLimits';
import { useRequests } from '../../hooks/useRequests';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { UNLIMITED } from '../../models/Limits';
import { formatHoursMinutesClock, formatLimitLabel, formatRelativeTime, packageDisplayName } from '../../i18n/format';

export function TodayScreen({
  familyId,
  onOpenRules,
  onOpenFamily,
}: {
  familyId: string;
  onOpenRules: () => void;
  onOpenFamily: () => void;
}) {
  const { t, i18n } = useTranslation();
  const { state, selectInstantLock, selectAllowAllDay, selectDefaultLimits } = useLimits(familyId);
  const { state: requestsState, approve, deny } = useRequests(familyId);
  const { devices } = usePairedDevices(familyId);

  const mode: TvMode = state.instantLocked ? 'lock' : state.allowAllDayActive ? 'allow' : 'limits';
  const noLimit = state.overallDailyMinutes === UNLIMITED;
  const overallMs = noLimit ? 0 : state.overallDailyMinutes * 60_000;
  const usedMs = state.totalUsageMillis;
  const leftMinutes = noLimit ? 0 : Math.max(0, Math.round((overallMs - usedMs) / 60_000));
  const overLimit = !noLimit && overallMs > 0 && usedMs >= overallMs;
  const progress = noLimit || overallMs <= 0 ? 0 : usedMs / overallMs;

  const pendingRequest = requestsState.pending[0];
  const noTvPaired = devices.length === 0;
  const tvName = devices.length > 0 ? devices[0]!.name : t('today.noTvPaired');

  // README §4 "Today" — states beyond the static reference: Lock active dims
  // the gauge (there's nothing to track while the TV is fully locked), Allow
  // active swaps the caption instead of showing the usual "of X · resets" line.
  const gauge = (
    <div style={{ opacity: mode === 'lock' ? 0.4 : 1 }}>
      <TimeGauge
        centerLabel={noLimit ? '—' : formatHoursMinutesClock(leftMinutes)}
        word={noLimit ? '' : overLimit ? t('today.gaugeWordOver') : t('today.gaugeWordLeft')}
        progress={progress}
        overLimit={overLimit}
      />
    </div>
  );
  const caption =
    mode === 'allow'
      ? t('limits.modeAllowCaption')
      : noLimit
        ? t('today.noDailyLimit')
        : t('today.gaugeCaption', { limit: formatHoursMinutesClock(state.overallDailyMinutes) });
  const livePill = (
    <span
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        height: 36,
        padding: '0 14px',
        borderRadius: 999,
        background: peachPlumColor.surface,
        boxShadow: '0 1px 2px rgba(42,30,46,0.05), 0 6px 18px rgba(42,30,46,0.07)',
        fontSize: 13,
        fontWeight: 500,
      }}
    >
      {devices.length > 0 && (
        <span style={{ width: 8, height: 8, borderRadius: '50%', background: peachPlumColor.ok, animation: 'pp-live-pulse 2s ease-out infinite', flexShrink: 0 }} />
      )}
      {tvName}
    </span>
  );
  const modeTiles = (
    <ModeTiles
      active={mode}
      disabled={noTvPaired}
      onSelect={(next) => {
        if (next === 'lock') selectInstantLock();
        else if (next === 'allow') selectAllowAllDay();
        else selectDefaultLimits();
      }}
    />
  );
  const requestStrip = pendingRequest && (
    <RequestStrip
      title={t('today.requestTitle', { minutes: pendingRequest.requestedMinutes, app: packageDisplayName(pendingRequest.appPackage, state.availableApps) })}
      subtitle={t('today.requestSubtitle', { time: formatRelativeTime(pendingRequest.createdAt) })}
      approveAria={t('today.approveAria', { minutes: pendingRequest.requestedMinutes })}
      denyAria={t('today.denyAria')}
      onApprove={() => approve(pendingRequest)}
      onDeny={() => deny(pendingRequest)}
    />
  );
  const usageRows = state.limits.map((limit, index) => {
    const usedMsForApp = state.usagePerApp[limit.packageName] ?? 0;
    const usedMinutes = Math.floor(usedMsForApp / 60_000);
    const swatchColor = appAccentFor(limit.packageName);
    const isUnlimited = limit.dailyLimitMinutes === UNLIMITED;
    const limitMs = limit.dailyLimitMinutes * 60_000;
    const timesUp = !isUnlimited && limit.dailyLimitMinutes >= 0 && limitMs > 0 && usedMsForApp >= limitMs;
    const rowProgress = isUnlimited || limitMs <= 0 ? 0 : Math.min(1, usedMsForApp / limitMs);
    return (
      <UsageRow
        key={limit.packageName}
        name={packageDisplayName(limit.packageName, state.availableApps)}
        swatchColor={timesUp ? peachPlumColor.over : swatchColor}
        valueLabel={timesUp ? t('limits.timesUp') : isUnlimited ? formatLimitLabel(usedMinutes) : `${formatLimitLabel(usedMinutes)} / ${formatLimitLabel(limit.dailyLimitMinutes)}`}
        progress={rowProgress}
        barColor={swatchColor}
        timesUp={timesUp}
        showBottomBorder={index === state.limits.length - 1}
      />
    );
  });

  return (
    <RedesignLayout active="today" familyId={familyId}>
      {({ isWide, openUnlock }) =>
        isWide ? (
          <main style={{ position: 'relative', flexGrow: 1, display: 'flex', flexDirection: 'column', gap: 22, padding: '36px 40px', boxSizing: 'border-box' }}>
            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <span style={{ fontWeight: 600, fontSize: 34, lineHeight: 1, letterSpacing: '-0.03em' }}>{t('today.navToday')}</span>
                <span style={{ fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>
                  {new Date().toLocaleDateString(i18n.language, { weekday: 'long', month: 'short', day: 'numeric' })}
                </span>
              </div>
              <SecondaryButton onClick={openUnlock} style={{ height: 44, fontSize: 14 }}>
                {t('today.unlockAria')}
              </SecondaryButton>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 22, alignItems: 'start' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
                <div style={wideCardStyle}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
                    {gauge}
                    <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted, marginTop: -6 }}>{caption}</span>
                    {livePill}
                  </div>
                  {modeTiles}
                </div>
                <div style={wideCardStyle}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ width: 44, height: 44, borderRadius: 14, background: peachPlumColor.tint, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke={peachPlumColor.ink} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <rect x="3" y="5" width="18" height="12" rx="2" />
                        <path d="M8 21h8" />
                      </svg>
                    </span>
                    <span style={{ display: 'flex', flexDirection: 'column', gap: 2, flexGrow: 1 }}>
                      <span style={{ fontSize: 16, fontWeight: 600 }}>{tvName}</span>
                      {devices.length > 0 && <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('settings.online')}</span>}
                    </span>
                    <SecondaryButton onClick={onOpenFamily} style={{ height: 40, padding: '0 16px', fontSize: 13, border: `1.5px solid ${peachPlumColor.hairline}` }}>
                      {t('today.navFamily')}
                    </SecondaryButton>
                  </div>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
                <div style={wideCardStyle}>
                  <span style={{ fontSize: 15, fontWeight: 600, color: peachPlumColor.muted }}>{t('requests.title')}</span>
                  {requestStrip}
                </div>
                <div style={wideCardStyle}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                    <span style={{ fontSize: 15, fontWeight: 600, color: peachPlumColor.muted }}>{t('today.usageByApp')}</span>
                    <a
                      href="#rules"
                      onClick={(e) => {
                        e.preventDefault();
                        onOpenRules();
                      }}
                      style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.ink, textDecoration: 'none' }}
                    >
                      {t('today.rulesLink')}
                    </a>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column' }}>{usageRows}</div>
                </div>
              </div>
            </div>
          </main>
        ) : (
          <div
            style={{
              position: 'relative',
              flexGrow: 1,
              display: 'flex',
              flexDirection: 'column',
              width: '100%',
              maxWidth: 430,
              margin: '0 auto',
              padding: `${peachPlumSpacing.phoneTopInset}px ${peachPlumSpacing.phoneGutter}px 0`,
              boxSizing: 'border-box',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>
                {new Date().toLocaleDateString(i18n.language, { weekday: 'long', month: 'short', day: 'numeric' })}
              </span>
              <button
                onClick={onOpenFamily}
                aria-label={t('today.familyAria')}
                style={{ width: 36, height: 36, borderRadius: '50%', border: 0, background: peachPlumColor.ink, color: peachPlumColor.surface, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
              >
                P
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: 8 }}>
              {gauge}
              <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted, marginTop: -6 }}>{caption}</span>
              <div style={{ marginTop: 10 }}>{livePill}</div>
            </div>

            <div style={{ marginTop: 20 }}>{modeTiles}</div>

            {requestStrip && <div style={{ marginTop: 16 }}>{requestStrip}</div>}

            <div style={{ display: 'flex', flexDirection: 'column', marginTop: 22, paddingBottom: 22 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', paddingBottom: 4 }}>
                <span style={{ fontSize: 16, fontWeight: 600 }}>{t('today.byApp')}</span>
                <a
                  href="#rules"
                  onClick={(e) => {
                    e.preventDefault();
                    onOpenRules();
                  }}
                  style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.ink, textDecoration: 'none' }}
                >
                  {t('today.rulesLink')}
                </a>
              </div>
              {usageRows}
            </div>
          </div>
        )
      }
    </RedesignLayout>
  );
}
