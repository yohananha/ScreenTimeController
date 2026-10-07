import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AccountSection } from './AccountSection';
import { useAccountDeletion } from '../hooks/useAccountDeletion';
import { signOut } from '../firebase/authRepository';

vi.mock('../hooks/useAccountDeletion');
vi.mock('../firebase/authRepository', () => ({ signOut: vi.fn(async () => undefined) }));

function setup(isOwner: boolean) {
  const actions = { busy: false, error: null, deleteAccount: vi.fn(), deleteFamily: vi.fn() };
  vi.mocked(useAccountDeletion).mockReturnValue(actions);
  render(<AccountSection familyId="fam-1" isOwner={isOwner} />);
  return actions;
}

describe('AccountSection', () => {
  it('a member sees only "Delete my account", with a warning that the family stays', async () => {
    const actions = setup(false);
    expect(screen.queryByRole('button', { name: 'Delete family' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Delete my account' }));
    expect(screen.getByText(/The family and its data stay/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Delete permanently' }));
    expect(actions.deleteAccount).toHaveBeenCalled();
  });

  it('the owner is warned that deleting the account deletes the family for everyone', async () => {
    setup(true);
    await userEvent.click(screen.getByRole('button', { name: 'Delete my account' }));
    expect(screen.getByText(/and the family you own — for every member/)).toBeInTheDocument();
  });

  it('the owner can delete just the family; nothing happens without confirming', async () => {
    const actions = setup(true);
    await userEvent.click(screen.getByRole('button', { name: 'Delete family' }));
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(actions.deleteFamily).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Delete family' }));
    await userEvent.click(screen.getByRole('button', { name: 'Delete permanently' }));
    expect(actions.deleteFamily).toHaveBeenCalled();
  });

  it('Sign out asks first: Cancel does nothing, confirming signs out', async () => {
    setup(false);
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Sign out?')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(signOut).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sign out' }));
    expect(signOut).toHaveBeenCalledTimes(1);
  });
});
