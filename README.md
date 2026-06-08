# ArdhiJAVA

## Description
ArdhiJAVA est une application Desktop (JavaFX) développée en Java. Elle propose diverses fonctionnalités liées à la gestion de l'agriculture, des parcelles, la marketplace, et l'intégration de services IA et de communication.

## Technologies utilisées
- **Frontend** : JavaFX
- **Backend** : Java 17+
- **Base de données** : MySQL 8
- **Gestionnaire de dépendances** : Maven

## Prérequis
- Java JDK 17+
- Maven
- Serveur MySQL 8

## Installation

### 1. Base de données
Le fichier SQL complet de la base de données est fourni à la racine du projet dans le dossier `database/` (`database/ardhi.sql`).
Pour que l'application fonctionne immédiatement :
1. Créez une base de données MySQL nommée `ardhi`.
2. Importez le fichier `database/ardhi.sql` dans cette base (via PhpMyAdmin, MySQL Workbench, ou en ligne de commande).

### 2. Variables d'environnement & Secrets
⚠️ **ATTENTION : Le projet nécessite des clés d'API (Groq, Stripe, Twilio) et des credentials Google pour fonctionner.**
Voir la section **Variables d'environnement** ci-dessous.

## Lancement
Pour compiler et lancer l'application via Maven :
```bash
mvn clean javafx:run
```
*(Ou exécutez directement la classe `Main` depuis votre IDE comme IntelliJ IDEA ou Eclipse).*

## Variables d'environnement

1. **Fichier de configuration général (`config.properties`)** :
   Allez dans `src/main/resources/`, copiez le fichier `config.properties.example` et renommez-le en `config.properties`. Remplissez-le avec vos propres clés API (Groq, Stripe, Twilio) et mots de passe.
   
2. **Fichiers Google (OAuth & Calendar)** :
   Le projet utilise 3 fichiers JSON différents pour se connecter aux APIs Google. Plutôt que de les coder en dur, **leurs chemins sont configurables directement dans votre `config.properties`** :
   - `google.credentials.employe.path` (Pour le Service Account du calendrier des employés)
   - `google.credentials.maintenance.path` (Pour le module Matériel/Maintenance)
   - `google.credentials.evenement.path` (Pour le module Événements)

   👉 **Ce que vous devez faire :** 
   Obtenez ces 3 fichiers JSON (via Google Cloud Console ou votre équipe), placez-les n'importe où sur votre machine (ou dans un dossier ignoré par Git), puis copiez leurs chemins absolus ou relatifs dans votre fichier `config.properties`.

*(Note : Assurez-vous que vos fichiers JSON et votre `config.properties` ne soient jamais poussés sur Git).*

## Démo
Vidéo : https://www.youtube.com/watch?v=EUZm2hC9VuE

## Auteurs, Classe, Tuteur
**Auteurs** : Souibgui Saifeddine, Affi Rim, Ben Attia Yasmine, Rahmouni Yasmine, Delhoumi Elyes, Haj Salem Adel  
**Classe** : 3A18  
**Tuteur** : El Hakim Imen, Gaudria Khaled
