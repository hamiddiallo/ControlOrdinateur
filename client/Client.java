package client;

import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.logging.*;
//cette classe permet de créer un client qui se connecte à un serveur distant et exécute des commandes à distance.
//Il peut également envoyer et recevoir des fichiers du serveur.
//Le client utilise SSLSocket pour établir une connexion sécurisée avec le serveur.
//Le client est capable de gérer les commandes de transfert de fichiers UPLOAD et DOWNLOAD.
public class Client {  
    private String serverAddress;//Adresse IP du serveur
    private int serverPort;// Port du serveur
    private String login;// Login de l'utilisateur
    private String password;// Mot de passe de l'utilisateur
    private SSLSocket socket; // Utilisation de SSLSocket pour une connexion sécurisée
    private Logger logger;// Utilisation de java.util.logging.Logger pour les messages de journalisation
    private BufferedReader in;// Flux d'entrée pour lire les données du serveur
    private PrintWriter out;// Flux de sortie pour envoyer des données au serveur

    // Constantes du protocole de communication
    public static final String COMMAND_PREFIX = "CMD:";
    public static final String RESULT_PREFIX = "RESULT:";
    public static final String ERROR_PREFIX = "ERROR:";
    public static final String DISCONNECT_MESSAGE = "DISCONNECT";
    public static final String AUTH_PREFIX = "AUTH:";
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String END_OF_RESPONSE = "END_OF_RESPONSE";

    // Constantes pour les commandes de transfert de fichiers
    public static final String UPLOAD_COMMAND = "UPLOAD";
    public static final String DOWNLOAD_COMMAND = "DOWNLOAD";

    /**
     * Constructeur du client
     * @param serverAddress Adresse IP du serveur
     * @param serverPort Port du serveur
     * @param login Login de l'utilisateur
     * @param password Mot de passe de l'utilisateur
     */
    public Client(String serverAddress, int serverPort, String login, String password) {// Constructeur pour initialiser les attributs du client
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.login = login;
        this.password = password;
        this.logger = Logger.getLogger(Client.class.getName());
    }

    // Établit une connexion avec le serveur et authentifie l'utilisateur
    // return true si la connexion et l'authentification réussissent, sinon false
     
    public boolean connect() {
        try {
            // Création de la fabrique de sockets SSL
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();// Créer une fabrique de sockets SSL
            socket = (SSLSocket) sslSocketFactory.createSocket(serverAddress, serverPort);//  Créer un socket SSL pour se connecter au serveur distant 

            // Initialisation des flux d'entrée/sortie
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer les informations d'authentification
            String authMessage = AUTH_PREFIX + " " + login + " " + password;
            sendMessage(authMessage);
            logger.info("Message d'authentification envoyé : " + authMessage);

            // Lire la réponse du serveur
            String response = in.readLine();
            logger.info("Réponse du serveur : " + response);

            if (AUTH_SUCCESS.equals(response)) {
                logger.info("Connecté au serveur " + serverAddress + ":" + serverPort);
                return true;
            } else {
                logger.warning("Échec de l'authentification: " + response);
                disconnect();
                return false;
            }
        } catch (IOException e) {
            logger.severe("Impossible de se connecter au serveur: " + e.getMessage());
            return false;
        }
    }

