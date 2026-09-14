import { render, screen } from '@testing-library/react';
import { Search } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { Section } from '../../../shared/layout/Section/Section';

describe('Section', () => {
  it('renders an icon title row, right-side description and content', () => {
    const { container } = render(
      <Section
        description="Retrouver un fichier par son nom."
        icon={Search}
        title="Barre de recherche"
      >
        <p>Contenu de la section</p>
      </Section>,
    );

    expect(screen.getByRole('region', { name: 'Barre de recherche' })).toBeVisible();
    expect(screen.getByText('Retrouver un fichier par son nom.')).toBeVisible();
    expect(screen.getByText('Contenu de la section')).toBeVisible();
    expect(container.querySelector('.shared-row.shared-section__header')).toBeTruthy();
    expect(container.querySelector('.shared-row.shared-section__title')).toBeTruthy();
  });
});