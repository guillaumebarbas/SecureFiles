import { FolderOpen } from 'lucide-react';
import { Section } from '../../shared/layout/Section/Section';
import { filesPageClassNames } from './style';

export function FilesPage() {
  return (
    <div className={filesPageClassNames.root}>
      <Section
        description="Un registre pret a recevoir les fichiers et leurs statuts de scan."
        icon={FolderOpen}
        title="Fichiers"
      >
        <div className={filesPageClassNames.emptyState}>
          <p className={filesPageClassNames.emptyTitle}>Le registre est pret.</p>
          <p>Les fichiers apparaitront ici apres leur depot.</p>
        </div>
      </Section>
    </div>
  );
}