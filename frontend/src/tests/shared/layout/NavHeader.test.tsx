import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { NavHeader } from '../../../shared/layout/NavHeader/NavHeader';

describe('NavHeader', () => {
  it('renders the application identity and description', () => {
    const { container } = render(
      <NavHeader description="Console de dépôt sécurisé" title="SecureFiles" />,
    );

    expect(screen.getByRole('heading', { name: 'SecureFiles', level: 2 })).toBeVisible();
    expect(screen.getByText('Console de dépôt sécurisé')).toBeVisible();
    expect(container.querySelector('.shared-row.nav-header')).toBeTruthy();
    expect(container.querySelector('.shared-column.nav-header__content')).toBeTruthy();
    expect(container.querySelector('.nav-header__icon')).toBeTruthy();
  });
});