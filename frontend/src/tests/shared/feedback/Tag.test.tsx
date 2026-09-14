import { render, screen } from '@testing-library/react';
import { ShieldCheck } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { Tag } from '../../../shared/feedback/Tag';

describe('Tag', () => {
  it('renders its required text, icon, semantic tone, and background color', () => {
    render(
      <Tag
        backgroundColor="#e7f7ef"
        icon={ShieldCheck}
        text="Fichier sain"
        textColor="#16a36b"
        tone="success"
      />,
    );

    const tag = screen.getByText('Fichier sain');

    expect(tag).toBeVisible();
    expect(tag).toHaveClass('shared-tag--success');
    expect(tag).toHaveStyle({
      backgroundColor: '#e7f7ef',
      color: '#16a36b',
    });
  });

  it('supports a warning tone for waiting states', () => {
    render(<Tag text="Analyse en attente" tone="warning" />);

    expect(screen.getByText('Analyse en attente')).toHaveClass('shared-tag--warning');
  });
});