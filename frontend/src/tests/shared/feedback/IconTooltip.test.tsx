import { fireEvent, render, screen } from '@testing-library/react';
import { FileText } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';
import { Icon } from '../../../shared/feedback/Icon';
import { Tooltip } from '../../../shared/feedback/Tooltip';

describe('Icon', () => {
  it('exposes a label for meaningful icons and keeps its size stable', () => {
    render(<Icon icon={FileText} label="Document" size={20} />);

    const icon = screen.getByLabelText('Document');

    expect(icon).toHaveAttribute('width', '20');
    expect(icon).toHaveAttribute('height', '20');
  });
});

describe('Tooltip', () => {
  it('associates its content with the action it describes', () => {
    render(
      <Tooltip content="Ouvrir le fichier">
        <button aria-label="Ouvrir" type="button">
          <FileText aria-hidden="true" size={18} />
        </button>
      </Tooltip>,
    );

    const action = screen.getByRole('button', { name: 'Ouvrir' });
    const tooltip = screen.getByRole('tooltip');

    expect(action).toHaveAttribute('aria-describedby', tooltip.id);
    expect(tooltip).toHaveTextContent('Ouvrir le fichier');
  });

  it('renders its content outside an overflow-constrained ancestor', () => {
    render(
      <div data-testid="table-viewport" style={{ overflow: 'auto' }}>
        <Tooltip content="Ouvrir le fichier">
          <button aria-label="Ouvrir" type="button">
            <FileText aria-hidden="true" size={18} />
          </button>
        </Tooltip>
      </div>,
    );

    const viewport = screen.getByTestId('table-viewport');
    const tooltip = screen.getByRole('tooltip');

    expect(viewport).not.toContainElement(tooltip);
  });

  it('keeps visible content inside the viewport near a horizontal edge', () => {
    const innerWidthDescriptor = Object.getOwnPropertyDescriptor(window, 'innerWidth');
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 320 });
    const boundsSpy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect')
      .mockImplementation(function getBounds(this: HTMLElement) {
        if (this.getAttribute('role') === 'tooltip') {
          return { bottom: 0, height: 30, left: 0, right: 200, top: 0, width: 200 } as DOMRect;
        }

        return { bottom: 220, height: 20, left: 280, right: 300, top: 200, width: 20 } as DOMRect;
      });

    try {
      render(
        <Tooltip content="Ouvrir le fichier">
          <button aria-label="Ouvrir" type="button">
            <FileText aria-hidden="true" size={18} />
          </button>
        </Tooltip>,
      );

      fireEvent.mouseEnter(screen.getByRole('button', { name: 'Ouvrir' }));

      expect(screen.getByRole('tooltip')).toHaveStyle({ left: '208px' });
    } finally {
      boundsSpy.mockRestore();
      if (innerWidthDescriptor) {
        Object.defineProperty(window, 'innerWidth', innerWidthDescriptor);
      }
    }
  });
});