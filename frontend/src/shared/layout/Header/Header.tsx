import type { ReactNode } from 'react';
import { Row } from '../Row';
import { headerClassNames } from './style';

type HeaderProps = {
  children?: ReactNode;
  className?: string;
  description?: string;
  eyebrow?: string;
  title: string;
};

export function Header({ children, className, description, eyebrow, title }: HeaderProps) {
  const headerClassName = [headerClassNames.root, className].filter(Boolean).join(' ');

  return (
    <header className={headerClassName}>
      <Row
        align="center"
        className={headerClassNames.row}
        gap="16px"
        justify="space-between"
        wrap="wrap"
      >
        <Row align="center" className={headerClassNames.heading} gap="14px" wrap="wrap">
          <h1>{title}</h1>
          {eyebrow ? <span className={headerClassNames.eyebrow}>{eyebrow}</span> : null}
        </Row>
        {description || children ? (
          <Row align="center" className={headerClassNames.content} gap="16px" wrap="wrap">
            {description ? <p className={headerClassNames.description}>{description}</p> : null}
            {children}
          </Row>
        ) : null}
      </Row>
    </header>
  );
}