   //Ferme la connexion avec le serveur
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {// Vérifier si le socket est ouvert avant de le fermer
                sendMessage(DISCONNECT_MESSAGE);// Envoyer un message de déconnexion au serveur
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            logger.warning("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

   //Envoie un message au serveur
    private void sendMessage(String message) {
        out.println(message);
    }

    //Reçoit un message multi-lignes du serveur jusqu'à ce qu'il rencontre END_OF_RESPONSE
    private String receiveMessage() throws IOException {
        StringBuilder response = new StringBuilder();// Créer un StringBuilder pour stocker la réponse du serveur
        String line;
        while ((line = in.readLine()) != null && !line.equals(END_OF_RESPONSE)) {// Lire les lignes du serveur jusqu'à ce qu'il rencontre END_OF_RESPONSE 
            response.append(line).append("\n");
        }
        return response.toString().trim();
    }

   //Permet d'Exécuter une commande sur le serveur distant
    public String executeCommand(String command) {// Méthode pour exécuter une commande sur le serveur distant et recevoir la réponse du serveur 
        try {
            sendMessage(COMMAND_PREFIX + command);
            String response = receiveMessage();// Recevoir la réponse du serveur

            if (response == null) {
                return "Erreur: Aucune réponse du serveur";
            }

            if (response.startsWith(RESULT_PREFIX)) {// Si la réponse commence par RESULT_PREFIX, cela signifie que la commande a été exécutée avec succès sur le serveur
                return response.substring(RESULT_PREFIX.length());
            } else if (response.startsWith(ERROR_PREFIX)) {// Si la réponse commence par ERROR_PREFIX, cela signifie qu'il y a eu une erreur lors de l'exécution de la commande sur le serveur
                return "Erreur: " + response.substring(ERROR_PREFIX.length());
            } else {// Si la réponse n'est pas conforme au protocole, cela signifie que le serveur a renvoyé une réponse inattendue comme exemple une erreur de communication ou une erreur interne du serveur 
                return "Réponse inattendue: " + response;
            }
        } catch (IOException e) {
            logger.severe("Erreur de communication: " + e.getMessage());
            return "Erreur de communication: " + e.getMessage();
        }
    }

    //Permet d'Envoyer un fichier au serveur (upload)
    public String uploadFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {// Vérifier si le fichier existe et est un fichier pour pouvoir l'envoyer
                return "Erreur: Fichier introuvable ou invalide";
            }

            // Envoyer la commande UPLOAD
            sendMessage(COMMAND_PREFIX + UPLOAD_COMMAND + " " + file.getName());// Envoyer la commande UPLOAD au serveur avec le nom du fichier à envoyer

            // Attendre la confirmation du serveur
            String serverResponse = in.readLine();
            if (!serverResponse.equals(RESULT_PREFIX + "Prêt à recevoir le fichier")) {// Si la réponse du serveur n'est pas "Prêt à recevoir le fichier", cela signifie qu'il y a eu une erreur lors de la préparation du serveur à recevoir le fichier alors on retourne un message d'erreur
                return "Erreur: Le serveur n'est pas prêt à recevoir le fichier";
            }

            // Envoyer le fichier
            byte[] buffer = new byte[8192];// Tampon pour stocker les données du fichier à envoyer 
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {// Lire les données du fichier dans le tampon jusqu'à la fin du fichier 
                socket.getOutputStream().write(buffer, 0, bytesRead);
            }

            // Envoyer un marqueur de fin de fichier
            out.println("END_OF_FILE");
            out.flush();

            // Attendre la confirmation du serveur
            String confirmation = in.readLine();
            if (confirmation.startsWith(RESULT_PREFIX)) {// Si la confirmation commence par RESULT_PREFIX, cela signifie que le fichier a été téléchargé avec succès donc on retourne le nom du fichier
                return confirmation.substring(RESULT_PREFIX.length());
            } else {// Sinon, cela signifie qu'il y a eu une erreur lors de l'envoi du fichier, donc on retourne un message d'erreur
                return "Erreur: " + confirmation;
            }
        } catch (IOException e) {
            logger.severe("Erreur lors de l'envoi du fichier: " + e.getMessage());
            return "Erreur lors de l'envoi du fichier: " + e.getMessage();
        }
    }

    
    // permet de  Télécharger un fichier depuis le serveur (download)
     
