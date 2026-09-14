import { NavHeader } from '../NavHeader/NavHeader';
import { NavItem, type NavItemDefinition } from '../NavItem/NavItem';
import { BackendStatus } from '../BackendStatus/BackendStatus';
import { Column } from '../Column';
import type { BackendStatusValue } from '../BackendStatus/style';
import { sideNavBarClassNames } from './style';

type SideNavBarProps = {
  activePath: string;
  backendStatus?: BackendStatusValue;
  description?: string;
  items: readonly NavItemDefinition[];
  onNavigate?: (href: string) => void;
  title?: string;
  version: string;
};

export function SideNavBar({
  activePath,
  backendStatus = 'unknown',
  description = 'Console de depot securise',
  items,
  onNavigate,
  title = 'SecureFiles',
  version,
}: SideNavBarProps) {
  return (
    <aside aria-label="Navigation SecureFiles" className={sideNavBarClassNames.root}>
      <Column className={sideNavBarClassNames.content}>
        <NavHeader description={description} title={title} />

        <nav aria-label="Sections de la console" className={sideNavBarClassNames.navigation}>
          <ul className={sideNavBarClassNames.list}>
            {items.map((item) => (
              <li key={item.href}>
                <NavItem {...item} active={item.href === activePath} onNavigate={onNavigate} />
              </li>
            ))}
          </ul>
        </nav>

        <footer className={sideNavBarClassNames.footer}>
          <span className={sideNavBarClassNames.version}>Version {version}</span>
          <BackendStatus status={backendStatus} />
        </footer>
      </Column>
    </aside>
  );
}