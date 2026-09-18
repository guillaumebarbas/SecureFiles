import { render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { beforeEach, vi } from 'vitest';
import { DashboardPage } from '../../../pages/Dashboard/DashboardPage';
import {
  deleteFile,
  getFileMetadata,
  getDownloadUrl,
  getUploadConfiguration,
  listFiles,
  uploadFile,
  type FileMetadataResponse,
  type FilesPageResponse,
} from '../../../api/filesApi';

vi.mock('../../../api/filesApi', () => ({
  deleteFile: vi.fn(),
  getFileMetadata: vi.fn(),
  getDownloadUrl: vi.fn(),
  getUploadConfiguration: vi.fn(),
  listFiles: vi.fn(),
  uploadFile: vi.fn(),
}));

const mockedUploadFile = vi.mocked(uploadFile);
const mockedGetFileMetadata = vi.mocked(getFileMetadata);
const mockedGetDownloadUrl = vi.mocked(getDownloadUrl);
const mockedDeleteFile = vi.mocked(deleteFile);
const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedListFiles = vi.mocked(listFiles);

const emptyFilesPage: FilesPageResponse = {
  content: [],
  hasNext: false,
  hasPrevious: false,
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 0,
};

function createFilesPageResponse(content: FileMetadataResponse[]): FilesPageResponse {
  return {
    content,
    hasNext: false,
    hasPrevious: false,
    page: 1,
    size: 10,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  };
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
    mockedListFiles.mockResolvedValue(emptyFilesPage);
  });

  it('renders the upload section and the recent files register', () => {
    render(<DashboardPage />);

    expect(screen.getByRole('heading', { name: 'Upload ton fichier' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Fichiers récents' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upload ton fichier' }).closest('section'))
      .toHaveClass('dashboard-page__upload-section');
    expect(screen.queryByRole('heading', { name: 'Depot rapide' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Espace de pilotage' })).not.toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Fichiers uploadés' })).toBeVisible();
  });

  it('loads recent files for an anonymous visitor', async () => {
    mockedListFiles.mockResolvedValue(createFilesPageResponse([{
      author: 'Alice Martin',
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'public-document.txt',
      sizeBytes: 12,
      status: 'CLEAN',
    }]));

    render(<DashboardPage isAuthenticated={false} />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('public-document.txt')).toBeVisible();
    expect(mockedListFiles).toHaveBeenCalledWith({
      direction: 'desc',
      page: 1,
      signal: expect.anything(),
      size: 10,
      sort: 'createdAt',
    });
  });

  it('shows an author column in the recent files register', () => {
    render(<DashboardPage />);

    expect(screen.getByRole('columnheader', { name: 'Auteur' })).toBeVisible();
  });

  it('opens the row action menu and streams a clean file or deletes it', async () => {
    const user = userEvent.setup();
    const file = {
      author: 'Alice Martin',
      canDelete: true,
      canDownload: true,
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'safe-document.txt',
      sizeBytes: 12,
      status: 'CLEAN' as const,
    };
    mockedGetDownloadUrl.mockReturnValue('/api/v1/files/11111111-1111-1111-1111-111111111111/content');
    mockedDeleteFile.mockResolvedValue();
    mockedListFiles
      .mockResolvedValueOnce(createFilesPageResponse([file]))
      .mockResolvedValueOnce(emptyFilesPage);

    render(<DashboardPage isAuthenticated />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    const row = await within(table).findByRole('row', { name: /safe-document\.txt/ });
    expect(within(table).getByRole('columnheader', { name: 'Action' })).toBeVisible();
    await user.click(within(row).getByRole('button', { name: 'Actions pour safe-document.txt' }));

    const menu = screen.getByRole('menu', { name: 'Actions pour safe-document.txt' });
    const downloadAction = within(menu).getByRole('menuitem', { name: 'Télécharger' });
    expect(downloadAction.tagName).toBe('A');
    expect(downloadAction).toHaveClass('shared-button-menu-actions--success');
    expect(downloadAction).toHaveAttribute(
      'href',
      '/api/v1/files/11111111-1111-1111-1111-111111111111/content',
    );
    const deleteAction = within(menu).getByRole('menuitem', { name: 'Supprimer' });
    expect(deleteAction).toHaveClass('shared-button-menu-actions--danger');
    await user.click(deleteAction);

    expect(mockedDeleteFile).toHaveBeenCalledWith(file.fileId);
    await waitFor(() => expect(mockedListFiles).toHaveBeenCalledTimes(2));
    expect(within(table).queryByText('safe-document.txt')).not.toBeInTheDocument();
  });

  it('does not offer actions when the backend grants no capability', async () => {
    const user = userEvent.setup();
    const file = {
      author: 'Alice Martin',
      canDelete: false,
      canDownload: false,
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'protected-document.txt',
      sizeBytes: 12,
      status: 'CLEAN' as const,
    };
    mockedListFiles.mockResolvedValue(createFilesPageResponse([file]));

    render(<DashboardPage isAuthenticated={false} />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    const row = await within(table).findByRole('row', { name: /protected-document\.txt/ });
    await user.click(within(row).getByRole('button', { name: 'Actions pour protected-document.txt' }));

    const menu = screen.getByRole('menu', { name: 'Actions pour protected-document.txt' });
    expect(within(menu).queryByRole('link', { name: 'Télécharger' })).not.toBeInTheDocument();
    expect(within(menu).queryByRole('menuitem', { name: 'Supprimer' })).not.toBeInTheDocument();
    expect(within(menu).getByText('Aucune action disponible.')).toBeVisible();
  });

  it('focuses the first action and closes the menu with Escape or an outside click', async () => {
    const user = userEvent.setup();
    const file = {
      canDelete: true,
      canDownload: true,
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'focusable-document.txt',
      sizeBytes: 12,
      status: 'CLEAN' as const,
    };
    mockedListFiles.mockResolvedValue(createFilesPageResponse([file]));

    render(<DashboardPage isAuthenticated />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    const row = await within(table).findByRole('row', { name: /focusable-document\.txt/ });
    const trigger = within(row).getByRole('button', { name: 'Actions pour focusable-document.txt' });
    await user.click(trigger);

    const menu = screen.getByRole('menu', { name: 'Actions pour focusable-document.txt' });
    expect(within(menu).getByRole('menuitem', { name: 'Télécharger' })).toHaveFocus();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu', { name: 'Actions pour focusable-document.txt' })).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();

    await user.click(trigger);
    expect(screen.getByRole('menu', { name: 'Actions pour focusable-document.txt' })).toBeVisible();
    await user.click(document.body);
    expect(screen.queryByRole('menu', { name: 'Actions pour focusable-document.txt' })).not.toBeInTheDocument();
  });

  it('hides the upload action until a file is selected', () => {
    render(<DashboardPage />);

    expect(screen.queryByRole('button', { name: 'Envoyer le fichier' })).not.toBeInTheDocument();
  });

  it('hydrates the recent files register from the backend', async () => {
    mockedListFiles.mockResolvedValue(createFilesPageResponse([{
      author: 'Alice Martin',
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'persisted-document.txt',
      sizeBytes: 12,
      status: 'CLEAN',
    }]));

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('persisted-document.txt')).toBeVisible();
    expect(within(table).getByText('Alice Martin')).toBeVisible();
    expect(mockedListFiles).toHaveBeenCalledWith({
      direction: 'desc',
      page: 1,
      signal: expect.anything(),
      size: 10,
      sort: 'createdAt',
    });
  });

  it('loads the next recent files page from the backend', async () => {
    const user = userEvent.setup();
    mockedListFiles
      .mockResolvedValueOnce({
        content: [{
          author: 'Alice Martin',
          createdAt: '2026-09-15T10:00:00Z',
          fileId: '11111111-1111-1111-1111-111111111111',
          originalFilename: 'first-page.txt',
          sizeBytes: 12,
          status: 'CLEAN',
        }],
        hasNext: true,
        hasPrevious: false,
        page: 1,
        size: 10,
        totalElements: 11,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        content: [{
          author: 'Bob Dupont',
          createdAt: '2026-09-14T10:00:00Z',
          fileId: '22222222-2222-2222-2222-222222222222',
          originalFilename: 'second-page.txt',
          sizeBytes: 24,
          status: 'CLEAN',
        }],
        hasNext: false,
        hasPrevious: true,
        page: 2,
        size: 10,
        totalElements: 11,
        totalPages: 2,
      });

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('first-page.txt')).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Page suivante' }));

    expect(await within(table).findByText('second-page.txt')).toBeVisible();
    expect(within(table).queryByText('first-page.txt')).not.toBeInTheDocument();
    expect(mockedListFiles).toHaveBeenLastCalledWith({
      direction: 'desc',
      page: 2,
      signal: expect.anything(),
      size: 10,
      sort: 'createdAt',
    });
  });

  it('requests the selected page size from the backend', async () => {
    const user = userEvent.setup();
    mockedListFiles
      .mockResolvedValueOnce(createFilesPageResponse([{
        createdAt: '2026-09-15T10:00:00Z',
        fileId: '11111111-1111-1111-1111-111111111111',
        originalFilename: 'ten-per-page.txt',
        sizeBytes: 12,
        status: 'CLEAN',
      }]))
      .mockResolvedValueOnce({
        content: [{
          createdAt: '2026-09-15T10:00:00Z',
          fileId: '22222222-2222-2222-2222-222222222222',
          originalFilename: 'twenty-five-per-page.txt',
          sizeBytes: 24,
          status: 'CLEAN',
        }],
        hasNext: false,
        hasPrevious: false,
        page: 1,
        size: 25,
        totalElements: 1,
        totalPages: 1,
      });

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    await within(table).findByText('ten-per-page.txt');
    const pageSizeSelect = screen.getByRole('combobox', { name: 'Éléments par page' });
    await user.selectOptions(pageSizeSelect, '25');

    expect(pageSizeSelect).toHaveValue('25');
    expect(await within(table).findByText('twenty-five-per-page.txt')).toBeVisible();
    expect(mockedListFiles).toHaveBeenLastCalledWith({
      direction: 'desc',
      page: 1,
      signal: expect.anything(),
      size: 25,
      sort: 'createdAt',
    });
  });

  it('filters recent files by several statuses and closes the filter panel with Escape', async () => {
    const user = userEvent.setup();
    render(<DashboardPage />);

    const filterButton = screen.getByRole('button', { name: 'Filtrer par statut' });
    await user.click(filterButton);

    const filterPanel = screen.getByRole('dialog', { name: 'Filtrer les fichiers par statut' });
    await user.click(within(filterPanel).getByRole('checkbox', { name: 'Sain' }));
    await user.click(within(filterPanel).getByRole('checkbox', { name: 'Analyse échouée' }));

    await waitFor(() => expect(mockedListFiles).toHaveBeenLastCalledWith({
      direction: 'desc',
      page: 1,
      signal: expect.anything(),
      size: 10,
      sort: 'createdAt',
      statuses: ['CLEAN', 'SCAN_FAILED'],
    }));

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Filtrer les fichiers par statut' })).not.toBeInTheDocument();
    expect(filterButton).toHaveFocus();
  });

  it('shows the terminal failure and precise cause in the status tag tooltip', async () => {
    mockedListFiles.mockResolvedValue(createFilesPageResponse([{
      createdAt: '2026-09-15T10:00:00Z',
      failureCause: 'CLAMAV_UNAVAILABLE',
      failureCode: 'SCAN_ATTEMPTS_EXHAUSTED',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'MicrosoftTeams.pkg',
      sizeBytes: 42,
      status: 'SCAN_FAILED',
    }]));

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    const tag = await within(table).findByText('SCAN_FAILED');
    const tooltip = screen.getByRole('tooltip', {
      name: 'Nombre maximal de tentatives atteint (SCAN_ATTEMPTS_EXHAUSTED). Cause : Service antivirus indisponible (CLAMAV_UNAVAILABLE)',
    });

    expect(tag).toHaveAttribute('aria-describedby', tooltip.id);
    expect(tooltip).toHaveTextContent(
      'Nombre maximal de tentatives atteint (SCAN_ATTEMPTS_EXHAUSTED). Cause : Service antivirus indisponible (CLAMAV_UNAVAILABLE)',
    );
  });

  it('shows a precise storage failure description in the status tag tooltip', async () => {
    mockedListFiles.mockResolvedValue(createFilesPageResponse([{
      createdAt: '2026-09-15T10:00:00Z',
      failureCode: 'STORAGE_SIZE_MISMATCH',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'large-video.mov',
      sizeBytes: 19_553_061,
      status: 'SCAN_FAILED',
    }]));

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    await within(table).findByText('large-video.mov');
    expect(screen.getByRole('tooltip', {
      name: 'Taille du fichier incohérente (STORAGE_SIZE_MISMATCH)',
    })).toBeVisible();
  });

  it('shows a retry action when the recent files register cannot be loaded', async () => {
    const user = userEvent.setup();
    mockedListFiles
      .mockRejectedValueOnce(new Error('La liste des fichiers ne peut pas être lue.'))
      .mockResolvedValueOnce(createFilesPageResponse([{
        createdAt: '2026-09-15T10:00:00Z',
        fileId: '11111111-1111-1111-1111-111111111111',
        originalFilename: 'retried-document.txt',
        sizeBytes: 12,
        status: 'CLEAN',
      }]));

    render(<DashboardPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La liste des fichiers ne peut pas être lue.',
    );
    await user.click(screen.getByRole('button', { name: 'Réessayer' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('retried-document.txt')).toBeVisible();
    expect(mockedListFiles).toHaveBeenCalledTimes(2);
  });

  it('uploads the selected file and displays the pending scan response', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    mockedUploadFile.mockResolvedValue({
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    });
    mockedGetFileMetadata.mockResolvedValue({
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    });
    mockedListFiles
      .mockResolvedValueOnce(emptyFilesPage)
      .mockResolvedValueOnce(createFilesPageResponse([{
        createdAt: '2026-09-15T10:00:00Z',
        fileId: '11111111-1111-1111-1111-111111111111',
        originalFilename: 'document.txt',
        sizeBytes: file.size,
        status: 'PENDING_SCAN',
      }]));

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(mockedUploadFile).toHaveBeenCalledWith(
      file,
      expect.objectContaining({ onProgress: expect.any(Function) }),
    );
    expect(await screen.findByRole('status')).toHaveTextContent('PENDING_SCAN');
    expect(screen.getAllByText('document.txt')).toHaveLength(2);
    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(within(table).getByText('document.txt')).toBeVisible();
    expect(within(table).getByText('PENDING_SCAN')).toBeVisible();
    expect(screen.queryByRole('link', { name: /telecharger/i })).not.toBeInTheDocument();
    await waitFor(() => expect(mockedListFiles).toHaveBeenCalledTimes(2));
  });

  it('shows upload progress while the request is pending', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    let resolveUpload: ((value: {
      createdAt: string;
      fileId: string;
      originalFilename: string;
      sizeBytes: number;
      status: 'PENDING_SCAN';
    }) => void) | undefined;
    mockedUploadFile.mockImplementation(async (_file, options) => {
      options?.onProgress?.(42);
      return new Promise((resolve) => {
        resolveUpload = resolve;
      });
    });

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(screen.getByRole('progressbar', { name: 'Progression de l\'upload' })).toHaveValue(42);
    expect(screen.getByText('42 %')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Envoi en cours...' })).toBeDisabled();

    resolveUpload?.({
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    });
  });

  it('follows scan status until the file is clean', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    const response = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN' as const,
    };
    mockedUploadFile.mockResolvedValue(response);
    mockedGetFileMetadata
      .mockResolvedValueOnce({ ...response, status: 'SCANNING' })
      .mockResolvedValueOnce({ ...response, status: 'CLEAN' });
    mockedListFiles
      .mockResolvedValueOnce(emptyFilesPage)
      .mockResolvedValueOnce(createFilesPageResponse([response]));

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('SCANNING')).toBeVisible();
    expect(await within(table).findByText('CLEAN', {}, { timeout: 2000 })).toBeVisible();
    expect(mockedGetFileMetadata).toHaveBeenCalledTimes(2);
  });

  it('stops polling when the scan blocks the file', async () => {
    const user = userEvent.setup();
    const file = new File(['malicious content'], 'document.txt', { type: 'text/plain' });
    const response = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN' as const,
    };
    mockedUploadFile.mockResolvedValue(response);
    mockedGetFileMetadata.mockResolvedValue({ ...response, status: 'INFECTED' });
    mockedListFiles
      .mockResolvedValueOnce(emptyFilesPage)
      .mockResolvedValueOnce(createFilesPageResponse([response]));

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploadés' });
    expect(await within(table).findByText('INFECTED')).toBeVisible();
    expect(screen.getByText('Fichier bloqué : une menace a été détectée.')).toBeVisible();
    await new Promise((resolve) => setTimeout(resolve, 350));
    expect(mockedGetFileMetadata).toHaveBeenCalledTimes(1);
  });

  it('displays an actionable error when the upload fails', async () => {
    const user = userEvent.setup();
    const file = new File(['invalid'], 'document.txt', { type: 'text/plain' });
    mockedUploadFile.mockRejectedValue(new Error('Le fichier ne peut pas être envoyé.'));

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Le fichier ne peut pas être envoyé.');
    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).not.toBeDisabled();
  });
});