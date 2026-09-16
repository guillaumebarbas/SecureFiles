import { render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import userEvent from '@testing-library/user-event';
import { beforeEach, vi } from 'vitest';
import { DashboardPage } from '../../../pages/Dashboard/DashboardPage';
import {
  getFileMetadata,
  getUploadConfiguration,
  listFiles,
  uploadFile,
} from '../../../api/filesApi';

vi.mock('../../../api/filesApi', () => ({
  getFileMetadata: vi.fn(),
  getUploadConfiguration: vi.fn(),
  listFiles: vi.fn(),
  uploadFile: vi.fn(),
}));

const mockedUploadFile = vi.mocked(uploadFile);
const mockedGetFileMetadata = vi.mocked(getFileMetadata);
const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedListFiles = vi.mocked(listFiles);

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
    mockedListFiles.mockResolvedValue([]);
  });

  it('renders the upload section and the recent files register', () => {
    render(<DashboardPage />);

    expect(screen.getByRole('heading', { name: 'Upload ton fichier' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Fichiers recents' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Upload ton fichier' }).closest('section'))
      .toHaveClass('dashboard-page__upload-section');
    expect(screen.queryByRole('heading', { name: 'Depot rapide' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Espace de pilotage' })).not.toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Fichiers uploades' })).toBeVisible();
  });

  it('hides the upload action until a file is selected', () => {
    render(<DashboardPage />);

    expect(screen.queryByRole('button', { name: 'Envoyer le fichier' })).not.toBeInTheDocument();
  });

  it('hydrates the recent files register from the backend', async () => {
    mockedListFiles.mockResolvedValue([{
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'persisted-document.txt',
      sizeBytes: 12,
      status: 'CLEAN',
    }]);

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
    expect(await within(table).findByText('persisted-document.txt')).toBeVisible();
    expect(mockedListFiles).toHaveBeenCalledWith({ signal: expect.anything() });
  });

  it('shows the scan failure code in the status tag tooltip', async () => {
    mockedListFiles.mockResolvedValue([{
      createdAt: '2026-09-15T10:00:00Z',
      failureCode: 'CLAMAV_UNAVAILABLE',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'MicrosoftTeams.pkg',
      sizeBytes: 42,
      status: 'SCAN_FAILED',
    }]);

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
    const tag = await within(table).findByText('SCAN_FAILED');
    const tooltip = screen.getByRole('tooltip', {
      name: 'Service antivirus indisponible (CLAMAV_UNAVAILABLE)',
    });

    expect(tag).toHaveAttribute('aria-describedby', tooltip.id);
    expect(tooltip).toHaveTextContent('Service antivirus indisponible (CLAMAV_UNAVAILABLE)');
  });

  it('shows a precise storage failure description in the status tag tooltip', async () => {
    mockedListFiles.mockResolvedValue([{
      createdAt: '2026-09-15T10:00:00Z',
      failureCode: 'STORAGE_SIZE_MISMATCH',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'large-video.mov',
      sizeBytes: 19_553_061,
      status: 'SCAN_FAILED',
    }]);

    render(<DashboardPage />);

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
    await within(table).findByText('large-video.mov');
    expect(screen.getByRole('tooltip', {
      name: 'Taille du fichier incoherente (STORAGE_SIZE_MISMATCH)',
    })).toBeVisible();
  });

  it('shows a retry action when the recent files register cannot be loaded', async () => {
    const user = userEvent.setup();
    mockedListFiles
      .mockRejectedValueOnce(new Error('La liste des fichiers ne peut pas etre lue.'))
      .mockResolvedValueOnce([{
        createdAt: '2026-09-15T10:00:00Z',
        fileId: '11111111-1111-1111-1111-111111111111',
        originalFilename: 'retried-document.txt',
        sizeBytes: 12,
        status: 'CLEAN',
      }]);

    render(<DashboardPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La liste des fichiers ne peut pas etre lue.',
    );
    await user.click(screen.getByRole('button', { name: 'Reessayer' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
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

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(mockedUploadFile).toHaveBeenCalledWith(
      file,
      expect.objectContaining({ onProgress: expect.any(Function) }),
    );
    expect(await screen.findByRole('status')).toHaveTextContent('PENDING_SCAN');
    expect(screen.getAllByText('document.txt')).toHaveLength(2);
    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
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

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
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

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    const table = screen.getByRole('table', { name: 'Fichiers uploades' });
    expect(await within(table).findByText('INFECTED')).toBeVisible();
    expect(screen.getByText('Fichier bloque : une menace a ete detectee.')).toBeVisible();
    await new Promise((resolve) => setTimeout(resolve, 350));
    expect(mockedGetFileMetadata).toHaveBeenCalledTimes(1);
  });

  it('displays an actionable error when the upload fails', async () => {
    const user = userEvent.setup();
    const file = new File(['invalid'], 'document.txt', { type: 'text/plain' });
    mockedUploadFile.mockRejectedValue(new Error('Le fichier ne peut pas etre envoye.'));

    render(<DashboardPage />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Le fichier ne peut pas etre envoye.');
    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).not.toBeDisabled();
  });
});