import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatusBadge, type Status } from './StatusBadge';

describe('StatusBadge', () => {
  const cases: [Status, string][] = [
    ['OnTrack', 'On track'],
    ['AlmostUp', 'Almost up'],
    ['TimesUp', "Time's up"],
    ['Paused', 'Paused'],
  ];

  it.each(cases)('renders the %s label', (status, label) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
