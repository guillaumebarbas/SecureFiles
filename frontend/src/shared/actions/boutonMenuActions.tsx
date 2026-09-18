import type { LucideIcon } from 'lucide-react';
import { Button } from './Button';
import { Tooltip } from '../feedback/Tooltip';
import { Row } from '../layout/Row';
import { boutonMenuActionsClassNames } from './style';

export type MenuActionColor = 'danger' | 'neutral' | 'success';

export type BoutonMenuActionsProps = {
  ariaLabel?: string;
  color: MenuActionColor;
  disabled?: boolean;
  href?: string;
  icon: LucideIcon;
  onClick?: () => void;
  text?: string;
};

export function BoutonMenuActions({
  ariaLabel,
  color,
  disabled = false,
  href,
  icon: Icon,
  onClick,
  text,
}: BoutonMenuActionsProps) {
  const className = [
    boutonMenuActionsClassNames.root,
    `${boutonMenuActionsClassNames.root}--${color}`,
  ].join(' ');
  const accessibleLabel = ariaLabel ?? text ?? 'Action';

  if (href !== undefined) {
    const link = (
      <a
        aria-disabled={disabled || undefined}
        aria-label={ariaLabel}
        className={className}
        href={disabled ? undefined : href}
        onClick={disabled ? undefined : onClick}
        role="menuitem"
      >
        <Row align="center" as="span" gap="8px">
          <Icon aria-hidden="true" size={17} />
          {text ? <span>{text}</span> : null}
        </Row>
      </a>
    );

    return text ? link : <Tooltip content={accessibleLabel}>{link}</Tooltip>;
  }

  return (
    <Button
      aria-label={ariaLabel}
      aria-disabled={disabled || undefined}
      className={className}
      disabled={disabled}
      icon={Icon}
      onClick={onClick}
      role="menuitem"
      variant="secondary"
    >
      {text}
    </Button>
  );
}