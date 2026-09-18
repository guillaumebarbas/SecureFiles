import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getDownloadUrl, type FileMetadataResponse } from '../../../api/filesApi';
import { FileActionsMenu } from '../../../shared/files/FileActionsMenu';

vi.mock('../../../api/filesApi', () => ({
  getDownloadUrl: vi.fn(),
}));

const mockedGetDownloadUrl = vi.mocked(getDownloadUrl);

function createFile(overrides: Partial<FileMetadataResponse> = {}): FileMetadataResponse {
  return {
    createdAt: '2026-09-15T10:00:00Z',
    fileId: '11111111-1111-1111-1111-111111111111',
    originalFilename: 'safe-document.txt',
    sizeBytes: 12,
    status: 'CLEAN',
    ...overrides,
  };
}

describe('FileActionsMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetDownloadUrl.mockReturnValue('/api/v1/files/file-id/content');
  });

  it('offers download and deletion for an authenticated clean file', async () => {
    const user = userEvent.setup();
    const file = createFile({ canDelete: true, canDownload: true });
    const onDelete = vi.fn().mockResolvedValue(true);

    render(<FileActionsMenu file={file} isAuthenticated onDelete={onDelete} />);

    await user.click(screen.getByRole('button', { name: 'Actions pour safe-document.txt' }));

    const menu = screen.getByRole('menu', { name: 'Actions pour safe-document.txt' });
    expect(within(menu).getByRole('menuitem', { name: 'Télécharger' })).toHaveAttribute(
      'href',
      '/api/v1/files/file-id/content',
    );

    await user.click(within(menu).getByRole('menuitem', { name: 'Supprimer' }));

    expect(onDelete).toHaveBeenCalledWith(file.fileId);
  });

  it('does not offer actions when the file is not clean or the user is not authenticated', async () => {
    const user = userEvent.setup();
    const file = createFile({ canDelete: true, canDownload: true, status: 'PENDING_SCAN' });
    const onDelete = vi.fn().mockResolvedValue(true);

    const { rerender } = render(
      <FileActionsMenu file={file} isAuthenticated onDelete={onDelete} />,
    );

    await user.click(screen.getByRole('button', { name: 'Actions pour safe-document.txt' }));
    let menu = screen.getByRole('menu', { name: 'Actions pour safe-document.txt' });
    expect(within(menu).queryByRole('menuitem', { name: 'Télécharger' })).not.toBeInTheDocument();
    expect(within(menu).getByRole('menuitem', { name: 'Supprimer' })).toBeInTheDocument();

    rerender(<FileActionsMenu file={file} isAuthenticated={false} onDelete={onDelete} />);
    menu = screen.getByRole('menu', { name: 'Actions pour safe-document.txt' });
    expect(within(menu).getByText('Aucune action disponible.')).toBeVisible();
  });
});