import { ShieldCheck } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { Column } from '../Column';
import { Row } from '../Row';
import { navHeaderClassNames } from './style';

type NavHeaderProps = {
  description: string;
  icon?: LucideIcon;
  title: string;
};

export function NavHeader({ description, icon: Icon = ShieldCheck, title }: NavHeaderProps) {
  return (
    <Row align="center" className={navHeaderClassNames.root} gap="12px">
      <div aria-hidden="true" className={navHeaderClassNames.icon}>
        <Icon size={22} />
      </div>
      <Column className={navHeaderClassNames.content} gap="2px">
        <h2>{title}</h2>
        <p>{description}</p>
      </Column>
    </Row>
  );
}