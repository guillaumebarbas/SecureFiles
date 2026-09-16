import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BackendStatus } from '../../../shared/layout/BackendStatus/BackendStatus';

describe('BackendStatus', () => {
  it('renders an accessible indicator for the current backend state', () => {
    render(<BackendStatus status="online" />);

    const statusButton = screen.getByRole('button', { name: 'Service online' });

    expect(statusButton).toBeVisible();
    expect(statusButton).toHaveClass('backend-status--online');
    expect(statusButton.querySelector('span.shared-row')).toBeTruthy();
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });
});