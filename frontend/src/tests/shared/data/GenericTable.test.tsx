import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { GenericTable, type TableColumn } from '../../../shared/data/GenericTable';

type FileRow = {
  id: string;
  name: string;
  status: string;
};

const columns: TableColumn<FileRow>[] = [
  { header: 'Nom', key: 'name', sortable: true },
  { header: 'Statut', key: 'status' },
];

const rows: FileRow[] = [
  { id: 'file-2', name: 'zulu.pdf', status: 'PENDING_SCAN' },
  { id: 'file-1', name: 'alpha.pdf', status: 'CLEAN' },
];

describe('GenericTable', () => {
  it('renders its caption, headers, and row values', () => {
    render(
      <GenericTable
        caption="Registre des fichiers"
        columns={columns}
        getRowKey={(row) => row.id}
        rows={rows}
      />,
    );

    const table = screen.getByRole('table', { name: 'Registre des fichiers' });

    expect(table).toHaveClass('shared-table--framed');
    expect(table.parentElement).toHaveClass('shared-table__viewport');
    expect(within(table).getByRole('columnheader', { name: /nom/i })).toBeVisible();
    expect(within(table).getByText('zulu.pdf')).toBeVisible();
    expect(within(table).getByText('CLEAN')).toBeVisible();
  });

  it('sorts a column through an accessible control and shows an empty state', async () => {
    const user = userEvent.setup();

    render(
      <GenericTable
        caption="Registre triable"
        columns={columns}
        getRowKey={(row) => row.id}
        rows={rows}
      />,
    );

    const table = screen.getByRole('table', { name: 'Registre triable' });
    const sortButton = within(table).getByRole('button', { name: 'Trier par Nom' });

    expect(within(table).queryByRole('tooltip')).not.toBeInTheDocument();
    await user.click(sortButton);

    const sortedRows = within(table).getAllByRole('row').slice(1);
    expect(sortedRows[0]).toHaveTextContent('alpha.pdf');
    expect(sortedRows[1]).toHaveTextContent('zulu.pdf');
    expect(within(table).getByRole('columnheader', { name: /nom/i })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );

    render(
      <GenericTable
        caption="Registre vide"
        columns={columns}
        emptyMessage="Aucun fichier"
        getRowKey={(row) => row.id}
        rows={[]}
      />,
    );

    expect(screen.getByText('Aucun fichier')).toBeVisible();
  });

  it('renders a newly inserted row first while keeping existing rows below it', () => {
    const insertedRow: FileRow = {
      id: 'file-3',
      name: 'new-file.pdf',
      status: 'PENDING_SCAN',
    };

    render(
      <GenericTable
        animatedRowKey={insertedRow.id}
        caption="Registre avec insertion"
        columns={columns}
        getRowKey={(row) => row.id}
        rows={[insertedRow, ...rows]}
      />,
    );

    const table = screen.getByRole('table', { name: 'Registre avec insertion' });
    const renderedRows = within(table).getAllByRole('row').slice(1);

    expect(renderedRows[0]).toHaveTextContent('new-file.pdf');
    expect(renderedRows[1]).toHaveTextContent('zulu.pdf');
    expect(renderedRows[2]).toHaveTextContent('alpha.pdf');
  });

  it('paginates sorted rows and exposes page and element counts', async () => {
    const user = userEvent.setup();
    const paginatedRows: FileRow[] = [
      ...rows,
      { id: 'file-3', name: 'middle.pdf', status: 'SCANNING' },
    ];

    render(
      <GenericTable
        caption="Registre pagine"
        columns={columns}
        getRowKey={(row) => row.id}
        pagination={{ pageSize: 2, pageSizeOptions: [2, 3] }}
        rows={paginatedRows}
      />,
    );

    const table = screen.getByRole('table', { name: 'Registre pagine' });
    expect(within(table).getByText('zulu.pdf')).toBeVisible();
    expect(within(table).getByText('alpha.pdf')).toBeVisible();
    expect(within(table).queryByText('middle.pdf')).not.toBeInTheDocument();
    expect(screen.getByText('Page 1 sur 2')).toBeVisible();
    expect(screen.getByText('3 elements')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Page suivante' }));

    expect(screen.getByText('Page 2 sur 2')).toBeVisible();
    expect(within(table).getByText('middle.pdf')).toBeVisible();
    expect(within(table).queryByText('zulu.pdf')).not.toBeInTheDocument();

    await user.click(within(table).getByRole('button', { name: 'Trier par Nom' }));

    expect(screen.getByText('Page 1 sur 2')).toBeVisible();
    expect(within(table).getByText('alpha.pdf')).toBeVisible();

    await user.selectOptions(screen.getByRole('combobox', { name: 'Elements par page' }), '3');

    expect(screen.getByText('Page 1 sur 1')).toBeVisible();
    expect(within(table).getByText('middle.pdf')).toBeVisible();
  });
});