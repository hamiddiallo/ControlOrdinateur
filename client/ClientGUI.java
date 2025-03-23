package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.border.EmptyBorder;


 // Interface graphique pour le client: Cette classe  Permet la connexion au serveur et l'envoi de commandes via une Interface Graphique
public class ClientGUI extends JFrame {
    private Client client;  // Client pour la connexion au serveur
    private JTextField serverField; // Champ pour le serveur
    private JTextField portField;   // Champ pour le port
    private JTextField loginField; // Champ pour le login
    private JPasswordField passwordField; // Champ pour le mot de passe
    private JButton connectButton;  // Bouton pour la connexion
    private JTextField commandField;    // Champ pour la commande
    private JButton sendButton; // Bouton pour envoyer la commande
    private JTextArea resultArea;   // Zone de résultat
    private JList<String> historyList;  // Liste d'historique
    private DefaultListModel<String> historyModel;  // Modèle pour l'historique
    private JButton uploadButton;   // Bouton pour l'upload
    private JButton downloadButton; // Bouton pour le download

    
    public ClientGUI() {
        super("Client de Contrôle à Distance");
    
        // Initialisation de l'interface
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
        // Panneau principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));//Le panneau principal est créé avec un BorderLayout et un espacement de 10 pixels.
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));//La bordure du panneau principal est définie pour un espacement de 10 pixels.
    
        // Panneau de connexion (Serveur, Port, Login, Mot de passe)
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); // On Utilise FlowLayout pour aligner horizontalement
        JLabel serverLabel = new JLabel("Serveur:");
        serverField = new JTextField("localhost", 10);
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("9999", 5);
        JLabel loginLabel = new JLabel("Login:");
        loginField = new JTextField(10);
        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordField = new JPasswordField(10);
    
        // Ajout des composants au panneau de connexion
        connectionPanel.add(serverLabel);
        connectionPanel.add(serverField);
        connectionPanel.add(portLabel);
        connectionPanel.add(portField);
        connectionPanel.add(loginLabel);
        connectionPanel.add(loginField);
        connectionPanel.add(passwordLabel);
        connectionPanel.add(passwordField);
    
        // Boutons (Connecter, Upload, Download)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); // on utilise FlowLayout pour aligner horizontalement
        connectButton = new JButton("Connecter");
        uploadButton = new JButton("Upload");
        downloadButton = new JButton("Download");
        uploadButton.setEnabled(false); // Désactivé par défaut
        downloadButton.setEnabled(false); // Désactivé par défaut
    
        // Ajout des boutons au panneau de boutons
        buttonPanel.add(connectButton);
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
    
        // Ajout des panneaux de connexion et de boutons au panneau principal
        JPanel topPanel = new JPanel(new BorderLayout()); // Panneau pour regrouper connexion et boutons
        topPanel.add(connectionPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
    
        mainPanel.add(topPanel, BorderLayout.NORTH);
    
        // Zone de résultat
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane resultScroll = new JScrollPane(resultArea);
        mainPanel.add(resultScroll, BorderLayout.CENTER);
    
        // Panneau de commande
        JPanel commandPanel = new JPanel(new BorderLayout(5, 0));//Le panneau de commande est créé pour regrouper le champ de texte et le bouton "Envoyer".
        commandField = new JTextField();
        sendButton = new JButton("Envoyer");
        sendButton.setEnabled(false);
    
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);//  Le champ de texte et le bouton "Envoyer" sont ajoutés au panneau de commande.
    
        // Panneau d'historique
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(150, 50));//La taille du panneau d'historique est définie pour afficher un nombre limité d'éléments.
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("Historique:"), BorderLayout.NORTH);//Un label "Historique" est ajouté au panneau d'historique.
        historyPanel.add(historyScroll, BorderLayout.CENTER);
    
        // Panneau sud (combinaison de commande et historique)
        JPanel southPanel = new JPanel(new BorderLayout(10, 0));//Le panneau sud est créé pour regrouper le panneau de commande et le panneau d'historique.
        southPanel.add(commandPanel, BorderLayout.CENTER);
        southPanel.add(historyPanel, BorderLayout.EAST);
    
        mainPanel.add(southPanel, BorderLayout.SOUTH);
    
        // Ajout du panneau principal
        add(mainPanel);
    
        // Gestionnaire d'événements pour le bouton "Connecter"
        connectButton.addActionListener(e -> {//
            if (client == null) {
                connect(); // Si le client n'est pas connecté, appeler la méthode connect()
            } else {
                disconnect(); // Si le client est déjà connecté, appeler la méthode disconnect()
            }
        });
    
        // Gestionnaire d'événements pour le bouton "Envoyer"
        sendButton.addActionListener(e -> sendCommand());//Lorsque le bouton est cliqué, la méthode sendCommand() est appelée pour envoyer la commande saisie dans le champ de texte.
    
        commandField.addActionListener(e -> sendCommand());//Lorsque la touche "Entrée" est pressée dans le champ de texte, la méthode sendCommand() est appelée pour envoyer la commande saisie.
    
        // Gestionnaire d'événements pour la liste d'historique (double-clic)
        historyList.addMouseListener(new MouseAdapter() {//Lorsqu'un double-clic est effectué sur un élément de la liste d'historique, la commande correspondante est insérée dans le champ de texte.
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Vérifie si c'est un double-clic
                    String selectedCommand = historyList.getSelectedValue(); // Récupère la commande sélectionnée
                    if (selectedCommand != null) {
                        commandField.setText(selectedCommand); // Insère la commande dans le champ de texte
                        commandField.requestFocus(); // Donne le focus au champ de texte
                    }
                }
            }
        });
    
        uploadButton.addActionListener(e -> uploadFile());//Lorsque le bouton "Upload" est cliqué, la méthode uploadFile() est appelée pour gérer l'upload d'un fichier.
        downloadButton.addActionListener(e -> downloadFile());// Lorsque le bouton "Download" est cliqué, la méthode downloadFile() est appelée pour gérer le download d'un fichier.
    
        // Gestion de la fermeture
        addWindowListener(new WindowAdapter() {// Lorsque la fenêtre est fermée, la connexion est fermée si elle est ouverte.
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    disconnect();//Si le client est connecté, la méthode disconnect() est appelée pour fermer la connexion.
                }
            }
        });
    
        setVisible(true);
    }
    /**
     * Établit une connexion avec le serveur
     */
    private void connect() {//La méthode connect() est appelée pour établir une connexion avec le serveur.
        try {
            String serverAddress = serverField.getText();
            int serverPort = Integer.parseInt(portField.getText());
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            // Configuration du contexte SSL
            System.setProperty("javax.net.ssl.trustStore", "client-truststore.jks"); // Chemin vers le truststore
            System.setProperty("javax.net.ssl.trustStorePassword", "hamid123"); // Mot de passe du truststore

            client = new Client(serverAddress, serverPort, login, password);
            if (client.connect()) {//Si la connexion est établie avec succès, le client est initialisé et les boutons sont activés.
                connectButton.setText("Déconnecter");
                sendButton.setEnabled(true);
                uploadButton.setEnabled(true);
                downloadButton.setEnabled(true);
                resultArea.append("Connecté au serveur " + serverAddress + ":" + serverPort + "\n");
                commandField.requestFocus();//Le focus est donné au champ de texte pour permettre à l'utilisateur de saisir une commande.
            } else {//Si la connexion échoue ou si l'authentification est invalide, le client est défini sur null et un message d'erreur est affiché.
                client = null;
                resultArea.append("Échec de la connexion ou authentification invalide\n");
            }
        } catch (NumberFormatException e) {//Si le port n'est pas un nombre valide, un message d'erreur est affiché.
            resultArea.append("Port invalide\n");
        }
    }

    /**
     * Ferme la connexion avec le serveur
     */
    private void disconnect() {//La méthode disconnect() est appelée pour fermer la connexion avec le serveur.
        if (client != null) {//Si le client est connecté, la connexion est fermée, le client est défini sur null et les boutons sont désactivés.
            client.disconnect();
            client = null;
            connectButton.setText("Connecter");
            sendButton.setEnabled(false);
            uploadButton.setEnabled(false);
            downloadButton.setEnabled(false);
            resultArea.append("Déconnecté du serveur\n");
        }
    }

    /**
     * Envoie une commande au serveur
     */
    private void sendCommand() {//La méthode sendCommand() est appelée pour envoyer une commande au serveur.
        if (client != null) {
            String command = commandField.getText().trim();//La commande saisie dans le champ de texte est récupérée et nettoyée.
            if (!command.isEmpty()) {//Si la commande n'est pas vide, elle est ajoutée à la zone de résultat et à l'historique.
                resultArea.append("> " + command + "\n");//La commande est ajoutée à la zone de résultat avec un préfixe ">".

                if (!historyModel.contains(command)) {//Si la commande n'est pas déjà dans l'historique, elle est ajoutée.
                    historyModel.addElement(command);
                }

                // Exécution de la commande dans un thread séparé pour ne pas bloquer l'interface
                new Thread(() -> {//La commande est exécutée dans un thread séparé pour ne pas bloquer l'interface.
                    String result = client.executeCommand(command);//La méthode executeCommand() est appelée pour exécuter la commande et récupérer le résultat.
                    SwingUtilities.invokeLater(() -> {//Le résultat est affiché dans la zone de résultat en utilisant SwingUtilities.invokeLater() pour garantir que l'interface est mise à jour dans le thread de l'interface.
                        resultArea.append(result + "\n");//Le résultat est ajouté à la zone de résultat.
                    });
                }).start();//Le thread est démarré pour exécuter la commande.

                commandField.setText("");//Le champ de texte est vidé après l'envoi de la commande.
            }
        }
    }

    /**
     * Gère l'upload d'un fichier
     */
    private void uploadFile() {//La méthode uploadFile() est appelée pour gérer l'upload d'un fichier.
        JFileChooser fileChooser = new JFileChooser();//Un sélecteur de fichiers est créé pour permettre à l'utilisateur de choisir un fichier à uploader.
        int returnValue = fileChooser.showOpenDialog(this);//Le sélecteur de fichiers est affiché et l'utilisateur peut choisir un fichier.
        if (returnValue == JFileChooser.APPROVE_OPTION) {//Si l'utilisateur a choisi un fichier, un thread est démarré pour gérer l'upload du fichier.
            File selectedFile = fileChooser.getSelectedFile();//Le fichier sélectionné est récupéré.
            new Thread(() -> {//Un thread est démarré pour gérer l'upload du fichier.
                String result = client.uploadFile(selectedFile.getAbsolutePath());//La méthode uploadFile() est appelée pour uploader le fichier et récupérer le résultat.
                SwingUtilities.invokeLater(() -> {//Le résultat est affiché dans la zone de résultat en utilisant SwingUtilities.invokeLater() pour garantir que l'interface est mise à jour dans le thread de l'interface.
                    resultArea.append(result + "\n");
                });
            }).start();
        }
    }

    /**
     * Gère le download d'un fichier
     */
    private void downloadFile() { //La méthode downloadFile() est appelée pour gérer le download d'un fichier.
        String fileName = JOptionPane.showInputDialog(this, "Entrez le nom du fichier à télécharger:");//Une boîte de dialogue est affichée pour demander le nom du fichier à télécharger.
        if (fileName != null && !fileName.isEmpty()) {//Si un nom de fichier est saisi, une autre boîte de dialogue est affichée pour demander le chemin de destination.
            String destinationPath = JOptionPane.showInputDialog(this, "Entrez le chemin de destination:");//
            if (destinationPath != null && !destinationPath.isEmpty()) {//Si un chemin de destination est saisi, un thread est démarré pour gérer le download du fichier.
                new Thread(() -> {//Un thread est démarré pour gérer le download du fichier.
                    String result = client.downloadFile(fileName, destinationPath);
                    SwingUtilities.invokeLater(() -> {
                        resultArea.append(result + "\n");
                    });
                }).start();
            } else {
                resultArea.append("Chemin de destination invalide\n");
            }
        } else {
            resultArea.append("Nom de fichier invalide\n");
        }
    }

    
    public static void main(String[] args) {//La méthode main() est utilisée pour démarrer l'interface graphique du client.
        SwingUtilities.invokeLater(() -> new ClientGUI());//L'interface graphique est créée dans un thread d'interface Swing en utilisant SwingUtilities.invokeLater().
    }
}