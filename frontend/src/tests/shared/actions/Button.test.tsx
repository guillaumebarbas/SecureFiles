import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Download } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';
import { Button } from '../../../shared/actions/Button';

describe('Button', () => {
  it('renders text and an optional icon without adding a tooltip', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();

    render(
      <Button variant="primary" icon={Download} onClick={onClick}>
        Telecharger
      </Button>,
    );

    const button = screen.getByRole('button', { name: /telecharger/i });

    expect(button).toBeVisible();
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    await user.click(button);
    expect(onClick).toHaveBeenCalledOnce();
  });

  it('provides a tooltip for icon-only accessible actions and supports all visual variants', () => {
    render(
      <>
        <Button variant="primary" aria-label="Primaire" icon={Download} />
        <Button variant="secondary" aria-label="Secondaire" icon={Download} />
        <Button variant="gradient" aria-label="Gradient" icon={Download} />
      </>,
    );

    expect(screen.getByRole('button', { name: 'Primaire' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Secondaire' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Gradient' })).toBeVisible();
    expect(screen.getAllByRole('tooltip')).toHaveLength(3);
  });
});