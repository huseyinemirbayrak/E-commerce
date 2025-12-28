import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;

public class RegisterFrame extends JFrame {

    private JTextField txtUsername, txtEmail, txtFirstName, txtLastName, txtPhone;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;
    private JButton btnRegister, btnCancel;

    public RegisterFrame() {
        setTitle("E-Commerce System - Register");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(9, 2, 10, 10));

        add(new JLabel("  Username:"));
        txtUsername = new JTextField();
        add(txtUsername);

        add(new JLabel("  Email:"));
        txtEmail = new JTextField();
        add(txtEmail);

        add(new JLabel("  Password:"));
        txtPassword = new JPasswordField();
        add(txtPassword);

        add(new JLabel("  First Name:"));
        txtFirstName = new JTextField();
        add(txtFirstName);

        add(new JLabel("  Last Name:"));
        txtLastName = new JTextField();
        add(txtLastName);

        add(new JLabel("  Phone:"));
        txtPhone = new JTextField();
        add(txtPhone);

        add(new JLabel("  Account Type:"));
        String[] roles = {"Customer", "Seller"};
        cmbRole = new JComboBox<>(roles);
        add(cmbRole);

        btnRegister = new JButton("Register");
        btnCancel = new JButton("Cancel");
        add(btnRegister);
        add(btnCancel);

        btnRegister.addActionListener(e -> registerUser());
        btnCancel.addActionListener(e -> this.dispose());

        setVisible(true);
    }

    private void registerUser() {
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        String fName = txtFirstName.getText().trim();
        String lName = txtLastName.getText().trim();
        String phone = txtPhone.getText().trim();
        String role = (String) cmbRole.getSelectedItem();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all mandatory fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO Users (Username, Email, PasswordHash, Role, FirstName, LastName, PhoneNumber) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password); 
            pstmt.setString(4, role);
            pstmt.setString(5, fName);
            pstmt.setString(6, lName);
            pstmt.setString(7, phone);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Registration Successful! Please Login.");
                
                if ("Seller".equals(role)) {
                    createCatalogForSeller(conn, username);
                }
                this.dispose();
            }

        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate")) {
                JOptionPane.showMessageDialog(this, "Username or Email already exists!", "Error", JOptionPane.WARNING_MESSAGE);
            } else {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        }
    }

    private void createCatalogForSeller(Connection conn, String username) throws SQLException {
        String idSql = "SELECT UserID FROM Users WHERE Username = ?";
        PreparedStatement p1 = conn.prepareStatement(idSql);
        p1.setString(1, username);
        ResultSet rs = p1.executeQuery();
        if (rs.next()) {
            int userId = rs.getInt(1);
            String catSql = "INSERT INTO Catalogs (SellerID, CatalogName, Description) VALUES (?, ?, ?)";
            PreparedStatement p2 = conn.prepareStatement(catSql);
            p2.setInt(1, userId);
            p2.setString(2, username + "'s Catalog");
            p2.setString(3, "Welcome to my shop!");
            p2.executeUpdate();
        }
    }
}