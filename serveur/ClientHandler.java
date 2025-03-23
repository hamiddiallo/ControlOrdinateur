package serveur;

import java.io.*;
import javax.net.ssl.SSLSocket;
// import java.util.logging.*;
//cette classe permet de gérer les commandes des clients et de les exécuter
//elle implémente l'interface Runnable pour être exécutée dans un thread
//elle contient des méthodes pour gérer l'upload et le download de fichiers
//elle contient une méthode pour exécuter une commande système
public class ClientHandler implements Runnable {
    private SSLSocket clientSocket;//socket du client
    private Server server;//serveur auquel le client est connecté
    private boolean running;//indique si le client est connecté ou non
    private BufferedReader in;//flux d'entrée pour lire les messages du client 
    private PrintWriter out;//flux de sortie pour envoyer des messages au client 
    private String clientAddress;//adresse IP du client 
    private boolean authenticated;//indique si le client est authentifié ou non
    private String login;//login du client 

    // Constantes pour les commandes de transfert de fichiers
    private static final String UPLOAD_COMMAND = "UPLOAD";//commande d'upload de fichier pour envoyer un fichier au serveur
    private static final String DOWNLOAD_COMMAND = "DOWNLOAD";//commande de download de fichier pour télécharger un fichier du serveur
    public static final String END_OF_FILE = "EOF";//marqueur de fin de fichier 

    public ClientHandler(SSLSocket clientSocket, Server server) {//Ce constructeur prend en paramètre le socket du client et le serveur et permet d'initialiser les attributs de la classe
        this.clientSocket = clientSocket;
        this.server = server;
        this.running = true;
        this.clientAddress = clientSocket.getInetAddress().getHostAddress();//récupérer l'adresse IP du client
        this.authenticated = false;

        // Journalisation de la connexion
        ClientCommandLogger.logConnection(clientAddress);
    }

    public String getClientAddress() {//méthode pour récupérer l'adresse IP du client
        return clientAddress;
    }

    public String getLogin() {//méthode pour récupérer le login du client
        return login;
    }

