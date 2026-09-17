import { FolderOpen } from 'lucide-react';
import { Section } from '../../shared/layout/Section/Section';
import { filesPageClassNames } from './style';

export function FilesPage() {
  return (
    <div className={filesPageClassNames.root}>
      <Section
        description="Un registre prêt à recevoir les fichiers et leurs statuts de scan."
        icon={FolderOpen}
        title="Fichiers"
      >
        <div className={filesPageClassNames.emptyState}>
          <p className={filesPageClassNames.emptyTitle}>Le registre est prêt.</p>
          <p>Les fichiers apparaîtront ici après leur dépôt.</p>
        </div>
      </Section>
    </div>
  );
}