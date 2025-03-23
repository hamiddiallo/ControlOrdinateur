package serveur;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.net.ssl.*;
import javax.swing.JOptionPane;
//Cette classe représente le serveur de fichiers sécurisé.
//Elle gère les connexions clients, les authentifications, les requêtes de fichiers et les requêtes de stockage.
//Elle utilise un pool de threads pour gérer les connexions clients de manière asynchrone.
//Elle utilise une connexion à une base de données MySQL pour l'authentification des utilisateurs.
//Elle utilise un répertoire de stockage pour stocker les fichiers envoyés par les clients.
//Elle utilise le protocole de communication suivant:
//  - Les commandes envoyées par les clients commencent par "CMD:"
//  - Les résultats envoyés par le serveur commencent par "RESULT:"
//  - Les erreurs envoyées par le serveur commencent par "ERROR:"
//  - Les messages de déconnexion sont "DISCONNECT"
//  - Les messages d'authentification commencent par "AUTH:"
//  - Les messages d'authentification réussie sont "AUTH_SUCCESS"
//  - Les messages d'authentification échouée sont "AUTH_FAILED"
//  - Les fichiers envoyés par les clients sont stockés dans le répertoire de stockage
public class Server {
    private int port;//Port du serveur
    private SSLServerSocket serverSocket; // Utilisation de SSLServerSocket pour une connexion sécurisée SSL / TLS avec les clients 
    private boolean running;//Indique si le serveur est en cours d'exécution ou non
    private ExecutorService threadPool;//Pool de threads pour gérer les connexions clients de manière asynchrone c'est a dire en parallèle sans bloquer le serveur ni les autres clients
    private Logger logger;//Logger pour enregistrer les événements du serveur dans un fichier journal et dans la console 
    private List<ClientHandler> clients;//Liste des clients connectés
    private Connection connection; // Connexion à la base de données
    private File storageDirectory; // Répertoire de stockage des fichiers

    // Constantes du protocole de communication
    public static final String COMMAND_PREFIX = "CMD:";
    public static final String RESULT_PREFIX = "RESULT:";
    public static final String ERROR_PREFIX = "ERROR:";
    public static final String DISCONNECT_MESSAGE = "DISCONNECT";
    public static final String AUTH_PREFIX = "AUTH:";
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
        this.logger = Logger.getLogger(Server.class.getName());
        this.clients = Collections.synchronizedList(new ArrayList<>());

        // Initialiser la connexion à la base de données
        initializeDatabase();

