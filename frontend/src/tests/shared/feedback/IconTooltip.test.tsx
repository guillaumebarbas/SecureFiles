import { render, screen } from '@testing-library/react';
import { FileText } from 'lucide-react';
import { describe, expect, it } from 'vitest';
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
});