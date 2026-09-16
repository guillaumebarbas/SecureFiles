import { CircleUserRound } from 'lucide-react';
import { Column } from '../../shared/layout/Column';
import { Section } from '../../shared/layout/Section/Section';
import { profileClassNames } from './style';

export function ProfilePage() {
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
              <dd>Utilisateur local</dd>
            </Column>
            <Column gap="4px">
              <dt>Rôle</dt>
              <dd>utilisateur</dd>
            </Column>
          </Column>
        </dl>
      </Section>
    </div>
  );
}
