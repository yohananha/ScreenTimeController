import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { UnlockSheet } from './UnlockSheet';
import { useCodes } from '../hooks/useCodes';

vi.mock('../hooks/useCodes');

describe('UnlockSheet', () => {
  it('choose state shows duration chips and a disabled "Everything" scope', () => {
    vi.mocked(useCodes).mockReturnValue({
      state: { isGenerating: false, active: null, error: null },
      generate: vi.fn(),
      dismiss: vi.fn(),
    });
    render(<UnlockSheet familyId="fam-1" onClose={() => {}} />);
    expect(screen.getByText('15 min')).toBeInTheDocument();
    expect(screen.getByText('1 hour')).toBeInTheDocument();
    expect(screen.getByText('Rest of day')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Everything' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Generate code' })).toBeInTheDocument();
  });

  it('generates a code for the selected duration', async () => {
    const generate = vi.fn();
    vi.mocked(useCodes).mockReturnValue({
      state: { isGenerating: false, active: null, error: null },
      generate,
      dismiss: vi.fn(),
    });
    render(<UnlockSheet familyId="fam-1" onClose={() => {}} />);
    await userEvent.click(screen.getByText('1 hour'));
    await userEvent.click(screen.getByRole('button', { name: 'Generate code' }));
    expect(generate).toHaveBeenCalledWith(60);
  });

  it('ready state shows the 6 code digits and can start a new code', async () => {
    const dismiss = vi.fn();
    vi.mocked(useCodes).mockReturnValue({
      state: { isGenerating: false, active: { code: '482193', extraMinutes: 30, expiresAt: new Date(Date.now() + 60_000) }, error: null },
      generate: vi.fn(),
      dismiss,
    });
    render(<UnlockSheet familyId="fam-1" onClose={() => {}} />);
    for (const digit of '482193') {
      expect(screen.getAllByText(digit).length).toBeGreaterThan(0);
    }
    expect(screen.getByText('Everything · 30m')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'New code' }));
    expect(dismiss).toHaveBeenCalledTimes(1);
  });

  it('closes when the scrim is clicked', async () => {
    vi.mocked(useCodes).mockReturnValue({
      state: { isGenerating: false, active: null, error: null },
      generate: vi.fn(),
      dismiss: vi.fn(),
    });
    const onClose = vi.fn();
    render(<UnlockSheet familyId="fam-1" onClose={onClose} />);
    await userEvent.click(screen.getByTestId('sheet-scrim'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
