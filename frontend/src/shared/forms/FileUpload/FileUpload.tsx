import { useEffect, useId, useState, type ChangeEvent, type FormEvent } from 'react';
import { Clock3, CloudUpload, LoaderCircle, ShieldAlert, ShieldCheck } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import {
  getFileMetadata,
  getUploadConfiguration,
  uploadFile,
  type FileMetadataResponse,
  type ScanStatus,
} from '../../../api/filesApi';
import { Button } from '../../actions/Button';
import { Tag } from '../../feedback/Tag';
import { Column } from '../../layout/Column';
import { Row } from '../../layout/Row';
import { fileUploadClassNames } from './style';

type UploadFeedback =
  | { kind: 'idle' }
  | { kind: 'uploading'; progress: number }
  | {
      kind: 'accepted';
      response: FileMetadataResponse;
      pollingError?: { message: string; retrying: boolean };
    }
  | { kind: 'error'; message: string };

type UploadConfigurationState =
  | { kind: 'loading' }
  | { kind: 'available'; maximumSizeBytes: number }
  | { kind: 'error'; message: string };

export type FileUploadProps = {
  onAccepted?: (response: FileMetadataResponse) => void;
  onStatusChange?: (response: FileMetadataResponse) => void;
};

const METADATA_POLL_INTERVAL_MS = 250;
const MAX_METADATA_POLL_RETRIES = 3;

function statusIcon(status: ScanStatus): LucideIcon {
  if (status === 'CLEAN') {
    return ShieldCheck;
  }

  if (status === 'INFECTED' || status === 'SCAN_FAILED' || status === 'REJECTED') {
    return ShieldAlert;
  }

  return Clock3;
}

function statusTone(status: ScanStatus): 'danger' | 'success' | 'warning' {
  if (status === 'CLEAN') {
    return 'success';
  }

  if (status === 'INFECTED' || status === 'SCAN_FAILED' || status === 'REJECTED') {
    return 'danger';
  }

  return 'warning';
}

function isTerminalStatus(status: ScanStatus) {
  return status === 'CLEAN'
    || status === 'INFECTED'
    || status === 'SCAN_FAILED'
    || status === 'REJECTED';
}

function shouldRetryMetadataPolling(error: unknown) {
  const status = error instanceof Error
    ? (error as Error & { status?: unknown }).status
    : undefined;
  if (typeof status !== 'number') {
    return true;
  }

  return status === 408 || status === 429 || status >= 500;
}

function statusMessage(status: ScanStatus) {
  switch (status) {
    case 'CLEAN':
      return 'Fichier sain. Le telechargement est autorise.';
    case 'INFECTED':
      return 'Fichier bloque : une menace a ete detectee.';
    case 'SCAN_FAILED':
      return "Fichier bloque : l'analyse antivirus a echoue.";
    case 'REJECTED':
      return 'Fichier rejete : il ne peut pas etre traite.';
    case 'SCANNING':
      return 'Transmission terminee. Analyse antivirus en cours...';
    case 'UPLOADING':
      return 'Transmission du fichier en cours...';
    case 'PENDING_SCAN':
      return 'Transmission terminee. Analyse antivirus en attente.';
  }
}

function formatMaximumUploadSize(maximumSizeBytes: number) {
  const units = ['o', 'Ko', 'Mo', 'Go'];
  let readableSize = maximumSizeBytes;
  let unitIndex = 0;
  while (readableSize >= 1024 && unitIndex < units.length - 1) {
    readableSize /= 1024;
    unitIndex += 1;
  }
  const fractionDigits = Number.isInteger(readableSize) ? 0 : 1;
  return `${readableSize.toFixed(fractionDigits)} ${units[unitIndex]}`;
}

