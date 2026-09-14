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
});