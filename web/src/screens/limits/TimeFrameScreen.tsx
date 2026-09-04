import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { SproutPrimaryButton, SproutGhostButton } from '../../components/SproutButton';
import { Modal } from '../../components/Modal';
import { BackChevron } from '../../components/Chevron';
import { useTimeFrame } from '../../hooks/useTimeFrame';
import {
  ALL_DAY_WINDOW,
  DAYS_OF_WEEK,
  dayOfWeekFromDate,
  windowsOverlap,
  type DayOfWeek,
  type TimeFrameWindow,
} from '../../models/TimeFrameSchedule';
import { dayDisplayName, isAllDayWindow, toTimeLabel, windowsSummary } from '../../i18n/format';

/** "HH:MM" (24h, locale-invariant) for pre-filling an <input type="time">. */
function toInputTime(minutes: number): string {
  const h = Math.floor(minutes / 60) % 24;
  const m = minutes % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

export function TimeFrameScreen({ familyId, onBack }: { familyId: string; onBack: () => void }) {
  const { t } = useTranslation();
  const { state, setEnabled, setWindows, copyToWeekdays, copyToWeekend, save } = useTimeFrame(familyId);
  const [editingDay, setEditingDay] = useState<DayOfWeek | null>(null);
  const hPad = useResponsivePadding();
  const today = dayOfWeekFromDate(new Date());

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center', position: 'relative' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 120px`, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingTop: 12 }}>
          <button
            onClick={onBack}
            aria-label={t('timeframe.backAria')}
            style={{ width: 38, height: 38, borderRadius: '50%', background: colors.surface, border: 'none' }}
          >
            <BackChevron />
          </button>
          <div>
            <div style={{ ...typography.title, color: colors.ink }}>{t('timeframe.title')}</div>
            <div style={{ ...typography.caption, color: colors.inkMuted }}>{t('timeframe.subtitle')}</div>
          </div>
        </div>

        <div style={{ background: colors.surface, borderRadius: radius.card, padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ ...typography.headline, color: colors.ink }}>{t('timeframe.enforceSchedule')}</span>
            <input
              type="checkbox"
              checked={state.schedule.enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              aria-label={t('timeframe.enforceSchedule')}
            />
          </div>
          {state.schedule.enabled && (
            <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 6 }}>{t('timeframe.enforceBody')}</p>
          )}
        </div>

        {state.schedule.enabled && (
          <div style={{ background: colors.surface, borderRadius: radius.card }}>
            {DAYS_OF_WEEK.map((day, index) => (
              <div key={day}>
                <DayRow
                  day={day}
                  isToday={day === today}
                  windows={state.schedule.windowsByDay[day] ?? []}
                  onClick={() => setEditingDay(day)}
                />
                {index < 6 && <div style={{ height: 1, background: colors.outline, margin: '0 16px' }} />}
              </div>
            ))}
            <div style={{ display: 'flex', gap: 8, padding: '8px 12px' }}>
              <SproutGhostButton onClick={() => copyToWeekdays('MONDAY')} style={{ padding: '8px 12px' }}>
                {t('timeframe.copyMonToWeekdays')}
              </SproutGhostButton>
              <SproutGhostButton onClick={() => copyToWeekend('SATURDAY')} style={{ padding: '8px 12px' }}>
                {t('timeframe.copySatToWeekend')}
              </SproutGhostButton>
            </div>
          </div>
        )}
      </div>

      {state.pendingChanges && (
        <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, padding: '24px 16px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: '100%', maxWidth: 600 - hPad * 2 }}>
            <SproutPrimaryButton onClick={save} disabled={state.saving} style={{ width: '100%' }}>
              {state.saving ? t('timeframe.saving') : t('timeframe.saveSchedule')}
            </SproutPrimaryButton>
          </div>
        </div>
      )}

      {editingDay && (
        <DayEditSheet
          day={editingDay}
          windows={state.schedule.windowsByDay[editingDay] ?? []}
          onDismiss={() => setEditingDay(null)}
          onSave={(windows) => {
            setWindows(editingDay, windows);
            setEditingDay(null);
          }}
        />
      )}
    </div>
  );
}

