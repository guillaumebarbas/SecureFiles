import { useEffect, useId, useRef, useState, type FocusEvent, type KeyboardEvent } from 'react';
import { Check, Clock3, CloudUpload, Filter, FolderOpen, RefreshCw, ShieldAlert, ShieldCheck, X } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import {
  listFiles,
  type FileSortField,
  type FileMetadataResponse,
  type FilesPageResponse,
  type ScanStatus,
  type SortDirection,
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
    serverSortKey: 'name',
    sortable: true,
  },
  {
    header: 'Auteur',
    key: 'author',
    render: (file) => file.author ?? 'Auteur inconnu',
    serverSortKey: 'author',
    sortable: true,
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
    serverSortKey: 'size',
    sortable: true,
  },
  {
    header: 'Ajouté le',
    key: 'createdAt',
    render: (file) => formatCreatedAt(file.createdAt),
    serverSortKey: 'createdAt',
    sortable: true,
  },
];

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_SORT_FIELD: FileSortField = 'createdAt';
const DEFAULT_SORT_DIRECTION: SortDirection = 'desc';
const PAGE_SIZE_OPTIONS = [5, 10, 25];
const statusFilterOptions: readonly { value: ScanStatus; label: string }[] = [
  { value: 'CLEAN', label: 'Sain' },
  { value: 'INFECTED', label: 'Infecté' },
  { value: 'PENDING_SCAN', label: "En attente d'analyse" },
  { value: 'REJECTED', label: 'Rejeté' },
  { value: 'SCAN_FAILED', label: 'Analyse échouée' },
  { value: 'SCANNING', label: 'Analyse en cours' },
  { value: 'UPLOADING', label: 'Envoi en cours' },
];
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
  const [sortField, setSortField] = useState<FileSortField>(DEFAULT_SORT_FIELD);
  const [sortDirection, setSortDirection] = useState<SortDirection>(DEFAULT_SORT_DIRECTION);
  const [selectedStatuses, setSelectedStatuses] = useState<ScanStatus[]>([]);
  const [isStatusFilterOpen, setIsStatusFilterOpen] = useState(false);
  const [refreshVersion, setRefreshVersion] = useState(0);
  const statusFilterRootRef = useRef<HTMLDivElement>(null);
  const statusFilterPanelId = useId().replace(/:/g, '') + '-status-filter';

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    setIsLoadingFiles(true);
    listFiles({
      direction: sortDirection,
      page,
      signal: controller.signal,
      size: pageSize,
      sort: sortField,
      ...(selectedStatuses.length > 0 ? { statuses: selectedStatuses } : {}),
    })
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
  }, [page, pageSize, refreshVersion, selectedStatuses, sortDirection, sortField]);

  useEffect(() => {
    if (!isStatusFilterOpen) {
      return;
    }
    statusFilterRootRef.current?.querySelector<HTMLInputElement>('input[type="checkbox"]')?.focus();
  }, [isStatusFilterOpen]);

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

  function handleSortChange(nextSortField: string) {
    const normalizedSortField = nextSortField as FileSortField;
    setSortDirection((currentDirection) => (
      normalizedSortField === sortField && currentDirection === 'asc' ? 'desc' : 'asc'
    ));
    setSortField(normalizedSortField);
    setPage(DEFAULT_PAGE);
  }

  function closeStatusFilter() {
    setIsStatusFilterOpen(false);
    statusFilterRootRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
  }

  function handleStatusFilterBlur(event: FocusEvent<HTMLDivElement>) {
    const nextFocusedElement = event.relatedTarget;
    if (
      !nextFocusedElement
      || !(nextFocusedElement instanceof Node)
      || !statusFilterRootRef.current?.contains(nextFocusedElement)
    ) {
      closeStatusFilter();
    }
  }

  function handleStatusFilterKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      closeStatusFilter();
    }
  }

  function handleStatusChange(status: ScanStatus, checked: boolean) {
    setSelectedStatuses((currentStatuses) => {
      const nextStatuses = checked
        ? [...currentStatuses, status]
        : currentStatuses.filter((currentStatus) => currentStatus !== status);
      return statusFilterOptions
        .map(({ value }) => value)
        .filter((value) => nextStatuses.includes(value));
    });
    setPage(DEFAULT_PAGE);
  }

  function clearStatusFilters() {
    setSelectedStatuses([]);
    setPage(DEFAULT_PAGE);
  }

  return (
    <div className={dashboardPageClassNames.root}>
      <Section
        className={dashboardPageClassNames.uploadSection}
        description="Dépose un fichier pour suivre son transfert et son analyse antivirus."
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
        title="Fichiers récents"
      >
        <div
          className="dashboard-page__files-toolbar"
          onBlur={handleStatusFilterBlur}
          onKeyDown={handleStatusFilterKeyDown}
          ref={statusFilterRootRef}
        >
          <div className="dashboard-page__status-filter">
            <Button
              aria-label="Filtrer par statut"
              aria-controls={statusFilterPanelId}
              aria-expanded={isStatusFilterOpen}
              aria-haspopup="dialog"
              icon={Filter}
              onClick={() => setIsStatusFilterOpen((isOpen) => !isOpen)}
              variant="secondary"
            >
              Filtrer
              {selectedStatuses.length > 0 ? (
                <span aria-label={`${selectedStatuses.length} filtres actifs`}>
                  {selectedStatuses.length}
                </span>
              ) : null}
            </Button>
            {isStatusFilterOpen ? (
              <div
                aria-label="Filtrer les fichiers par statut"
                className="dashboard-page__status-filter-panel"
                id={statusFilterPanelId}
                role="dialog"
              >
                <fieldset>
                  <legend>Statut</legend>
                  {statusFilterOptions.map(({ label, value }) => (
                    <label className="dashboard-page__status-filter-option" key={value}>
                      <input
                        checked={selectedStatuses.includes(value)}
                        onChange={(event) => handleStatusChange(value, event.target.checked)}
                        type="checkbox"
                      />
                      <span>{label}</span>
                      {selectedStatuses.includes(value) ? <Check aria-hidden="true" size={15} /> : null}
                    </label>
                  ))}
                </fieldset>
                {selectedStatuses.length > 0 ? (
                  <Button icon={X} onClick={clearStatusFilters} variant="secondary">
                    Effacer les filtres
                  </Button>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>
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
              Réessayer
            </Button>
          </Row>
        ) : null}
        <GenericTable
          animatedRowKey={animatedFileId}
          caption="Fichiers uploadés"
          columns={dashboardFileColumns}
          emptyMessage="Aucun fichier uploadé pour le moment."
          getRowKey={(file) => file.fileId}
          rows={files}
          serverPagination={{
            page: filesPagination.page,
            pageSize: filesPagination.size,
            pageSizeOptions: PAGE_SIZE_OPTIONS,
            sortDirection: sortDirection === 'asc' ? 'ascending' : 'descending',
            sortKey: sortField,
            totalElements: filesPagination.totalElements,
            totalPages: filesPagination.totalPages,
            hasNext: filesPagination.hasNext,
            hasPrevious: filesPagination.hasPrevious,
            onPageChange: setPage,
            onPageSizeChange: handlePageSizeChange,
            onSortChange: handleSortChange,
          }}
        />
      </Section>
    </div>
  );
}

function readErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : 'La liste des fichiers ne peut pas être lue.';
}
