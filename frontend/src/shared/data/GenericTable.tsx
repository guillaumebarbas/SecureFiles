import { ArrowUpDown } from 'lucide-react';
import { useState, type Key, type ReactNode } from 'react';

export type TableColumn<Row extends object> = {
  header: string;
  key: keyof Row;
  render?: (row: Row) => ReactNode;
  sortable?: boolean;
  sortValue?: (row: Row) => number | string;
};

type SortState<Row extends object> = {
  direction: 'ascending' | 'descending';
  key: keyof Row;
};

type GenericTableProps<Row extends object> = {
  caption: string;
  columns: TableColumn<Row>[];
  emptyMessage?: string;
  getRowKey?: (row: Row, index: number) => Key;
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
  caption,
  columns,
  emptyMessage = 'Aucun element a afficher.',
  getRowKey,
  rows,
}: GenericTableProps<Row>) {
  const [sortState, setSortState] = useState<SortState<Row> | null>(null);
  const sortedRows = sortState
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

  function handleSort(column: TableColumn<Row>) {
    if (!column.sortable) {
      return;
    }

    setSortState((currentSort) => ({
      direction:
        currentSort?.key === column.key && currentSort.direction === 'ascending'
          ? 'descending'
          : 'ascending',
      key: column.key,
    }));
  }

  return (
    <table className="shared-table">
      <caption>{caption}</caption>
      <thead>
        <tr>
          {columns.map((column) => {
            const isSorted = sortState?.key === column.key;
            const sortDirection = isSorted ? sortState.direction : 'none';

            return (
              <th key={String(column.key)} aria-sort={column.sortable ? sortDirection : undefined} scope="col">
                {column.sortable ? (
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
        {sortedRows.length > 0 ? (
          sortedRows.map((row, index) => (
            <tr key={getRowKey?.(row, index) ?? index}>
              {columns.map((column) => (
                <td key={String(column.key)}>{readCellValue(row, column)}</td>
              ))}
            </tr>
          ))
        ) : (
          <tr>
            <td colSpan={columns.length}>{emptyMessage}</td>
          </tr>
        )}
      </tbody>
    </table>
  );
}