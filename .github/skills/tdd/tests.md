# Bons et mauvais tests

Ces principes sont adaptes du guide TDD de [mattpocock/skills](https://github.com/mattpocock/skills), sous licence MIT.

## Bon test

- Observe un comportement qui interesse l'utilisateur ou l'appelant.
- Passe par une interface publique ou une seam convenue.
- Reste valide apres un refactoring interne.
- Decrit ce que le systeme fait, pas comment il le fait.
- Verifie une intention logique claire avec une valeur attendue independante.

Exemple de forme :

```typescript
test('upload should expose pending scan status when a file is accepted', async () => {
  const result = await uploadFile(validFile);

  expect(result.status).toBe('PENDING_SCAN');
});
```

## Mauvais test

- Verifie une methode privee ou le nom d'un collaborateur interne.
- Attend un nombre d'appels ou un ordre interne qui ne fait pas partie du contrat.
- Recalcule l'attendu avec le meme code que l'implementation.
- Interroge directement la base pour prouver un comportement qui devrait etre observable par un port ou un cas d'utilisation.
- Utilise un snapshot ou un mock de maniere a ne jamais pouvoir contredire l'implementation.