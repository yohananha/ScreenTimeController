import { afterEach, describe, expect, it } from 'vitest';
import { act, render } from '@testing-library/react';
import i18n from '../i18n/i18n';
import { TimeGauge } from './TimeGauge';
import { formatHoursMinutesClock } from '../i18n/format';

describe('TimeGauge', () => {
  afterEach(async () => {
    await act(async () => {
      await i18n.changeLanguage('en');
    });
  });

  it('renders the h:mm label and word, and leaves the track unmirrored', () => {
    const { container } = render(<TimeGauge centerLabel={formatHoursMinutesClock(85)} word="left" progress={0.5} />);
    const texts = container.querySelectorAll('text');
    expect(texts[0]).toHaveTextContent('1:25');
    expect(texts[1]).toHaveTextContent('left');
    const [track] = container.querySelectorAll('path');
    expect(track).not.toHaveAttribute('transform');
  });

  it('does not mirror the fill path in LTR', async () => {
    await i18n.changeLanguage('en');
    const { container } = render(<TimeGauge centerLabel="1:25" word="left" progress={0.5} />);
    const paths = container.querySelectorAll('path');
    const fill = paths[1];
    expect(fill).not.toHaveAttribute('transform');
  });

  it('mirrors only the fill path in RTL, never the digits', async () => {
    await i18n.changeLanguage('he');
    const { container } = render(<TimeGauge centerLabel="1:25" word="נשארו" progress={0.5} />);
    const paths = container.querySelectorAll('path');
    const [track, fill] = paths;
    expect(track).not.toHaveAttribute('transform');
    expect(fill).toHaveAttribute('transform', 'translate(300 0) scale(-1 1)');
    const texts = container.querySelectorAll('text');
    expect(texts[0]).toHaveTextContent('1:25');
  });

  it('switches the fill to the over colour and skips the gradient when overLimit is set', () => {
    const { container } = render(<TimeGauge centerLabel="0:00" word="over" progress={1} overLimit />);
    const paths = container.querySelectorAll('path');
    const fill = paths[1]!;
    expect(fill).toHaveAttribute('stroke', '#B3261E');
  });

  it('renders no fill path at zero progress', () => {
    const { container } = render(<TimeGauge centerLabel="—" word="" progress={0} />);
    expect(container.querySelectorAll('path')).toHaveLength(1);
  });
});
