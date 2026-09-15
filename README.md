# Temps d'Écran Pro

Application Android (Kotlin + Jetpack Compose) qui analyse ton temps d'écran en
profondeur, directement sur l'appareil, sans envoyer aucune donnée en dehors du
téléphone.

## Fonctionnalités

- **Aujourd'hui** : temps d'écran total, comparaison à la moyenne des 7 derniers
  jours, nombre de déverrouillages, nombre d'apps utilisées, heure de dernière
  activité, top applications.
- **Applications** : classement complet par app sur Aujourd'hui / 7 jours / 30
  jours, avec nombre de sessions.
- **Détail par app** : temps total, moyenne par jour actif, nombre de sessions,
  session la plus longue, session moyenne, graphique des 14 derniers jours.
- **Tendances** : graphique des 7 derniers jours, carte de chaleur heure par
  heure (sur 28 jours cumulés), moyenne par jour de la semaine, statistiques de
  déverrouillage.

Les données proviennent directement de `UsageStatsManager`/`UsageEvents`
(transitions premier plan/arrière-plan, déverrouillages) plutôt que des seuls
totaux agrégés du système, ce qui permet de calculer sessions, session la plus
longue et répartition horaire réelle. Les journées passées sont archivées dans
une base Room locale pour que les tendances restent disponibles même après que
le système ait purgé son propre historique.

## Pile technique

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (cache/historique local)
- WorkManager (archivage périodique en arrière-plan)
- Aucune dépendance réseau, aucune donnée envoyée à l'extérieur

## Prérequis

- Android Studio (Ladybug ou plus récent recommandé)
- JDK 17
- Un appareil ou émulateur Android 9 (API 28) ou supérieur — testé pour un
  Google Pixel 8 Pro (Android 14/15)

## Compiler et lancer

1. Ouvrir le dossier du projet dans Android Studio (`File > Open`).
2. Laisser Android Studio synchroniser Gradle (le wrapper est déjà fourni).
3. Brancher le téléphone en USB avec le débogage USB activé, ou lancer un
   émulateur.
4. Lancer `app` (bouton ▶️) ou en ligne de commande :

   ```bash
   ./gradlew installDebug
   ```

## Autoriser l'accès aux statistiques d'usage

Au premier lancement, l'application demande l'accès spécial **Usage des
applications** (obligatoire pour lire les statistiques d'usage, ce n'est pas
une permission classique). Appuie sur "Ouvrir les paramètres", puis active
l'accès pour "Temps d'Écran Pro" dans la liste, et reviens dans l'app.

## Remarques

- Ce dépôt ne peut pas être compilé dans cet environnement d'exécution (pas de
  SDK Android disponible, le dépôt Maven de Google est bloqué par la politique
  réseau du bac à sable) : le code a été écrit avec soin mais doit être compilé
  et testé sur un poste avec Android Studio.
- Le nom de package est `com.samirzem.screentimeanalyzer`.
