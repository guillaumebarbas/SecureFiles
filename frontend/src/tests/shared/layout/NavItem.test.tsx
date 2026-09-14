import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FolderOpen } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';
import { NavItem } from '../../../shared/layout/NavItem/NavItem';

describe('NavItem', () => {
  it('navigates through its callback and exposes the active state', async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();

    render(
      <NavItem
        active
        href="/files"
        icon={FolderOpen}
        label="Fichiers"
        onNavigate={onNavigate}
      />,
    );

    const link = screen.getByRole('link', { name: 'Fichiers' });

    expect(link).toHaveAttribute('aria-current', 'page');
    expect(link).toHaveAttribute('href', '/files');
    expect(link.querySelector('.shared-row')).toBeTruthy();
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();

    await user.click(link);

    expect(onNavigate).toHaveBeenCalledWith('/files');
  });
});