import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RequestStrip } from './RequestStrip';

describe('RequestStrip', () => {
  it('renders the title and subtitle', () => {
    render(
      <RequestStrip
        title="15 more minutes on YouTube?"
        subtitle="Asked from the TV · now"
        approveAria="Approve 15 minutes"
        denyAria="Deny"
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    expect(screen.getByText('15 more minutes on YouTube?')).toBeInTheDocument();
    expect(screen.getByText('Asked from the TV · now')).toBeInTheDocument();
  });

  it('calls onApprove and onDeny from their respective buttons', async () => {
    const onApprove = vi.fn();
    const onDeny = vi.fn();
    render(
      <RequestStrip
        title="15 more minutes on YouTube?"
        subtitle="Asked from the TV · now"
        approveAria="Approve 15 minutes"
        denyAria="Deny"
        onApprove={onApprove}
        onDeny={onDeny}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Approve 15 minutes' }));
    expect(onApprove).toHaveBeenCalledTimes(1);
    await userEvent.click(screen.getByRole('button', { name: 'Deny' }));
    expect(onDeny).toHaveBeenCalledTimes(1);
  });
});
