import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginFrame() {
        setTitle("E-Commerce System - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        add(new JLabel("  Email:"));
        txtEmail = new JTextField("alice.johnson@email.com"); // Demo kolaylığı için dolu
        add(txtEmail);

        add(new JLabel("  Password:"));
        txtPassword = new JPasswordField("12345"); // Demo şifre
        add(txtPassword);

        btnLogin = new JButton("Login");
        btnRegister = new JButton("Register");
        add(btnLogin);
        add(btnRegister);

        btnLogin.addActionListener(e -> loginAction());
        btnRegister.addActionListener(e -> new RegisterFrame()); // RegisterFrame'i açar

        setVisible(true);
    }

    private void loginAction() {
        String email = txtEmail.getText();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM Users WHERE Email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("Role");
                int userId = rs.getInt("UserID");
                
                JOptionPane.showMessageDialog(this, "Welcome " + rs.getString("FirstName") + " (" + role + ")");
                
                if ("Customer".equals(role)) {
                    new CustomerDashboard(userId).setVisible(true);
                } else if ("Seller".equals(role)) {
                     new SellerDashboard(userId).setVisible(true);
                } else if ("Administrator".equals(role)) {
                     new AdminDashboard(userId).setVisible(true);
                }
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "User not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}