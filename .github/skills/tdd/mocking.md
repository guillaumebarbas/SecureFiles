# Quand utiliser un mock

Ces principes sont adaptes du guide TDD de [mattpocock/skills](https://github.com/mattpocock/skills), sous licence MIT.

## Limites systeme

Utiliser `@Mock` uniquement pour une classe, interface ou port de production dont depend
le systeme de production reellement teste :

- API externe ;
- base de donnees dans un test unitaire, quand un test d'integration n'est pas le meilleur choix ;
- horloge ou aleatoire ;
- systeme de fichiers ou service ClamAV dans les tests du domaine lorsqu'un port sortant
	de production le justifie ; cela ne cree pas de test pour la couche `application` ou
	`infrastructure`.

Ne pas mocker le systeme teste, les classes internes ou les modules que le test controle
lui-meme. Le test doit appeler une methode publique du systeme de production reel.
Les mappers purs sont testes par leur API publique. Les couches `application` et
`infrastructure` ne font pas l'objet de tests de couche.

## Conception testable

- Injecter les dependances externes au lieu de les creer dans la methode testee.
- Preferer des interfaces de ports explicites a un client generique qui impose une logique conditionnelle dans chaque mock.
- Donner a chaque operation externe un contrat et une forme de retour claire.
- Ne pas creer de fake, stub, spy, implementation anonyme de port ou classe auxiliaire dediee aux tests.