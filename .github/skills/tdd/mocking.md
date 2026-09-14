# Quand utiliser un mock

Ces principes sont adaptes du guide TDD de [mattpocock/skills](https://github.com/mattpocock/skills), sous licence MIT.

## Limites systeme

Mocker uniquement les limites qui doivent etre isolees :

- API externe ;
- base de donnees dans un test unitaire, quand un test d'integration n'est pas le meilleur choix ;
- horloge ou aleatoire ;
- systeme de fichiers ou service ClamAV dans les tests d'application.

Ne pas mocker les classes internes ou les modules que le test controle lui-meme. Preferer un vrai test d'integration lorsque le mapping, le protocole ou la persistance est le risque teste.

## Conception testable

- Injecter les dependances externes au lieu de les creer dans la methode testee.
- Preferer des interfaces de ports explicites a un client generique qui impose une logique conditionnelle dans chaque mock.
- Donner a chaque operation externe un contrat et une forme de retour claire.
- Utiliser un fake simple lorsque son comportement est plus lisible qu'un mock configure avec de nombreux effets.