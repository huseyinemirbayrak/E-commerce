import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegisterCustomer;
    private JButton btnRegisterSeller;

    public LoginFrame() {
        setTitle("E-Commerce Order Management System - Login");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel lblTitle = new JLabel("E-Commerce Management System", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(lblTitle, BorderLayout.NORTH);
        
        // Login Panel
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        txtEmail = new JTextField(20);
        loginPanel.add(txtEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(20);
        loginPanel.add(txtPassword, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(150, 30));
        loginPanel.add(btnLogin, gbc);
        
        mainPanel.add(loginPanel, BorderLayout.CENTER);
        
        // Register Panel
        JPanel registerPanel = new JPanel(new FlowLayout());
        JLabel lblRegister = new JLabel("Don't have an account?");
        btnRegisterCustomer = new JButton("Sign Up as Customer");
        btnRegisterSeller = new JButton("Sign Up as Seller");
        
        registerPanel.add(lblRegister);
        registerPanel.add(btnRegisterCustomer);
        registerPanel.add(btnRegisterSeller);
        
        mainPanel.add(registerPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Event Handlers
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        btnRegisterCustomer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegisterFrame("Customer").setVisible(true);
            }
        });
        
        btnRegisterSeller.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegisterFrame("Seller").setVisible(true);
            }
        });
    }
    
    private void performLogin() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both email and password!", 
                "Validation Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT user_id, name, email, password, role, is_active FROM Users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                boolean isActive = rs.getBoolean("is_active");
                
                if (!isActive) {
                    JOptionPane.showMessageDialog(this, 
                        "Account is deactivated. Please contact administrator.", 
                        "Account Disabled", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                boolean passwordValid = false;
                if (dbPassword.length() == 64) {
                    passwordValid = PasswordUtil.verifyPassword(password, dbPassword);
                } else {
     
                    passwordValid = password.equals(dbPassword);
                }
                
                if (passwordValid) {
                    int userId = rs.getInt("user_id");
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    
                    User currentUser = new User(userId, name, email, role);
                    
                    JOptionPane.showMessageDialog(this, 
                        "Login successful! Welcome " + name, 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    dispose();
                    
                    if ("Customer".equals(role)) {
                        new CustomerDashboard(currentUser).setVisible(true);
                    } else if ("Seller".equals(role)) {
                        new SellerDashboard(currentUser).setVisible(true);
                    } else if ("Administrator".equals(role)) {
                        new AdminDashboard(currentUser).setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid password!", 
                        "Authentication Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, 
                    "User not found! Please check your email.", 
                    "Authentication Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database error: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}
