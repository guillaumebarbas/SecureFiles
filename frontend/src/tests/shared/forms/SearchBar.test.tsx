import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SearchBar } from '../../../shared/forms/SearchBar';

describe('SearchBar', () => {
  it('does not add a tooltip to its text submit action', () => {
    render(<SearchBar onSubmit={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Entrer' })).toBeVisible();
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });
});