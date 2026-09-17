import { render, screen } from '@testing-library/react';
import { FolderOpen, Home, LayoutGrid } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { SideNavBar } from '../../../shared/layout/SideNavBar/SideNavBar';

describe('SideNavBar', () => {
  it('renders the identity, navigation items, version and backend status', () => {
    const { container } = render(
      <SideNavBar
        activePath="/"
        backendStatus="unknown"
        items={[
          { href: '/', icon: Home, label: 'Dashboard' },
          { href: '/components', icon: LayoutGrid, label: 'Bibliothèque' },
          { href: '/files', icon: FolderOpen, label: 'Fichiers' },
        ]}
        version="0.1.0"
      />,
    );

    expect(screen.getByRole('complementary', { name: 'Navigation SecureFiles' })).toBeVisible();
    expect(container.querySelector('aside > .shared-column.app-sidebar__content')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'SecureFiles', level: 2 })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Bibliothèque' })).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('link', { name: 'Fichiers' })).not.toHaveAttribute('aria-current');
    expect(screen.getByText('Version 0.1.0')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Vérification du service...' })).toBeVisible();
  });
});