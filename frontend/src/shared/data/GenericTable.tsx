import { ArrowLeft, ArrowRight, ArrowUpDown } from 'lucide-react';
import { useEffect, useId, useState, type Key, type ReactNode } from 'react';
import { Button } from '../actions/Button';
import { Row } from '../layout/Row';

export type TableColumn<Row extends object> = {
  header: string;
  key: keyof Row;
  render?: (row: Row) => ReactNode;
  sortable?: boolean;
  sortValue?: (row: Row) => number | string;
  serverSortKey?: string;
};

type SortState<Row extends object> = {
  direction: 'ascending' | 'descending';
  key: keyof Row;
};

export type TablePagination = {
  pageSize: number;
  pageSizeOptions?: number[];
};

export type ServerTablePagination<Row extends object> = {
  page: number;
  pageSize: number;
  pageSizeOptions?: number[];
  sortDirection?: SortState<Row>['direction'];
  sortKey?: string;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onSortChange?: (sortKey: string) => void;
};

type GenericTableProps<Row extends object> = {
  animatedRowKey?: Key | null;
  caption: string;
  columns: TableColumn<Row>[];
  emptyMessage?: string;
  getRowKey?: (row: Row, index: number) => Key;
  pagination?: TablePagination;
  serverPagination?: ServerTablePagination<Row>;
  rows: Row[];
};

function compareValues(left: unknown, right: unknown) {
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right;
  }

  return String(left ?? '').localeCompare(String(right ?? ''), undefined, {
    numeric: true,
    sensitivity: 'base',
  });
}

function readCellValue<Row extends object>(row: Row, column: TableColumn<Row>) {
  if (column.render) {
    return column.render(row);
  }

  const value = row[column.key];
  return value === null || value === undefined ? '' : String(value);
}

