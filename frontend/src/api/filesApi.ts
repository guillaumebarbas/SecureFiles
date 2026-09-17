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
  author?: string;
  failureCode?: string | null;
  sizeBytes: number | null;
};

export type FilesPageResponse = {
  content: FileMetadataResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type UploadFileOptions = {
  onProgress?: (progress: number) => void;
};

export type UploadConfiguration = {
  maximumSizeBytes: number;
};

export type BackendHealthStatus = 'online' | 'offline';

export type UserRole = 'developpeur' | 'admin' | 'utilisateur';

export type UserProfile = {
  userId: string;
  name: string;
  roles: UserRole[];
};

export type UserCredentials = {
  name: string;
  password: string;
};

export type RegisterUserRequest = UserCredentials & {
  roles: UserRole[];
};

type BackendHealthResponse = {
  status?: string;
};

export type GetFileMetadataOptions = {
  signal?: AbortSignal;
};

export type ListFilesOptions = {
  direction?: SortDirection;
  page?: number;
  size?: number;
  sort?: FileSortField;
  statuses?: ScanStatus[];
  signal?: AbortSignal;
};

export type FileSortField = 'name' | 'author' | 'size' | 'createdAt';
export type SortDirection = 'asc' | 'desc';

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

export async function checkBackendHealth(): Promise<BackendHealthStatus> {
  try {
    const response = await axios.get<BackendHealthResponse>('/actuator/health');
    return response.data.status === 'UP' ? 'online' : 'offline';
  } catch {
    return 'offline';
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
    throw normalizeApiError(error, 'UPLOAD_REQUEST_FAILED', 'Le fichier ne peut pas être envoyé.');
  }
}

export async function getUploadConfiguration(): Promise<UploadConfiguration> {
  try {
    const response = await axios.get<UploadConfiguration>('/api/v1/files/config');
    const maximumSizeBytes = response.data.maximumSizeBytes;
    if (!Number.isSafeInteger(maximumSizeBytes) || maximumSizeBytes <= 0) {
      throw new FilesApiError(
        'INVALID_UPLOAD_CONFIGURATION',
        'La taille maximale autorisée est invalide.',
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
      'La taille maximale autorisée ne peut pas être lue.',
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
      'Le statut du fichier ne peut pas être lu.',
    );
  }
}

export async function listFiles(
  options: ListFilesOptions = {},
): Promise<FilesPageResponse> {
  try {
    const params: Record<string, number | string | string[]> = {
      page: options.page ?? 1,
      size: options.size ?? 10,
    };
    if (options.sort) {
      params.sort = options.sort;
    }
    if (options.direction) {
      params.direction = options.direction;
    }
    if (options.statuses && options.statuses.length > 0) {
      params.status = options.statuses;
    }

    const response = await axios.get<FilesPageResponse>('/api/v1/files', {
      params,
      paramsSerializer: options.statuses?.length ? { indexes: null } : undefined,
      signal: options.signal,
    });
    return response.data;
  } catch (error) {
    throw normalizeApiError(
      error,
      'FILES_LIST_REQUEST_FAILED',
      'La liste des fichiers ne peut pas être lue.',
    );
  }
}

export async function registerUser(request: RegisterUserRequest): Promise<UserProfile> {
  await prepareAuthRequest();

  try {
    const response = await axios.post<UserProfile>('/api/v1/auth/register', request, {
      withCredentials: true,
    });
    return response.data;
  } catch (error) {
    throw normalizeApiError(error, 'REGISTRATION_REQUEST_FAILED', 'Le compte ne peut pas être créé.');
  }
}

export async function loginUser(credentials: UserCredentials): Promise<UserProfile> {
  await prepareAuthRequest();

  try {
    const response = await axios.post<UserProfile>('/api/v1/auth/login', credentials, {
      withCredentials: true,
    });
    return response.data;
  } catch (error) {
    throw normalizeApiError(error, 'LOGIN_REQUEST_FAILED', 'La connexion ne peut pas être effectuée.');
  }
}

export async function getCurrentUser(): Promise<UserProfile | undefined> {
  try {
    const response = await axios.get<UserProfile>('/api/v1/users/me', {
      withCredentials: true,
    });
    if (response.status === 204) {
      return undefined;
    }
    return response.data;
  } catch (error) {
    throw normalizeApiError(
      error,
      'CURRENT_USER_REQUEST_FAILED',
      'Le profil courant ne peut pas être lu.',
    );
  }
}

export async function logoutUser(): Promise<void> {
  await prepareAuthRequest();

  try {
    await axios.post('/api/v1/auth/logout', undefined, { withCredentials: true });
  } catch (error) {
    throw normalizeApiError(error, 'LOGOUT_REQUEST_FAILED', 'La déconnexion ne peut pas être effectuée.');
  }
}

async function prepareAuthRequest(): Promise<void> {
  try {
    await axios.get('/api/v1/auth/csrf', { withCredentials: true });
  } catch (error) {
    throw normalizeApiError(
      error,
      'CSRF_REQUEST_FAILED',
      'La protection de la session ne peut pas être initialisée.',
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