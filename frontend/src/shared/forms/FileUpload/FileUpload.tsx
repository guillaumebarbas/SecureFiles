import {
  useEffect,
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
  type MouseEvent,
} from 'react';
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

type AcceptedFeedbackVisibility = 'hidden' | 'hiding' | 'visible';

export type FileUploadProps = {
  isAuthenticated?: boolean;
  onAuthenticationRequired?: () => void;
  onAccepted?: (response: FileMetadataResponse) => void;
  onStatusChange?: (response: FileMetadataResponse) => void;
};

const METADATA_POLL_INTERVAL_MS = 250;
const MAX_METADATA_POLL_RETRIES = 3;
const ACCEPTED_FEEDBACK_DISMISS_DELAY_MS = 10_000;
const ACCEPTED_FEEDBACK_EXIT_DURATION_MS = 320;
const AUTHENTICATION_REQUIRED_MESSAGE = 'Vous devez être connecté pour sélectionner un fichier.';

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
      return 'Fichier sain. Le téléchargement est autorisé.';
    case 'INFECTED':
      return 'Fichier bloqué : une menace a été détectée.';
    case 'SCAN_FAILED':
      return "Fichier bloqué : l'analyse antivirus a échoué.";
    case 'REJECTED':
      return 'Fichier rejeté : il ne peut pas être traité.';
    case 'SCANNING':
      return 'Transmission terminée. Analyse antivirus en cours...';
    case 'UPLOADING':
      return 'Transmission du fichier en cours...';
    case 'PENDING_SCAN':
      return 'Transmission terminée. Analyse antivirus en attente.';
  }
}

function formatFileSize(sizeBytes: number) {
  const units = ['o', 'Ko', 'Mo', 'Go'];
  let readableSize = sizeBytes;
  let unitIndex = 0;
  while (readableSize >= 1024 && unitIndex < units.length - 1) {
    readableSize /= 1024;
    unitIndex += 1;
  }
  const fractionDigits = Number.isInteger(readableSize) ? 0 : 1;
  return `${readableSize.toFixed(fractionDigits)} ${units[unitIndex]}`;
}

