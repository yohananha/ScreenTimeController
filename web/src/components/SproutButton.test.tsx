import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SproutPrimaryButton, SproutGhostButton } from './SproutButton';

describe('SproutPrimaryButton', () => {
  it('renders enabled and responds to clicks', () => {
    const onClick = vi.fn();
    render(<SproutPrimaryButton onClick={onClick}>Save</SproutPrimaryButton>);
    const button = screen.getByRole('button', { name: 'Save' });
    expect(button).not.toBeDisabled();
    button.click();
    expect(onClick).toHaveBeenCalledOnce();
  });

  it('renders disabled', () => {
    render(
      <SproutPrimaryButton onClick={() => {}} disabled>
        Save
      </SproutPrimaryButton>,
    );
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
  });
});

describe('SproutGhostButton', () => {
  it('renders its label', () => {
    render(<SproutGhostButton onClick={() => {}}>Cancel</SproutGhostButton>);
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });
});
