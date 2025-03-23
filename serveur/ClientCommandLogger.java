package serveur;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class ClientCommandLogger {
    private static final Logger logger = Logger.getLogger(ClientCommandLogger.class.getName());

    static {
        try {
            // Créer le fichier journal.log s'il n'existe pas
            File logFile = new File("journal.log");
            if (!logFile.exists()) {// Si le fichier n'existe pas, le créer
                logFile.createNewFile();
            }

            // Configurer le FileHandler pour écrire dans le fichier journal.log
            FileHandler fileHandler = new FileHandler("journal.log", true); // true pour ajouter au fichier existant
            fileHandler.setFormatter(new SimpleFormatter()); // Format simple pour les logs
            fileHandler.setLevel(Level.ALL); // Niveau de log

            // Ajouter le FileHandler au Logger
            logger.addHandler(fileHandler);

            // Ajouter un ConsoleHandler pour afficher les logs dans la console
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            logger.addHandler(consoleHandler);

            // Configurer le niveau de log global
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Erreur lors de la configuration du Logger : " + e.getMessage());
        }
    }

   //Log une commande client.
    public static void logCommand(String clientAddress, String command) {
        logger.info("Commande reçue de " + clientAddress + ": " + command);
    }

    //Log la connexion d'un client.
    public static void logConnection(String clientAddress) {
        logger.info("Client connecté: " + clientAddress);
    }

   //Log la déconnexion d'un client.
    public static void logDisconnection(String clientAddress) {
        logger.info("Client déconnecté: " + clientAddress);
    }

    //Log une erreur de connexion.
    public static void logConnectionError(String clientAddress, String errorMessage) {
        logger.severe("Erreur de connexion du client " + clientAddress + ": " + errorMessage);
    }

    //Log une erreur de déconnexion.
    public static void logDisconnectionError(String clientAddress, String errorMessage) {
        logger.severe("Erreur de déconnexion du client " + clientAddress + ": " + errorMessage);
    }

    
    // Log une erreur d'exécution de commande.
    
    public static void logCommandError(String clientAddress, String command, String errorMessage) {
        logger.severe("Erreur d'exécution de commande pour le client " + clientAddress + ": " + command + " -> " + errorMessage);
    }
}