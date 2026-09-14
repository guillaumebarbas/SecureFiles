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
- Ne proposer une action de download que pour un fichier `CLEAN`.
- Garder les styles web dans `frontend/src/styles.css` ; ne pas utiliser `StyleSheet`, reserve a React Native.
- Donner un tooltip au survol et au focus a chaque bouton ou element actionnable.
- Valider toute modification avec le test cible puis `npm run build` depuis `frontend/`.