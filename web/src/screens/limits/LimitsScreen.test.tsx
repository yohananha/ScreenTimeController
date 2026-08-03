import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { LimitsScreen } from './LimitsScreen';
import { useLimits } from '../../hooks/useLimits';
import { defaultLockoutSettings } from '../../models/LockoutSettings';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../../models/TimeFrameSchedule';
import { DEFAULT_OVERALL_MINUTES } from '../../models/Limits';

vi.mock('../../hooks/useLimits');

function mockUseLimits(overrides: Partial<ReturnType<typeof useLimits>['state']> = {}) {
  vi.mocked(useLimits).mockReturnValue({
    state: {
      limits: [],
      overallDailyMinutes: DEFAULT_OVERALL_MINUTES,
      availableApps: [],
      lockout: defaultLockoutSettings(),
      timeFrame: DEFAULT_TIME_FRAME_SCHEDULE,
      allowAllDayActive: false,
      instantLocked: false,
      usagePerApp: {},
      totalUsageMillis: 0,
      ...overrides,
    },
    writeError: null,
    clearWriteError: vi.fn(),
    setLimit: vi.fn(),
    removeLimit: vi.fn(),
    setOverallLimit: vi.fn(),
    setLockoutConfig: vi.fn(),
    unlockNow: vi.fn(),
    selectInstantLock: vi.fn(),
    selectAllowAllDay: vi.fn(),
    selectDefaultLimits: vi.fn(),
  });
}

describe('LimitsScreen', () => {
  it('renders the title and hero card label', () => {
    mockUseLimits();
    render(<LimitsScreen familyId="fam-1" onOpenHistory={() => {}} onOpenTimeFrame={() => {}} />);
    expect(screen.getByText('Limits')).toBeInTheDocument();
    expect(screen.getByText("TODAY'S SCREEN TIME")).toBeInTheDocument();
  });

  it('renders the Add limit FAB', () => {
    mockUseLimits();
    render(<LimitsScreen familyId="fam-1" onOpenHistory={() => {}} onOpenTimeFrame={() => {}} />);
    expect(screen.getByText('+ Add limit')).toBeInTheDocument();
  });

  it('renders the App limits section header with a count', () => {
    mockUseLimits({ limits: [{ packageName: 'com.a', dailyLimitMinutes: 30 }] });
    render(<LimitsScreen familyId="fam-1" onOpenHistory={() => {}} onOpenTimeFrame={() => {}} />);
    expect(screen.getByText('App limits')).toBeInTheDocument();
    expect(screen.getByText('1 app')).toBeInTheDocument();
  });

  it('renders a progress bar with ARIA semantics', () => {
    mockUseLimits();
    render(<LimitsScreen familyId="fam-1" onOpenHistory={() => {}} onOpenTimeFrame={() => {}} />);
    expect(screen.getAllByRole('progressbar').length).toBeGreaterThan(0);
  });
});