function DayRow({
  day,
  isToday,
  windows,
  onClick,
}: {
  day: DayOfWeek;
  isToday: boolean;
  windows: TimeFrameWindow[];
  onClick: () => void;
}) {
  const { t } = useTranslation();
  const color = windows.length === 0 ? colors.overText : isAllDayWindow(windows) ? colors.positiveText : colors.inkMuted;
  return (
    <div
      onClick={onClick}
      style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 16px', cursor: 'pointer' }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ ...typography.headline, color: colors.ink }}>{dayDisplayName(day)}</span>
        {isToday && (
          <span
            style={{
              ...typography.caption,
              color: colors.ink,
              background: colors.accentContainer,
              borderRadius: radius.pill,
              padding: '3px 8px',
            }}
          >
            {t('timeframe.today')}
          </span>
        )}
      </div>
      <span style={{ ...typography.caption, color }}>{windowsSummary(windows)}</span>
    </div>
  );
}

/** Either adding a brand-new window, or editing the window at `index` in-place. */
type DialogState = { mode: 'add' } | { mode: 'edit'; index: number };

function DayEditSheet({
  day,
  windows,
  onDismiss,
  onSave,
}: {
  day: DayOfWeek;
  windows: TimeFrameWindow[];
  onDismiss: () => void;
  onSave: (windows: TimeFrameWindow[]) => void;
}) {
  const { t } = useTranslation();
  const [localWindows, setLocalWindows] = useState<TimeFrameWindow[]>(windows);
  const [dialogState, setDialogState] = useState<DialogState | null>(null);
  const allDay = isAllDayWindow(localWindows);

  return (
    <Modal onClose={onDismiss}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span style={{ ...typography.title, color: colors.ink }}>{dayDisplayName(day)}</span>
        <button onClick={onDismiss} aria-label={t('timeframe.closeAria')} style={{ width: 32, height: 32, borderRadius: '50%', background: colors.background, border: 'none' }}>
          ✕
        </button>
      </div>

      <div style={{ marginBottom: 12 }}>
        <SproutGhostButton
          onClick={() => setLocalWindows([ALL_DAY_WINDOW])}
          disabled={allDay}
          style={{ width: '100%' }}
        >
          {allDay ? t('timeframe.allowAllDayActive') : t('timeframe.allowAllDay')}
        </SproutGhostButton>
      </div>

      {localWindows.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <div style={{ ...typography.headline, color: colors.ink }}>{t('timeframe.blockedAllDayOn', { day: dayDisplayName(day) })}</div>
          <div style={{ ...typography.caption, color: colors.inkMuted, marginTop: 8 }}>{t('timeframe.addWindowToAllow')}</div>
        </div>
      ) : (
        localWindows.map((window, index) => (
          <div
            key={index}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: colors.background,
              borderRadius: radius.input,
              padding: '12px 14px',
              marginBottom: 8,
            }}
          >
            <span style={{ ...typography.bodyStrong, color: colors.ink }}>
              {isAllDayWindow([window]) ? t('limits.allDay') : `${toTimeLabel(window.startMinute)} — ${toTimeLabel(window.endMinute)}`}
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              {!isAllDayWindow([window]) && (
                <button
                  onClick={() => setDialogState({ mode: 'edit', index })}
                  aria-label={t('timeframe.editWindowAria')}
                  style={{ width: 32, height: 32, borderRadius: '50%', background: colors.accentContainer, border: 'none' }}
                >
                  ✎
                </button>
              )}
              <button
                onClick={() => setLocalWindows(localWindows.filter((_, i) => i !== index))}
                aria-label={t('timeframe.removeWindowAria')}
                style={{ width: 32, height: 32, borderRadius: '50%', background: colors.overContainer, border: 'none', color: colors.overText }}
              >
                ✕
              </button>
            </div>
          </div>
        ))
      )}

      <div style={{ marginTop: 8 }}>
        <SproutGhostButton onClick={() => setDialogState({ mode: 'add' })} style={{ width: '100%' }}>
          {t('timeframe.addWindow')}
        </SproutGhostButton>
      </div>
      <div style={{ marginTop: 8 }}>
        <SproutPrimaryButton onClick={() => onSave(localWindows)} style={{ width: '100%' }}>
          {t('common.done')}
        </SproutPrimaryButton>
      </div>

      {dialogState && (
        <WindowDialog
          initial={dialogState.mode === 'edit' ? localWindows[dialogState.index] : undefined}
          existingWindows={dialogState.mode === 'edit' ? localWindows.filter((_, i) => i !== dialogState.index) : localWindows}
          onDismiss={() => setDialogState(null)}
          onSave={(window) => {
            setLocalWindows(
              dialogState.mode === 'edit'
                ? localWindows.map((w, i) => (i === dialogState.index ? window : w))
                : [...localWindows, window],
            );
            setDialogState(null);
          }}
        />
      )}
    </Modal>
  );
}

