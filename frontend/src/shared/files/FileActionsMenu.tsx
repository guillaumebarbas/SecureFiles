import { useState } from 'react';
import { Download, Trash } from 'lucide-react';
import { getDownloadUrl, type FileMetadataResponse } from '../../api/filesApi';
import { MenuActions, type MenuAction } from '../actions/menuActions';
import { fileActionsMenuClassNames } from './style';

type FileActionsMenuProps = {
  file: FileMetadataResponse;
  isAuthenticated: boolean;
  onDelete: (fileId: string) => Promise<boolean>;
};

export function FileActionsMenu({ file, isAuthenticated, onDelete }: FileActionsMenuProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const actionLabel = `Actions pour ${file.originalFilename}`;
  const canDownload = isAuthenticated && file.status === 'CLEAN' && file.canDownload === true;
  const canDelete = isAuthenticated && file.canDelete === true;

  async function handleDelete(): Promise<boolean> {
    if (!canDelete || isDeleting) {
      return false;
    }

    setIsDeleting(true);
    try {
      return await onDelete(file.fileId);
    } finally {
      setIsDeleting(false);
    }
  }

  const actions: MenuAction[] = [];
  if (canDownload) {
    actions.push({
      color: 'success',
      href: getDownloadUrl(file.fileId),
      icon: Download,
      id: 'download',
      text: 'Télécharger',
    });
  }
  if (canDelete) {
    actions.push({
      color: 'danger',
      disabled: isDeleting,
      icon: Trash,
      id: 'delete',
      onSelect: handleDelete,
      text: isDeleting ? 'Suppression...' : 'Supprimer',
    });
  }

  return (
    <div className={fileActionsMenuClassNames.root}>
      <MenuActions actions={actions} ariaLabel={actionLabel} />
    </div>
  );
}