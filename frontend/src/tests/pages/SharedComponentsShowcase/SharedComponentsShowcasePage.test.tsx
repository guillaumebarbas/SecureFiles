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

    expect(screen.getByText('Recherche envoyee : rapport.pdf')).toBeVisible();
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
});