export function FileUpload({ onAccepted, onStatusChange }: FileUploadProps) {
  const inputId = `shared-file-upload-input-${useId().replace(/:/g, '')}`;
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [feedback, setFeedback] = useState<UploadFeedback>({ kind: 'idle' });
  const [uploadConfiguration, setUploadConfiguration] = useState<UploadConfigurationState>({
    kind: 'loading',
  });
  const [uploadConfigurationRequestVersion, setUploadConfigurationRequestVersion] = useState(0);
  const acceptedFileId = feedback.kind === 'accepted' ? feedback.response.fileId : null;
  const selectedFileExceedsMaximumSize = selectedFile !== null
    && uploadConfiguration.kind === 'available'
    && selectedFile.size > uploadConfiguration.maximumSizeBytes;
  const uploadValidationMessage = uploadConfiguration.kind === 'error'
    ? uploadConfiguration.message
    : selectedFileExceedsMaximumSize
      ? 'Le fichier depasse la taille maximale autorisee.'
      : null;

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setSelectedFile(event.target.files?.[0] ?? null);
    setFeedback({ kind: 'idle' });
  }

  function handleUploadConfigurationRetry() {
    setUploadConfiguration({ kind: 'loading' });
    setUploadConfigurationRequestVersion((currentVersion) => currentVersion + 1);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) {
      return;
    }
    if (uploadConfiguration.kind !== 'available') {
      setFeedback({
        kind: 'error',
        message: uploadConfiguration.kind === 'error'
          ? uploadConfiguration.message
          : 'La taille maximale autorisee est en cours de chargement.',
      });
      return;
    }
    if (selectedFileExceedsMaximumSize) {
      setFeedback({ kind: 'error', message: 'Le fichier depasse la taille maximale autorisee.' });
      return;
    }

    setFeedback({ kind: 'uploading', progress: 0 });
    try {
      const response = await uploadFile(selectedFile, {
        onProgress: (progress) => {
          setFeedback((currentFeedback) => currentFeedback.kind === 'uploading'
            ? { kind: 'uploading', progress }
            : currentFeedback);
        },
      });
      setFeedback({ kind: 'accepted', response });
      onAccepted?.(response);
    } catch (error) {
      setFeedback({
        kind: 'error',
        message: error instanceof Error ? error.message : 'Le fichier ne peut pas etre envoye.',
      });
    }
  }

  useEffect(() => {
    let cancelled = false;

    getUploadConfiguration()
      .then((configuration) => {
        if (!cancelled) {
          setUploadConfiguration({
            kind: 'available',
            maximumSizeBytes: configuration.maximumSizeBytes,
          });
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setUploadConfiguration({
            kind: 'error',
            message: error instanceof Error
              ? error.message
              : 'La taille maximale autorisee ne peut pas etre lue.',
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [uploadConfigurationRequestVersion]);

  useEffect(() => {
    if (acceptedFileId === null) {
      return undefined;
    }
    const fileId = acceptedFileId;
    if (feedback.kind !== 'accepted' || isTerminalStatus(feedback.response.status)) {
      return undefined;
    }

    const requestController = new AbortController();
    let cancelled = false;
    let pollingTimer: ReturnType<typeof setTimeout> | null = null;
    let retryCount = 0;

    function scheduleNextPoll(poll: () => Promise<void>) {
      if (!cancelled) {
        pollingTimer = setTimeout(poll, METADATA_POLL_INTERVAL_MS);
      }
    }

    async function pollMetadata() {
      try {
        const response = await getFileMetadata(fileId, {
          signal: requestController.signal,
        });
        if (cancelled) {
          return;
        }

        retryCount = 0;
        onStatusChange?.(response);
        setFeedback((currentFeedback) => {
          if (currentFeedback.kind !== 'accepted'
            || currentFeedback.response.fileId !== fileId) {
            return currentFeedback;
          }

          return { kind: 'accepted', response };
        });
        if (!isTerminalStatus(response.status)) {
          scheduleNextPoll(pollMetadata);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        retryCount += 1;
        const retryable = shouldRetryMetadataPolling(error);
        const willRetry = retryable && retryCount <= MAX_METADATA_POLL_RETRIES;
        setFeedback((currentFeedback) => {
          if (currentFeedback.kind !== 'accepted'
            || currentFeedback.response.fileId !== fileId) {
            return currentFeedback;
          }

          return {
            ...currentFeedback,
            pollingError: {
              message: error instanceof Error
                ? error.message
                : 'Le statut du fichier ne peut pas etre lu.',
              retrying: willRetry,
            },
          };
        });
        if (willRetry) {
          scheduleNextPoll(pollMetadata);
        }
      }
    }

    scheduleNextPoll(pollMetadata);
    return () => {
      cancelled = true;
      requestController.abort();
      if (pollingTimer) {
        clearTimeout(pollingTimer);
      }
    };
  }, [acceptedFileId]);

  const isUploading = feedback.kind === 'uploading';
  const isUploadDisabled = isUploading
    || uploadConfiguration.kind !== 'available'
    || selectedFileExceedsMaximumSize;

  return (
    <div className={fileUploadClassNames.root}>
      <form className={fileUploadClassNames.form} onSubmit={handleSubmit}>
        <Column gap="16px">
          <label className={fileUploadClassNames.zone} htmlFor={inputId}>
            <span className={fileUploadClassNames.zoneContent}>
              <CloudUpload aria-hidden="true" size={34} />
              <span>Choisissez un fichier</span>
              <span className={fileUploadClassNames.zoneHint}>PDF, image ou archive securisee</span>
              {uploadConfiguration.kind === 'available' ? (
                <span className={fileUploadClassNames.zoneHint}>
                  Taille maximale autorisee : {formatMaximumUploadSize(
                    uploadConfiguration.maximumSizeBytes,
                  )}
                </span>
              ) : null}
            </span>
            <input
              aria-label="Choisir un fichier"
              className={fileUploadClassNames.input}
              disabled={isUploading}
              id={inputId}
              onChange={handleFileChange}
              type="file"
            />
          </label>
          {selectedFile ? (
            <p className={fileUploadClassNames.selectedFile}>
              Fichier selectionne : <strong>{selectedFile.name}</strong> ({selectedFile.size} octets)
            </p>
          ) : null}
          {selectedFile ? (
            <Row align="center" gap="12px" wrap="wrap">
              <Button disabled={isUploadDisabled} icon={CloudUpload} type="submit">
                {isUploading ? 'Envoi en cours...' : 'Envoyer le fichier'}
              </Button>
              {isUploading ? (
                <Column gap="6px">
                  <p aria-live="polite" className={fileUploadClassNames.progressMessage}>
                    <LoaderCircle
                      aria-hidden="true"
                      className={fileUploadClassNames.loading}
                      size={16}
                    />
                    Transmission du fichier...
                  </p>
                  <Row align="center" gap="8px">
                    <progress
                      aria-label="Progression de l'upload"
                      className={fileUploadClassNames.progressBar}
                      max={100}
                      value={feedback.progress}
                    />
                    <span className={fileUploadClassNames.progressValue}>
                      {feedback.progress} %
                    </span>
                  </Row>
                </Column>
              ) : null}
            </Row>
          ) : null}
        </Column>
      </form>
      {uploadValidationMessage ? (
        <div
          aria-live="assertive"
          className={`${fileUploadClassNames.feedback} ${fileUploadClassNames.feedbackError}`}
          role="alert"
        >
          <Row align="center" gap="12px" wrap="wrap">
            <span>{uploadValidationMessage}</span>
            {uploadConfiguration.kind === 'error' ? (
              <Button onClick={handleUploadConfigurationRetry} type="button" variant="secondary">
                Reessayer la verification
              </Button>
            ) : null}
          </Row>
        </div>
      ) : null}
      <UploadFeedbackView feedback={feedback} />
    </div>
  );
}

function UploadFeedbackView({ feedback }: { feedback: UploadFeedback }) {
  if (feedback.kind === 'error') {
    return (
      <div
        aria-live="assertive"
        className={`${fileUploadClassNames.feedback} ${fileUploadClassNames.feedbackError}`}
        role="alert"
      >
        {feedback.message}
      </div>
    );
  }

  if (feedback.kind !== 'accepted') {
    return null;
  }

  const StatusIcon = statusIcon(feedback.response.status);
  return (
    <div aria-live="polite" className={fileUploadClassNames.feedback} role="status">
      <Row align="center" gap="10px" wrap="wrap">
        <Tag
          icon={StatusIcon}
          text={feedback.response.status}
          tone={statusTone(feedback.response.status)}
        />
        <strong>{feedback.response.originalFilename}</strong>
      </Row>
      <p className={fileUploadClassNames.feedbackMessage}>
        {statusMessage(feedback.response.status)}
      </p>
      {feedback.pollingError ? (
        <p className={fileUploadClassNames.feedbackPollingError} role="alert">
          {feedback.pollingError.retrying
            ? 'Suivi du traitement interrompu temporairement'
            : 'Suivi du traitement arrete'}
          : {feedback.pollingError.message}
        </p>
      ) : null}
      <p className={fileUploadClassNames.feedbackId}>
        Identifiant : {feedback.response.fileId}
      </p>
    </div>
  );
}