import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { App } from '../App';

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
  });

  afterEach(() => {
    window.history.replaceState({}, '', '/');
  });

  it('renders the shared components showcase as the active view', () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Composants reutilisables' })).toBeVisible();
  });

  it('changes the active view and URL when a navigation item is selected', async () => {
    const user = userEvent.setup();

    render(<App />);
    await user.click(screen.getByRole('link', { name: 'Fichiers' }));

    expect(window.location.pathname).toBe('/files');
    expect(screen.getByRole('link', { name: 'Fichiers' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { name: 'Fichiers' })).toBeVisible();
  });
});