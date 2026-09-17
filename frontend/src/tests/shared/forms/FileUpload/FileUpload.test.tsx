import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getFileMetadata,
  getUploadConfiguration,
  uploadFile,
  type UploadFileResponse,
} from '../../../../api/filesApi';
import { FileUpload } from '../../../../shared/forms/FileUpload/FileUpload';

vi.mock('../../../../api/filesApi', () => ({
  getFileMetadata: vi.fn(),
  getUploadConfiguration: vi.fn(),
  uploadFile: vi.fn(),
}));

const mockedGetFileMetadata = vi.mocked(getFileMetadata);
const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedUploadFile = vi.mocked(uploadFile);

describe('FileUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the upload action only after a file is selected', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });

    render(<FileUpload />);

    expect(screen.getByText('Choisissez un fichier')).toBeVisible();
    expect(screen.getByLabelText('Choisir un fichier')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Envoyer le fichier' })).not.toBeInTheDocument();

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);

    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).toBeVisible();
  });

  it('requests authentication before an anonymous visitor selects a file', async () => {
    const user = userEvent.setup();
    const onAuthenticationRequired = vi.fn();

    render(
      <FileUpload
        isAuthenticated={false}
        onAuthenticationRequired={onAuthenticationRequired}
      />,
    );

    await user.click(screen.getByText('Choisissez un fichier'));

    expect(screen.getByLabelText('Choisir un fichier')).toBeDisabled();
    expect(onAuthenticationRequired).toHaveBeenCalledOnce();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Vous devez être connecté pour sélectionner un fichier.',
    );
    expect(mockedUploadFile).not.toHaveBeenCalled();
  });

  it('displays the selected file size with a readable unit', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'MicrosoftTeams.pkg', { type: 'application/octet-stream' });
    Object.defineProperty(file, 'size', { value: 357099924 });

    render(<FileUpload />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);

    expect(screen.getByText(/340\.6 Mo/)).toBeVisible();
  });

  it('displays the maximum upload size loaded from the backend', async () => {
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1_073_741_824 });

    render(<FileUpload />);

    expect(await screen.findByText('Taille maximale autorisée : 1 Go')).toBeVisible();
  });

  it('blocks upload locally when selected file exceeds the configured maximum size', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 5 });

    render(<FileUpload />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Le fichier dépasse la taille maximale autorisée.',
    );
    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).toBeDisabled();
    expect(mockedUploadFile).not.toHaveBeenCalled();
  });

  it('blocks upload until the maximum size configuration can be retried', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    mockedGetUploadConfiguration
      .mockRejectedValueOnce(new Error('La taille maximale autorisée ne peut pas être lue.'))
      .mockResolvedValueOnce({ maximumSizeBytes: 1024 });

    render(<FileUpload />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La taille maximale autorisée ne peut pas être lue.',
    );
    expect(mockedUploadFile).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Réessayer la vérification' }));
    await waitFor(() => expect(mockedGetUploadConfiguration).toHaveBeenCalledTimes(2));

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).toBeEnabled();
    });
  });

  it('displays upload progress while the request is pending', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    let resolveUpload: ((response: UploadFileResponse) => void) | undefined;
    mockedUploadFile.mockImplementation(async (_file, options) => {
      options?.onProgress?.(42);
      return new Promise((resolve) => {
        resolveUpload = resolve;
      });
    });

    render(<FileUpload />);

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

  it('notifies the parent when upload and scanning statuses change', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    const pendingResponse: UploadFileResponse = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    };
    const cleanResponse = { ...pendingResponse, status: 'CLEAN' as const };
    const onAccepted = vi.fn();
    const onStatusChange = vi.fn();
    mockedUploadFile.mockResolvedValue(pendingResponse);
    mockedGetFileMetadata.mockResolvedValue(cleanResponse);

    render(<FileUpload onAccepted={onAccepted} onStatusChange={onStatusChange} />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(onAccepted).toHaveBeenCalledWith(pendingResponse);
    expect(await screen.findByText('CLEAN', {}, { timeout: 2000 })).toBeVisible();
    expect(onStatusChange).toHaveBeenCalledWith(cleanResponse);
  });

  it('clears the accepted file and invites a new selection', async () => {
    const user = userEvent.setup();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    const response: UploadFileResponse = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    };
    mockedUploadFile.mockResolvedValue(response);
    mockedGetFileMetadata.mockResolvedValue({ ...response, status: 'CLEAN' });

    render(<FileUpload />);

    const fileInput = screen.getByLabelText('Choisir un fichier') as HTMLInputElement;
    await user.upload(fileInput, file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(await screen.findByText('Cliquez sur la zone pour choisir un nouveau fichier')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Envoyer le fichier' })).not.toBeInTheDocument();
    expect(fileInput.files).toHaveLength(0);

    await user.upload(fileInput, file);

    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).toBeVisible();
  });

  it('dismisses the accepted summary after ten seconds with a smooth exit state', async () => {
    vi.useFakeTimers();
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    mockedUploadFile.mockResolvedValue({
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'CLEAN',
    });

    render(<FileUpload />);

    await act(async () => {
      fireEvent.change(screen.getByLabelText('Choisir un fichier'), { target: { files: [file] } });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));
      await Promise.resolve();
    });

    expect(screen.getByRole('status')).toHaveClass('shared-file-upload__feedback--accepted');

    act(() => {
      vi.advanceTimersByTime(10_000);
    });

    expect(screen.getByRole('status')).toHaveClass('shared-file-upload__feedback--hiding');

    act(() => {
      vi.advanceTimersByTime(320);
    });

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows an actionable error when upload fails', async () => {
    const user = userEvent.setup();
    const file = new File(['invalid'], 'document.txt', { type: 'text/plain' });
    mockedUploadFile.mockRejectedValue(new Error('Le fichier ne peut pas être envoyé.'));

    render(<FileUpload />);

    await user.upload(screen.getByLabelText('Choisir un fichier'), file);
    await user.click(screen.getByRole('button', { name: 'Envoyer le fichier' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Le fichier ne peut pas être envoyé.');
    expect(screen.getByRole('button', { name: 'Envoyer le fichier' })).not.toBeDisabled();
  });
});