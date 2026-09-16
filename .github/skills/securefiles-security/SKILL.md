---
name: securefiles-security
description: Use when changing file upload, quarantine, antivirus scanning, storage, download, validation, concurrency, or sensitive logging in SecureFiles.
---

# Securite du flux de fichiers

## A charger

- [AGENTS.md](../../../AGENTS.md)
- [README.md](../../../README.md)
- [Clean Code](../../../rules/clean_code.md)
- [Strategie de test](../../../rules/strategy_test.md)

## Checklist obligatoire

- Un nouvel upload commence a `PENDING_SCAN` et reste indisponible pendant `SCANNING`.
- Seul le cas d'utilisation de download, avec l'appui du domaine, peut autoriser un flux pour `CLEAN`.
- Une panne, une reponse inconnue ou un timeout ClamAV produit un echec ferme ; aucun fallback ne transforme l'erreur en fichier sain.
- Les octets transitent par des flux et ne sont pas charges integralement dans PostgreSQL ou en memoire sans necessite.
- Le nom, la taille et le type MIME sont valides et neutralises a la frontiere HTTP ; aucun chemin client ne devient un chemin de stockage.
- Le stockage des octets passe par un port. PostgreSQL conserve les metadonnees, jamais le contenu.
- Les logs excluent secret, token, contenu, hash inutilement expose et donnees personnelles non necessaires.
- Les transitions de statut sont atomiques ou protegees contre les doubles scans et les courses entre scan et download.
- Toute modification du domaine ajoute un test du refus associe et verifie les chemins
	d'exception. Les modifications des couches `application` et `infrastructure` n'ajoutent
	pas de test de couche ; seuls les mappers purs sont testes.

## Revue rapide

Identifier le chemin complet `HTTP -> application -> port -> infrastructure` et son chemin d'erreur avant de modifier une condition. Une modification d'un controleur ne doit jamais contourner la decision metier de telechargement.