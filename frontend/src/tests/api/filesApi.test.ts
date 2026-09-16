import axios from 'axios';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  checkBackendHealth,
  getFileMetadata,
  getUploadConfiguration,
  listFiles,
  uploadFile,
} from '../../api/filesApi';

vi.mock('axios', () => ({
  default: {
    get: vi.fn(),
    isAxiosError: (error: unknown) => Boolean((error as { isAxiosError?: boolean }).isAxiosError),
    post: vi.fn(),
  },
}));

describe('filesApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('reports the backend online when the actuator health is UP', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: { status: 'UP' } } as never);

    await expect(checkBackendHealth()).resolves.toBe('online');

    expect(axios.get).toHaveBeenCalledWith('/actuator/health');
  });

  it('reports the backend offline when the actuator health is not UP', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: { status: 'DOWN' } } as never);

    await expect(checkBackendHealth()).resolves.toBe('offline');
  });

  it('reports the backend offline when the health request fails', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('Backend unavailable'));

    await expect(checkBackendHealth()).resolves.toBe('offline');
  });

  it('posts the selected file as multipart form data', async () => {
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    const response = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    };
    vi.mocked(axios.post).mockResolvedValue({ data: response } as never);

    await expect(uploadFile(file)).resolves.toEqual(response);

    const [url, body] = vi.mocked(axios.post).mock.calls[0];
    expect(url).toBe('/api/v1/files');
    expect((body as FormData).get('file')).toBe(file);
  });

  it('reports upload progress and completes at one hundred percent', async () => {
    const file = new File(['safe content'], 'document.txt', { type: 'text/plain' });
    const progressValues: number[] = [];
    const response = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: file.size,
      status: 'PENDING_SCAN',
    };
    vi.mocked(axios.post).mockImplementation(async (_url, _body, config) => {
      config?.onUploadProgress?.({ loaded: 5, total: 10 } as never);
      return { data: response } as never;
    });

    await uploadFile(file, { onProgress: (progress) => progressValues.push(progress) });

    expect(progressValues).toEqual([50, 100]);
  });

  it('gets file metadata by identifier', async () => {
    const fileId = '11111111-1111-1111-1111-111111111111';
    const response = {
      createdAt: '2026-09-15T10:00:00Z',
      fileId,
      failureCode: 'CLAMAV_UNAVAILABLE',
      originalFilename: 'document.txt',
      sizeBytes: 12,
      status: 'SCANNING',
    };
    vi.mocked(axios.get).mockResolvedValue({ data: response } as never);

    await expect(getFileMetadata(fileId)).resolves.toEqual(response);

    expect(axios.get).toHaveBeenCalledWith(`/api/v1/files/${fileId}`, expect.any(Object));
  });

  it('gets the maximum upload size from the configuration endpoint', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: { maximumSizeBytes: 1024 } } as never);

    await expect(getUploadConfiguration()).resolves.toEqual({ maximumSizeBytes: 1024 });

    expect(axios.get).toHaveBeenCalledWith('/api/v1/files/config');
  });

  it('gets the files visible to the current requester with an abort signal', async () => {
    const response = [{
      createdAt: '2026-09-15T10:00:00Z',
      fileId: '11111111-1111-1111-1111-111111111111',
      originalFilename: 'document.txt',
      sizeBytes: 12,
      status: 'PENDING_SCAN',
    }];
    const controller = new AbortController();
    vi.mocked(axios.get).mockResolvedValue({ data: response } as never);

    await expect(listFiles({ signal: controller.signal })).resolves.toEqual(response);

    expect(axios.get).toHaveBeenCalledWith('/api/v1/files', { signal: controller.signal });
  });

  it('normalizes an API error without exposing the raw response', async () => {
    vi.mocked(axios.post).mockRejectedValue({
      isAxiosError: true,
      response: { data: { code: 'INVALID_FILENAME', message: 'Le nom est invalide.' } },
    });

    await expect(uploadFile(new File(['content'], 'document.txt'))).rejects.toMatchObject({
      code: 'INVALID_FILENAME',
      message: 'Le nom est invalide.',
    });
  });
});