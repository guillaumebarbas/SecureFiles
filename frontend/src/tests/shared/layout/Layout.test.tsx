import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Column } from '../../../shared/layout/Column';
import { Row } from '../../../shared/layout/Row';

describe('Row and Column', () => {
  it('renders children in a customizable row', () => {
    render(
      <Row align="center" gap="16px" justify="space-between">
        <span>Premier element</span>
        <span>Deuxieme element</span>
      </Row>,
    );

    const row = screen.getByText('Premier element').parentElement;

    expect(row).toHaveStyle({
      alignItems: 'center',
      gap: '16px',
      justifyContent: 'space-between',
    });
    expect(screen.getByText('Deuxieme element')).toBeVisible();
  });

  it('renders children in a customizable column', () => {
    render(
      <Column align="stretch" gap="8px">
        <span>Premier panneau</span>
        <span>Deuxieme panneau</span>
      </Column>,
    );

    const column = screen.getByText('Premier panneau').parentElement;

    expect(column).toHaveStyle({ alignItems: 'stretch', gap: '8px' });
    expect(screen.getByText('Deuxieme panneau')).toBeVisible();
  });
});