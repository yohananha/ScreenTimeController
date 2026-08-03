import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ChipGroup } from './Chips';

describe('ChipGroup', () => {
  it('shows all options', () => {
    render(<ChipGroup options={[15, 30, 60]} selected={30} onSelect={() => {}} label={(v) => `${v}m`} />);
    expect(screen.getByText('15m')).toBeInTheDocument();
    expect(screen.getByText('30m')).toBeInTheDocument();
    expect(screen.getByText('60m')).toBeInTheDocument();
  });

  it('selection toggles on click', async () => {
    const onSelect = vi.fn();
    render(<ChipGroup options={[15, 30, 60]} selected={30} onSelect={onSelect} label={(v) => `${v}m`} />);
    await userEvent.click(screen.getByText('60m'));
    expect(onSelect).toHaveBeenCalledWith(60);
  });
});
