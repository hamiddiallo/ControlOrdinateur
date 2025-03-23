package gestionUtilisateur;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class UserGUI extends JFrame {
    // Composants de l'interface
    private JTable userTable;
    private JTextField loginField, passwordField;
    private JButton addButton, updateButton, deleteButton, clearButton;
    private DefaultTableModel tableModel;
    
    // Connexion à la base de données
    private Connection connection;
    private PreparedStatement ps;
    private ResultSet rs;
    
    // Constructeur
    public UserGUI() {
        // Configuration de la fenêtre
        setTitle("Gestion des Utilisateurs");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialisation des composants
        initComponents();
        
        // Connexion à la base de données
        connectDB();
        
        // Chargement initial des données
        loadUserData();
        
        setVisible(true);
    }
    
    // Méthode pour initialiser les composants de l'interface
    private void initComponents() {
        // Panel principal avec BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel pour les champs de formulaire
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        formPanel.add(new JLabel("Login:"));
        loginField = new JTextField(20);
        formPanel.add(loginField);
        
        formPanel.add(new JLabel("Mot de passe:"));
        passwordField = new JTextField(20);
        formPanel.add(passwordField);
        
        // Panel pour les boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        addButton = new JButton("Ajouter");
        updateButton = new JButton("Modifier");
        deleteButton = new JButton("Supprimer");
        clearButton = new JButton("Effacer");
        
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        
        // Regroupement des panels de formulaire et boutons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Configuration de la table
        String[] columnNames = {"ID", "Login", "Mot de passe"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Rend la table non éditable
            }
        };
        userTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(userTable);
        
        // Ajout des composants au panel principal
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Ajout du panel principal à la fenêtre
        add(mainPanel);
        
        // Ajout des écouteurs d'événements
        addEventListeners();
    }
    
    // Méthode pour ajouter les écouteurs d'événements
    private void addEventListeners() {
        // Sélection d'une ligne dans la table
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow != -1) {
                    loginField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    passwordField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                }
            }
        });
        
        // Bouton Ajouter
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUser();
            }
        });
        
        // Bouton Modifier
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUser();
            }
        });
        
        // Bouton Supprimer
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteUser();
            }
        });
        
        // Bouton Effacer
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });
    }
    
    // Méthode pour se connecter à la base de données
    private void connectDB() {
        try {
            // Chargement du pilote JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Établissement de la connexion
            String url = "jdbc:mysql://localhost:8889/poo";
            String user = "poo";
            String password = "passer";
            connection = DriverManager.getConnection(url, user, password);
            
            // Création de la table si elle n'existe pas
            createTableIfNotExists();
            
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion à la base de données: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Méthode pour créer la table si elle n'existe pas
    private void createTableIfNotExists() {
        try {
            Statement stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS user (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "login VARCHAR(50) NOT NULL UNIQUE, " +
                         "password VARCHAR(50) NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la création de la table: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Méthode pour charger les données des utilisateurs
    private void loadUserData() {
        try {
            // Vider le modèle de table
            tableModel.setRowCount(0);
            
            // Requête SQL pour récupérer tous les utilisateurs
            String sql = "SELECT * FROM user";
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            
            // Ajouter chaque utilisateur au modèle de table
            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                String password = rs.getString("password");
                
                tableModel.addRow(new Object[]{id, login, password});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des données: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Méthode pour ajouter un utilisateur
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors du hachage du mot de passe", e);
        }
    }

    // Méthode pour ajouter un utilisateur avec mot de passe haché
    private void addUser() {
        String login = loginField.getText();
        String password = passwordField.getText();
        
        if (login.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs", 
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Vérifier si le login existe déjà
            String checkSql = "SELECT COUNT(*) FROM user WHERE login = ?";
            ps = connection.prepareStatement(checkSql);
            ps.setString(1, login);
            rs = ps.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            if (count > 0) {
                JOptionPane.showMessageDialog(this, "Ce login existe déjà", 
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Hacher le mot de passe avant insertion
            String hashedPassword = hashPassword(password);
            
            // Insérer le nouvel utilisateur avec mot de passe haché
            String sql = "INSERT INTO user (login, password) VALUES (?, ?)";
            ps = connection.prepareStatement(sql);
            ps.setString(1, login);
            ps.setString(2, hashedPassword);
            int result = ps.executeUpdate();
            
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Utilisateur ajouté avec succès");
                clearFields();
                loadUserData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Méthode pour modifier un utilisateur avec un mot de passe haché
    private void updateUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur à modifier",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
        String login = loginField.getText();
        String password = passwordField.getText();
        
        if (login.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs", 
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Vérifier si le login existe déjà (sauf pour l'utilisateur actuel)
            String checkSql = "SELECT COUNT(*) FROM user WHERE login = ? AND id != ?";
            ps = connection.prepareStatement(checkSql);
            ps.setString(1, login);
            ps.setInt(2, id);
            rs = ps.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            if (count > 0) {
                JOptionPane.showMessageDialog(this, "Ce login existe déjà", 
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Hacher le nouveau mot de passe
            String hashedPassword = hashPassword(password);
            
            // Mettre à jour l'utilisateur avec le mot de passe haché
            String sql = "UPDATE user SET login = ?, password = ? WHERE id = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, login);
            ps.setString(2, hashedPassword);
            ps.setInt(3, id);
            int result = ps.executeUpdate();
            
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Utilisateur modifié avec succès");
                clearFields();
                loadUserData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la modification: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Méthode pour supprimer un utilisateur
        // Méthode pour supprimer un utilisateur
        private void deleteUser() {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un utilisateur à supprimer",
                        "Avertissement", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Êtes-vous sûr de vouloir supprimer cet utilisateur ?", 
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String sql = "DELETE FROM user WHERE id = ?";
                    ps = connection.prepareStatement(sql);
                    ps.setInt(1, id);
                    int result = ps.executeUpdate();
                    
                    if (result > 0) {
                        JOptionPane.showMessageDialog(this, "Utilisateur supprimé avec succès");
                        clearFields();
                        loadUserData();
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Erreur lors de la suppression: " + e.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
        
        // Méthode pour effacer les champs de formulaire
        private void clearFields() {
            loginField.setText("");
            passwordField.setText("");
            userTable.clearSelection();
        }
        
        // Méthode principale pour lancer l'application
        public static void main(String[] args) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new UserGUI();
                }
            });
        }
    }