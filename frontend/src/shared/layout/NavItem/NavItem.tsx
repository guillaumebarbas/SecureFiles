import type { MouseEvent } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Row } from '../Row';
import { navItemClassNames } from './style';

export type NavItemDefinition = {
  href: string;
  icon: LucideIcon;
  label: string;
};

type NavItemProps = NavItemDefinition & {
  active?: boolean;
  onNavigate?: (href: string) => void;
};

export function NavItem({ active = false, href, icon: Icon, label, onNavigate }: NavItemProps) {
  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    if (!onNavigate) {
      return;
    }

    event.preventDefault();
    onNavigate(href);
  }

  const className = [navItemClassNames.root, active ? navItemClassNames.active : null]
    .filter(Boolean)
    .join(' ');

  return (
    <a
      aria-current={active ? 'page' : undefined}
      className={className}
      href={href}
      onClick={handleClick}
    >
      <Row align="center" className={navItemClassNames.content} gap="10px">
        <Icon aria-hidden="true" size={18} />
        <span>{label}</span>
      </Row>
    </a>
  );
}