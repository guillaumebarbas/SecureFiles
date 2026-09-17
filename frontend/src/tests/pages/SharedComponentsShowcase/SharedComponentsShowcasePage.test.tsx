import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getUploadConfiguration } from '../../../api/filesApi';
import { SharedComponentsShowcasePage } from '../../../pages/SharedComponentsShowcase/SharedComponentsShowcasePage';

vi.mock('../../../api/filesApi', () => ({
  getUploadConfiguration: vi.fn(),
}));

const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);

describe('SharedComponentsShowcasePage', () => {
  beforeEach(() => {
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
  });

  it('displays the submitted search value', async () => {
    const user = userEvent.setup();

    render(<SharedComponentsShowcasePage />);

    await user.type(screen.getByRole('searchbox', { name: /rechercher/i }), 'rapport.pdf');
    await user.click(screen.getByRole('button', { name: /entrer/i }));

    expect(screen.getByText('Recherche envoyée : rapport.pdf')).toBeVisible();
  });

  it('presents the reusable component examples', () => {
    render(<SharedComponentsShowcasePage />);

    expect(screen.getByRole('heading', { name: 'Actions' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Action principale' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upload de fichier' })).toBeVisible();
    expect(screen.getByLabelText('Choisir un fichier')).toBeInTheDocument();

    const table = screen.getByRole('table', { name: 'Exemple de registre' });

    expect(within(table).getByText('CLEAN')).toBeVisible();
  });

  it('demonstrates disconnected and connected login states', async () => {
    const user = userEvent.setup();

    render(<SharedComponentsShowcasePage />);

    const loginButton = screen.getByRole('button', { name: 'Se connecter' });

    expect(loginButton).toBeVisible();
    await user.click(loginButton);

    const connectionForm = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(connectionForm).getByLabelText('Pseudo'), 'Utilisateur de démonstration');
    await user.type(within(connectionForm).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(connectionForm).getByRole('button', { name: 'Se connecter' }));

    const accountButton = screen.getByRole('button', { name: 'Utilisateur de démonstration' });

    expect(accountButton).toBeVisible();
    await user.click(accountButton);
    expect(await screen.findByRole('menuitem', { name: 'Se déconnecter' })).toBeVisible();

    await user.click(screen.getByRole('menuitem', { name: 'Se déconnecter' }));

    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeVisible();
  });
});