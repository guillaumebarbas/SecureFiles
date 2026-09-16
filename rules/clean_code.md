# Clean Code

## Portee

Ces regles s'appliquent au backend Java, au frontend React/TypeScript et aux scripts du projet. Elles servent a garder des changements lisibles, testables et compatibles avec l'architecture SecureFiles.

## Regles principales

- Donner des noms explicites aux classes, fonctions, variables et tests. Eviter les abreviations ambigues et les variables d'une seule lettre.
- Garder chaque fonction courte et focalisee sur une responsabilite. Extraire un bloc uniquement lorsqu'il represente une intention nommable et reutilisable.
- Structurer les cas d'utilisation par etapes nommees et lisibles : validation, creation, transfert, transition metier, publication et resultat. Une methode publique de use case ne doit pas contenir tout le workflow detaille.
- Ne pas imbriquer de classes dans le code de production backend. Chaque type de production doit etre une classe top-level dans son propre fichier, meme lorsqu'il s'agit d'un detail technique du domaine.
- Dans les tests du domaine, appeler une classe et une methode de production. Ne pas creer de fake, stub, spy, implementation anonyme de port ou classe auxiliaire dediee aux tests. `@Mock` est autorise uniquement pour un collaborateur de production du systeme teste.
- Ne pas tester les couches `application` et `infrastructure` comme des couches ; tester uniquement les mappers purs de conversion lorsqu'ils existent.
- Preferer des objets immuables et des fonctions sans effet de bord lorsque cela simplifie le raisonnement.
- Respecter les frontieres de l'architecture : le domaine reste independant de Spring, JPA, HTTP et ClamAV ; les dependances entrent par des ports.
- Valider les donnees a la frontiere HTTP et conserver les invariants metier dans le domaine ou le cas d'utilisation qui les controle.
- Rendre les erreurs explicites. Ne jamais masquer une exception, transformer une panne antivirus en succes, ou utiliser une valeur par defaut qui affaiblit la securite.
- Eviter la duplication de logique metier, mais ne pas creer d'abstraction speculative pour supprimer quelques lignes seulement.
- Ne pas charger integralement un contenu de fichier en memoire lorsque le flux suffit.
- Ne jamais journaliser de secret, token, contenu de fichier ou donnee sensible. Les logs doivent aider au diagnostic sans exposer les donnees.
- Utiliser les commentaires pour expliquer une contrainte, une decision ou un protocole non evident ; ne pas commenter une instruction triviale.
- Preferer une modification locale et coherentement testee a une refactorisation generale sans rapport avec la demande.
- Conserver les API publiques et les contrats HTTP existants, sauf si leur evolution est necessaire et documentee.

## Definition de fini

Avant de considerer un changement termine :

1. Le code se lit depuis les noms et la structure, sans commentaire narratif superflu.
2. Les erreurs et les cas limites sont traites au bon niveau.
3. Les tests couvrent le comportement ajoute ou modifie.
4. Les validations ciblees passent et aucun secret n'a ete ajoute aux sources, logs ou exemples de configuration.