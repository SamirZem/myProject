# Clash Analyzer

Application Android (pensée pour le Pixel 8 Pro, Android 14+) qui analyse tes parties de Clash
Royale par **capture d'écran en direct** : élixir, PV des tours et cartes jouées sont lus
localement pendant la partie, puis l'app te donne les erreurs commises, les bons mouvements, des
conseils, et une note de performance. Rien de ce qui est capturé n'est jamais écrit sur le
disque : les images sont traitées en mémoire et jetées au fur et à mesure.

## Pourquoi 3 dossiers

| Dossier | Rôle |
|---|---|
| `android/analyzer` | Le moteur d'analyse, en Kotlin pur (aucune dépendance Android). Contient toute la logique : base de cartes, heuristiques post-match et heuristiques "live" (trades d'élixir, sur-investissement, défense manquée, gestion du double élixir). Testable avec `gradle`/`gradlew` seul, sans SDK Android. |
| `android/app` | L'application Android : capture d'écran (`MediaProjection`), lecture de pixels (élixir/PV/cartes), UI Compose, base locale (Room), calibration. |
| `backend` | Petit serveur relais (Node/Express) **optionnel** devant l'API officielle Clash Royale. Utile uniquement pour le recoupement de score via l'API et l'import de deck par lien — la clé API de Supercell est verrouillée à une IP fixe, donc elle ne peut jamais vivre dans l'app mobile, il faut ce serveur pour ces deux fonctionnalités-là. L'analyse principale (capture live) n'en a pas besoin du tout. |

## Comment ça analyse une partie

Deux sources, complémentaires :

1. **Capture live** (le cœur du projet) : pendant que tu joues, l'app échantillonne l'écran
   toutes les ~500 ms et en déduit l'élixir des deux joueurs, les PV des tours et les cartes que
   tu joues. À partir de ça elle détecte des trades d'élixir positifs/négatifs, les
   sur-investissements punis, les défenses manquées alors que tu avais de l'élixir disponible, et
   le gaspillage d'élixir en double élixir. Elle détecte aussi, par différence d'image (pas de
   reconnaissance de carte), *quand* l'adversaire déploie quelque chose sur son terrain — jamais
   *quoi* — pour mesurer ton temps de réaction aux poussées adverses.
2. **API officielle** (recoupement) : en complément, l'app peut aller chercher ta dernière partie
   sur l'API officielle (couronnes, PV de tours restants, decks) via le serveur relais, pour une
   analyse de deck/résultat plus posée et une vérification du score.

**Limite assumée** : la capture d'écran est de la vision par ordinateur "classique" (lecture de
couleurs de pixels sur des zones calibrées), pas un modèle entraîné pour reconnaître précisément
chaque troupe adverse ou son placement exact au pixel près. Elle donne le *timing* et les
*échanges d'élixir*, pas une reconnaissance parfaite de chaque unité sur le terrain.

**Découvert en analysant une vraie capture d'écran fournie par l'utilisateur** (voir
`CalibrationProfile.default()`) : Clash Royale n'affiche **jamais** le compteur d'élixir de
l'adversaire en 1v1 classique — contrairement à l'hypothèse initiale du projet, ce n'est donc pas
observable par capture d'écran, et le champ correspondant a été retiré du modèle de données. Autre
point corrigé : les barres de PV des tours ne sont **pas** d'une couleur unique — bleues côté
allié, rouges/roses côté adversaire — la calibration distingue maintenant les deux.

## Build et APK

Cet environnement de développement n'a pas d'accès au dépôt Maven de Google (`dl.google.com`, qui
héberge l'Android Gradle Plugin et AndroidX) ni de SDK Android/émulateur — impossible d'y compiler
`android/app` directement. Le workflow GitHub Actions
[`android-build.yml`](.github/workflows/android-build.yml) s'en charge à la place (les runners
GitHub ont un accès réseau complet) : il tourne à chaque push sur `android/**`, exécute les tests
du module `analyzer`, puis build `app-debug.apk` et le publie comme artefact téléchargeable sur
l'onglet **Actions** du dépôt.

- **`android/analyzer`** a été compilé et testé avec succès aussi bien localement
  (`./gradlew :analyzer:test`, 10 tests qui passent) que sur CI.
- **`android/app`** build maintenant **avec succès sur CI** (`./gradlew :app:assembleDebug`) —
  plusieurs bugs réels ont été trouvés et corrigés en cours de route (voir l'historique de commits
  sur la branche) : couplage d'un type entre modules qui perturbait KSP/Room, API Material3
  expérimentale utilisée sans opt-in. Il reste conseillé d'ouvrir le projet dans Android Studio
  avant d'installer l'APK sur un vrai Pixel 8 Pro, pour repérer d'éventuels soucis que seul un test
  sur device révèle (permissions, calibration, performance).
- Les **valeurs de calibration par défaut** ont été mesurées à partir d'une vraie capture d'écran
  Clash Royale (1008×2244, fournie par l'utilisateur), pas devinées à l'aveugle — position et
  couleurs de la barre d'élixir, des 4 cases de main et des tours princesses (alliées et
  ennemies) viennent d'une analyse pixel par pixel de cette image. Deux limites subsistent : les
  tours du roi n'apparaissent jamais endommagées sur cette capture (aucune barre de PV visible
  avant le premier dégât), donc leur position reste une estimation géométrique ; et les couleurs
  "vide/endommagée" des barres n'ont pas pu être mesurées puisque toutes les barres étaient
  pleines sur cette capture. Utilise l'écran **Calibration** pour affiner ces deux points, et pour
  corriger tout écart dû à la résolution ou aux proportions d'écran de ton propre appareil.

## Démarrage rapide

### 1. Backend (optionnel — passe directement à l'étape 2 si tu veux juste l'analyse live)

```bash
cd backend
cp .env.example .env   # renseigne CLASH_ROYALE_API_KEY
npm install
npm test                # tourne sans clé API, avec un fetch mocké
npm start
```

Déploie-le ensuite sur un service à IP fixe (VPS...) — voir `backend/README.md`. Restreins la clé
API à cette IP sur developer.clashroyale.com. Sans serveur ni téléphone à IP fixe, ces deux
fonctionnalités précises ne sont pas faisables — tout le reste de l'app (capture live, y compris
l'import de deck par capture d'écran) fonctionne sans rien déployer.

### 2. App Android

```bash
cd android
./gradlew :analyzer:test   # vérifie le moteur d'analyse (ne nécessite pas le SDK Android)
```

Puis ouvre le dossier `android/` dans Android Studio pour builder et lancer `app` sur ton Pixel 8
Pro (il te faudra le SDK Android, qu'Android Studio installe automatiquement).

Dans l'app :
1. **Paramètres** (écran d'accueil) : renseigne ton deck actuel — soit à la main (8 noms), soit
   via **Importer le deck depuis une capture d'écran** (photo de ton menu Deck en jeu, aucun
   serveur requis : ça enregistre le deck *et* une empreinte de reconnaissance par carte d'un
   coup). L'URL du serveur relais et le tag joueur ne servent qu'au recoupement API / import par
   lien — laisse-les vides sinon.
2. **Calibration** : lance une capture, ouvre Clash Royale, reviens dans l'app et ajuste les
   zones (barre d'élixir, cases de main, tours) sur l'aperçu en direct. Tu peux aussi y capturer
   des empreintes de carte pendant une partie (aucune image Supercell n'est jamais intégrée à
   l'app — les empreintes viennent uniquement de ton propre écran).
3. **Démarrer la capture**, joue ta partie, puis **Arrêter et analyser**.
4. Consulte le résultat dans **Historique**.
