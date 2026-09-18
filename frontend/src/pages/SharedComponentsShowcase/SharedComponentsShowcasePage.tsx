import { useState } from 'react';
import {
  CircleUserRound,
  Clock3,
  CloudUpload,
  Download,
  Info,
  Search,
  ShieldCheck,
  Upload,
} from 'lucide-react';
import type { FileMetadataResponse } from '../../api/filesApi';
import { Button } from '../../shared/actions/Button';
import { GenericTable, type TableColumn } from '../../shared/data/GenericTable';
import { FileActionsMenu } from '../../shared/files/FileActionsMenu';
import { Icon } from '../../shared/feedback/Icon';
import { Tag } from '../../shared/feedback/Tag';
import { Tooltip } from '../../shared/feedback/Tooltip';
import { FileUpload } from '../../shared/forms/FileUpload/FileUpload';
import { SearchBar } from '../../shared/forms/SearchBar';
import { Column } from '../../shared/layout/Column';
import { Login, type LoginUser } from '../../shared/layout/Login/Login';
import { Row } from '../../shared/layout/Row';
import { Section } from '../../shared/layout/Section/Section';
import { showcaseClassNames } from './style';

type ShowcaseFile = {
  id: string;
  name: string;
  size: string;
  status: 'CLEAN' | 'PENDING_SCAN';
};

const showcaseFiles: ShowcaseFile[] = [
  { id: 'showcase-1', name: 'rapport-annuel.pdf', size: '2.4 MB', status: 'CLEAN' },
  { id: 'showcase-2', name: 'archive-a-verifier.zip', size: '18.7 MB', status: 'PENDING_SCAN' },
];

const showcaseActionFile: FileMetadataResponse = {
  canDelete: true,
  canDownload: true,
  createdAt: '2026-09-15T10:00:00Z',
  fileId: 'showcase-file-actions',
  originalFilename: 'rapport-annuel.pdf',
  sizeBytes: 2400000,
  status: 'CLEAN',
};

const showcaseColumns: TableColumn<ShowcaseFile>[] = [
  { header: 'Nom', key: 'name', sortable: true },
  {
    header: 'Statut',
    key: 'status',
    render: (file) => (
      <Tag
        icon={file.status === 'CLEAN' ? ShieldCheck : Clock3}
        text={file.status}
        tone={file.status === 'CLEAN' ? 'success' : 'warning'}
      />
    ),
  },
  { header: 'Taille', key: 'size' },
];

export function SharedComponentsShowcasePage() {
  const [loggedInUser, setLoggedInUser] = useState<LoginUser>();
  const [submittedSearch, setSubmittedSearch] = useState('');
  const demoUser: LoginUser = { name: 'Utilisateur de démonstration' };

  return (
    <div className={showcaseClassNames.root}>
      <Section
        description="Un champ compact pour retrouver un fichier par son nom."
        icon={Search}
        title="Barre de recherche"
      >
        <SearchBar onSubmit={setSubmittedSearch} />
        <p aria-live="polite" className="search-result">
          {submittedSearch ? `Recherche envoyée : ${submittedSearch}` : 'Aucune recherche envoyée.'}
        </p>
      </Section>

      <Section
        description="Trois niveaux d&apos;emphase pour les actions de la console."
        icon={Upload}
        title="Actions"
      >
        <Row align="center" gap="12px" wrap="wrap">
          <Button icon={Upload} variant="primary">
            Action principale
          </Button>
          <Button icon={Download} variant="secondary">
            Action secondaire
          </Button>
          <Button variant="gradient">Action accentuée</Button>
        </Row>
      </Section>

      <Section
        description="Un menu contextualisé pour télécharger ou supprimer un fichier autorisé."
        icon={Download}
        title="Actions d'un fichier"
      >
        <FileActionsMenu
          file={showcaseActionFile}
          isAuthenticated
          onDelete={async () => true}
        />
      </Section>

      <Section
        description="Un exemple local des états de connexion du header."
        icon={CircleUserRound}
        title="Connexion"
      >
        <Login
          onLogin={() => setLoggedInUser(demoUser)}
          onLogout={() => setLoggedInUser(undefined)}
            onRegister={() => setLoggedInUser(demoUser)}
          user={loggedInUser}
        />
      </Section>

      <Section
        description="Une zone partagée pour sélectionner un fichier et suivre son traitement."
        icon={CloudUpload}
        title="Upload de fichier"
      >
        <FileUpload />
      </Section>

      <Section
        description="Les statuts combinent toujours un libellé, une icône et un contraste lisible."
        icon={ShieldCheck}
        title="Tags et aide contextuelle"
      >
        <Row align="center" gap="10px" wrap="wrap">
          <Tag icon={ShieldCheck} text="CLEAN" tone="success" />
          <Tag icon={Clock3} text="PENDING_SCAN" tone="warning" />
          <Tooltip content="Voir les détails du composant">
            <button aria-label="Voir les détails" className="showcase-icon-button" type="button">
              <Icon icon={Info} size={18} />
            </button>
          </Tooltip>
        </Row>
      </Section>

      <Section
        description="Des primitives simples pour aligner les contenus sans logique de presentation cachee."
        icon={Info}
        title="Row et Column"
      >
        <Row className="layout-example" gap="16px" wrap="wrap">
          <Column className="layout-example__column" gap="4px">
            <strong>Dépôt</strong>
            <span>Zone d&apos;action verticale</span>
          </Column>
          <Column className="layout-example__column" gap="4px">
            <strong>Registre</strong>
            <span>Zone de lecture verticale</span>
          </Column>
        </Row>
      </Section>

      <Section
        className="showcase-section--table"
        description="Un registre lisible, triable et prêt à accueillir les statuts de scan."
        icon={Download}
        title="GenericTable"
      >
        <GenericTable
          caption="Exemple de registre"
          columns={showcaseColumns}
          getRowKey={(file) => file.id}
          rows={showcaseFiles}
        />
      </Section>
    </div>
  );
}