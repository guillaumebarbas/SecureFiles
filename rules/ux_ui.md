# Regles de design visuel SecureFiles

Ces regles decrivent uniquement le langage visuel de la console SecureFiles : direction artistique, palette, typographie, surfaces, controles, animation et adaptation visuelle aux tailles d'ecran. Les regles de workflow, d'API, d'accessibilite et de statut metier restent dans les agents et les skills concernes.

## Direction generale

- Donner une impression de confiance calme, de precision et de controle ; eviter l'esthetique marketing ou decorative.
- Construire une interface operationnelle, dense mais aeree, avec une hierarchie visuelle nette et des zones fonctionnelles facilement scannables.
- Utiliser un fond clair et legerement teinte, des surfaces blanches ou translucides sobres et une grille discrete pour donner de la profondeur sans bruit visuel.
- Garder une composition editoriale sobre : titre fort dans la zone d'introduction, panneaux compacts, espaces blancs reguliers et actions visuellement prioritaires.
- Ne pas utiliser de hero surdimensionne, de cartes decoratives empilees, de blobs, d'orbes ou de bokeh comme decoration.

## Palette de couleurs

- Definir les couleurs dans des variables CSS semantiques : fond, surface, texte principal, texte secondaire, bordure, action primaire et etats.
- Partir d'une base coherente : canvas `#F4F7FB`, surface `#FFFFFF`, surface secondaire `#EEF3FB`, texte principal `#17233D`, texte secondaire `#6D7890` et bordure `#DBE3F0`.
- Utiliser `#3967F6` pour l'action primaire et la confiance, `#2F56D6` au survol, `#16A36B` pour le succes, `#D8911F` pour l'attente et `#DF3E55` pour le blocage ou l'erreur.
- Reserver chaque couleur d'etat a son role : vert pour confirmation, ambre pour attente, rouge pour blocage et gris pour information neutre.
- Garantir une separation visuelle suffisante entre le texte, le fond, les bordures et les controles ; ne jamais rendre une information critique dependante d'une seule teinte.
- Eviter les palettes dominees par le violet, les degrades violet-blanc et les fonds noirs par defaut.
- Utiliser un degrade uniquement sur une grande surface non interactive : introduction, hero fonctionnel, fond d'un etat vide ou panneau d'ambiance.
- Preferer un degrade clair a deux ou trois tons voisins, par exemple `linear-gradient(135deg, #F8FAFF 0%, #EAF0FF 55%, #FFFFFF 100%)`, avec une difference de luminosite douce.
- Limiter un degrade a deux ou trois arrets de couleur, avec une direction lisible de `120deg` a `160deg` et un contraste de luminosite doux ; ne pas juxtaposer des couleurs d'etat dans un meme degrade.
- Utiliser un degrade pour guider le regard vers une zone fonctionnelle ou separer une introduction du registre ; le supprimer si le texte, le contraste ou la lecture du statut devient moins clair.
- Poser les textes et controles sur une zone suffisamment unie du degrade ; ajouter une surface opaque ou translucide si le contraste fluctue sous le contenu.
- Preferer un degrade statique et discret ; ne pas animer les arrets de couleur, la position ou l'angle d'un degrade sur une interaction ordinaire.
- Ne pas utiliser de degrade sur le texte, les icones, les bordures, les badges, les champs ou les boutons d'action.
- Ne pas mettre de degrade sur les boutons, badges de statut, champs, petites cartes, textes ou icones : ces elements utilisent des couleurs pleines pour rester lisibles et stables.
- Ne pas utiliser un degrade pour masquer une hierarchie faible ; si une surface n'est pas un accent visuel, elle reste unie.

## Typographie

- Utiliser `Space Grotesk` pour les titres et `Manrope` pour le texte courant ; prevoir des fallbacks proches et charger ces polices de maniere explicite plutot que de compter sur une pile systeme generique.
- Utiliser `IBM Plex Mono` uniquement pour les identifiants, ports, dates techniques, hashes et valeurs qui beneficient d'un alignement regulier.
- Respecter cette echelle de tailles sur desktop : display `48px / 1.1 / 700`, titre de page `36px / 1.15 / 700`, titre de section `24px / 1.2 / 700`, titre de panneau `18px / 1.3 / 700`, texte courant `16px / 1.55 / 400`, texte secondaire `14px / 1.45 / 400`, libelle `13px / 1.35 / 600` et micro-texte `12px / 1.35 / 500`.
- Reduire seulement les niveaux display et titre de page sur mobile : display `36px` et titre de page `30px` ; conserver le texte courant a `16px` pour eviter une lecture trop petite.
- Utiliser un seul titre `h1` par vue ; le titre de page exprime la tache ou le registre en cours, pas une phrase marketing.
- Utiliser `h2` pour les sections principales, `h3` pour les panneaux et des elements `p` pour le texte explicatif ; ne pas choisir une taille selon l'apparence souhaitee sans respecter ce niveau semantique.
- Garder les titres de page sur une ou deux lignes maximum quand la largeur le permet ; autoriser le retour a la ligne naturel plutot que de reduire excessivement la taille.
- Garder les paragraphes explicatifs entre 45 et 75 caracteres par ligne environ ; utiliser une largeur maximale de `65ch` pour les textes longs.
- Utiliser le poids `700` pour les titres, `600` pour les libelles et actions textuelles, `400` pour le texte courant ; reserver `500` aux informations secondaires qui doivent rester lisibles.
- Utiliser la couleur de texte principal pour les titres et le contenu essentiel, la couleur secondaire pour les aides et metadonnees, et une couleur d'etat uniquement pour une information d'etat.
- Ne pas ecrire un paragraphe entier en capitales, ne pas souligner les titres et ne pas utiliser l'italique comme seul moyen de distinguer une information critique.
- Definir les tailles et interlignes dans des variables CSS semantiques ; ne pas utiliser de valeurs arbitraires repetees composant par composant.
- Garder `letter-spacing: 0` ; ne jamais utiliser de letter-spacing negatif et ne pas faire dependre la taille du texte uniquement de la largeur de la fenetre.
- Verifier les textes longs, les noms de fichiers et les erreurs avant livraison ; ils doivent revenir a la ligne, etre tronques avec intention ou disposer d'une hauteur stable sans chevaucher un autre element.

