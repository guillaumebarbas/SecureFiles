---
name: plan-implement
description: "Use the SecureFiles analysis, approval, implementation, validation, and prompt journal workflow."
argument-hint: "Describe the feature, fix, or refactor to analyze and implement after approval"
agent: "SecureFilesAgent"
tools: [read, search, agent, edit, execute, todo]
---

# Plan Implement

`/plan-implement` est le nom court du workflow detaille [plan-approve-implement.prompt.md](plan-approve-implement.prompt.md).

Appliquer integralement ce prompt de reference, notamment :

- analyser le code et formuler une hypothese falsifiable avant toute modification ;
- produire, relire et faire approuver explicitement le `PLAN FINAL` ;
- ne commencer l'implementation qu'apres une approbation explicite ;
- appliquer le cycle TDD et les validations ciblees SecureFiles ;
- ajouter avant la fin de l'invocation un bloc a la fin de `resume.md` contenant la demande utilisateur verbatim et le resultat factuel obtenu, y compris si le workflow s'arrete sur la pause d'approbation ou un blocage.

Ne pas dupliquer ou contourner les regles du prompt de reference. En cas de divergence, le contenu de [plan-approve-implement.prompt.md](plan-approve-implement.prompt.md) est la source complete du workflow.