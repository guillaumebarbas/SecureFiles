import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getStorageQuota } from '../../../api/filesApi';
import { ProfilePage } from '../../../pages/Profile/ProfilePage';

vi.mock('../../../api/filesApi', () => ({
  getStorageQuota: vi.fn(),
}));

const mockedGetStorageQuota = vi.mocked(getStorageQuota);

describe('ProfilePage', () => {
  beforeEach(() => {
    mockedGetStorageQuota.mockReset();
    mockedGetStorageQuota.mockResolvedValue({ quotaBytes: 100, usedBytes: 40 });
  });

  it('shows the profile name and current role', () => {
    render(<ProfilePage user={{
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['developpeur', 'utilisateur'],
    }} />);

    expect(screen.getByRole('heading', { name: 'Informations du profil', level: 2 })).toBeVisible();
    expect(screen.getByText('Alice Martin')).toBeVisible();
    expect(screen.getByText('Rôle')).toBeVisible();
    expect(screen.getByText('developpeur')).toBeVisible();
    expect(screen.getByText('utilisateur')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Rôles disponibles', level: 2 })).not.toBeInTheDocument();
    expect(screen.queryByText('admin')).not.toBeInTheDocument();
  });

  it('shows the available storage for the authenticated user', async () => {
    render(<ProfilePage user={{
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    }} />);

    expect(await screen.findByRole('heading', { name: 'Stockage disponible', level: 2 })).toBeVisible();
    expect(screen.getByRole('progressbar', { name: 'Stockage disponible' })).toHaveAttribute(
      'aria-valuemax',
      '100',
    );
    expect(mockedGetStorageQuota).toHaveBeenCalledWith(expect.objectContaining({ signal: expect.any(AbortSignal) }));
  });

  it('does not request storage for an anonymous user', () => {
    render(<ProfilePage />);

    expect(screen.getByText('Connectez-vous pour consulter votre stockage disponible.')).toBeVisible();
    expect(mockedGetStorageQuota).not.toHaveBeenCalled();
  });

  it('shows a retry action when storage cannot be loaded', async () => {
    const user = userEvent.setup();
    mockedGetStorageQuota
      .mockRejectedValueOnce(new Error('Le service de stockage est indisponible.'))
      .mockResolvedValueOnce({ quotaBytes: 100, usedBytes: 40 });

    render(<ProfilePage user={{
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    }} />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Le service de stockage est indisponible.');
    await user.click(screen.getByRole('button', { name: 'Réessayer' }));

    expect(await screen.findByRole('progressbar', { name: 'Stockage disponible' })).toBeVisible();
    expect(mockedGetStorageQuota).toHaveBeenCalledTimes(2);
  });

  it('clamps available storage at zero when the quota is full', async () => {
    mockedGetStorageQuota.mockResolvedValueOnce({ quotaBytes: 100, usedBytes: 100 });

    render(<ProfilePage user={{
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    }} />);

    const progressBar = await screen.findByRole('progressbar', { name: 'Stockage disponible' });
    expect(progressBar).toHaveAttribute('aria-valuemax', '100');
    expect(progressBar).toHaveAttribute('aria-valuenow', '0');
    expect(screen.getByText('0 octets / 100 octets')).toBeVisible();
  });

  it('formats a large quota with the console storage labels', async () => {
    mockedGetStorageQuota.mockResolvedValueOnce({
      quotaBytes: 10 * 1024 ** 3,
      usedBytes: 1024 ** 3,
    });

    render(<ProfilePage user={{
      userId: '11111111-1111-1111-1111-111111111111',
      name: 'Alice Martin',
      roles: ['utilisateur'],
    }} />);

    expect(await screen.findByText('9 Go / 10 Go')).toBeVisible();
  });
});
