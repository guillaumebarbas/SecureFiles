import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Header } from '../../../shared/layout/Header/Header';

describe('Header', () => {
  it('renders its title and received elements in a row', () => {
    render(
      <Header title="SecureFiles">
        <span>Contexte de la vue</span>
        <span>Action de la vue</span>
      </Header>,
    );

    const header = screen.getByRole('banner');

    expect(screen.getByRole('heading', { name: 'SecureFiles', level: 1 })).toBeVisible();
    expect(screen.getByText('Contexte de la vue')).toBeVisible();
    expect(screen.getByText('Action de la vue')).toBeVisible();
    expect(header.querySelector('.shared-row.app-header__row')).toBeTruthy();
    expect(header.querySelector('.shared-row.app-header__content')).toBeTruthy();
  });
});