        // Initialiser le répertoire de stockage des fichiers
        initializeStorageDirectory();
    }

    private void initializeDatabase() {//Cette mmethode Initialise la connexion à la base de données MySQL pour l'authentification des utilisateurs
        try {
            // Chargement du pilote JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");//Chargement du pilote JDBC pour MySQL 

            // Établissement de la connexion
            String url = "jdbc:mysql://localhost:8889/poo?useSSL=false";//URL de connexion à la base de données
            String user = "poo";//Nom d'utilisateur
            String password = "passer";//Mot de passe
            connection = DriverManager.getConnection(url, user, password);//Connexion à la base de données avec l'URL, le nom d'utilisateur et le mot de passe

            logger.info("Connexion à la base de données réussie");
        } catch (ClassNotFoundException e) {//Si le pilote JDBC n'est pas trouvé alors on affiche un message d'erreur et on lance une exception
            logger.severe("Erreur lors du chargement du pilote JDBC: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Erreur lors du chargement du pilote JDBC: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);//Affichage d'un message d'erreur dans la boîte de dialogue
            throw new RuntimeException("Impossible de charger le pilote JDBC", e);
        } catch (SQLException e) {//Si une erreur SQL se produit alors on affiche un message d'erreur et on lance une exception 
            logger.severe("Erreur lors de la connexion à la base de données: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Erreur de connexion à la base de données: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("Impossible de se connecter à la base de données", e);
        }
    }

    private void initializeStorageDirectory() {//Cette méthode initialise le répertoire de stockage des fichiers sur le serveur 
        storageDirectory = new File("server_storage");
        if (!storageDirectory.exists()) {//Si le répertoire de stockage n'existe pas alors on le crée
            if (storageDirectory.mkdir()) {//Si le répertoire de stockage est créé avec succès alors on affiche un message d'information
                logger.info("Répertoire de stockage créé : " + storageDirectory.getAbsolutePath());
            } else {//Sinon on affiche un message d'erreur et on lance une exception
                logger.severe("Impossible de créer le répertoire de stockage : " + storageDirectory.getAbsolutePath());
                throw new RuntimeException("Impossible de créer le répertoire de stockage");
            }
        } else {//Si le répertoire de stockage existe alors on affiche un message d'information
            logger.info("Répertoire de stockage trouvé : " + storageDirectory.getAbsolutePath());
        }
    }

    private String hashPassword(String password) {//Cette méthode hache le mot de passe en utilisant l'algorithme de hachage SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");//Création d'une instance de MessageDigest avec l'algorithme de hachage SHA-256 
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {//Parcours des octets du hachage pour les convertir en chaîne hexadécimale 
                hexString.append(String.format("%02x", b));//Conversion des octets en chaîne hexadécimale
            }

            return hexString.toString();//Retourne le mot de passe haché sous forme de chaîne hexadécimale
        } catch (NoSuchAlgorithmException e) {//Si l'algorithme de hachage n'est pas trouvé alors on affiche un message d'erreur et on lance une exception
            throw new RuntimeException("Erreur lors du hachage du mot de passe", e);
        }
    }

    public boolean authenticate(String login, String password) {//Cette méthode authentifie un utilisateur en vérifiant le login et le mot de passe dans la base de données
        String query = "SELECT password FROM user WHERE login = ?";//Requête SQL pour obtenir le mot de passe de l'utilisateur avec le login spécifié
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();// Exécution de la requête SQL pour obtenir le mot de passe de l'utilisateur

            if (resultSet.next()) {//Si un utilisateur est trouvé avec le login spécifié alors on vérifie le mot de passe
                String storedPassword = resultSet.getString("password");
                String hashedPassword = hashPassword(password);
                return storedPassword != null && storedPassword.equals(hashedPassword);//Si le mot de passe haché correspond au mot de passe stocké dans la base de données alors l'authentification est réussie (return true sinon return false)
            } else {//Sinon on affiche un message d'erreur et on retourne false
                logger.warning("Aucun utilisateur trouvé avec le login : " + login);
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de l'authentification: " + e.getMessage());
        }
        return false;//Retourne false si l'authentification échoue
    }

    public void start() {//Cette méthode démarre le serveur en écoutant les connexions entrantes sur le port spécifié et en créant un ClientHandler pour chaque connexion client 
        try {
            // Création de la fabrique de sockets SSL
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();//Création d'une fabrique de sockets SSL pour créer des SSLServerSockets
            serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);//Création d'un SSLServerSocket pour écouter les connexions entrantes sur le port spécifié
            running = true;
            logger.info("Serveur SSL démarré sur le port " + port);

            while (running) {//Boucle pour accepter les connexions clients et créer un ClientHandler pour chaque connexion client et l'exécuter dans le pool de threads
                // Accepter une nouvelle connexion et la caster en SSLSocket
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();//
                logger.info("Nouvelle connexion SSL: " + clientSocket.getInetAddress().getHostAddress());//Affichage de l'adresse IP du client qui s'est connecté

                // Créer un ClientHandler avec le SSLSocket
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);//Ajout du ClientHandler à la liste des clients connectés
                threadPool.execute(handler);// Exécution du ClientHandler dans le pool de threads pour gérer la connexion client de manière asynchrone
            }
        } catch (IOException e) {//Si une erreur d'entrée/sortie se produit alors on affiche un message d'erreur et on arrête le serveur 
            if (running) {
                logger.severe("Erreur de démarrage du serveur SSL: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    public void stop() {//Cette méthode arrête le serveur en fermant toutes les connexions clients, en arrêtant le pool de threads et en fermant le socket du serveur   
        running = false;

        // Fermeture de toutes les connexions clients
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();

        // Arrêt du pool de threads
        threadPool.shutdown();

        // Fermeture de la connexion à la base de données
        if (connection != null) {
            try {
                connection.close();
                logger.info("Connexion à la base de données fermée");
            } catch (SQLException e) {
                logger.severe("Erreur lors de la fermeture de la connexion à la base de données: " + e.getMessage());
            }
        }

        // Fermeture du socket du serveur
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.severe("Erreur lors de l'arrêt du serveur SSL: " + e.getMessage());
        }

        logger.info("Serveur SSL arrêté");
    }

    public void removeClient(ClientHandler client) {//Cette méthode supprime un ClientHandler de la liste des clients connectés
        clients.remove(client);
    }

    public List<ClientHandler> getClients() {//Cette méthode retourne la liste des clients connectés
        return Collections.unmodifiableList(clients);
    }

    
    public boolean fileExists(String fileName) {//Cette méthode vérifie si un fichier existe dans le répertoire de stockage des fichiers
        File file = new File(storageDirectory, fileName);
        return file.exists() && file.isFile();//Retourne true si le fichier existe et est un fichier, sinon retourne false
    }

    
    public String getFilePath(String fileName) {//Cette méthode retourne le chemin absolu d'un fichier dans le répertoire de stockage des fichiers
        return new File(storageDirectory, fileName).getAbsolutePath();
    }

    public static void main(String[] args) {//Méthode principale pour démarrer le serveur avec un port spécifié en argument
        int port = 9999; // Port par défaut si aucun port n'est spécifié
        if (args.length > 0) {//Si un port est spécifié en argument alors on l'utilise
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {//Si le port spécifié n'est pas un entier alors on affiche un message d'erreur et on utilise le port par défaut
                System.out.println("Port invalide, utilisation du port par défaut: 9999");
            }
        }

        // Configuration du contexte SSL
        System.setProperty("javax.net.ssl.keyStore", "keystore.jks");//Définition du fichier de clés pour le serveur SSL (keystore.jks)
        System.setProperty("javax.net.ssl.keyStorePassword", "hamid123");//Définition du mot de passe du fichier de clés pour le serveur SSL

        Server server = new Server(port);//Création d'une instance de serveur avec le port spécifié
        server.start();//Démarrage du serveur pour écouter les connexions entrantes
    }
}