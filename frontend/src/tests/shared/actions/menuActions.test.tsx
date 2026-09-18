import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Download, Trash } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';
import { MenuActions, type MenuAction } from '../../../shared/actions/menuActions';

function createActions(onDelete: () => void | boolean | Promise<void | boolean>): MenuAction[] {
  return [
    {
      color: 'success',
      href: '/download/file-1',
      icon: Download,
      id: 'download',
      text: 'Télécharger',
    },
    {
      color: 'danger',
      icon: Trash,
      id: 'delete',
      onSelect: onDelete,
      text: 'Supprimer',
    },
  ];
}

describe('MenuActions', () => {
  it('renders its actions in order and keeps links as links', async () => {
    const user = userEvent.setup();

    const { container } = render(
      <MenuActions
        actions={createActions(vi.fn())}
        ariaLabel="Actions du fichier"
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Actions du fichier' }));

    expect(container.querySelector('.shared-menu-actions')).toHaveClass('shared-menu-actions--open');
    const menu = screen.getByRole('menu', { name: 'Actions du fichier' });
    expect(menu.parentElement).toBe(document.body);
    const menuItems = within(menu).getAllByRole('menuitem');

    expect(menuItems).toHaveLength(2);
    expect(menuItems[0]).toHaveTextContent('Télécharger');
    expect(menuItems[1]).toHaveTextContent('Supprimer');
    expect(menuItems[0].tagName).toBe('A');
    expect(menuItems[0]).toHaveAttribute('href', '/download/file-1');
    expect(menuItems[1].tagName).toBe('BUTTON');
    expect(menuItems[0]).toHaveFocus();
  });

  it('closes after a successful action and stays open when the action returns false', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn().mockResolvedValueOnce(false).mockResolvedValueOnce(true);

    render(
      <MenuActions
        actions={createActions(onDelete)}
        ariaLabel="Actions du fichier"
      />,
    );

    const trigger = screen.getByRole('button', { name: 'Actions du fichier' });
    await user.click(trigger);
    await user.click(screen.getByRole('menuitem', { name: 'Supprimer' }));

    expect(screen.getByRole('menu', { name: 'Actions du fichier' })).toBeInTheDocument();

    await user.click(screen.getByRole('menuitem', { name: 'Supprimer' }));

    await waitFor(() => {
      expect(screen.queryByRole('menu', { name: 'Actions du fichier' })).not.toBeInTheDocument();
    });
  });

  it('shows an empty message and closes with Escape or an outside click', async () => {
    const user = userEvent.setup();

    render(
      <MenuActions
        actions={[]}
        ariaLabel="Actions du fichier"
        emptyMessage="Aucune action disponible."
      />,
    );

    const trigger = screen.getByRole('button', { name: 'Actions du fichier' });
    await user.click(trigger);
    expect(screen.getByText('Aucune action disponible.')).toBeVisible();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('menu', { name: 'Actions du fichier' })).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();

    await user.click(trigger);
    await user.click(document.body);
    expect(screen.queryByRole('menu', { name: 'Actions du fichier' })).not.toBeInTheDocument();
  });
});