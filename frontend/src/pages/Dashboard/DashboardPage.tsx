import { useEffect, useState } from 'react';
import { Clock3, CloudUpload, FolderOpen, RefreshCw, ShieldAlert, ShieldCheck } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import {
  listFiles,
  type FileMetadataResponse,
  type FilesPageResponse,
  type ScanStatus,
} from '../../api/filesApi';
import { Button } from '../../shared/actions/Button';
import { GenericTable, type TableColumn } from '../../shared/data/GenericTable';
import { FileUpload } from '../../shared/forms/FileUpload/FileUpload';
import { Tag } from '../../shared/feedback/Tag';
import { Section } from '../../shared/layout/Section/Section';
import { Row } from '../../shared/layout/Row';
import { fileFailureDetails } from '../../shared/constants/fileFailureCodes';
import { dashboardPageClassNames } from './style';

const dashboardFileColumns: TableColumn<FileMetadataResponse>[] = [
  {
    header: 'Nom',
    key: 'originalFilename',
  },
  {
    header: 'Auteur',
    key: 'author',
    render: (file) => file.author ?? 'Auteur inconnu',
  },
  {
    header: 'Statut',
    key: 'status',
    render: (file) => (
      <Tag
        details={fileFailureDetails(file.failureCode)}
        icon={statusIcon(file.status)}
        text={file.status}
        tone={statusTone(file.status)}
      />
    ),
  },
  {
    header: 'Taille',
    key: 'sizeBytes',
    render: (file) => formatFileSize(file.sizeBytes),
  },
  {
    header: 'Ajoute le',
    key: 'createdAt',
    render: (file) => formatCreatedAt(file.createdAt),
  },
];

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [5, 10, 25];
const EMPTY_FILES_PAGE: FilesPageResponse = {
  content: [],
  hasNext: false,
  hasPrevious: false,
  page: DEFAULT_PAGE,
  size: DEFAULT_PAGE_SIZE,
  totalElements: 0,
  totalPages: 0,
};

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

function formatFileSize(sizeBytes: number | null) {
  if (sizeBytes === null) {
    return 'Taille inconnue';
  }

  if (sizeBytes < 1024) {
    return `${sizeBytes} o`;
  }

  const units = ['Ko', 'Mo', 'Go'];
  let readableSize = sizeBytes;
  let unitIndex = -1;
  while (readableSize >= 1024 && unitIndex < units.length - 1) {
    readableSize /= 1024;
    unitIndex += 1;
  }

  return `${readableSize.toFixed(readableSize < 10 ? 1 : 0)} ${units[unitIndex]}`;
}

function formatCreatedAt(createdAt: string) {
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(createdAt));
}

export type DashboardPageProps = {
  isAuthenticated?: boolean;
  onAuthenticationRequired?: () => void;
};

export function DashboardPage({
  isAuthenticated = true,
  onAuthenticationRequired,
}: DashboardPageProps = {}) {
  const [files, setFiles] = useState<FileMetadataResponse[]>([]);
  const [filesPagination, setFilesPagination] = useState<FilesPageResponse>(EMPTY_FILES_PAGE);
  const [animatedFileId, setAnimatedFileId] = useState<string | null>(null);
  const [filesError, setFilesError] = useState<string | null>(null);
  const [isLoadingFiles, setIsLoadingFiles] = useState(true);
  const [page, setPage] = useState(DEFAULT_PAGE);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [refreshVersion, setRefreshVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    setIsLoadingFiles(true);
    listFiles({ page, signal: controller.signal, size: pageSize })
      .then((serverPage) => {
        if (!isActive) {
          return;
        }
        setFiles(serverPage.content);
        setFilesPagination(serverPage);
        setFilesError(null);
      })
      .catch((error: unknown) => {
        if (!isActive || controller.signal.aborted) {
          return;
        }
        setFilesError(readErrorMessage(error));
      })
      .finally(() => {
        if (isActive) {
          setIsLoadingFiles(false);
        }
      });

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [page, pageSize, refreshVersion]);

  function handleFileAccepted(file: FileMetadataResponse) {
    setAnimatedFileId(file.fileId);
    setFilesError(null);
    setPage(DEFAULT_PAGE);
    setRefreshVersion((currentVersion) => currentVersion + 1);
  }

  function handleFileStatusChange(file: FileMetadataResponse) {
    setFiles((currentFiles) => currentFiles.map((currentFile) => (
      currentFile.fileId === file.fileId ? file : currentFile
    )));
  }

  function handleFilesRetry() {
    setRefreshVersion((currentVersion) => currentVersion + 1);
  }

  function handlePageSizeChange(nextPageSize: number) {
    setPageSize(nextPageSize);
    setPage(DEFAULT_PAGE);
  }

  return (
    <div className={dashboardPageClassNames.root}>
      <Section
        className={dashboardPageClassNames.uploadSection}
        description="Depose un fichier pour suivre son transfert et son analyse antivirus."
        icon={CloudUpload}
        title="Upload ton fichier"
      >
        <Row className={dashboardPageClassNames.uploadContent} justify="center" wrap="wrap">
          <FileUpload
            isAuthenticated={isAuthenticated}
            onAuthenticationRequired={onAuthenticationRequired}
            onAccepted={handleFileAccepted}
            onStatusChange={handleFileStatusChange}
          />
        </Row>
      </Section>
      <Section
        description="Les fichiers acceptes apparaissent ici avec leur statut de scan."
        icon={FolderOpen}
        title="Fichiers recents"
      >
        {isLoadingFiles && files.length === 0 ? (
          <p aria-live="polite" className={dashboardPageClassNames.filesLoading}>
            Chargement des fichiers...
          </p>
        ) : null}
        {filesError ? (
          <Row
            align="center"
            className={dashboardPageClassNames.filesError}
            justify="space-between"
            wrap="wrap"
          >
            <p role="alert">{filesError}</p>
            <Button icon={RefreshCw} onClick={handleFilesRetry} variant="secondary">
              Reessayer
            </Button>
          </Row>
        ) : null}
        <GenericTable
          animatedRowKey={animatedFileId}
          caption="Fichiers uploades"
          columns={dashboardFileColumns}
          emptyMessage="Aucun fichier upload pour le moment."
          getRowKey={(file) => file.fileId}
          rows={files}
          serverPagination={{
            page: filesPagination.page,
            pageSize: filesPagination.size,
            pageSizeOptions: PAGE_SIZE_OPTIONS,
            totalElements: filesPagination.totalElements,
            totalPages: filesPagination.totalPages,
            hasNext: filesPagination.hasNext,
            hasPrevious: filesPagination.hasPrevious,
            onPageChange: setPage,
            onPageSizeChange: handlePageSizeChange,
          }}
        />
      </Section>
    </div>
  );
}

function readErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : 'La liste des fichiers ne peut pas etre lue.';
}
