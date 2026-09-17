import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  checkBackendHealth,
  getCurrentUser,
  getUploadConfiguration,
  listFiles,
  loginUser,
  logoutUser,
  registerUser,
  type FilesPageResponse,
  type UserProfile,
} from '../api/filesApi';
import { App } from '../App';

vi.mock('../api/filesApi', () => ({
  checkBackendHealth: vi.fn(),
  getCurrentUser: vi.fn(),
  getUploadConfiguration: vi.fn(),
  listFiles: vi.fn(),
  loginUser: vi.fn(),
  logoutUser: vi.fn(),
  registerUser: vi.fn(),
}));

const mockedCheckBackendHealth = vi.mocked(checkBackendHealth);
const mockedGetCurrentUser = vi.mocked(getCurrentUser);
const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedListFiles = vi.mocked(listFiles);
const mockedLoginUser = vi.mocked(loginUser);
const mockedLogoutUser = vi.mocked(logoutUser);
const mockedRegisterUser = vi.mocked(registerUser);

const emptyFilesPage: FilesPageResponse = {
  content: [],
  hasNext: false,
  hasPrevious: false,
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 0,
};

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
    mockedCheckBackendHealth.mockResolvedValue('online');
    mockedGetCurrentUser.mockRejectedValue(new Error('Not authenticated'));
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
    mockedListFiles.mockResolvedValue(emptyFilesPage);
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

  it('loads the recent files register when the visitor has no session', async () => {
    mockedGetCurrentUser.mockResolvedValue(undefined);
    mockedListFiles.mockResolvedValue({
      content: [{
        author: 'Alice Martin',
        createdAt: '2026-09-15T10:00:00Z',
        fileId: '11111111-1111-1111-1111-111111111111',
        originalFilename: 'public-document.txt',
        sizeBytes: 12,
        status: 'CLEAN',
      }],
      hasNext: false,
      hasPrevious: false,
      page: 1,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });

    render(<App />);

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
    expect(await within(table).findByText('public-document.txt')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeVisible();
  });

  it('opens the connection form before an anonymous visitor selects a file', async () => {
    const user = userEvent.setup();

    render(<App />);

    await user.click(screen.getByText('Choisissez un fichier'));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Vous devez etre connecte pour selectionner un fichier.',
    );
    expect(await screen.findByRole('form', { name: 'Connexion' })).toBeVisible();
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

  it('loads the authenticated account and displays cumulative roles', async () => {
    mockedGetCurrentUser.mockResolvedValue({
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['developpeur', 'utilisateur'],
    });

    render(<App />);

    expect(await screen.findByRole('button', { name: 'Alice Martin' })).toBeVisible();
    await userEvent.click(screen.getByRole('link', { name: 'Profil' }));
    expect(await screen.findByText('developpeur')).toBeVisible();
    expect(screen.getByText('utilisateur')).toBeVisible();
  });

  it('logs in through the backend and keeps the session in the account state', async () => {
    const user = userEvent.setup();
    const profile: UserProfile = {
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    };
    mockedLoginUser.mockResolvedValue(profile);

    render(<App />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    const form = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(form).getByLabelText('Pseudo'), 'Alice Martin');
    await user.type(within(form).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(form).getByRole('button', { name: 'Se connecter' }));

    expect(await screen.findByRole('button', { name: 'Alice Martin' })).toBeVisible();
    expect(mockedLoginUser).toHaveBeenCalledWith({ name: 'Alice Martin', password: 'mot-de-passe' });
  });

  it('keeps the connection form open when the backend rejects the password', async () => {
    const user = userEvent.setup();
    mockedLoginUser.mockRejectedValue(new Error('Le pseudo ou le mot de passe est invalide.'));

    render(<App />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    const form = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(form).getByLabelText('Pseudo'), 'Alice Martin');
    await user.type(within(form).getByLabelText('Mot de passe'), 'mauvais-mot-de-passe');
    await user.click(within(form).getByRole('button', { name: 'Se connecter' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Le pseudo ou le mot de passe est invalide.',
    );
    expect(screen.getByRole('form', { name: 'Connexion' })).toBeVisible();
  });

  it('registers then logs in with the selected cumulative roles', async () => {
    const user = userEvent.setup();
    const profile: UserProfile = {
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['developpeur', 'utilisateur'],
    };
    mockedRegisterUser.mockResolvedValue(profile);
    mockedLoginUser.mockResolvedValue(profile);

    render(<App />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    await user.click(screen.getByRole('button', { name: 'Pas encore inscrit' }));
    const form = screen.getByRole('form', { name: 'Inscription' });
    await user.type(within(form).getByLabelText('Pseudo'), 'Alice Martin');
    await user.type(within(form).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(form).getByRole('button', { name: 'Développeur' }));
    await user.click(within(form).getByRole('button', { name: "S'inscrire" }));

    expect(await screen.findByRole('button', { name: 'Alice Martin' })).toBeVisible();
    expect(mockedRegisterUser).toHaveBeenCalledWith({
      name: 'Alice Martin',
      password: 'mot-de-passe',
      roles: ['developpeur', 'utilisateur'],
    });
    expect(mockedLoginUser).toHaveBeenCalledWith({ name: 'Alice Martin', password: 'mot-de-passe' });
  });

  it('logs out through the backend and clears the account state', async () => {
    const user = userEvent.setup();
    mockedGetCurrentUser.mockResolvedValue({
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    });
    mockedLogoutUser.mockResolvedValue();

    render(<App />);

    await user.hover(await screen.findByRole('button', { name: 'Alice Martin' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Se déconnecter' }));

    expect(mockedLogoutUser).toHaveBeenCalledOnce();
    expect(await screen.findByRole('button', { name: 'Se connecter' })).toBeVisible();
  });
});