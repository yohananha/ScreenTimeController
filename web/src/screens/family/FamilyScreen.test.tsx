import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { FamilyScreen } from './FamilyScreen';
import { useFamily } from '../../hooks/useFamily';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { useLanguage } from '../../hooks/useLanguage';
import { useNotificationStatus } from '../../hooks/useNotifications';

vi.mock('../../hooks/useFamily');
vi.mock('../../hooks/usePairedDevices');
vi.mock('../../hooks/useLanguage');
vi.mock('../../hooks/useNotifications');

describe('FamilyScreen', () => {
  it('shows the owner, a paired TV, and the language/notifications rows', () => {
    vi.mocked(useFamily).mockReturnValue({
      state: {
        inviteCode: null,
        joining: false,
        error: null,
        family: { id: 'fam-1', ownerUid: 'me', members: { me: 'ADMIN' }, deviceIds: [] },
        displayNames: {},
      },
      createFamily: vi.fn(),
      generateInvite: vi.fn(),
      joinByCode: vi.fn(),
      setMemberRole: vi.fn(),
      removeMember: vi.fn(),
    });
    vi.mocked(usePairedDevices).mockReturnValue({
      devices: [{ id: 'tv-1', name: 'Living Room TV' }],
      state: { busy: false, success: false, message: null },
      claim: vi.fn(),
      reset: vi.fn(),
      rename: vi.fn(),
      unpair: vi.fn(),
    });
    vi.mocked(useLanguage).mockReturnValue({ select: vi.fn(), current: null });
    vi.mocked(useNotificationStatus).mockReturnValue({ status: 'granted', enabling: false, enable: vi.fn() });

    render(
      <MemoryRouter>
        <FamilyScreen familyId="fam-1" uid="me" />
      </MemoryRouter>,
    );

    expect(screen.getByText('You')).toBeInTheDocument();
    expect(screen.getByText('Owner · you')).toBeInTheDocument();
    expect(screen.getByText('Living Room TV')).toBeInTheDocument();
    expect(screen.getByText('Language')).toBeInTheDocument();
    expect(screen.getByText('Notifications')).toBeInTheDocument();
  });
});