export function GenericTable<Row extends object>({
  animatedRowKey = null,
  caption,
  columns,
  emptyMessage = 'Aucun élément à afficher.',
  getRowKey,
  pagination,
  rows,
  serverPagination,
}: GenericTableProps<Row>) {
  const captionId = `shared-table-caption-${useId().replace(/:/g, '')}`;
  const [sortState, setSortState] = useState<SortState<Row> | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(pagination?.pageSize ?? 10);
  const localPaginationEnabled = pagination !== undefined && serverPagination === undefined;
  const sortedRows = !serverPagination && sortState
    ? [...rows].sort((left, right) => {
        const column = columns.find(({ key }) => key === sortState.key);

        if (!column) {
          return 0;
        }

        const leftValue = column.sortValue ? column.sortValue(left) : left[column.key];
        const rightValue = column.sortValue ? column.sortValue(right) : right[column.key];
        const result = compareValues(leftValue, rightValue);

        return sortState.direction === 'ascending' ? result : -result;
      })
    : rows;
  const paginationOptions = pagination ?? serverPagination;
  const pageSizeOptions = paginationOptions
    ? Array.from(new Set([paginationOptions.pageSize, ...(paginationOptions.pageSizeOptions ?? [])]))
        .filter((option) => Number.isInteger(option) && option > 0)
    : [];
  const totalElements = serverPagination?.totalElements ?? sortedRows.length;
  const totalPages = serverPagination
    ? serverPagination.totalPages
    : pagination
    ? Math.max(1, Math.ceil(totalElements / pageSize))
    : 1;
  const visiblePage = serverPagination?.page ?? (pagination
    ? Math.min(currentPage, totalPages)
    : 1);
  const visibleRows = serverPagination
    ? rows
    : pagination
    ? sortedRows.slice((visiblePage - 1) * pageSize, visiblePage * pageSize)
    : sortedRows;
  const rowKeys = visibleRows.map((row, index) => getRowKey?.(row, index) ?? index);
  const animatedRowIndex = animatedRowKey === null
    ? -1
    : rowKeys.findIndex((rowKey) => rowKey === animatedRowKey);

  useEffect(() => {
    if (!localPaginationEnabled) {
      return;
    }
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [localPaginationEnabled, totalPages]);

  useEffect(() => {
    if (animatedRowKey !== null && localPaginationEnabled) {
      setCurrentPage(1);
    }
  }, [animatedRowKey, localPaginationEnabled]);

  function handleSort(column: TableColumn<Row>) {
    if (!column.sortable) {
      return;
    }

    if (serverPagination) {
      serverPagination.onSortChange?.(column.serverSortKey ?? String(column.key));
      return;
    }

    setSortState((currentSort) => ({
      direction:
        currentSort?.key === column.key && currentSort.direction === 'ascending'
          ? 'descending'
          : 'ascending',
      key: column.key,
    }));
    setCurrentPage(1);
  }

  function handlePageSizeChange(nextPageSize: string) {
    const nextPageSizeNumber = Number(nextPageSize);
    if (serverPagination) {
      serverPagination.onPageSizeChange(nextPageSizeNumber);
      return;
    }
    setPageSize(nextPageSizeNumber);
    setCurrentPage(1);
  }

  function goToPreviousPage() {
    if (serverPagination) {
      serverPagination.onPageChange(Math.max(1, visiblePage - 1));
      return;
    }
    setCurrentPage((page) => Math.max(1, Math.min(page, totalPages) - 1));
  }

  function goToNextPage() {
    if (serverPagination) {
      serverPagination.onPageChange(Math.min(totalPages, visiblePage + 1));
      return;
    }
    setCurrentPage((page) => Math.min(totalPages, Math.max(1, page) + 1));
  }

  return (
    <>
      <p className="shared-table__caption" id={captionId}>{caption}</p>
      <div className="shared-table__viewport">
        <table aria-labelledby={captionId} className="shared-table shared-table--framed">
          <thead>
            <tr>
              {columns.map((column) => {
                const sortingEnabled = serverPagination === undefined || serverPagination.onSortChange !== undefined;
                const isSortable = column.sortable && sortingEnabled;
                const serverSortKey = column.serverSortKey ?? String(column.key);
                const isSorted = isSortable && serverPagination
                  ? serverPagination.sortKey === serverSortKey
                  : isSortable && sortState?.key === column.key;
                const sortDirection = isSorted
                  ? serverPagination?.sortDirection ?? sortState?.direction ?? 'none'
                  : 'none';

                return (
                  <th
                    key={String(column.key)}
                    aria-sort={isSortable ? sortDirection : undefined}
                    scope="col"
                  >
                    {isSortable ? (
                      <button
                        aria-label={`Trier par ${column.header}`}
                        className="shared-table__sort-button"
                        onClick={() => handleSort(column)}
                        type="button"
                      >
                        <span>{column.header}</span>
                        <ArrowUpDown aria-hidden="true" size={16} />
                      </button>
                    ) : (
                      column.header
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {visibleRows.length > 0 ? (
              visibleRows.map((row, index) => {
                const isNewRow = index === animatedRowIndex;
                const isPushedRow = animatedRowIndex === 0 && index > 0;
                const rowClassName = [
                  isNewRow ? 'shared-table__row--new' : '',
                  isPushedRow ? 'shared-table__row--pushed' : '',
                ].filter(Boolean).join(' ');

                return (
                  <tr className={rowClassName || undefined} key={rowKeys[index]}>
                    {columns.map((column) => (
                      <td key={String(column.key)}>{readCellValue(row, column)}</td>
                    ))}
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan={columns.length}>{emptyMessage}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {paginationOptions && totalElements > 0 ? (
        <Row
          align="center"
          className="shared-table__pagination"
          justify="space-between"
          wrap="wrap"
        >
          <Row align="center" className="shared-table__pagination-summary" gap="12px" wrap="wrap">
            <span aria-live="polite">Page {visiblePage} sur {totalPages}</span>
            <span>{totalElements} éléments</span>
            <label>
              Éléments par page
              <select
                aria-label="Éléments par page"
                onChange={(event) => handlePageSizeChange(event.target.value)}
                value={serverPagination?.pageSize ?? pageSize}
              >
                {pageSizeOptions.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            </label>
          </Row>
          <Row align="center" className="shared-table__pagination-controls" gap="8px">
            <Button
              aria-label="Page précédente"
              disabled={serverPagination ? !serverPagination.hasPrevious : visiblePage === 1}
              icon={ArrowLeft}
              onClick={goToPreviousPage}
              variant="secondary"
            />
            <Button
              aria-label="Page suivante"
              disabled={serverPagination ? !serverPagination.hasNext : visiblePage === totalPages}
              icon={ArrowRight}
              onClick={goToNextPage}
              variant="secondary"
            />
          </Row>
        </Row>
      ) : null}
    </>
  );
}