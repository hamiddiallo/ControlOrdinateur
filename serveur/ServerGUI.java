package serveur;

import java.util.logging.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
//Cette classe est une interface graphique pour le serveur. Elle affiche un journal des événements du serveur, une liste des clients connectés et un bouton pour démarrer/arrêter le serveur.
public class ServerGUI extends JFrame {
    private Server server;//Serveur de contrôle à distance
    private JTextArea logArea;//Zone de texte pour afficher les logs
    private JButton startStopButton;//Bouton pour démarrer/arrêter le serveur
    private JList<String> clientsList;//Liste des clients connectés (adresse IP)
    private DefaultListModel<String> clientsModel;//Modèle pour la liste des clients (adresse IP + login)
    private int port;// Port du serveur
    private boolean serverRunning;//Indique si le serveur est en cours d'exécution
    private Thread updateClientsThread; // Thread pour mettre à jour la liste des clients

    
    public ServerGUI() {//Constructeur de la classe , il initialise l'interface graphique du serveur et configure le logger pour rediriger les logs vers l'interface graphique
        super("Serveur de Contrôle à Distance");
        this.port = 9999; // Port par défaut
        this.serverRunning = false;// Le serveur n'est pas en cours d'exécution par défaut

        // Initialisation de l'interface
        setSize(800, 600);//Taille de la fenêtre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// Fermer l'application quand la fenêtre est fermée par l'utilisateur

        // Panneau principal avec BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));//Création d'un panneau principal avec une bordure de 10 pixels de chaque côté
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));//Ajout d'une bordure vide autour du panneau principal pour l'espacement des composants 

        // Zone de log
        logArea = new JTextArea();//Création d'une zone de texte pour afficher les logs du serveur 
        logArea.setEditable(false);//Désactiver l'édition de la zone de texte pour empêcher l'utilisateur de modifier les logs 
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));// Définir la police de caractères de la zone de texte à monospace pour une meilleure lisibilité
        JScrollPane logScroll = new JScrollPane(logArea);//Ajout de la zone de texte à un JScrollPane pour permettre le défilement des logs 

        // Titre de la zone de log
        JPanel logPanel = new JPanel(new BorderLayout(0, 5));
        logPanel.add(new JLabel("Journal du serveur:"), BorderLayout.NORTH);//Ajout d'un label pour le titre de la zone de texte avec un espacement de 5 pixels en bas
        logPanel.add(logScroll, BorderLayout.CENTER);//Ajout de la zone de texte à un panneau avec un espacement de 5 pixels en bas

        mainPanel.add(logPanel, BorderLayout.CENTER);

        // Liste des clients
        clientsModel = new DefaultListModel<>();//Création d'un modèle par défaut pour la liste des clients pour stocker les adresses IP des clients connectés 
        clientsList = new JList<>(clientsModel);//Création d'une liste avec le modèle des clients pour afficher les adresses IP des clients connectés et leur login
        JScrollPane clientsScroll = new JScrollPane(clientsList);//Ajout de la liste des clients à un JScrollPane pour permettre le défilement des clients connectés 

        // Personnalisation de l'affichage des clients
        clientsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = (String) value;
                if (text.contains("(")) {//Vérifier si le texte contient un login
                    if (text.contains("Non authentifié")) {
                        setForeground(Color.RED); // Rouge pour les clients non authentifiés
                    } else {
                        setForeground(Color.BLUE); // Bleu pour les clients authentifiés
                    }
                } else {
                    setForeground(Color.BLACK); // Noir par défaut
                }
                return this;
            }
        });

        // Panneau des clients avec BorderLayout pour l'espacement des composants 
        JPanel clientsPanel = new JPanel(new BorderLayout(0, 5));
        clientsPanel.add(new JLabel("Clients connectés:"), BorderLayout.NORTH);
        clientsPanel.add(clientsScroll, BorderLayout.CENTER);
        clientsPanel.setPreferredSize(new Dimension(200, 0));

        mainPanel.add(clientsPanel, BorderLayout.EAST);

        // Panneau de contrôle avec un FlowLayout pour aligner les composants à gauche
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel portLabel = new JLabel("Port:");
        JTextField portField = new JTextField(String.valueOf(port), 5);
        startStopButton = new JButton("Démarrer");

        controlPanel.add(portLabel);//Ajout d'un label pour le champ de texte du port
        controlPanel.add(portField);//Ajout d'un champ de texte pour saisir le port du serveur
        controlPanel.add(startStopButton);//Ajout d'un bouton pour démarrer/arrêter le serveur

        mainPanel.add(controlPanel, BorderLayout.NORTH);//Ajout du panneau de contrôle en haut du panneau principal 

        // Ajout du panneau principal à la fenêtre principale 
        add(mainPanel);

        // Gestionnaire d'événements
        startStopButton.addActionListener(e -> {//Ajout d'un gestionnaire d'événements pour le bouton de démarrage/arrêt du serveur
            if (!serverRunning) {//Vérifier si le serveur n'est pas en cours d'exécution alors démarrer le serveur 
                try {
                    port = Integer.parseInt(portField.getText());
                    startServer();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Port invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } else {//Si le serveur est en cours d'exécution alors arrêter le serveur 
                stopServer();
            }
        });

        // Configuration du logger
        configureLogger();

        // Gestionnaire de fermeture
        addWindowListener(new WindowAdapter() {//Ajout d'un gestionnaire d'événements pour la fermeture de la fenêtre
            @Override
            public void windowClosing(WindowEvent e) {//Arrêter le serveur lors de la fermeture de la fenêtre
                if (serverRunning) {
                    stopServer();
                }
            }
        });

        setVisible(true);//Rendre la fenêtre visible 
    }

   
     //Configure le logger pour rediriger les logs vers l'interface graphique
    private void configureLogger() { 
        Logger clientCommandLogger = Logger.getLogger(ClientCommandLogger.class.getName());

        // Supprimer les handlers existants pour éviter les doublons
        Handler[] clientCommandHandlers = clientCommandLogger.getHandlers();
        for (Handler handler : clientCommandHandlers) {
            clientCommandLogger.removeHandler(handler);
        }

        // Créer un handler personnalisé pour rediriger les logs vers la JTextArea
        Handler textAreaHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {//Publier un enregistrement de log dans la JTextArea
                SwingUtilities.invokeLater(() -> {
                    logArea.append(record.getLevel() + ": " + record.getMessage() + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength()); // Faire défiler vers le bas
                });
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        // Configurer le niveau de log et ajouter le handler
        textAreaHandler.setLevel(Level.ALL);//Définir le niveau de log du handler à ALL pour afficher tous les logs
        clientCommandLogger.addHandler(textAreaHandler);//Ajouter le handler personnalisé au logger pour rediriger les logs vers la JTextArea
        clientCommandLogger.setLevel(Level.ALL);
    }

    
    private void startServer() {// cette méthode démarre le serveur sur le port spécifié et met à jour l'interface graphique en conséquence
        try {
            configureLogger(); // Configurer le logger avant de démarrer le serveur

            // Configuration du contexte SSL
            System.setProperty("javax.net.ssl.keyStore", "keystore.jks"); // Chemin vers le keystore
            System.setProperty("javax.net.ssl.keyStorePassword", "hamid123"); // Mot de passe du keystore

            server = new Server(port);
            serverRunning = true;
            startStopButton.setText("Arrêter");

            // Démarrage du serveur dans un thread séparé pour ne pas bloquer l'interface graphique 
            new Thread(() -> {
                server.start();
                // Mise à jour de l'interface quand le serveur s'arrête
                SwingUtilities.invokeLater(() -> {
                    serverRunning = false;
                    startStopButton.setText("Démarrer");
                    clientsModel.clear();
                });
            }).start();

            // Thread pour mettre à jour la liste des clients
            updateClientsThread = new Thread(() -> {
                while (serverRunning) {
                    updateClientsList();
                    try {
                        Thread.sleep(2000); // Mise à jour toutes les 2 secondes 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            updateClientsThread.start();
        } catch (Exception e) {//Gestion des exceptions lors du démarrage du serveur
            JOptionPane.showMessageDialog(this, "Erreur lors du démarrage du serveur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            serverRunning = false;
            startStopButton.setText("Démarrer");
        }
    }

    
    private void stopServer() {// cette méthode arrête le serveur et met à jour l'interface graphique en conséquence
        if (server != null) {
            server.stop();
            serverRunning = false;
            startStopButton.setText("Démarrer");
            clientsModel.clear();

            // Arrêt du thread de mise à jour des clients
            if (updateClientsThread != null) {
                updateClientsThread.interrupt();
            }
        }
    }

   
    private void updateClientsList() {// cette méthode met à jour la liste des clients connectés en récupérant les adresses IP des clients à partir du serveur et en les affichant dans l'interface graphique 
        if (server != null && serverRunning) {
            List<ClientHandler> clients = server.getClients();
            Set<String> currentAddresses = new HashSet<>();

            // Collecte des adresses actuelles
            for (ClientHandler client : clients) {
                currentAddresses.add(client.getClientAddress());
            }

            // Mise à jour de l'interface
            SwingUtilities.invokeLater(() -> {
                // Suppression des clients déconnectés
                for (int i = clientsModel.size() - 1; i >= 0; i--) {
                    String address = clientsModel.getElementAt(i);
                    if (!currentAddresses.contains(address)) {
                        clientsModel.removeElementAt(i);//Supprimer les clients déconnectés de la liste des clients
                    }
                }

                // Ajout des nouveaux clients
                for (ClientHandler client : clients) {
                    String address = client.getClientAddress();
                    String login = client.getLogin();
                    String displayText = address + (login != null ? " (" + login + ")" : " (Non authentifié)");
                    if (!contains(clientsModel, displayText)) {
                        clientsModel.addElement(displayText);//Ajouter les nouveaux clients à la liste des clients
                    }
                }
            });
        }
    }

    
    private boolean contains(DefaultListModel<String> model, String address) {// cette méthode vérifie si une adresse IP est déjà présente dans la liste des clients connectés pour éviter les doublons
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(address)) {
                return true;
            }
        }
        return false;
    }

   //Méthode principale pour démarrer l'application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI());
    }
}