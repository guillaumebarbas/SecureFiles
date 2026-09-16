import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getUploadConfiguration, listFiles } from '../api/filesApi';
import { App } from '../App';

vi.mock('../api/filesApi', () => ({
  getUploadConfiguration: vi.fn(),
  listFiles: vi.fn(),
}));

const mockedGetUploadConfiguration = vi.mocked(getUploadConfiguration);
const mockedListFiles = vi.mocked(listFiles);

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
    mockedGetUploadConfiguration.mockResolvedValue({ maximumSizeBytes: 1024 });
    mockedListFiles.mockResolvedValue([]);
  });

  afterEach(() => {
    window.history.replaceState({}, '', '/');
  });

  it('renders the dashboard as the active default view', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('aria-current', 'page');
  });

  it('changes the active view and URL when a navigation item is selected', async () => {
    const user = userEvent.setup();

    render(<App />);

    expect(screen.getByRole('banner')).toBeVisible();
    expect(screen.getByRole('complementary', { name: 'Navigation SecureFiles' })).toBeVisible();
    await user.click(screen.getByRole('link', { name: 'Fichiers' }));

    expect(window.location.pathname).toBe('/files');
    expect(screen.getByRole('link', { name: 'Fichiers' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Fichiers' })).toBeVisible();
    expect(screen.getByRole('banner')).toBeVisible();
    expect(screen.getByRole('complementary', { name: 'Navigation SecureFiles' })).toBeVisible();
  });

  it('opens the shared components library on its dedicated route', async () => {
    const user = userEvent.setup();

    render(<App />);
    await user.click(screen.getByRole('link', { name: 'Bibliotheque' }));

    expect(window.location.pathname).toBe('/components');
    expect(screen.getByRole('link', { name: 'Bibliotheque' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Composants reutilisables' })).toBeVisible();
  });
});