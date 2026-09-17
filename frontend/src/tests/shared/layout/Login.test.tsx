import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Login } from '../../../shared/layout/Login/Login';

describe('Login', () => {
  it('opens the connection form without submitting when disconnected', async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();

    render(<Login onLogin={onLogin} />);

    const loginButton = screen.getByRole('button', { name: 'Se connecter' });

    expect(loginButton).toBeVisible();
    await user.click(loginButton);

    expect(screen.getByRole('form', { name: 'Connexion' })).toBeVisible();
    expect(screen.getByLabelText('Pseudo')).toBeVisible();
    expect(screen.getByLabelText('Mot de passe')).toBeVisible();
    expect(onLogin).not.toHaveBeenCalled();
    expect(screen.queryByRole('menuitem', { name: 'Se déconnecter' })).not.toBeInTheDocument();
  });

  it('submits the connection form with the entered pseudo', async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();

    render(<Login onLogin={onLogin} />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    const connectionForm = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(connectionForm).getByLabelText('Pseudo'), 'Alice Martin');
    await user.type(within(connectionForm).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(connectionForm).getByRole('button', { name: 'Se connecter' }));

    expect(onLogin).toHaveBeenCalledOnce();
    expect(onLogin).toHaveBeenCalledWith({ name: 'Alice Martin', password: 'mot-de-passe' });
    expect(screen.queryByRole('form', { name: 'Connexion' })).not.toBeInTheDocument();
  });

  it('keeps the connection form open when the form surface is clicked', async () => {
    const user = userEvent.setup();

    render(<Login />);

    const loginButton = screen.getByRole('button', { name: 'Se connecter' });
    await user.click(loginButton);
    const connectionForm = screen.getByRole('form', { name: 'Connexion' });

    await user.click(screen.getByRole('dialog'));

    expect(connectionForm).toBeVisible();
  });

  it('keeps the connection form open when an error is already displayed', async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn().mockRejectedValue(new Error('Identifiants invalides'));

    render(<Login error="Identifiants invalides" onLogin={onLogin} />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    const connectionForm = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(connectionForm).getByLabelText('Pseudo'), 'Alice Martin');
    await user.type(within(connectionForm).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(connectionForm).getByRole('button', { name: 'Se connecter' }));

    expect(onLogin).toHaveBeenCalledOnce();
    expect(connectionForm).toBeVisible();
  });

  it('submits the registration form through its dedicated callback', async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();
    const onRegister = vi.fn();

    render(<Login onLogin={onLogin} onRegister={onRegister} />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    await user.click(screen.getByRole('button', { name: 'Pas encore inscrit' }));
    const registrationForm = screen.getByRole('form', { name: 'Inscription' });
    await user.type(within(registrationForm).getByLabelText('Pseudo'), ' Alice Martin ');
    await user.type(within(registrationForm).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(registrationForm).getByRole('button', { name: 'Développeur' }));
    await user.click(within(registrationForm).getByRole('button', { name: "S'inscrire" }));

    expect(onRegister).toHaveBeenCalledOnce();
    expect(onRegister).toHaveBeenCalledWith({
      name: 'Alice Martin',
      password: 'mot-de-passe',
      roles: ['developpeur', 'utilisateur'],
    });
    expect(onLogin).not.toHaveBeenCalled();
  });

  it('keeps the registration form open and blocks a password shorter than eight characters', async () => {
    const user = userEvent.setup();
    const onRegister = vi.fn();

    render(<Login onRegister={onRegister} />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    await user.click(screen.getByRole('button', { name: 'Pas encore inscrit' }));
    const registrationForm = screen.getByRole('form', { name: 'Inscription' });
    await user.type(within(registrationForm).getByLabelText('Pseudo'), 'Alice Martin');
    const passwordInput = within(registrationForm).getByLabelText('Mot de passe');
    await user.type(passwordInput, 'court');
    await user.click(within(registrationForm).getByRole('button', { name: "S'inscrire" }));

    expect(onRegister).not.toHaveBeenCalled();
    expect(screen.getByText('8 à 255 caractères.')).toBeVisible();
    expect(screen.getByRole('form', { name: 'Inscription' })).toBeVisible();
  });

  it('toggles cumulative roles with accessible pressed states', async () => {
    const user = userEvent.setup();

    render(<Login />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    await user.click(screen.getByRole('button', { name: 'Pas encore inscrit' }));

    const registrationForm = screen.getByRole('form', { name: 'Inscription' });
    const developerRole = within(registrationForm).getByRole('button', { name: 'Développeur' });
    const administratorRole = within(registrationForm).getByRole('button', { name: 'Administrateur' });
    const userRole = within(registrationForm).getByRole('button', { name: 'Utilisateur' });

    expect(userRole).toHaveAttribute('aria-pressed', 'true');
    expect(developerRole).toHaveAttribute('aria-pressed', 'false');
    expect(administratorRole).toHaveAttribute('aria-pressed', 'false');
    expect(administratorRole).toBeDisabled();

    await user.click(developerRole);

    expect(developerRole).toHaveAttribute('aria-pressed', 'true');
    expect(administratorRole).toHaveAttribute('aria-pressed', 'false');
    expect(userRole).toHaveAttribute('aria-pressed', 'true');
  });

  it('keeps the connection form open for a pseudo made only of spaces', async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();

    render(<Login onLogin={onLogin} />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    const connectionForm = screen.getByRole('form', { name: 'Connexion' });
    await user.type(within(connectionForm).getByLabelText('Pseudo'), '   ');
    await user.type(within(connectionForm).getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(within(connectionForm).getByRole('button', { name: 'Se connecter' }));

    expect(onLogin).not.toHaveBeenCalled();
    expect(screen.getByRole('form', { name: 'Connexion' })).toBeVisible();
  });

  it('switches from the connection form to the registration form', async () => {
    const user = userEvent.setup();

    render(<Login />);

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    await user.click(screen.getByRole('button', { name: 'Pas encore inscrit' }));

    expect(screen.queryByRole('form', { name: 'Connexion' })).not.toBeInTheDocument();
    expect(screen.getByRole('form', { name: 'Inscription' })).toBeVisible();
    expect(screen.getByRole('button', { name: "S'inscrire" })).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Déjà inscrit' }));

    expect(screen.getByRole('form', { name: 'Connexion' })).toBeVisible();
  });

  it('reveals the sign-out action on hover for a connected user', async () => {
    const user = userEvent.setup();
    const onLogout = vi.fn();

    render(<Login onLogout={onLogout} user={{ name: 'Alice Martin' }} />);

    const accountButton = screen.getByRole('button', { name: 'Alice Martin' });

    expect(accountButton).toHaveAttribute('aria-expanded', 'false');

    await user.hover(accountButton);

    const logoutButton = await screen.findByRole('menuitem', { name: 'Se déconnecter' });

    expect(logoutButton).toBeVisible();
    expect(accountButton).toHaveAttribute('aria-expanded', 'true');

    await user.click(logoutButton);

    expect(onLogout).toHaveBeenCalledOnce();
  });

  it('opens the connected menu with focus and closes it with Escape', async () => {
    const user = userEvent.setup();

    render(<Login user={{ name: 'Alice Martin' }} />);

    await user.tab();

    const accountButton = screen.getByRole('button', { name: 'Alice Martin' });

    expect(accountButton).toHaveFocus();
    expect(await screen.findByRole('menuitem', { name: 'Se déconnecter' })).toBeVisible();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('menuitem', { name: 'Se déconnecter' })).not.toBeInTheDocument();
    expect(accountButton).toHaveAttribute('aria-expanded', 'false');
  });
});
