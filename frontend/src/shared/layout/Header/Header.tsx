import type { ReactNode } from 'react';
import { Row } from '../Row';
import { headerClassNames } from './style';

type HeaderProps = {
  children?: ReactNode;
  className?: string;
  eyebrow?: string;
  title: string;
};

export function Header({ children, className, eyebrow, title }: HeaderProps) {
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
        {children ? (
          <Row align="center" className={headerClassNames.content} gap="16px" wrap="wrap">
            {children}
          </Row>
        ) : null}
      </Row>
    </header>
  );
}