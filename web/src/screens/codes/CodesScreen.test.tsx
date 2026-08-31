import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CodesScreen } from './CodesScreen';
import { useCodes } from '../../hooks/useCodes';

vi.mock('../../hooks/useCodes');

describe('CodesScreen', () => {
  it('no-code state shows 4 placeholder tiles', () => {
    vi.mocked(useCodes).mockReturnValue({
      state: { isGenerating: false, active: null, error: null },
      generate: vi.fn(),
      dismiss: vi.fn(),
    });
    render(<CodesScreen familyId="fam-1" />);
    expect(screen.getAllByText('–')).toHaveLength(4);
    expect(screen.getByText('Generate a code below.')).toBeInTheDocument();
  });

  it('active-code state shows 6 digit tiles and the Single-use pill', () => {
    vi.mocked(useCodes).mockReturnValue({
      state: {
        isGenerating: false,
        active: { code: '123456', extraMinutes: 30, expiresAt: new Date(Date.now() + 60_000) },
        error: null,
      },
      generate: vi.fn(),
      dismiss: vi.fn(),
    });
    render(<CodesScreen familyId="fam-1" />);
    for (const digit of '123456') {
      expect(screen.getAllByText(digit).length).toBeGreaterThan(0);
    }
    expect(screen.getByText('Single-use')).toBeInTheDocument();
    expect(screen.getByText('Unlocks Everything for 30 min')).toBeInTheDocument();
  });
});
