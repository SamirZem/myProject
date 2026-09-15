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
| `backend` | Petit serveur relais (Node/Express) devant l'API officielle Clash Royale. Obligatoire car la clé API de Supercell est verrouillée à une IP fixe — elle ne peut donc jamais vivre dans l'app mobile. |

## Comment ça analyse une partie

Deux sources, complémentaires :

1. **Capture live** (le cœur du projet) : pendant que tu joues, l'app échantillonne l'écran
   toutes les ~500 ms et en déduit l'élixir des deux joueurs, les PV des tours et les cartes que
   tu joues. À partir de ça elle détecte des trades d'élixir positifs/négatifs, les
   sur-investissements punis, les défenses manquées alors que tu avais de l'élixir disponible, et
   le gaspillage d'élixir en double élixir.
2. **API officielle** (recoupement) : en complément, l'app peut aller chercher ta dernière partie
   sur l'API officielle (couronnes, PV de tours restants, decks) via le serveur relais, pour une
   analyse de deck/résultat plus posée et une vérification du score.

**Limite assumée** : la capture d'écran est de la vision par ordinateur "classique" (lecture de
couleurs de pixels sur des zones calibrées), pas un modèle entraîné pour reconnaître précisément
chaque troupe adverse ou son placement exact au pixel près. Elle donne le *timing* et les
*échanges d'élixir*, pas une reconnaissance parfaite de chaque unité sur le terrain.

## Ce qui n'a pas pu être testé ici

Ce projet a été développé dans un environnement sans SDK Android ni appareil/émulateur, et sans
accès au dépôt Maven de Google (`dl.google.com`) qui héberge l'Android Gradle Plugin et AndroidX.
Concrètement :

- **`android/analyzer` a été compilé et testé avec succès** (`./gradlew :analyzer:test`, 10 tests
  qui passent) — c'est le module qui ne dépend pas du SDK Android.
- **`android/app` n'a pas pu être compilé ni exécuté** dans cette session. Le code a été écrit
  avec soin à partir des API Android standard (MediaProjection, Compose, Room, Retrofit, DataStore)
  mais **doit être ouvert dans Android Studio, sur une machine avec accès réseau normal, pour être
  buildé, corrigé si besoin et testé sur un vrai Pixel 8 Pro**.
- Les **valeurs de calibration par défaut** (position de la barre d'élixir, des tours, des cases
  de cartes, couleurs de référence) sont des estimations raisonnables de la mise en page de Clash
  Royale, pas des mesures faites sur un vrai écran. Il faudra presque certainement les ajuster via
  l'écran **Calibration** de l'app avant que l'analyse live soit fiable.

## Démarrage rapide

### 1. Backend

```bash
cd backend
cp .env.example .env   # renseigne CLASH_ROYALE_API_KEY
npm install
npm test                # tourne sans clé API, avec un fetch mocké
npm start
```

Déploie-le ensuite sur un service à IP fixe (VPS, Render, Fly.io, Railway...) — voir
`backend/README.md`. Restreins la clé API à cette IP sur developer.clashroyale.com.

### 2. App Android

```bash
cd android
./gradlew :analyzer:test   # vérifie le moteur d'analyse (ne nécessite pas le SDK Android)
```

Puis ouvre le dossier `android/` dans Android Studio pour builder et lancer `app` sur ton Pixel 8
Pro (il te faudra le SDK Android, qu'Android Studio installe automatiquement).

Dans l'app :
1. **Paramètres** (écran d'accueil) : renseigne l'URL de ton serveur relais, ton tag joueur, et
   ton deck actuel (8 noms de cartes).
2. **Calibration** : lance une capture, ouvre Clash Royale, reviens dans l'app et ajuste les
   zones (barre d'élixir, cases de main, tours) sur l'aperçu en direct. Capture aussi une
   empreinte pour chacune de tes 8 cartes (aucune image Supercell n'est jamais intégrée à l'app —
   les empreintes viennent uniquement de ton propre écran).
3. **Démarrer la capture**, joue ta partie, puis **Arrêter et analyser**.
4. Consulte le résultat dans **Historique**.
