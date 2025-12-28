import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Main extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public Main() {
        setTitle("E-Commerce System Giriş");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Ekranın ortasında açılır
        setLayout(new GridLayout(4, 1, 10, 10));

        // 1. BAŞLIK
        JLabel lblTitle = new JLabel("E-Ticaret Sistemine Hoşgeldiniz", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        add(lblTitle);

        // 2. GİRİŞ ALANLARI
        JPanel panelInput = new JPanel(new GridLayout(2, 2, 5, 5));
        
        panelInput.add(new JLabel("  Kullanıcı Adı:"));
        txtUsername = new JTextField("customer_alice"); // Test kolaylığı için dolu gelsin
        panelInput.add(txtUsername);
        
        panelInput.add(new JLabel("  Şifre:"));
        txtPassword = new JPasswordField("12345"); // Test için dolu gelsin
        panelInput.add(txtPassword);
        
        add(panelInput);

        // 3. BUTONLAR
        JPanel panelButtons = new JPanel(new FlowLayout());
        JButton btnLogin = new JButton("Giriş Yap (Login)");
        JButton btnRegister = new JButton("Kayıt Ol (Register)");
        
        panelButtons.add(btnLogin);
        panelButtons.add(btnRegister);
        add(panelButtons);

        // 4. ETİKET (Durum Mesajı)
        JLabel lblStatus = new JLabel("Lütfen giriş yapınız...", SwingConstants.CENTER);
        add(lblStatus);

        // --- BUTON OLAYLARI ---

        // Giriş Butonu
        btnLogin.addActionListener(e -> {
            String user = txtUsername.getText();
            String pass = new String(txtPassword.getPassword());
            performLogin(user, pass);
        });

        // Kayıt Butonu (RegisterFrame dosyan varsa onu açar)
        btnRegister.addActionListener(e -> {
            // Eğer RegisterFrame.java dosyan varsa aşağıdaki satırın başındaki // işaretlerini kaldır.
            new RegisterFrame().setVisible(true);
        });
    }

    private void performLogin(String username, String password) {
        // Eğer senin dosya adın DbHelper ise, burayı DbHelper.getConnection() yap!
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            // SQL Sorgusu: Kullanıcıyı bul
            String sql = "SELECT * FROM Users WHERE Username = ? AND PasswordHash = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Kullanıcı bulundu! Bilgilerini alıp User nesnesi yapıyoruz.
                int id = rs.getInt("UserID");
                String role = rs.getString("Role");
                String fullName = rs.getString("FirstName") + " " + rs.getString("LastName");
                
                // User sınıfını kullanarak oturum nesnesi oluştur
                User currentUser = new User(id, username, role, fullName);

                JOptionPane.showMessageDialog(this, "Giriş Başarılı! Hoşgeldin: " + fullName);
                
                // ROLE GÖRE EKRAN AÇMA MANTIĞI
                if (role.equalsIgnoreCase("Customer")) {
                    new CustomerDashboard(currentUser).setVisible(true);
                } else if (role.equalsIgnoreCase("Seller")) {
                    new SellerDashboard(currentUser).setVisible(true);
                } else if (role.equalsIgnoreCase("Administrator")) {
                    new AdminDashboard(currentUser).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Hata: Tanımsız Rol -> " + role);
                }
                
                dispose(); // Login ekranını kapat

            } else {
                JOptionPane.showMessageDialog(this, "Hatalı Kullanıcı Adı veya Şifre!");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı Bağlantı Hatası: " + ex.getMessage());
        }
    }

    // Programın Başlangıç Noktası
    public static void main(String[] args) {
        // Arayüzü güvenli modda başlat
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}