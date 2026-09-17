---
name: "SecureFiles Frontend"
description: "Use when creating or modifying the SecureFiles React, TypeScript, Vite, CSS, upload, file list, scan status, or download user interface."
applyTo: "frontend/**/*.{ts,tsx,css,html}"
---

# Instructions frontend SecureFiles

- Charger [TDD](../skills/tdd/SKILL.md) avant toute implementation et appliquer `red-green-refactor`.
- Charger [Frontend UX/UI](../skills/securefiles-frontend/SKILL.md) pour les comportements de la console.
- Charger [Regles de design visuel](../../rules/ux_ui.md) pour la palette, les surfaces, les animations, les tooltips et le responsive visuel.
- Utiliser `frontend/src/api/filesApi.ts` pour les appels HTTP et Axios, jamais `fetch` directement dans les composants.
- Pour toute icone, toujours utiliser le MCP Lucide configure dans `.vscode/mcp.json` pour rechercher et choisir l'icone, puis importer le composant correspondant depuis `lucide-react`.
- Ne pas dessiner manuellement une icone SVG lorsqu'une icone Lucide adaptee existe ; justifier toute icone personnalisee necessaire.
- Garder les etats de scan visibles et explicites.
- Rediger tous les textes visibles en francais correct, avec les accents, l'orthographe et les accords grammaticaux necessaires ; ne jamais normaliser les codes techniques, les statuts API ou les identifiants.
- Ne proposer une action de download que pour un fichier `CLEAN`.
- Garder les styles web dans `frontend/src/styles.css` ; ne pas utiliser `StyleSheet`, reserve a React Native.
- Placer tous les fichiers de test frontend sous `frontend/src/tests`, avec une arborescence miroir de `frontend/src` et des imports relatifs adaptes.
- Donner a chaque composant et chaque page un fichier `style.ts` voisin pour ses styles propres ; conserver les styles globaux dans `frontend/src/styles.css`.
- Limiter chaque composant a 500 lignes maximum ; extraire les sous-composants lorsqu'une responsabilite devient distincte.
- Extraire les constantes et callbacks reutilises ou volumineux au lieu de surcharger le JSX de la vue.
- Garder les retours JSX lisibles, avec une composition de page qui orchestre les composants sans contenir toute leur logique.
- Organiser l'arborescence par parcours utilisateur : placer les primitives reutilisables dans `frontend/src/shared` et la composition specifique dans un dossier de fonctionnalite ou de page.
- Donner un tooltip au survol et au focus uniquement aux controles sans texte visible, notamment les boutons et icones icon-only ; les controles qui affichent deja un libelle ne doivent pas ajouter de tooltip.
- Utiliser `Row` et `Column` pour aligner les elements dans les pages et composants ; ne pas ajouter `display: flex` directement dans une composition, sauf dans l'implementation d'une primitive de layout ou d'un composant dont l'alignement est sa responsabilite propre.
- Valider toute modification avec le test cible puis `npm run build` depuis `frontend/`.