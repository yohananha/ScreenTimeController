import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '../i18n/i18n';
import { ModeTiles } from './ModeTiles';

describe('ModeTiles', () => {
  afterEach(async () => {
    await act(async () => {
      await i18n.changeLanguage('en');
    });
  });

  it('marks the active tile pressed and the others not', () => {
    render(<ModeTiles active="limits" onSelect={() => {}} />);
    expect(screen.getByRole('button', { name: 'Limits' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Lock' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Allow' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('calls onSelect with the tapped mode', async () => {
    const onSelect = vi.fn();
    render(<ModeTiles active="limits" onSelect={onSelect} />);
    await userEvent.click(screen.getByRole('button', { name: 'Lock' }));
    expect(onSelect).toHaveBeenCalledWith('lock');
  });

  it('disables the tiles when there is no TV to switch modes on', async () => {
    const onSelect = vi.fn();
    render(<ModeTiles active="limits" onSelect={onSelect} disabled />);
    const lockTile = screen.getByRole('button', { name: 'Lock' });
    expect(lockTile).toBeDisabled();
    await userEvent.click(lockTile);
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('keeps the Hebrew mode labels on one line — the exact strings from README §5.7', async () => {
    await i18n.changeLanguage('he');
    render(<ModeTiles active="allow" onSelect={() => {}} />);
    expect(screen.getByRole('button', { name: 'נעילה' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'מגבלות' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'הכול מותר' })).toHaveAttribute('aria-pressed', 'true');
  });
});
