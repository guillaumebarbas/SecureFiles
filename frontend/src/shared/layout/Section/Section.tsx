import { useId, type ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { Row } from '../Row';
import { sectionClassNames } from './style';

type SectionProps = {
  children: ReactNode;
  className?: string;
  description: string;
  icon: LucideIcon;
  title: string;
};

export function Section({ children, className, description, icon: Icon, title }: SectionProps) {
  const headingId = `shared-section-heading-${useId().replace(/:/g, '')}`;
  const sectionClassName = [sectionClassNames.root, className].filter(Boolean).join(' ');

  return (
    <section aria-labelledby={headingId} className={sectionClassName}>
      <Row
        align="flex-start"
        className={sectionClassNames.header}
        justify="space-between"
        wrap="wrap"
      >
        <Row align="center" className={sectionClassNames.title} gap="9px">
          <Icon aria-hidden="true" size={18} />
          <h2 id={headingId}>{title}</h2>
        </Row>
        <p className={sectionClassNames.description}>{description}</p>
      </Row>
      <div className={sectionClassNames.content}>{children}</div>
    </section>
  );
}