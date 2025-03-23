# Développement d’un Logiciel de Contrôle d'ordinateur à Distance en Java<br>
## Membre du groupe<br>
Mamadou Abdoul Hamid Diallo <br>
Isaac Feglar Fiacre Memini Edou <br>
Ousmane Ali Brahim <br>
Presentation Youtube : https://youtu.be/6JK00vu_Xjc <br>
# Fontionnalité Implementé
- Communication sécurisée via **SSLSocket** (SSL/TLS).
- Support de multiples connexions simultanées via un système de threading.
- Transmission sécurisée des commandes et de leurs résultats.
- Authentification : Mise en place d’un système de login/mot de passe pour sécuriser l’accès.<br>
- Chiffrement : Utilisation de SSL/TLS pour sécuriser les échanges.<br>
- Transfert de fichiers : Possibilité d’envoyer des fichiers du client vers le serveur UPLOAD  et inversement. DOWNLOAD <br>
- Journalisation avancée : Sauvegarde des connexions et des commandes dans un fichier log. (journal.log)<br>
# Cette application dispose d'un programme console Client/Serveur et d'une interface graphique Client/serveur 
## 🚀 Technologies Utilisées
- **Java** (version 11)
- **Sockets** (`javax.net.ssl.SSLSocket` et `javax.net.ssl.SSLSocketFactory`)
- **Multithreading** (`java.util.concurrent`)
- **Flux d'entrée/sortie** (`java.io`)
- **Swing** pour l'interface graphique
- **Runtime/ProcessBuilder** pour l'exécution des commandes
- **Java JDBC pour la communication avec la base de donnees des utilisateurs 
