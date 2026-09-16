import axios from 'axios';

export type ScanStatus =
  | 'CLEAN'
  | 'INFECTED'
  | 'PENDING_SCAN'
  | 'REJECTED'
  | 'SCAN_FAILED'
  | 'SCANNING'
  | 'UPLOADING';

export type UploadFileResponse = {
  createdAt: string;
  fileId: string;
  originalFilename: string;
  sizeBytes: number;
  status: ScanStatus;
};

export type FileMetadataResponse = Omit<UploadFileResponse, 'sizeBytes'> & {
  failureCode?: string | null;
  sizeBytes: number | null;
};

export type UploadFileOptions = {
  onProgress?: (progress: number) => void;
};

export type UploadConfiguration = {
  maximumSizeBytes: number;
};

export type GetFileMetadataOptions = {
  signal?: AbortSignal;
};

export type ListFilesOptions = {
  signal?: AbortSignal;
};

type ApiErrorResponse = {
  code: string;
  message: string;
  status?: number;
};

export class FilesApiError extends Error {
  readonly code: string;
  readonly status?: number;

  constructor(code: string, message: string, status?: number) {
    super(message);
    this.code = code;
    this.status = status;
    this.name = 'FilesApiError';
  }
}

function readApiError(error: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError(error)) {
    return null;
  }

  const responseData = error.response?.data as Partial<ApiErrorResponse> | undefined;
  if (
    !responseData
    || typeof responseData.code !== 'string'
    || typeof responseData.message !== 'string'
  ) {
    return null;
  }

  return {
    code: responseData.code,
    message: responseData.message,
    status: error.response?.status,
  };
}

export async function uploadFile(
  file: File,
  options: UploadFileOptions = {},
): Promise<UploadFileResponse> {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axios.post<UploadFileResponse>('/api/v1/files', formData, {
      onUploadProgress: (event) => {
        options.onProgress?.(calculateUploadProgress(event.loaded, event.total, file.size));
      },
    });
    options.onProgress?.(100);
    return response.data;
  } catch (error) {
    throw normalizeApiError(error, 'UPLOAD_REQUEST_FAILED', 'Le fichier ne peut pas etre envoye.');
  }
}

export async function getUploadConfiguration(): Promise<UploadConfiguration> {
  try {
    const response = await axios.get<UploadConfiguration>('/api/v1/files/config');
    const maximumSizeBytes = response.data.maximumSizeBytes;
    if (!Number.isSafeInteger(maximumSizeBytes) || maximumSizeBytes <= 0) {
      throw new FilesApiError(
        'INVALID_UPLOAD_CONFIGURATION',
        'La taille maximale autorisee est invalide.',
      );
    }
    return { maximumSizeBytes };
  } catch (error) {
    if (error instanceof FilesApiError) {
      throw error;
    }
    throw normalizeApiError(
      error,
      'UPLOAD_CONFIGURATION_REQUEST_FAILED',
      'La taille maximale autorisee ne peut pas etre lue.',
    );
  }
}

export async function getFileMetadata(
  fileId: string,
  options: GetFileMetadataOptions = {},
): Promise<FileMetadataResponse> {
  try {
    const response = await axios.get<FileMetadataResponse>(
      `/api/v1/files/${encodeURIComponent(fileId)}`,
      { signal: options.signal },
    );
    return response.data;
  } catch (error) {
    throw normalizeApiError(
      error,
      'FILE_METADATA_REQUEST_FAILED',
      'Le statut du fichier ne peut pas etre lu.',
    );
  }
}

export async function listFiles(
  options: ListFilesOptions = {},
): Promise<FileMetadataResponse[]> {
  try {
    const response = await axios.get<FileMetadataResponse[]>('/api/v1/files', {
      signal: options.signal,
    });
    return response.data;
  } catch (error) {
    throw normalizeApiError(
      error,
      'FILES_LIST_REQUEST_FAILED',
      'La liste des fichiers ne peut pas etre lue.',
    );
  }
}

function calculateUploadProgress(loaded: number, total: number | undefined, fileSize: number): number {
  const effectiveTotal = total ?? fileSize;
  if (effectiveTotal <= 0) {
    return loaded > 0 ? 100 : 0;
  }

  return Math.min(100, Math.max(0, Math.round((loaded / effectiveTotal) * 100)));
}

function normalizeApiError(error: unknown, fallbackCode: string, fallbackMessage: string): FilesApiError {
  const apiError = readApiError(error);
  if (apiError) {
    return new FilesApiError(apiError.code, apiError.message, apiError.status);
  }

  return new FilesApiError(fallbackCode, fallbackMessage);
}