import type { CSSProperties, ReactNode } from 'react';

type ColumnProps = {
  align?: CSSProperties['alignItems'];
  children: ReactNode;
  className?: string;
  gap?: CSSProperties['gap'];
  justify?: CSSProperties['justifyContent'];
};

export function Column({ align, children, className, gap, justify }: ColumnProps) {
  const columnClassName = ['shared-column', className].filter(Boolean).join(' ');

  return (
    <div className={columnClassName} style={{ alignItems: align, gap, justifyContent: justify }}>
      {children}
    </div>
  );
}