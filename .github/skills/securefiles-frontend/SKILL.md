---
name: securefiles-frontend
description: Use when changing the React/Vite console, file upload workflow, file list, scan statuses, responsive layout, or accessibility in SecureFiles.
---

# Frontend SecureFiles

## A charger

- [Regles UX/UI](../../../rules/ux_ui.md)
- [Clean Code](../../../rules/clean_code.md)
- [Securite du flux](../securefiles-security/SKILL.md) pour les statuts, l'upload et le download.
- [Contrat HTTP](../../../README.md)

## Regles d'implementation

- Centraliser tous les appels HTTP et Axios dans `frontend/src/api/filesApi.ts` ; ne pas utiliser `fetch` directement dans les composants.
- Pour toute icone, toujours utiliser le MCP Lucide configure dans `.vscode/mcp.json` pour rechercher et choisir l'icone, puis utiliser le composant correspondant de `lucide-react`.
- Ne pas dessiner manuellement une icone SVG lorsqu'une icone Lucide adaptee existe ; justifier toute exception.
- Garder les statuts `PENDING_SCAN`, `SCANNING`, `CLEAN`, `INFECTED` et `SCAN_FAILED` visibles avec texte et icone.
- N'afficher une action de telechargement active que lorsque `getDownloadUrl` retourne une URL pour un fichier `CLEAN`.
- Gerer explicitement chargement, liste vide, upload en cours, progression, erreur reseau et rafraichissement.
- Utiliser des controles semantiques, des labels accessibles, un focus visible et des noms pour les boutons d'icone.
- Preserver la grille de la console de reference : synthese rapide, depot evident, registre lisible et etat du service antivirus.
- Stabiliser les dimensions des lignes, badges et boutons pour eviter les sauts pendant un changement de statut.
- Tester desktop et mobile, les noms longs, les listes volumineuses et les erreurs sans chevauchement.

## Validation

- Executer `npm run build` depuis `frontend/` apres une modification React ou CSS.
- Pour une modification visuelle importante, verifier le rendu dans le navigateur a une largeur desktop et une largeur mobile.