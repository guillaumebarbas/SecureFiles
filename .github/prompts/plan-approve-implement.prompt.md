---
name: plan-approve-implement
description: "Use when a request needs repository analysis, an implementation plan, explicit user approval, implementation only after approval, and a prompt/result entry in resume.md. Also available as /plan-implement."
argument-hint: "Describe the feature, fix, or refactor to analyze and implement after approval"
agent: "SecureFilesAgent"
tools: [read, search, agent, edit, execute, todo]
---

# Plan Approve Implement

Utiliser ce prompt pour separer clairement la comprehension, la planification et l'implementation. L'implementation est interdite tant que l'utilisateur n'a pas approuve explicitement le plan final.

## Phase 1 - Analyse et exploration

1. Lire `AGENTS.md` a la racine, le `AGENTS.md` le plus proche du code vise, le contrat pertinent de `README.md`, les instructions applicables et les tests voisins.
2. Comprendre la demande, le besoin metier, les contraintes, les invariants de securite et le resultat attendu.
3. Identifier le code qui decide directement le comportement : port, use-case, composant, hook, adaptateur, endpoint ou test existant. Ne pas s'arreter a un fichier qui ne fait que transmettre ou enregistrer l'appel.
4. Formuler une hypothese locale falsifiable et le controle le moins couteux qui pourrait la refuter.
5. Pour une tache qui traverse plusieurs fichiers, une couche ou une frontiere d'architecture, invoquer le sous-agent `SecureFilesPlanner` avec l'outil `#tool:agent`. Lui transmettre la demande, les chemins pertinents et la contrainte de lecture seule. Ne lui demander ni modification, ni commande, ni commit.
6. Relire et verifier les conclusions du sous-agent dans le code local. Le sous-agent fournit une piste d'exploration, pas une decision automatique.

Pendant cette phase, ne creer, modifier ou supprimer aucun fichier. Ne pas installer de dependance, ne pas formater le depot, ne pas commiter et ne pas pousser.

## Phase 2 - Plan et auto-relecture

Presenter une premiere reflexion structuree avec :

- la comprehension de la demande et le comportement observable vise ;
- les fichiers, symboles et contrats concernes, avec leur responsabilite ;
- les invariants, risques de securite, contraintes de compatibilite et dependances ;
- les options examinees et la raison du choix retenu ;
- les tests a ecrire ou a modifier, selon la strategie TDD du projet ;
- les commandes de validation prevues ;
- les points qui restent incertains.

Relire ensuite le plan comme un reviewer independant. Chercher explicitement :

- une responsabilite placee dans la mauvaise couche ;
- une modification plus large que necessaire ;
- un comportement concurrent, de streaming ou de securite oublie ;
- un test qui verifierait un detail interne plutot qu'un contrat observable ;
- une dependance ou une migration non documentee ;
- une validation manquante ou trop tardive ;
- une occasion de simplifier, reduire le risque ou rendre le plan plus reversible.

Afficher ensuite un `PLAN FINAL` corrige, ordonne et suffisamment concret pour etre execute.

## Pause obligatoire pour validation

Apres `PLAN FINAL`, s'arreter et demander a l'utilisateur une approbation explicite. Ne pas modifier de fichier, ne pas lancer de commande d'implementation et ne pas commencer les tests de la tranche avant cette approbation.

Considerer comme approbation explicite une reponse telle que `approuve`, `go`, `implemente` ou une confirmation equivalente. Si la reponse est ambigue, demander une clarification. Si l'utilisateur modifie le besoin, revenir a la phase d'analyse et produire un plan revise avant toute modification.

## Phase 3 - Implementation apres approbation

1. Reprendre uniquement le plan approuve et signaler toute divergence necessaire avant de l'introduire.
2. Pour toute implementation frontend ou backend, charger [TDD](../skills/tdd/SKILL.md) et appliquer `red-green-refactor` sur une seam publique.
3. Commencer par le test rouge le plus cible, produire le minimum pour passer au vert, puis refactorer sans changer le comportement.
4. Apres la premiere modification substantielle, lancer immediatement la validation executable la plus ciblee. Ne pas elargir la portee avant ce controle.
5. Executer les validations prevues par le plan, puis verifier les risques restants et les fichiers effectivement modifies.
6. Fournir un compte rendu final avec les marqueurs de contexte du projet, les changements, les validations executees, les risques restants et les ecarts eventuels par rapport au plan approuve.
8. Avant de terminer l'invocation, ajouter un bloc a la fin de `resume.md` avec la demande utilisateur qui a declenche le workflow, copiee verbatim, et un resultat factuel correspondant a l'etat reel du plan, de l'implementation et des validations. Cette etape s'applique aussi si le workflow s'arrete sur la pause d'approbation ou sur un blocage ; le resultat doit alors l'indiquer clairement. Respecter la regle de redaction des secrets de `SecureFilesAgent.agent.md` et ne jamais reecrire les blocs precedents.

Si une validation refute l'hypothese ou si le perimetre change, s'arreter, expliquer le nouveau constat et demander une nouvelle approbation avant d'elargir l'implementation.