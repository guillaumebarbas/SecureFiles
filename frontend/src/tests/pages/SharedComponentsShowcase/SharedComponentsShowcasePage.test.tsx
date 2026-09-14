import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { SharedComponentsShowcasePage } from '../../../pages/SharedComponentsShowcase/SharedComponentsShowcasePage';

describe('SharedComponentsShowcasePage', () => {
  it('displays the submitted search value', async () => {
    const user = userEvent.setup();

    render(<SharedComponentsShowcasePage />);

    await user.type(screen.getByRole('searchbox', { name: /rechercher/i }), 'rapport.pdf');
    await user.click(screen.getByRole('button', { name: /entrer/i }));

    expect(screen.getByText('Recherche envoyee : rapport.pdf')).toBeVisible();
  });

  it('presents the reusable component examples', () => {
    render(<SharedComponentsShowcasePage />);

    expect(screen.getByRole('heading', { name: 'Actions' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Action principale' })).toBeVisible();

    const table = screen.getByRole('table', { name: 'Exemple de registre' });

    expect(within(table).getByText('CLEAN')).toBeVisible();
  });
});