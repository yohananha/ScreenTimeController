import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { RulesScreen } from './RulesScreen';
import { useLimits } from '../../hooks/useLimits';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { UNLIMITED } from '../../models/Limits';
import { defaultLockoutSettings } from '../../models/LockoutSettings';
import { DEFAULT_TIME_FRAME_SCHEDULE } from '../../models/TimeFrameSchedule';

vi.mock('../../hooks/useLimits');
vi.mock('../../hooks/usePairedDevices');

function renderRules() {
  vi.mocked(usePairedDevices).mockReturnValue({
    devices: [],
    state: { busy: false, success: false, message: null },
    claim: vi.fn(),
    reset: vi.fn(),
    rename: vi.fn(),
    unpair: vi.fn(),
  });
  render(
    <MemoryRouter>
      <RulesScreen familyId="fam-1" onOpenFamily={() => {}} onOpenTimeFrame={() => {}} />
    </MemoryRouter>,
  );
}

describe('RulesScreen', () => {
  it('shows the general rules and the app limits list', () => {
    vi.mocked(useLimits).mockReturnValue({
      state: {
        limits: [{ packageName: 'com.example.youtube', dailyLimitMinutes: 60 }, { packageName: 'com.example.disney', dailyLimitMinutes: UNLIMITED }],
        overallDailyMinutes: 240,
        availableApps: [{ packageName: 'com.example.youtube', label: 'YouTube' }, { packageName: 'com.example.disney', label: 'Disney+' }],
        lockout: defaultLockoutSettings(),
        timeFrame: DEFAULT_TIME_FRAME_SCHEDULE,
        allowAllDayActive: false,
        instantLocked: false,
        usagePerApp: {},
        totalUsageMillis: 0,
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
    renderRules();
    expect(screen.getByText('Overall daily limit')).toBeInTheDocument();
    expect(screen.getByText('4h per day')).toBeInTheDocument();
    expect(screen.getByText('YouTube')).toBeInTheDocument();
    expect(screen.getByText('1h per day')).toBeInTheDocument();
    expect(screen.getByText('Disney+')).toBeInTheDocument();
    expect(screen.getByText('Always allowed')).toBeInTheDocument();
  });
});