## Mise en page et surfaces

- Utiliser une grille d'espacement stable, idealement basee sur des multiples de 4 ou 8 px.
- Aligner les bords, titres, controles et contenus sur des axes constants ; eviter les blocs places uniquement pour remplir l'espace.
- Garder une largeur de lecture maitrisee et des colonnes qui se reduisent progressivement avant de passer en une colonne.
- Utiliser les cartes uniquement pour des elements repetes, des panneaux fonctionnels ou des outils reellement encadres ; ne pas placer une carte dans une autre carte.
- Preferer les bandes et mises en page non encadrees pour les grandes sections de page.
- Stabiliser les dimensions des lignes, boutons, badges, icones et zones de depot afin d'eviter les sauts de mise en page.

## Bordures, rayons et ombres

- Utiliser des bordures fines, sobres et coherentes pour separer les surfaces.
- Limiter le `border-radius` des cartes et panneaux a 8 px ou moins, sauf convention explicite du design existant.
- Reserver les formes tres arrondies aux controles qui representent clairement un etat ou une pastille ; ne pas transformer toute l'interface en capsules.
- Utiliser des ombres diffuses et peu contrastees pour soulever une surface, jamais pour donner un effet plastique ou flottant a chaque element.
- Conserver la meme logique de rayon et d'ombre entre les panneaux, les boutons et les elements repetes.

## Controles et iconographie

- Utiliser les icones Lucide ou la bibliotheque deja presente au lieu de dessiner des SVG equivalents a la main.
- Preferer une icone familiere pour les actions d'outil ; ajouter un libelle lorsque le symbole seul peut etre ambigu.
- Donner aux boutons d'icone une dimension stable, une zone de clic reguliere et un etat hover/focus visuellement coherent.
- Chaque bouton ou element actionnable, y compris bouton d'icone, lien d'action, toggle, selecteur et declencheur de menu, doit afficher un tooltip au survol et au focus avec le verbe et la cible de l'action.
- Un tooltip doit apparaitre apres environ 150 a 250 ms, rester proche du controle, utiliser une surface sombre stable et ne jamais masquer le controle ou l'information principale.
- Utiliser les badges et pastilles pour les etats courts, avec un contraste de fond et de texte mesure et sans surcharge de bordures.
- Ne pas utiliser une icone uniquement comme decoration si elle concurrence le titre ou l'action principale.

## Animation et mouvement

- Utiliser peu d'animations, mais leur donner une intention : apparition de page, revelation progressive, progression, changement d'etat ou chargement.
- Animer de preference `opacity` et `transform`, pas la geometrie qui provoquerait des decalages ou des reflows.
- **Hover** : faire varier la couleur, la bordure ou l'ombre en 150 a 180 ms ; un element peut monter de 1 a 2 px, sans agrandissement superieur a 2 %.
- **Focus** : renforcer la bordure ou l'anneau de focus sans deplacer le layout ; l'etat doit rester visible pendant toute l'interaction.
- **Clic/pression** : utiliser une reduction legere de `scale(0.98)` ou un deplacement de 1 px pendant 80 a 120 ms, puis revenir a l'etat normal.
- **Entree** : combiner `opacity` et `translateY(8px)` sur 240 a 320 ms ; echelonner les groupes de 40 a 60 ms maximum entre elements.
- **Transition d'etat** : animer couleur, opacite ou ombre sur 180 a 240 ms ; ne pas animer la taille si cela deplace les voisins.
- **Chargement** : reserver les boucles infinies au spinner, a la progression ou a une activite en cours clairement identifiee ; une rotation peut durer 800 a 1000 ms.
- Ne pas ajouter de rebond, de parallaxe ou d'effet sonore a une interaction ordinaire.
- Respecter `prefers-reduced-motion` en supprimant les deplacements et en conservant uniquement les changements d'etat indispensables.

## Adaptation visuelle responsive

- Conserver la hierarchie, les alignements et les priorites visuelles lorsque la largeur diminue ; ne pas simplement reduire tout le contenu.
- Passer les compositions multi-colonnes en une colonne lorsque les titres, boutons ou lignes ne tiennent plus confortablement.
- Faire tenir les textes dans leur conteneur : autoriser le retour a la ligne, le tronquage controle et une hauteur stable.
- Ne jamais laisser un panneau, un badge, une icone ou un bouton chevaucher un autre element sur mobile.
- Verifier au minimum une largeur desktop et une largeur mobile avant de finaliser une modification visuelle.

## Verification visuelle

- Controler la palette, les contrastes, la densite, les rayons, les ombres et les etats hover/focus avant livraison.
- Tester les textes longs, les listes vides, les etats de chargement et les erreurs visuellement sans laisser le contenu provoquer de saut ou de debordement.
- Conserver une coherence entre les nouveaux composants et la direction visuelle de l'ecran de reference.