import { CircleUserRound } from 'lucide-react';
import type { UserProfile } from '../../api/filesApi';
import { Column } from '../../shared/layout/Column';
import { Section } from '../../shared/layout/Section/Section';
import { profileClassNames } from './style';

type ProfilePageProps = {
  user?: UserProfile;
};

export function ProfilePage({ user }: ProfilePageProps) {
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
    </div>
  );
}
