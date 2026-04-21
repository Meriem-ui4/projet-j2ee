# Memory Game — Guide de démarrage complet

## Stack technique
- **Java 17** + Spring MVC 5.3 + Thymeleaf 3.1
- **Hibernate 5.6** (ORM)
- **MySQL 8** via **WampServer** (remplace H2)
- **BCrypt** pour le hachage des mots de passe
- **Maven** pour la compilation
- Déployable sur **Tomcat 9/10**

---


1. ** Configuration Identifiants MySQL de WampServer :**
   ```
   Hôte     : localhost
   Port     : 3306
   Utilisateur : root
   Mot de passe : (vide)
   >  Si le mot de passe root n'est pas vide dans votre WampServer, modifiez
> `ds.setPassword("")` dans `AppConfig.java` avec votre mot de passe.



---

## 2. Configuration de la connexion dans le code

Fichier : `src/main/java/com/memorygame/AppConfig.java`

```java
ds.setUrl("jdbc:mysql://localhost:3306/memorygame?useSSL=false&serverTimezone=UTC&...");
ds.setUsername("root");
ds.setPassword("");   // ← votre mot de passe MySQL
```

---

## 3. Compiler et déployer

###  Tomcat via Maven (développement)
```bash

mvn clean package
mvn tomcat7:run
```
Accéder à : http://localhost:8080


```
## 4. Structure
```
memory-game/
├── pom.xml
├── database_setup.sql
├── README.md
└── src/main/
    ├── java/com/memorygame/
    │   ├── AppConfig.java          ← Config MySQL
    │   ├── AppInitializer.java
    │   ├── WebConfig.java
    │   ├── model/
    │   │   ├── Player.java
    │   │   ├── SavedGame.java
    │   │   └── ScoreEntry.java
    │   ├── repository/
    │   │   ├── PlayerRepository.java
    │   │   ├── SavedGameRepository.java
    │   │   └── ScoreEntryRepository.java
    │   ├── service/
    │   │   ├── PlayerService.java
    │   │   └── GameService.java
    │   └── controller/
    │       ├── AuthController.java
    │       └── GameController.java
    ├── resources/static/
    │   └── images/
    │       ├── theme1/{niveau1,niveau2,niveau3}/
    │       ├── theme2/{niveau1,niveau2,niveau3}/
    │       └── theme3/{niveau1,niveau2,niveau3}/
    └── webapp/WEB-INF/templates/
        ├── fragments/
        │   ├── styles.html
        │   └── scripts.html
        ├── auth/
        │   ├── login.html
        │   └── register.html
        └── game/
            ├── menu.html
            ├── play.html
            └── leaderboard.html
```

