import type { ReactNode } from 'react';
import { Row } from '../Row';
import { headerClassNames } from './style';

type HeaderProps = {
  children?: ReactNode;
  className?: string;
  title: string;
};

export function Header({ children, className, title }: HeaderProps) {
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
        <h1>{title}</h1>
        {children ? (
          <Row align="center" className={headerClassNames.content} gap="12px" wrap="wrap">
            {children}
          </Row>
        ) : null}
      </Row>
    </header>
  );
}