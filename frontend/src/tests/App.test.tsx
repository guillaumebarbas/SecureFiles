import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { checkBackendHealth, getUploadConfiguration, listFiles } from '../api/filesApi';
import { App } from '../App';

vi.mock('../api/filesApi', () => ({
  checkBackendHealth: vi.fn(),
  getUploadConfiguration: vi.fn(),
  listFiles: vi.fn(),
}));

const mockedCheckBackendHealth = vi.mocked(checkBackendHealth);
const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedListFiles = vi.mocked(listFiles);

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
    mockedCheckBackendHealth.mockResolvedValue('online');
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
    mockedListFiles.mockResolvedValue([]);
  });

  afterEach(() => {
    window.history.replaceState({}, '', '/');
  });

  it('renders the dashboard as the active default view', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Dashboard', level: 1 })).toBeVisible();
    expect(screen.getByText("Vue d'ensemble")).toHaveClass('app-header__eyebrow');
    expect(screen.queryByText("SecureFiles / Vue d'ensemble")).not.toBeInTheDocument();
    expect(screen.queryByText(
      "Une vue d'accueil pour suivre rapidement l'etat du service et retrouver les zones de travail.",
    )).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Service online' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page');
  });

  it('does not expose the files view in the primary navigation', () => {
    render(<App />);

    expect(screen.getByRole('banner')).toBeVisible();
    expect(screen.getByRole('complementary', { name: 'Navigation SecureFiles' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Fichiers' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Bibliotheque' })).toBeVisible();
  });

  it('opens the shared components library on its dedicated route', async () => {
    const user = userEvent.setup();

    render(<App />);
    await user.click(screen.getByRole('link', { name: 'Bibliotheque' }));

    expect(window.location.pathname).toBe('/components');
    expect(screen.getByRole('link', { name: 'Bibliotheque' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Composants reutilisables', level: 1 })).toBeVisible();
    expect(screen.getByText('Bibliotheque partagee')).toHaveClass('app-header__eyebrow');
    expect(screen.queryByText('SecureFiles / Bibliotheque partagee')).not.toBeInTheDocument();
  });

  it('opens the profile page from the primary navigation', async () => {
    const user = userEvent.setup();

    render(<App />);
    await user.click(screen.getByRole('link', { name: 'Profil' }));

    expect(window.location.pathname).toBe('/profile');
    expect(screen.getByRole('link', { name: 'Profil' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Profil', level: 1 })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Informations du profil', level: 2 })).toBeVisible();
  });

  it('renders the offline service state when the health check fails', async () => {
    mockedCheckBackendHealth.mockResolvedValue('offline');

    render(<App />);

    expect(await screen.findByRole('button', { name: 'Service offline' })).toBeVisible();
  });
});