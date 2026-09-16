import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProfilePage } from '../../../pages/Profile/ProfilePage';

describe('ProfilePage', () => {
  it('shows the profile name and current role', () => {
    render(<ProfilePage />);

    expect(screen.getByRole('heading', { name: 'Informations du profil', level: 2 })).toBeVisible();
    expect(screen.getByText('Utilisateur local')).toBeVisible();
    expect(screen.getByText('Rôle')).toBeVisible();
    expect(screen.getByText('utilisateur')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Rôles disponibles', level: 2 })).not.toBeInTheDocument();
    expect(screen.queryByText('developpeur')).not.toBeInTheDocument();
    expect(screen.queryByText('admin')).not.toBeInTheDocument();
  });
});