function WindowDialog({
  initial,
  existingWindows,
  onDismiss,
  onSave,
}: {
  /** Present when editing an existing window; absent when adding a new one. */
  initial?: TimeFrameWindow;
  /** The day's other windows — the candidate is rejected if it overlaps any of these. */
  existingWindows: TimeFrameWindow[];
  onDismiss: () => void;
  onSave: (window: TimeFrameWindow) => void;
}) {
  const { t } = useTranslation();
  const isEdit = initial !== undefined;
  const [start, setStart] = useState(initial ? toInputTime(initial.startMinute) : '16:00');
  const [end, setEnd] = useState(initial ? toInputTime(initial.endMinute) : '20:00');
  const [error, setError] = useState<string | null>(null);

  const toMinutes = (hhmm: string): number => {
    const [h, m] = hhmm.split(':').map(Number);
    return (h ?? 0) * 60 + (m ?? 0);
  };

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 12px' }}>{isEdit ? t('timeframe.editWindowTitle') : t('timeframe.addWindowTitle')}</h2>
      <label style={{ display: 'block', marginBottom: 12 }}>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>{t('timeframe.startTime')}</span>
        <input type="time" value={start} onChange={(e) => setStart(e.target.value)} style={{ display: 'block', width: '100%', padding: 10, marginTop: 4, borderRadius: radius.input, border: `1px solid ${colors.outline}` }} />
      </label>
      <label style={{ display: 'block' }}>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>{t('timeframe.endTime')}</span>
        <input type="time" value={end} onChange={(e) => setEnd(e.target.value)} style={{ display: 'block', width: '100%', padding: 10, marginTop: 4, borderRadius: radius.input, border: `1px solid ${colors.outline}` }} />
      </label>
      {error && <p style={{ ...typography.caption, color: colors.warningText }}>{error}</p>}
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onDismiss}>{t('common.cancel')}</SproutGhostButton>
        <SproutPrimaryButton
          onClick={() => {
            const startMin = toMinutes(start);
            const endMin = toMinutes(end);
            if (endMin <= startMin) {
              setError(t('timeframe.endAfterStart'));
              return;
            }
            const candidate: TimeFrameWindow = { startMinute: startMin, endMinute: endMin };
            if (existingWindows.some((w) => windowsOverlap(w, candidate))) {
              setError(t('timeframe.overlapsExisting'));
              return;
            }
            onSave(candidate);
          }}
        >
          {isEdit ? t('common.save') : t('timeframe.add')}
        </SproutPrimaryButton>
      </div>
    </Modal>
  );
}
