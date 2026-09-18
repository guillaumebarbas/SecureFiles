import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Download, Trash } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';
import { BoutonMenuActions } from '../../../shared/actions/boutonMenuActions';

describe('BoutonMenuActions', () => {
  it('renders a link with its text and semantic color', () => {
    render(
      <BoutonMenuActions
        color="success"
        href="/api/v1/files/file-1/content"
        icon={Download}
        text="Télécharger"
      />,
    );

    const action = screen.getByRole('menuitem', { name: 'Télécharger' });

    expect(action.tagName).toBe('A');
    expect(action).toHaveAttribute('href', '/api/v1/files/file-1/content');
    expect(action).toHaveClass('shared-button-menu-actions--success');
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('renders an icon-only button with a tooltip and invokes its callback', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();

    render(
      <BoutonMenuActions
        ariaLabel="Supprimer le fichier"
        color="danger"
        icon={Trash}
        onClick={onClick}
      />,
    );

    const action = screen.getByRole('menuitem', { name: 'Supprimer le fichier' });

    expect(action).toHaveClass('shared-button-menu-actions--danger');
    expect(screen.getByRole('tooltip')).toHaveTextContent('Supprimer le fichier');

    await user.click(action);

    expect(onClick).toHaveBeenCalledOnce();
  });
});