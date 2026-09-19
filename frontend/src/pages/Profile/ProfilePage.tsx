import { useEffect, useState } from 'react';
import { CircleUserRound, HardDrive, RotateCcw } from 'lucide-react';
import { getStorageQuota, type StorageQuotaResponse, type UserProfile } from '../../api/filesApi';
import { Button } from '../../shared/actions/Button';
import { BarreProgression } from '../../shared/feedback/BarreProgression/BarreProgression';
import { Column } from '../../shared/layout/Column';
import { Section } from '../../shared/layout/Section/Section';
import { profileClassNames } from './style';

type ProfilePageProps = {
  user?: UserProfile;
};

type StorageState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'ready'; quota: StorageQuotaResponse }
  | { kind: 'error'; message: string };

export function ProfilePage({ user }: ProfilePageProps) {
  const [storageState, setStorageState] = useState<StorageState>({ kind: 'idle' });
  const [storageRequestKey, setStorageRequestKey] = useState(0);

  useEffect(() => {
    if (!user) {
      setStorageState({ kind: 'idle' });
      return undefined;
    }

    const controller = new AbortController();
    setStorageState({ kind: 'loading' });

    getStorageQuota({ signal: controller.signal })
      .then((quota) => {
        if (!controller.signal.aborted) {
          setStorageState({ kind: 'ready', quota });
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setStorageState({
            kind: 'error',
            message: error instanceof Error
              ? error.message
              : 'La capacité de stockage ne peut pas être lue.',
          });
        }
      });

    return () => controller.abort();
  }, [storageRequestKey, user?.userId]);

  const availableStorage = storageState.kind === 'ready'
    ? Math.max(0, storageState.quota.quotaBytes - storageState.quota.usedBytes)
    : 0;

  return (
    <div className={profileClassNames.root}>
      <Section
        description="Les informations principales du compte actuellement utilise dans la console."
        icon={CircleUserRound}
        title="Informations du profil"
      >
        <dl className={profileClassNames.name}>
          <Column gap="16px">
            <Column gap="4px">
              <dt>Nom</dt>
              <dd>{user?.name ?? 'Aucun utilisateur connecté'}</dd>
            </Column>
            <Column gap="4px">
              <dt>Rôle</dt>
              <dd>
                {user?.roles.length ? (
                  <Column gap="4px">
                    {user.roles.map((role) => <span key={role}>{role}</span>)}
                  </Column>
                ) : 'Aucun rôle disponible'}
              </dd>
            </Column>
          </Column>
        </dl>
      </Section>
      <Section
        description="L'espace restant sur la limite de stockage associée à votre compte."
        icon={HardDrive}
        title="Stockage disponible"
      >
        {storageState.kind === 'loading' ? (
          <p role="status">Chargement du stockage disponible…</p>
        ) : null}
        {storageState.kind === 'error' ? (
          <Column gap="12px">
            <p role="alert">{storageState.message}</p>
            <Button icon={RotateCcw} onClick={() => setStorageRequestKey((key) => key + 1)}>
              Réessayer
            </Button>
          </Column>
        ) : null}
        {storageState.kind === 'idle' ? (
          <p>Connectez-vous pour consulter votre stockage disponible.</p>
        ) : null}
        {storageState.kind === 'ready' ? (
          <BarreProgression
            currentValue={availableStorage}
            gradient="linear-gradient(90deg, #3967F6 0%, #6F8FFF 100%)"
            maxValue={storageState.quota.quotaBytes}
            title="Stockage disponible"
            valueFormatter={formatStorageBytes}
          />
        ) : null}
      </Section>
    </div>
  );
}

function formatStorageBytes(value: number): string {
  const units = [
    { label: 'Go', size: 1024 ** 3 },
    { label: 'Mo', size: 1024 ** 2 },
    { label: 'Ko', size: 1024 },
  ];
  const unit = units.find((candidate) => value >= candidate.size);
  if (!unit) {
    return `${Math.round(value)} octets`;
  }
  return `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(value / unit.size)} ${unit.label}`;
}
