import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class AdminDashboard extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private JTable usersTable;
    private JTable categoriesTable;
    private JTable shipmentsTable;

    public AdminDashboard(User user) {
        this.currentUser = user;
        setTitle("Yönetici Paneli (Admin) - " + user.getFullName());
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // SEKMELER
        tabbedPane.addTab("Kullanıcı Yönetimi", createUserPanel());
        tabbedPane.addTab("Kategoriler", createCategoryPanel());
        tabbedPane.addTab("Kargo & Siparişler", createShipmentPanel());
        tabbedPane.addTab("Sistem İstatistikleri", createStatsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Çıkış Butonu
        JButton logoutBtn = new JButton("Çıkış Yap");
        logoutBtn.addActionListener(e -> {
            new Main().setVisible(true);
            dispose();
        });
        add(logoutBtn, BorderLayout.SOUTH);
    }

    // --- 1. KULLANICI YÖNETİMİ ---
    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Username");
        model.addColumn("Rol");
        model.addColumn("Ad Soyad");
        model.addColumn("Email");

        usersTable = new JTable(model);
        loadUsers(model);

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Kullanıcı Ekle");
        JButton delBtn = new JButton("Seçili Kullanıcıyı Sil");

        addBtn.addActionListener(e -> addUser(model));
        delBtn.addActionListener(e -> deleteUser(model));

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);

        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // --- 2. KATEGORİ YÖNETİMİ ---
    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Kategori Adı");
        model.addColumn("Açıklama");

        categoriesTable = new JTable(model);
        // Kategorileri yükle
        tabbedPane.addChangeListener(e -> {
            if(tabbedPane.getSelectedIndex() == 1) loadCategories(model);
        });

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Kategori Ekle");
        JButton delBtn = new JButton("Sil");

        addBtn.addActionListener(e -> addCategory(model));
        delBtn.addActionListener(e -> deleteCategory(model));

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);

        panel.add(new JScrollPane(categoriesTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // --- 3. KARGO YÖNETİMİ ---
    private JPanel createShipmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Sipariş ID");
        model.addColumn("Takip No");
        model.addColumn("Durum");
        model.addColumn("Kargo Firması");

        shipmentsTable = new JTable(model);
        
        tabbedPane.addChangeListener(e -> {
            if(tabbedPane.getSelectedIndex() == 2) loadShipments(model);
        });

        JButton updateBtn = new JButton("Durumu Güncelle (Teslim Edildi Yap)");
        updateBtn.addActionListener(e -> updateShipmentStatus(model));

        panel.add(new JScrollPane(shipmentsTable), BorderLayout.CENTER);
        panel.add(updateBtn, BorderLayout.SOUTH);
        return panel;
    }

    // --- 4. İSTATİSTİKLER (SQL ZORUNLU) ---
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        JLabel l1 = new JLabel("Yükleniyor...");
        JLabel l2 = new JLabel("Yükleniyor...");
        JLabel l3 = new JLabel("Yükleniyor...");
        JButton refresh = new JButton("İstatistikleri Güncelle");

        refresh.addActionListener(e -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Toplam Satış (Total Sales) 
                Statement st = conn.createStatement();
                ResultSet rs1 = st.executeQuery("SELECT SUM(TotalAmount) FROM Orders WHERE OrderStatus != 'Cancelled'");
                if(rs1.next()) l1.setText("Toplam Sistem Cirosu: " + rs1.getDouble(1) + " TL");

                // En Popüler Kategori (Top-Selling Category) 
                ResultSet rs2 = st.executeQuery(
                    "SELECT c.CategoryName FROM OrderItems oi " +
                    "JOIN Products p ON oi.ProductID = p.ProductID " +
                    "JOIN Categories c ON p.CategoryID = c.CategoryID " +
                    "GROUP BY c.CategoryName ORDER BY SUM(oi.Quantity) DESC LIMIT 1");
                if(rs2.next()) l2.setText("En Çok Satan Kategori: " + rs2.getString(1));

                // En İyi Satıcı (Top Seller) 
                ResultSet rs3 = st.executeQuery(
                    "SELECT u.FirstName, u.LastName FROM Orders o " +
                    "JOIN Users u ON o.SellerID = u.UserID " +
                    "WHERE o.OrderStatus != 'Cancelled' " +
                    "GROUP BY o.SellerID, u.FirstName, u.LastName " +
                    "ORDER BY SUM(o.TotalAmount) DESC LIMIT 1");
                if(rs3.next()) l3.setText("Şampiyon Satıcı: " + rs3.getString(1) + " " + rs3.getString(2));

            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        panel.add(l1); panel.add(l2); panel.add(l3); panel.add(refresh);
        return panel;
    }

    // --- HELPER METHODS (SQL İŞLEMLERİ) ---

    private void loadUsers(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Users")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("UserID"), rs.getString("Username"),
                    rs.getString("Role"), rs.getString("FirstName") + " " + rs.getString("LastName"),
                    rs.getString("Email")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addUser(DefaultTableModel model) {
        // Basit Ekleme Formu
        JTextField userField = new JTextField();
        String[] roles = {"Customer", "Seller", "Administrator"};
        JComboBox<String> roleBox = new JComboBox<>(roles);
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();

        Object[] message = {
            "Kullanıcı Adı:", userField,
            "Rol:", roleBox,
            "Ad:", nameField,
            "Email:", emailField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Yeni Kullanıcı Ekle", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO Users (Username, PasswordHash, Role, FirstName, LastName, Email) VALUES (?, '12345', ?, ?, 'Soyad', ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, userField.getText());
                ps.setString(2, (String) roleBox.getSelectedItem());
                ps.setString(3, nameField.getText());
                ps.setString(4, emailField.getText());
                ps.executeUpdate();
                loadUsers(model);
                JOptionPane.showMessageDialog(this, "Kullanıcı eklendi! Şifresi: 12345");
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage()); }
        }
    }

    private void deleteUser(DefaultTableModel model) {
        int row = usersTable.getSelectedRow();
        if (row == -1) return;
        int userId = (int) usersTable.getValueAt(row, 0);

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM Users WHERE UserID = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();
            loadUsers(model);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadCategories(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Categories")) {
            while(rs.next()) model.addRow(new Object[]{rs.getInt("CategoryID"), rs.getString("CategoryName"), rs.getString("Description")});
        } catch(SQLException e) { e.printStackTrace(); }
    }

    private void addCategory(DefaultTableModel model) {
        String name = JOptionPane.showInputDialog("Kategori Adı:");
        if (name != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // CreatedBy admin olmalı
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Categories (CategoryName, Description, CreatedBy) VALUES (?, 'Admin ekledi', ?)");
                ps.setString(1, name);
                ps.setInt(2, currentUser.getId());
                ps.executeUpdate();
                loadCategories(model);
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void deleteCategory(DefaultTableModel model) {
        int row = categoriesTable.getSelectedRow();
        if(row != -1) {
            int catId = (int) categoriesTable.getValueAt(row, 0);
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Categories WHERE CategoryID = ?");
                ps.setInt(1, catId);
                ps.executeUpdate();
                loadCategories(model);
            } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Bu kategori silinemez (ürünler bağlı olabilir)!"); }
        }
    }

    private void loadShipments(DefaultTableModel model) {
        model.setRowCount(0);
        // Shipments tablosu ve Orders birleşimi
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT s.OrderID, s.TrackingNumber, s.ShipmentStatus, s.Carrier FROM Shipments s";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()) {
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
        } catch(SQLException e) { e.printStackTrace(); }
    }
    
    private void updateShipmentStatus(DefaultTableModel model) {
        int row = shipmentsTable.getSelectedRow();
        if(row == -1) return;
        int orderId = (int) shipmentsTable.getValueAt(row, 0); // OrderID
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Shipment durumunu Delivered yap
            PreparedStatement ps = conn.prepareStatement("UPDATE Shipments SET ShipmentStatus = 'Delivered', ActualDelivery = NOW() WHERE OrderID = ?");
            ps.setInt(1, orderId);
            ps.executeUpdate();
            
            // Order durumunu da Delivered yap
            PreparedStatement ps2 = conn.prepareStatement("UPDATE Orders SET OrderStatus = 'Delivered' WHERE OrderID = ?");
            ps2.setInt(1, orderId);
            ps2.executeUpdate();
            
            loadShipments(model);
            JOptionPane.showMessageDialog(this, "Sipariş Teslim Edildi (Delivered) olarak güncellendi.");
        } catch(SQLException e) { e.printStackTrace(); }
    }
}