    public void close() {//méthode pour fermer la connexion avec le client
        running = false;
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {//vérifier si le socket du client est ouvert et le fermer
                clientSocket.close();
            }
        } catch (IOException e) {
            ClientCommandLogger.logDisconnectionError(clientAddress, e.getMessage());//journaliser l'erreur de déconnexion
        }
    }

    @Override
    public void run() {//méthode run de l'interface Runnable, elle est appelée lorsqu'un thread est démarré pour exécuter le clientHandler elle gère les commandes des clients et permet de les exécuter . elle ass
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));// créer un flux d'entrée pour lire les messages du client
            out = new PrintWriter(clientSocket.getOutputStream(), true);// créer un flux de sortie pour envoyer des messages au client

            // Étape d'authentification
            String authMessage = in.readLine();
            if (authMessage != null && authMessage.startsWith(Server.AUTH_PREFIX)) {// vérifier si le message d'authentification est valide 
                String[] parts = authMessage.split(" ");// décomposer le message d'authentification en deux parties : le login et le mot de passe pour les stocker dans les attributs de la classe 
                if (parts.length == 3) {
                    String login = parts[1];
                    String password = parts[2];

                    if (server.authenticate(login, password)) {// vérifier si le login et le mot de passe sont valides 
                        this.login = login;
                        out.println(Server.AUTH_SUCCESS);// envoyer un message d'authentification réussie au client
                        authenticated = true;
                        ClientCommandLogger.logCommand(clientAddress, "Client authentifié: " + clientAddress + " (Login: " + login + ")");//journaliser l'authentification du client
                    } else {// si l'authentification échoue, envoyer un message d'erreur au client et fermer la connexion
                        out.println(Server.AUTH_FAILED);
                        ClientCommandLogger.logConnectionError(clientAddress, "Échec de l'authentification");
                        close();
                        return;
                    }
                } else {// si le message d'authentification est invalide, envoyer un message d'erreur au client et fermer la connexion
                    ClientCommandLogger.logConnectionError(clientAddress, "Format d'authentification invalide : " + authMessage);
                    out.println(Server.AUTH_FAILED);
                    close();
                    return;
                }
            } else {
                ClientCommandLogger.logConnectionError(clientAddress, "Message d'authentification manquant ou invalide : " + authMessage);
                out.println(Server.AUTH_FAILED);
                close();
                return;
            }

            // Boucle principale de traitement des commandes
            while (running && authenticated) {//tant que le client est connecté et authentifié, lire les commandes du client et les executer
                String message = in.readLine();
                if (message == null || message.equals(Server.DISCONNECT_MESSAGE)) {//si le message est null ou égal à la commande de déconnexion, sortir de la boucle
                    break;
                }

                if (message.startsWith(Server.COMMAND_PREFIX)) {//si le message est une commande, la traiter
                    String command = message.substring(Server.COMMAND_PREFIX.length());//extraire la commande du message
                    ClientCommandLogger.logCommand(clientAddress, command);// journaliser la commande du client 

                    if (command.startsWith(UPLOAD_COMMAND)) {//si la commande est d'upload de fichier, appeler la méthode handleFileUpload pour traiter l'upload du fichier 
                        handleFileUpload(command);
                    } else if (command.startsWith(DOWNLOAD_COMMAND)) {//si la commande est de download de fichier, appeler la méthode handleFileDownload pour traiter le download du fichier
                        handleFileDownload(command);
                    } else {//si la commande est une commande système, l'exécuter
                        try {
                            String result = executeCommand(command);//appeler la méthode executeCommand pour exécuter la commande
                            out.println(Server.RESULT_PREFIX + result);//envoyer le résultat de la commande au client
                        } catch (Exception e) {//en cas d'erreur lors de l'exécution de la commande, envoyer un message d'erreur au client
                            ClientCommandLogger.logCommandError(clientAddress, command, e.getMessage());
                            out.println(Server.ERROR_PREFIX + e.getMessage());
                        }
                        out.println("END_OF_RESPONSE");//envoyer un marqueur de fin de réponse
                    }
                }
            }
        } catch (IOException e) {//en cas d'erreur lors de la communication avec le client, journaliser l'erreur et fermer la connexion
            ClientCommandLogger.logConnectionError(clientAddress, "Erreur de communication: " + e.getMessage());
        } finally {//fermer les ressources et journaliser la déconnexion du client
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                ClientCommandLogger.logDisconnectionError(clientAddress, "Erreur lors de la fermeture des ressources: " + e.getMessage());
            }
            server.removeClient(this);
            ClientCommandLogger.logDisconnection(clientAddress);
        }
    }

    
    //Gère l'upload d'un fichier depuis le client vers le serveur
    //il prend en paramètre la commande d'upload
    //La commande doit être de la forme "UPLOAD nom_fichier"
    
    private void handleFileUpload(String command) {//méthode pour gérer l'upload de fichier depuis le client vers le serveur
        try {
            String[] parts = command.split(" ");//découper la commande en deux parties : la commande et le nom du fichier
            if (parts.length == 2) {//vérifier si la commande est valide
                String fileName = parts[1];
                File file = new File("server_storage/" + fileName);//créer un fichier pour stocker le fichier reçu

                // Indiquer au client que le serveur est prêt à recevoir le fichier
                ClientCommandLogger.logCommand(clientAddress, "Prêt à recevoir le fichier " + fileName);//journaliser la réception du fichier
                out.println(Server.RESULT_PREFIX + "Prêt à recevoir le fichier");//envoyer un message au client pour indiquer que le serveur est prêt à recevoir le fichier
                out.flush();//vider le flux de sortie

                try (FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = clientSocket.getInputStream()) {//créer un flux d'entrée pour lire les données du fichier
                    byte[] buffer = new byte[8192];//créer un tableau de bytes pour stocker les données du fichier. ce tableau est constitué de 8192 octets et constitue un tampon pour lire les données du fichier
                    int bytesRead;

                    // Lire les données du fichier
                    ClientCommandLogger.logCommand(clientAddress, "Début de la réception du fichier : " + fileName);
                    while ((bytesRead = is.read(buffer)) != -1) {//lire les données du fichier et les stocker dans le tableau buffer
                        // Vérifier si le marqueur de fin de fichier a été reçu
                        String data = new String(buffer, 0, bytesRead);
                        if (data.contains("END_OF_FILE")) {//si le marqueur de fin de fichier est reçu, sortir de la boucle
                            ClientCommandLogger.logCommand(clientAddress, "Marqueur de fin de fichier reçu");
                            break; // Sortir de la boucle
                        }
                        fos.write(buffer, 0, bytesRead);//écrire les données du fichier dans le fichier de destination
                    }

                    // Envoyer une confirmation de réception du fichier
                    ClientCommandLogger.logCommand(clientAddress, "Fin de la réception du fichier : " + fileName);
                    out.println(Server.RESULT_PREFIX + "Fichier reçu avec succès: " + fileName);
                    out.flush();
                }
            } else {//si la commande est invalide, envoyer un message d'erreur au client
                ClientCommandLogger.logCommandError(clientAddress, command, "Format de commande UPLOAD invalide");
                out.println(Server.ERROR_PREFIX + "Format de commande UPLOAD invalide");
            }
        } catch (IOException e) {//en cas d'erreur lors de la réception du fichier, envoyer un message d'erreur au client
            ClientCommandLogger.logCommandError(clientAddress, command, "Erreur lors de la réception du fichier : " + e.getMessage());
            out.println(Server.ERROR_PREFIX + "Erreur lors de la réception du fichier: " + e.getMessage());
        } finally {
            out.println("END_OF_RESPONSE");//envoyer un marqueur de fin de réponse au client
        }
    }

    
    //Gère le download d'un fichier depuis le serveur vers le client
    //il prend en paramètre la commande de download
    //La commande doit être de la forme "DOWNLOAD nom_fichier"
    private void handleFileDownload(String command) {//méthode pour gérer le download de fichier depuis le serveur vers le client
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            String[] parts = command.split(" ");//découper la commande en deux parties : la commande et le nom du fichier
            if (parts.length == 2) {
                String fileName = parts[1];//récupérer le nom du fichier
                File file = new File(fileName);
                if (file.exists() && file.isFile()) {//vérifier si le fichier existe et est un fichier simple c'est a dire pas un répertoire
                    // Initialiser os avant de l'utiliser
                    os = clientSocket.getOutputStream();//créer un flux de sortie pour envoyer le fichier au client

                    // Envoyer un message de début de transfert
                    ClientCommandLogger.logCommand(clientAddress, "Début du transfert du fichier : " + fileName);//journaliser le début du transfert du fichier
                    String startMessage = Server.RESULT_PREFIX + "Début du transfert du fichier: " + fileName + "\n";//envoyer un message au client pour indiquer le début du transfert du fichier
                    os.write(startMessage.getBytes());//écrire le message dans le flux de sortie 
                    os.flush();//vider le flux de sortie

                    // Envoyer le fichier
                    fis = new FileInputStream(file);//créer un flux d'entrée pour lire les données du fichier et les envoyer au client
                    byte[] buffer = new byte[8192];//créer un tableau de bytes pour stocker les données du fichier. ce tableau est constitué de 8192 octets et constitue un tampon pour lire les données du fichier
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {//lire les données du fichier et les stocker dans le tableau buffer 
                        os.write(buffer, 0, bytesRead);
                    }

                    // Envoyer un marqueur de fin de fichier
                    ClientCommandLogger.logCommand(clientAddress, "Fin du transfert du fichier : " + fileName);//journaliser la fin du transfert du fichier
                    String endMarker = "END_OF_FILE\n";//envoyer un marqueur de fin de fichier
                    os.write(endMarker.getBytes());//écrire le marqueur de fin de fichier dans le flux de sortie 
                    os.flush();
                } else {//si le fichier n'existe pas, envoyer un message d'erreur au client
                    // Initialiser os avant de l'utiliser
                    os = clientSocket.getOutputStream();

                    // Envoyer un message d'erreur si le fichier n'existe pas
                    ClientCommandLogger.logCommandError(clientAddress, command, "Fichier introuvable : " + fileName);
                    String errorMessage = Server.ERROR_PREFIX + "Fichier introuvable: " + fileName + "\n";
                    os.write(errorMessage.getBytes());
                    os.flush();
                }
            } else {//si la commande est invalide, envoyer un message d'erreur au client 
                // Initialiser os avant de l'utiliser
                os = clientSocket.getOutputStream();

                // Envoyer un message d'erreur si la commande est invalide
                ClientCommandLogger.logCommandError(clientAddress, command, "Format de commande DOWNLOAD invalide");
                String errorMessage = Server.ERROR_PREFIX + "Format de commande DOWNLOAD invalide\n";
                os.write(errorMessage.getBytes());
                os.flush();
            }
        } catch (IOException e) {//en cas d'erreur lors de l'envoi du fichier, envoyer un message d'erreur au client
            ClientCommandLogger.logCommandError(clientAddress, command, "Erreur lors de l'envoi du fichier : " + e.getMessage());
            try {
                // Envoyer un message d'erreur en cas d'exception
                if (os != null) {
                    String errorMessage = Server.ERROR_PREFIX + "Erreur lors de l'envoi du fichier: " + e.getMessage() + "\n";
                    os.write(errorMessage.getBytes());
                    os.flush();
                }
            } catch (IOException ex) {//en cas d'erreur lors de l'envoi du message d'erreur, journaliser l'erreur
                ClientCommandLogger.logCommandError(clientAddress, command, "Erreur lors de l'envoi du message d'erreur : " + ex.getMessage());
            }
        } finally {
            // Fermer les ressources
            try {
                if (fis != null) fis.close();
                if (os != null) os.close();
            } catch (IOException e) {//en cas d'erreur lors de la fermeture des ressources, journaliser l'erreur
                ClientCommandLogger.logCommandError(clientAddress, command, "Erreur lors de la fermeture des ressources : " + e.getMessage());
            }
        }
    }

    
     // Exécute les commandes systèmes
     // Prend en paramètre la commande à exécuter et Retourne le résultat de la commande
    private String executeCommand(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();//créer un processBuilder pour exécuter la commande système
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {//vérifier si le système d'exploitation est Windows
            processBuilder.command("cmd.exe", "/c", command);//si c'est le cas, exécuter la commande dans un shell Windows
        } else {//sinon, exécuter la commande dans un shell Unix (Linux, MacOs, etc.)
            processBuilder.command("/bin/sh", "-c", command);
        }

        Process process = processBuilder.start();//démarrer le processus pour exécuter la commande système
        StringBuilder output = new StringBuilder();//créer un StringBuilder pour stocker la sortie de la commande

        try (BufferedReader reader = new BufferedReader(//créer un BufferedReader pour lire la sortie standard de la commande afin de la stocker dans le StringBuilder pour afficher le résultat de la commande
                new InputStreamReader(process.getInputStream()))) {//créer un flux d'entrée pour lire la sortie standard de la commande 
            String line;
            while ((line = reader.readLine()) != null) {//lire chaque ligne de la sortie standard de la commande et l'ajouter au StringBuilder
                output.append(line).append("\n");
            }
        }

        try (BufferedReader reader = new BufferedReader( //créer un BufferedReader pour lire la sortie d'erreur de la commande afin de la stocker dans le StringBuilder pour afficher les erreurs de la commande
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {//lire chaque ligne de la sortie d'erreur de la commande et l'ajouter au StringBuilder
                output.append("ERROR: ").append(line).append("\n");
            }
        }

        try {
            process.waitFor();//attendre que le processus se termine
        } catch (InterruptedException e) {//en cas d'interruption du thread, journaliser l'erreur et lancer une IOException
            Thread.currentThread().interrupt();
            throw new IOException("Commande interrompue: " + e.getMessage());
        }

        return output.toString();//retourner le résultat de la commande
    }
}