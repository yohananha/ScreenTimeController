import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CodeSlotInput } from './CodeSlotInput';

function type(value: string, props: { slots?: number; alphanumeric?: boolean } = {}) {
  const onValueChange = vi.fn();
  render(<CodeSlotInput value="" onValueChange={onValueChange} {...props} />);
  fireEvent.change(screen.getByLabelText('Code'), { target: { value } });
  return onValueChange;
}

describe('CodeSlotInput', () => {
  it('default (pairing) mode keeps digits only, capped at 6', () => {
    expect(type('12a3-45678')).toHaveBeenCalledWith('123456');
  });

  it('alphanumeric (invite) mode uppercases and drops separators', () => {
    expect(type('abcd-2345', { slots: 8, alphanumeric: true })).toHaveBeenCalledWith('ABCD2345');
  });

  it('alphanumeric mode drops the 0/O/1/I look-alikes', () => {
    expect(type('o0i1ab', { slots: 8, alphanumeric: true })).toHaveBeenCalledWith('AB');
  });

  it('alphanumeric mode caps input at the slot count', () => {
    expect(type('ABCDEFGHJK', { slots: 8, alphanumeric: true })).toHaveBeenCalledWith('ABCDEFGH');
  });
});
