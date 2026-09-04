import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TimeFrameScreen } from './TimeFrameScreen';
import { useTimeFrame } from '../../hooks/useTimeFrame';
import type { TimeFrameSchedule } from '../../models/TimeFrameSchedule';

vi.mock('../../hooks/useTimeFrame');

function mockUseTimeFrame(schedule: TimeFrameSchedule, overrides: Partial<ReturnType<typeof useTimeFrame>> = {}) {
  const setWindows = vi.fn();
  vi.mocked(useTimeFrame).mockReturnValue({
    state: { schedule, pendingChanges: false, saving: false },
    setEnabled: vi.fn(),
    setWindows,
    copyToWeekdays: vi.fn(),
    copyToWeekend: vi.fn(),
    save: vi.fn(),
    ...overrides,
  });
  return { setWindows };
}

describe('TimeFrameScreen', () => {
  it('"Allow all day" sets a single 00:00-24:00 window for that day', () => {
    const { setWindows } = mockUseTimeFrame({ enabled: true, windowsByDay: {} });
    render(<TimeFrameScreen familyId="fam-1" onBack={() => {}} />);

    fireEvent.click(screen.getByText('Monday'));
    fireEvent.click(screen.getByText('Allow all day'));
    fireEvent.click(screen.getByText('Done'));

    expect(setWindows).toHaveBeenCalledWith('MONDAY', [{ startMinute: 0, endMinute: 1440 }]);
  });

  it('editing an existing window replaces it in place instead of adding a new one', () => {
    const schedule: TimeFrameSchedule = {
      enabled: true,
      windowsByDay: { MONDAY: [{ startMinute: 480, endMinute: 600 }] }, // 8:00-10:00
    };
    const { setWindows } = mockUseTimeFrame(schedule);
    render(<TimeFrameScreen familyId="fam-1" onBack={() => {}} />);

    fireEvent.click(screen.getByText('Monday'));
    fireEvent.click(screen.getByLabelText('Edit window'));

    const inputs = screen.getAllByDisplayValue(/^\d{2}:\d{2}$/) as HTMLInputElement[];
    const startInput = inputs[0]!;
    const endInput = inputs[1]!;
    expect(startInput.value).toBe('08:00');
    expect(endInput.value).toBe('10:00');
    fireEvent.change(endInput, { target: { value: '11:00' } });
    fireEvent.click(screen.getByText('Save'));
    fireEvent.click(screen.getByText('Done'));

    expect(setWindows).toHaveBeenCalledWith('MONDAY', [{ startMinute: 480, endMinute: 660 }]);
  });

  it('rejects a new window that overlaps one already on that day', () => {
    const schedule: TimeFrameSchedule = {
      enabled: true,
      windowsByDay: { MONDAY: [{ startMinute: 480, endMinute: 600 }] }, // 8:00-10:00
    };
    const { setWindows } = mockUseTimeFrame(schedule);
    render(<TimeFrameScreen familyId="fam-1" onBack={() => {}} />);

    fireEvent.click(screen.getByText('Monday'));
    fireEvent.click(screen.getByText('+ Add window'));

    const startInput = screen.getByText('Start time').closest('label')!.querySelector('input')!;
    const endInput = screen.getByText('End time').closest('label')!.querySelector('input')!;
    fireEvent.change(startInput, { target: { value: '09:00' } });
    fireEvent.change(endInput, { target: { value: '09:30' } });
    fireEvent.click(screen.getByText('Add'));

    expect(screen.getByText('This overlaps a window you already have')).toBeInTheDocument();
    // The dialog stayed open (rejected the candidate) instead of adding it.
    expect(screen.getByText('Add window')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Done'));
    expect(setWindows).toHaveBeenCalledWith('MONDAY', [{ startMinute: 480, endMinute: 600 }]);
  });
});
