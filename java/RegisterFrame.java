import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class RegisterFrame extends JFrame {
    private JTextField txtName;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    private JButton btnCancel;
    private String role;

    public RegisterFrame(String role) {
        this.role = role;
        setTitle("Register as " + role);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(20);
        mainPanel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        txtEmail = new JTextField(20);
        mainPanel.add(txtEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(20);
        mainPanel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        txtConfirmPassword = new JPasswordField(20);
        mainPanel.add(txtConfirmPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnRegister = new JButton("Register");
        btnCancel = new JButton("Cancel");
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);
        mainPanel.add(buttonPanel, gbc);
        
        add(mainPanel);
        
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerUser();
            }
        });
        
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private void registerUser() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());
        
        // Validation using ValidationUtil
        try {
            ValidationUtil.validateName(name);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validatePassword(password);
        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(this, 
                e.getMessage(), 
                "Validation Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Passwords do not match!", 
                "Validation Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);  // Start transaction
            
            // Check if email already exists
            String checkSql = "SELECT user_id FROM Users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, 
                    "Email already exists! Please use a different email.", 
                    "Registration Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Hash password before storing
            String hashedPassword = PasswordUtil.hashPassword(password);
            
            // Insert new user
            String sql = "INSERT INTO Users (name, email, password, role, is_active) VALUES (?, ?, ?, ?, TRUE)";
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, ValidationUtil.sanitizeString(name));
            pstmt.setString(2, email.toLowerCase());
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, role);
            
            int result = pstmt.executeUpdate();
            
            if (result > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    
                    // If seller, create catalog
                    if ("Seller".equals(role)) {
                        String catalogSql = "INSERT INTO Catalogs (seller_id, catalog_name, description, is_available) VALUES (?, ?, ?, TRUE)";
                        PreparedStatement catalogStmt = conn.prepareStatement(catalogSql);
                        catalogStmt.setInt(1, userId);
                        catalogStmt.setString(2, name + "'s Catalog");
                        catalogStmt.setString(3, "Welcome to my shop!");
                        catalogStmt.executeUpdate();
                    }
                }
                
                conn.commit();  // Commit transaction
                JOptionPane.showMessageDialog(this, 
                    "Registration successful! Please login.", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                conn.rollback();
            }
            
        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            JOptionPane.showMessageDialog(this, 
                "Email already exists! Please use a different email.", 
                "Registration Error", 
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database error: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
