import { act, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { BarreProgression } from '../../../shared/feedback/BarreProgression/BarreProgression';

describe('BarreProgression', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('animates the displayed value to the target and applies the gradient', async () => {
    const frameCallbacks: Array<(time: number) => void> = [];
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => {
      frameCallbacks.push(callback);
      return frameCallbacks.length;
    });
    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => undefined);

    render(
      <BarreProgression
        currentValue={60}
        gradient="linear-gradient(90deg, #3967F6, #16A36B)"
        maxValue={100}
        title="Stockage disponible"
        valueFormatter={(value) => `${Math.round(value)} unités`}
      />,
    );

    const progressBar = screen.getByRole('progressbar', { name: 'Stockage disponible' });
    expect(progressBar).toHaveAttribute('aria-valuenow', '0');
    expect(progressBar.querySelector('.shared-progress-bar__fill')).toHaveStyle({
      background: 'linear-gradient(90deg, #3967F6, #16A36B)',
    });

    act(() => frameCallbacks.shift()?.(0));
    act(() => frameCallbacks.shift()?.(600));

    await waitFor(() => expect(progressBar).toHaveAttribute('aria-valuenow', '60'));
    expect(screen.getByText('60 unités / 100 unités')).toBeVisible();
  });

  it('shows the target immediately when reduced motion is preferred', async () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList);

    render(
      <BarreProgression
        currentValue={60}
        gradient="linear-gradient(90deg, #3967F6, #16A36B)"
        maxValue={100}
        title="Stockage disponible"
      />,
    );

    await waitFor(() => {
      expect(screen.getByRole('progressbar', { name: 'Stockage disponible' }))
        .toHaveAttribute('aria-valuenow', '60');
    });
  });
});