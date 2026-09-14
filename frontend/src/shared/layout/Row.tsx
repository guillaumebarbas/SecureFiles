import type { CSSProperties, ReactNode } from 'react';

type RowProps = {
  align?: CSSProperties['alignItems'];
  as?: 'div' | 'span';
  children: ReactNode;
  className?: string;
  gap?: CSSProperties['gap'];
  justify?: CSSProperties['justifyContent'];
  wrap?: CSSProperties['flexWrap'];
};

export function Row({ align, as: Component = 'div', children, className, gap, justify, wrap = 'nowrap' }: RowProps) {
  const rowClassName = ['shared-row', className].filter(Boolean).join(' ');

  return (
    <Component
      className={rowClassName}
      style={{ alignItems: align, flexWrap: wrap, gap, justifyContent: justify }}
    >
      {children}
    </Component>
  );
}