    public String downloadFile(String fileName, String destinationPath) {
        try {
            // Envoyer la commande DOWNLOAD
            sendMessage(COMMAND_PREFIX + DOWNLOAD_COMMAND + " " + fileName);// Envoyer la commande DOWNLOAD au serveur avec le nom du fichier à télécharger 

            // Recevoir le fichier
            File file = new File(destinationPath, fileName);// Créer un fichier dans le répertoire de destination avec le nom du fichier téléchargé
            try (FileOutputStream fos = new FileOutputStream(file)) {// Créer un flux de sortie pour écrire le fichier
                byte[] buffer = new byte[8192];// Tampon pour stocker les données du fichier téléchargé  ce tampon est de 8 Ko . ainsi, nous lisons 8 Ko de données à la fois à partir du socket
                int bytesRead;
                while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {// Lire les données du socket dans le tampon jusqu'à ce que la fin du fichier soit atteinte 
                    String data = new String(buffer, 0, bytesRead);// Convertir les données lues en chaîne de caractères pour vérifier si nous avons atteint la fin du fichier 
                    if (data.contains("END_OF_FILE")) {
                        fos.write(buffer, 0, bytesRead - "END_OF_FILE\n".length());// Écrire les données dans le fichier sans le marqueur de fin de fichier 
                        break;// Sortir de la boucle si nous avons atteint la fin du fichier 
                    }
                    fos.write(buffer, 0, bytesRead);// Écrire les données dans le fichier 
                }
            }

            return "Fichier téléchargé avec succès: " + file.getAbsolutePath();// Retourner le chemin absolu du fichier téléchargé
        } catch (IOException e) {
            logger.severe("Erreur lors du téléchargement du fichier: " + e.getMessage());
            return "Erreur lors du téléchargement du fichier: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";// Adresse par défaut
        int serverPort = 9999;// Port par défaut

        // Parsing des arguments
        if (args.length >= 1) {// Si l'adresse IP est spécifiée
            serverAddress = args[0];
        }
        if (args.length >= 2) {// Si le port est spécifié
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Port invalide, utilisation du port par défaut: 9999");
            }
        }

        // Configuration du contexte SSL
        System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks");// Fichier de confiance
        System.setProperty("javax.net.ssl.trustStorePassword", "hamid123");// Mot de passe du truststore

        // Demander le login et le mot de passe
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));// Lecture de la console
        try {
            System.out.print("Login: ");
            String login = consoleReader.readLine();// Lire le login
            System.out.print("Mot de passe: ");
            String password = consoleReader.readLine();// Lire le mot de passe

            Client client = new Client(serverAddress, serverPort, login, password);// Création du client pour se connecter au serveur distant
            if (client.connect()) {
                System.out.println("Client de contrôle à distance. Tapez 'exit' pour quitter.");

                while (true) {
                    System.out.print("> ");
                    String input = consoleReader.readLine();

                    if (input == null || input.equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Gestion des commandes de transfert de fichiers
                    if (input.startsWith(UPLOAD_COMMAND)) {// Si la commande est UPLOAD
                        String[] parts = input.split(" ");
                        if (parts.length == 2) {// Si le chemin du fichier est spécifié        
                            String filePath = parts[1];//   Récupérer le chemin du fichier
                            String result = client.uploadFile(filePath);// Envoi du fichier     
                            System.out.println(result);
                        } else {// Si le chemin du fichier n'est pas spécifié
                            System.out.println("Usage: UPLOAD <chemin_du_fichier>");// Afficher un message d'erreur avec l'utilisation correcte
                        }
                    } else if (input.startsWith(DOWNLOAD_COMMAND)) {// Si la commande est DOWNLOAD
                        String[] parts = input.split(" ");// Séparer les parties de la commande
                        if (parts.length == 3) {// Si le nom du fichier et le chemin de destination sont spécifiés
                            String fileName = parts[1];
                            String destinationPath = parts[2];
                            String result = client.downloadFile(fileName, destinationPath);
                            System.out.println(result);
                        } else {// Si le nom du fichier et le chemin de destination ne sont pas spécifiés
                            System.out.println("Usage: DOWNLOAD <nom_du_fichier> <chemin_de_destination>");// Afficher un message d'erreur avec l'utilisation correcte
                        }
                    } else {// Si la commande n'est pas un transfert de fichier
                        String result = client.executeCommand(input);// Exécuter la commande sur le serveur 
                        System.out.println(result);
                    }
                }
            } else {
                System.out.println("Échec de la connexion ou de l'authentification.");
            }
        } catch (IOException e) {
            System.out.println("Erreur de lecture console: " + e.getMessage());
        } finally {
            try {
                consoleReader.close();
            } catch (IOException e) {
                System.out.println("Erreur lors de la fermeture du client: " + e.getMessage());
            }
        }
    }
}