export function FileUpload({
  isAuthenticated = true,
  onAuthenticationRequired,
  onAccepted,
  onStatusChange,
}: FileUploadProps) {
  const inputId = `shared-file-upload-input-${useId().replace(/:/g, '')}`;
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [feedback, setFeedback] = useState<UploadFeedback>({ kind: 'idle' });
  const [acceptedFeedbackVisibility, setAcceptedFeedbackVisibility] = useState<AcceptedFeedbackVisibility>('hidden');
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
      ? 'Le fichier dépasse la taille maximale autorisée.'
      : null;

  const acceptedFeedbackDismissTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const acceptedFeedbackRemovalTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  function clearAcceptedFeedbackTimers() {
    if (acceptedFeedbackDismissTimer.current) {
      clearTimeout(acceptedFeedbackDismissTimer.current);
      acceptedFeedbackDismissTimer.current = null;
    }
    if (acceptedFeedbackRemovalTimer.current) {
      clearTimeout(acceptedFeedbackRemovalTimer.current);
      acceptedFeedbackRemovalTimer.current = null;
    }
  }

  function scheduleAcceptedFeedbackDismissal() {
    clearAcceptedFeedbackTimers();
    setAcceptedFeedbackVisibility('visible');
    acceptedFeedbackDismissTimer.current = setTimeout(() => {
      acceptedFeedbackDismissTimer.current = null;
      setAcceptedFeedbackVisibility('hiding');
      acceptedFeedbackRemovalTimer.current = setTimeout(() => {
        acceptedFeedbackRemovalTimer.current = null;
        setAcceptedFeedbackVisibility('hidden');
      }, ACCEPTED_FEEDBACK_EXIT_DURATION_MS);
    }, ACCEPTED_FEEDBACK_DISMISS_DELAY_MS);
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    clearAcceptedFeedbackTimers();
    setAcceptedFeedbackVisibility('hidden');
    setSelectedFile(event.target.files?.[0] ?? null);
    setFeedback({ kind: 'idle' });
  }

  function requestAuthentication() {
    setFeedback({ kind: 'error', message: AUTHENTICATION_REQUIRED_MESSAGE });
    onAuthenticationRequired?.();
  }

  function handleFileSelectionClick(event: MouseEvent<HTMLLabelElement>) {
    if (!isAuthenticated) {
      event.preventDefault();
      requestAuthentication();
    }
  }

  function handleUploadConfigurationRetry() {
    setUploadConfiguration({ kind: 'loading' });
    setUploadConfigurationRequestVersion((currentVersion) => currentVersion + 1);
  }

  function clearFileSelection() {
    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) {
      return;
    }
    if (!isAuthenticated) {
      requestAuthentication();
      return;
    }
    if (uploadConfiguration.kind !== 'available') {
      setFeedback({
        kind: 'error',
        message: uploadConfiguration.kind === 'error'
          ? uploadConfiguration.message
          : 'La taille maximale autorisée est en cours de chargement.',
      });
      return;
    }
    if (selectedFileExceedsMaximumSize) {
      setFeedback({ kind: 'error', message: 'Le fichier dépasse la taille maximale autorisée.' });
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
      clearFileSelection();
      setFeedback({ kind: 'accepted', response });
      scheduleAcceptedFeedbackDismissal();
      onAccepted?.(response);
    } catch (error) {
      setFeedback({
        kind: 'error',
        message: error instanceof Error ? error.message : 'Le fichier ne peut pas être envoyé.',
      });
    }
  }

  useEffect(() => () => clearAcceptedFeedbackTimers(), []);

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
                : 'La taille maximale autorisée ne peut pas être lue.',
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
                : 'Le statut du fichier ne peut pas être lu.',
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
          <label
            className={fileUploadClassNames.zone}
            htmlFor={inputId}
            onClick={handleFileSelectionClick}
          >
            <span className={fileUploadClassNames.zoneContent}>
              <CloudUpload aria-hidden="true" size={34} />
              <span>Choisissez un fichier</span>
              {feedback.kind === 'accepted' && !selectedFile ? (
                <span className={fileUploadClassNames.zoneHint}>
                  Cliquez sur la zone pour choisir un nouveau fichier
                </span>
              ) : null}
              <span className={fileUploadClassNames.zoneHint}>PDF, image ou archive sécurisée</span>
              {uploadConfiguration.kind === 'available' ? (
                <span className={fileUploadClassNames.zoneHint}>
                    Taille maximale autorisée : {formatFileSize(
                    uploadConfiguration.maximumSizeBytes,
                  )}
                </span>
              ) : null}
            </span>
            <input
              aria-label="Choisir un fichier"
              className={fileUploadClassNames.input}
              disabled={isUploading || !isAuthenticated}
              id={inputId}
              onChange={handleFileChange}
              ref={fileInputRef}
              type="file"
            />
          </label>
          {selectedFile ? (
            <p className={fileUploadClassNames.selectedFile}>
              Fichier sélectionné : <strong>{selectedFile.name}</strong> ({formatFileSize(selectedFile.size)})
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
                Réessayer la vérification
              </Button>
            ) : null}
          </Row>
        </div>
      ) : null}
      <UploadFeedbackView
        acceptedFeedbackVisibility={acceptedFeedbackVisibility}
        feedback={feedback}
      />
    </div>
  );
}

function UploadFeedbackView({
  acceptedFeedbackVisibility,
  feedback,
}: {
  acceptedFeedbackVisibility: AcceptedFeedbackVisibility;
  feedback: UploadFeedback;
}) {
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

  if (acceptedFeedbackVisibility === 'hidden') {
    return null;
  }

  const StatusIcon = statusIcon(feedback.response.status);
  const feedbackClassName = [
    fileUploadClassNames.feedback,
    fileUploadClassNames.feedbackAccepted,
    acceptedFeedbackVisibility === 'hiding' ? fileUploadClassNames.feedbackHiding : null,
  ].filter(Boolean).join(' ');
  return (
    <div aria-live="polite" className={feedbackClassName} role="status">
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
            : 'Suivi du traitement arrêté'}
          : {feedback.pollingError.message}
        </p>
      ) : null}
      <p className={fileUploadClassNames.feedbackId}>
        Identifiant : {feedback.response.fileId}
      </p>
    </div>
  );
}