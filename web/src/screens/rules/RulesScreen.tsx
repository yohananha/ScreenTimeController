import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor } from '../../theme/tokens';
import { appAccentFor } from '../../theme/appAccent';
import { WhiteCard, wideCardStyle } from '../../components/WhiteCard';
import { SettingsRow } from '../../components/SettingsRow';
import { AppRow } from '../../components/AppRow';
import { RedesignLayout } from '../redesign/RedesignLayout';
import { useLimits } from '../../hooks/useLimits';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { UNLIMITED } from '../../models/Limits';
import { formatLimitLabel, packageDisplayName, summarizeSchedule } from '../../i18n/format';
import { PickAppDialog, EditLimitDialog, EditOverallLimitDialog } from '../limits/LimitsScreen';
import { EditLockoutDialog } from '../settings/SettingsScreen';
import type { InstalledApp } from '../../models/InstalledApp';

export function RulesScreen({
  familyId,
  onOpenFamily,
  onOpenTimeFrame,
}: {
  familyId: string;
  onOpenFamily: () => void;
  onOpenTimeFrame: () => void;
}) {
  const { t } = useTranslation();
  const { state, setLimit, removeLimit, setOverallLimit, setLockoutConfig, unlockNow } = useLimits(familyId);
  const { devices } = usePairedDevices(familyId);
  const lockout = state.lockout;

  const [editing, setEditing] = useState<{ packageName: string; displayName: string; defaultMinutes: number } | null>(null);
  const [picking, setPicking] = useState(false);
  const [editingOverall, setEditingOverall] = useState(false);
  const [editingLockout, setEditingLockout] = useState(false);

  const overallValue = state.overallDailyMinutes === UNLIMITED ? t('limits.noOverallLimit') : t('rules.perDay', { limit: formatLimitLabel(state.overallDailyMinutes) });
  const lockoutValue = lockout.mode === 'TIMER' ? t('limits.lockoutTimer', { minutes: lockout.durationMinutes }) : t('limits.lockoutParent');
  const tvName = devices.length > 0 ? devices[0]!.name : t('today.noTvPaired');

  const generalRows = (
    <>
      <SettingsRow label={t('limits.overallLimitTitle')} value={overallValue} onClick={() => setEditingOverall(true)} />
      <SettingsRow label={t('limits.allowedHours')} value={summarizeSchedule(state.timeFrame)} onClick={onOpenTimeFrame} showTopBorder />
      <SettingsRow label={t('limits.codeLockout')} value={lockoutValue} onClick={() => setEditingLockout(true)} showTopBorder />
      {lockout.locked && lockout.mode === 'PARENT_UNLOCK' && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: 12 }}>
          <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.over }}>{t('limits.lockoutLockedNotice')}</span>
          <button
            onClick={unlockNow}
            style={{ height: 36, padding: '0 14px', borderRadius: 999, border: `1.5px solid ${peachPlumColor.ink}`, background: 'transparent', color: peachPlumColor.ink, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 12, fontWeight: 600, flexShrink: 0 }}
          >
            {t('limits.unlockNow')}
          </button>
        </div>
      )}
    </>
  );

  const appRows = state.limits.map((limit, index) => (
    <AppRow
      key={limit.packageName}
      name={packageDisplayName(limit.packageName, state.availableApps)}
      swatchColor={appAccentFor(limit.packageName)}
      value={limit.dailyLimitMinutes === UNLIMITED ? t('limits.alwaysAllowed') : t('rules.perDay', { limit: formatLimitLabel(limit.dailyLimitMinutes) })}
      onClick={() => setEditing({ packageName: limit.packageName, displayName: packageDisplayName(limit.packageName, state.availableApps), defaultMinutes: limit.dailyLimitMinutes })}
      showTopBorder={index > 0}
    />
  ));

  const availableToAdd: InstalledApp[] = state.availableApps.filter((app) => !state.limits.some((l) => l.packageName === app.packageName));

  return (
    <RedesignLayout active="rules" familyId={familyId}>
      {({ isWide }) => (
        <>
          {isWide ? (
            <main style={{ position: 'relative', flexGrow: 1, display: 'flex', flexDirection: 'column', gap: 22, padding: '36px 40px', boxSizing: 'border-box' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <span style={{ fontWeight: 600, fontSize: 34, lineHeight: 1, letterSpacing: '-0.03em' }}>{t('today.navRules')}</span>
                <span style={{ fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>{tvName}</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 22, alignItems: 'start' }}>
                <div style={wideCardStyle}>
                  <span style={{ fontSize: 15, fontWeight: 600, color: peachPlumColor.muted }}>{t('rules.subtitle')}</span>
                  <div style={{ display: 'flex', flexDirection: 'column' }}>{generalRows}</div>
                </div>
                <div style={wideCardStyle}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                    <span style={{ fontSize: 15, fontWeight: 600, color: peachPlumColor.muted }}>{t('limits.appLimits')}</span>
                    <a href="#add" onClick={(e) => { e.preventDefault(); setPicking(true); }} style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.ink, textDecoration: 'none' }}>
                      {t('rules.addAppLink')}
                    </a>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column' }}>{appRows}</div>
                </div>
              </div>
            </main>
          ) : (
            <div style={{ position: 'relative', flexGrow: 1, display: 'flex', flexDirection: 'column', width: '100%', maxWidth: 430, margin: '0 auto', padding: '56px 22px 22px', boxSizing: 'border-box' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: 18 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                  <span style={{ fontWeight: 600, fontSize: 30, lineHeight: 1, letterSpacing: '-0.03em' }}>{t('today.navRules')}</span>
                  <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('rules.subtitle')}</span>
                </div>
                <button
                  onClick={onOpenFamily}
                  aria-label={t('today.familyAria')}
                  style={{ width: 36, height: 36, borderRadius: '50%', border: 0, background: peachPlumColor.ink, color: peachPlumColor.surface, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                  P
                </button>
              </div>

              <WhiteCard style={{ padding: '4px 16px' }}>{generalRows}</WhiteCard>

              <div style={{ display: 'flex', flexDirection: 'column', marginTop: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', paddingBottom: 6 }}>
                  <span style={{ fontWeight: 600, fontSize: 18, letterSpacing: '-0.01em' }}>{t('limits.appLimits')}</span>
                  <a href="#add" onClick={(e) => { e.preventDefault(); setPicking(true); }} style={{ fontSize: 13, fontWeight: 600, color: peachPlumColor.ink, textDecoration: 'none' }}>
                    {t('rules.addAppLink')}
                  </a>
                </div>
                <WhiteCard>{appRows}</WhiteCard>
              </div>
            </div>
          )}

          {picking && (
            <PickAppDialog
              available={availableToAdd}
              tvHasNoApps={state.availableApps.length === 0}
              onDismiss={() => setPicking(false)}
              onPick={(app) => {
                setPicking(false);
                setEditing({ packageName: app.packageName, displayName: app.label, defaultMinutes: 60 });
              }}
            />
          )}

          {editing && (
            <EditLimitDialog
              target={editing}
              onDismiss={() => setEditing(null)}
              onSave={(minutes) => {
                setLimit(editing.packageName, minutes);
                setEditing(null);
              }}
              onRemove={() => {
                removeLimit(editing.packageName);
                setEditing(null);
              }}
            />
          )}

          {editingOverall && (
            <EditOverallLimitDialog
              currentMinutes={state.overallDailyMinutes}
              onDismiss={() => setEditingOverall(false)}
              onSave={(minutes) => {
                setOverallLimit(minutes);
                setEditingOverall(false);
              }}
            />
          )}

          {editingLockout && (
            <EditLockoutDialog
              current={lockout}
              onDismiss={() => setEditingLockout(false)}
              onSave={(minutes, mode) => {
                setLockoutConfig(minutes, mode);
                setEditingLockout(false);
              }}
            />
          )}

        </>
      )}
    </RedesignLayout>
  );
}
