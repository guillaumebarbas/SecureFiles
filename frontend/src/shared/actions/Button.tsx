import { isValidElement, type ButtonHTMLAttributes, type ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Tooltip } from '../feedback/Tooltip';

type ButtonVariant = 'primary' | 'secondary' | 'gradient';

type ButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  children?: ReactNode;
  icon?: LucideIcon;
  tooltip?: string;
  variant?: ButtonVariant;
};

function containsVisibleText(node: ReactNode): boolean {
  if (typeof node === 'string') {
    return node.trim().length > 0;
  }

  if (typeof node === 'number') {
    return true;
  }

  if (Array.isArray(node)) {
    return node.some(containsVisibleText);
  }

  if (isValidElement<{ children?: ReactNode }>(node)) {
    return containsVisibleText(node.props.children);
  }

  return false;
}

export function Button({
  'aria-label': ariaLabel,
  children,
  className,
  icon: Icon,
  title,
  tooltip,
  type = 'button',
  variant = 'primary',
  ...buttonProps
}: ButtonProps) {
  const tooltipLabel = tooltip ?? title ?? ariaLabel ?? (typeof children === 'string' ? children : undefined);
  const buttonClassName = ['shared-button', `shared-button--${variant}`, className]
    .filter(Boolean)
    .join(' ');
  const tooltipContent = tooltipLabel ?? 'Action';
  const buttonHasText = containsVisibleText(children);
  const button = (
    <button
      {...buttonProps}
      aria-label={ariaLabel}
      className={buttonClassName}
      title={buttonHasText ? undefined : tooltipLabel}
      type={type}
    >
      {Icon ? <Icon aria-hidden={Boolean(children)} size={18} /> : null}
      {children}
    </button>
  );

  return buttonHasText ? button : <Tooltip content={tooltipContent}>{button}</Tooltip>;
}