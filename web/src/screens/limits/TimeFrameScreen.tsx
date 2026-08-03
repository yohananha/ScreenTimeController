import { useState } from 'react';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { SproutPrimaryButton, SproutGhostButton } from '../../components/SproutButton';
import { Modal } from '../../components/Modal';
import { useTimeFrame } from '../../hooks/useTimeFrame';
import { DAYS_OF_WEEK, dayOfWeekFromDate, type DayOfWeek, type TimeFrameWindow } from '../../models/TimeFrameSchedule';
import { isAllDayWindow, toTimeLabel, windowsSummary } from './format';

function dayDisplayName(day: DayOfWeek): string {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

export function TimeFrameScreen({ familyId, onBack }: { familyId: string; onBack: () => void }) {
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
            aria-label="Back"
            style={{ width: 38, height: 38, borderRadius: '50%', background: colors.surface, border: 'none' }}
          >
            ←
          </button>
          <div>
            <div style={{ ...typography.title, color: colors.ink }}>Allowed hours</div>
            <div style={{ ...typography.caption, color: colors.inkMuted }}>When the TV is allowed to be on</div>
          </div>
        </div>

        <div style={{ background: colors.surface, borderRadius: radius.card, padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ ...typography.headline, color: colors.ink }}>Enforce a schedule</span>
            <input
              type="checkbox"
              checked={state.schedule.enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              aria-label="Enforce a schedule"
            />
          </div>
          {state.schedule.enabled && (
            <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 6 }}>
              Outside these hours the TV is blocked. Bonus codes still work.
            </p>
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
                Copy Mon to weekdays
              </SproutGhostButton>
              <SproutGhostButton onClick={() => copyToWeekend('SATURDAY')} style={{ padding: '8px 12px' }}>
                Copy Sat to weekend
              </SproutGhostButton>
            </div>
          </div>
        )}
      </div>

      {state.pendingChanges && (
        <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, padding: '24px 16px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: '100%', maxWidth: 600 - hPad * 2 }}>
            <SproutPrimaryButton onClick={save} disabled={state.saving} style={{ width: '100%' }}>
              {state.saving ? 'Saving…' : 'Save schedule'}
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
            Today
          </span>
        )}
      </div>
      <span style={{ ...typography.caption, color }}>{windowsSummary(windows)}</span>
    </div>
  );
}

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
  const [localWindows, setLocalWindows] = useState<TimeFrameWindow[]>(windows);
  const [adding, setAdding] = useState(false);

  return (
    <Modal onClose={onDismiss}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span style={{ ...typography.title, color: colors.ink }}>{dayDisplayName(day)}</span>
        <button onClick={onDismiss} aria-label="Close" style={{ width: 32, height: 32, borderRadius: '50%', background: colors.background, border: 'none' }}>
          ✕
        </button>
      </div>

      {localWindows.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <div style={{ ...typography.headline, color: colors.ink }}>Blocked all day on {dayDisplayName(day)}s</div>
          <div style={{ ...typography.caption, color: colors.inkMuted, marginTop: 8 }}>
            Add a window to allow some screen time.
          </div>
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
              {toTimeLabel(window.startMinute)} — {toTimeLabel(window.endMinute)}
            </span>
            <button
              onClick={() => setLocalWindows(localWindows.filter((_, i) => i !== index))}
              aria-label="Remove window"
              style={{ width: 32, height: 32, borderRadius: '50%', background: colors.overContainer, border: 'none', color: colors.overText }}
            >
              ✕
            </button>
          </div>
        ))
      )}

      <div style={{ marginTop: 8 }}>
        <SproutGhostButton onClick={() => setAdding(true)} style={{ width: '100%' }}>
          + Add window
        </SproutGhostButton>
      </div>
      <div style={{ marginTop: 8 }}>
        <SproutPrimaryButton onClick={() => onSave(localWindows)} style={{ width: '100%' }}>
          Done
        </SproutPrimaryButton>
      </div>

      {adding && (
        <AddWindowDialog
          onDismiss={() => setAdding(false)}
          onAdd={(window) => {
            setLocalWindows([...localWindows, window]);
            setAdding(false);
          }}
        />
      )}
    </Modal>
  );
}

function AddWindowDialog({
  onDismiss,
  onAdd,
}: {
  onDismiss: () => void;
  onAdd: (window: TimeFrameWindow) => void;
}) {
  const [start, setStart] = useState('16:00');
  const [end, setEnd] = useState('20:00');
  const [error, setError] = useState<string | null>(null);

  const toMinutes = (hhmm: string): number => {
    const [h, m] = hhmm.split(':').map(Number);
    return (h ?? 0) * 60 + (m ?? 0);
  };

  return (
    <Modal onClose={onDismiss}>
      <h2 style={{ ...typography.headline, margin: '0 0 12px' }}>Add window</h2>
      <label style={{ display: 'block', marginBottom: 12 }}>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>Start time</span>
        <input type="time" value={start} onChange={(e) => setStart(e.target.value)} style={{ display: 'block', width: '100%', padding: 10, marginTop: 4, borderRadius: radius.input, border: `1px solid ${colors.outline}` }} />
      </label>
      <label style={{ display: 'block' }}>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>End time</span>
        <input type="time" value={end} onChange={(e) => setEnd(e.target.value)} style={{ display: 'block', width: '100%', padding: 10, marginTop: 4, borderRadius: radius.input, border: `1px solid ${colors.outline}` }} />
      </label>
      {error && <p style={{ ...typography.caption, color: colors.warningText }}>{error}</p>}
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onDismiss}>Cancel</SproutGhostButton>
        <SproutPrimaryButton
          onClick={() => {
            const startMin = toMinutes(start);
            const endMin = toMinutes(end);
            if (endMin <= startMin) {
              setError('End must be after start');
              return;
            }
            onAdd({ startMinute: startMin, endMinute: endMin });
          }}
        >
          Add
        </SproutPrimaryButton>
      </div>
    </Modal>
  );
}
