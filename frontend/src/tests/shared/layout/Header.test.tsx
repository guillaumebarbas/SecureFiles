import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Header } from '../../../shared/layout/Header/Header';

describe('Header', () => {
  it('renders its route context and received elements', () => {
    render(
      <Header
        eyebrow="Vue d'ensemble"
        title="Dashboard"
      >
        <span>Contexte de la vue</span>
        <span>Action de la vue</span>
      </Header>,
    );

    const header = screen.getByRole('banner');
    const heading = header.querySelector('.shared-row.app-header__heading');
    const content = header.querySelector('.shared-row.app-header__content');

    expect(screen.getByRole('heading', { name: 'Dashboard', level: 1 })).toBeVisible();
    expect(screen.getByText("Vue d'ensemble")).toHaveClass('app-header__eyebrow');
    expect(screen.queryByText("SecureFiles / Vue d'ensemble")).not.toBeInTheDocument();
    expect(screen.queryByText("Une vue d'accueil pour suivre rapidement l'etat du service.")).not.toBeInTheDocument();
    expect(screen.getByText('Contexte de la vue')).toBeVisible();
    expect(screen.getByText('Action de la vue')).toBeVisible();
    expect(heading).toBeTruthy();
    expect(heading?.querySelector('h1')).toHaveTextContent('Dashboard');
    expect(heading?.firstElementChild).toHaveTextContent('Dashboard');
    expect(heading?.lastElementChild).toHaveTextContent("Vue d'ensemble");
    expect(content).toBeTruthy();
    expect(content?.querySelector('.app-header__description')).toBeNull();
  });
});