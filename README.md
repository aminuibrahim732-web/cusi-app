# CUSI – V7 Hors ligne

Application Android de révision au Certificat Unique en Soins Infirmiers.

## Fonctionnement
- 100 % hors ligne après installation.
- Aucun serveur.
- Aucune base de données distante.
- Les cas cliniques et QCM sont intégrés dans l'application.
- Les résultats sont sauvegardés localement sur le téléphone.
- Profil étudiant local.
- Examens blancs chronométrés.
- Score et pourcentage.
- Historique local.
- Attestation de réussite affichée localement.

## Contenu
La base `app/src/main/assets/cusi_database.json` contient les données structurées à partir du document fourni.
Les réponses officielles ne sont utilisées que lorsqu'elles sont effectivement disponibles dans le document.
Les distracteurs ajoutés pour former des QCM sont générés pour l'entraînement et ne doivent pas être considérés comme des réponses officielles du document.

## Construire l'APK
Ouvrir le dossier dans Android Studio, synchroniser Gradle puis :
Build > Build APK(s)

L'application n'a besoin d'aucun compte serveur ni connexion Internet pour fonctionner.
