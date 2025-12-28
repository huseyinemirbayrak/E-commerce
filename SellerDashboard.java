import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class SellerDashboard extends JFrame {
    private int sellerId;
    private JTabbedPane tabbedPane;
    private JTable tableProducts, tableOrders;
    private DefaultTableModel modelProducts, modelOrders;

    public SellerDashboard(int userId) {
        this.sellerId = userId;
        setTitle("Seller Dashboard - ID: " + userId);
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("My Products", createProductPanel());
        tabbedPane.addTab("Incoming Orders", createOrderPanel());
        tabbedPane.addTab("Statistics", createStatsPanel());

        add(tabbedPane);
        
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) loadMyProducts();
            else if (tabbedPane.getSelectedIndex() == 1) loadIncomingOrders();
            else if (tabbedPane.getSelectedIndex() == 2) updateStats((JPanel)tabbedPane.getComponentAt(2));
        });
        
        loadMyProducts(); 
    }

    // PRODUCT MANAGMENT //
    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"ID", "Name", "Stock", "Price", "Active"};
        modelProducts = new DefaultTableModel(cols, 0);
        tableProducts = new JTable(modelProducts);
        panel.add(new JScrollPane(tableProducts), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton btnRestock = new JButton("Update Stock");
        JButton btnAdd = new JButton("Add New Product");
        JButton btnRefresh = new JButton("Refresh");

        btnRestock.addActionListener(e -> updateStockAction());
        btnRefresh.addActionListener(e -> loadMyProducts());
        
        // Add Product özelliği yeni bir pencere gerektirir, demo için mesaj veriyoruz
        btnAdd.addActionListener(e -> JOptionPane.showMessageDialog(this, "Add Product Feature requires a form dialog."));

        btnPanel.add(btnRefresh);
        btnPanel.add(btnRestock);
        btnPanel.add(btnAdd);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadMyProducts() {
        modelProducts.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            // View kullanıyoruz: vw_SellerProductCatalog
            // Ancak bu view Seller ismini içeriyor, ID filtrelemesi için join veya doğrudan tablo sorgusu daha güvenli.
            // DDL'e göre direkt Products ve Catalogs tablolarını kullanmak en temizi.
            String sql = "SELECT p.ProductID, p.ProductName, p.StockQuantity, p.Price, p.IsActive " +
                         "FROM Products p JOIN Catalogs c ON p.CatalogID = c.CatalogID " +
                         "WHERE c.SellerID = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, sellerId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                modelProducts.addRow(new Object[]{
                    rs.getInt("ProductID"), rs.getString("ProductName"), 
                    rs.getInt("StockQuantity"), rs.getDouble("Price"), rs.getBoolean("IsActive")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateStockAction() {
        int row = tableProducts.getSelectedRow();
        if (row == -1) return;
        
        int prodId = (int) modelProducts.getValueAt(row, 0);
        String currentStock = modelProducts.getValueAt(row, 2).toString();
        
        String newStockStr = JOptionPane.showInputDialog(this, "Enter New Stock Quantity:", currentStock);
        if (newStockStr == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            int newStock = Integer.parseInt(newStockStr);
            String sql = "UPDATE Products SET StockQuantity = ? WHERE ProductID = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, newStock);
            pst.setInt(2, prodId);
            pst.executeUpdate();
            loadMyProducts();
            JOptionPane.showMessageDialog(this, "Stock Updated!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ORDER MANAGMENT//
    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"OrderID", "Customer", "Date", "Total", "Status"};
        modelOrders = new DefaultTableModel(cols, 0);
        tableOrders = new JTable(modelOrders);
        panel.add(new JScrollPane(tableOrders), BorderLayout.CENTER);
        
        JButton btnConfirm = new JButton("Confirm Order (Prepare for Shipping)");
        panel.add(btnConfirm, BorderLayout.SOUTH);
        
        btnConfirm.addActionListener(e -> confirmOrderAction());
        
        return panel;
    }

    private void loadIncomingOrders() {
        modelOrders.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {

            String sql = "SELECT o.OrderID, u.FirstName, o.OrderDate, o.TotalAmount, o.OrderStatus " +
                         "FROM Orders o JOIN Users u ON o.CustomerID = u.UserID " +
                         "WHERE o.SellerID = ? ORDER BY o.OrderDate DESC";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, sellerId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                modelOrders.addRow(new Object[]{
                    rs.getInt("OrderID"), rs.getString("FirstName"), rs.getTimestamp("OrderDate"),
                    rs.getDouble("TotalAmount"), rs.getString("OrderStatus")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
    
    private void confirmOrderAction() {
        int row = tableOrders.getSelectedRow();
        if (row == -1) return;
        
        int orderId = (int) modelOrders.getValueAt(row, 0);
        String status = (String) modelOrders.getValueAt(row, 4);
        
        if (!"Pending".equals(status)) {
            JOptionPane.showMessageDialog(this, "Only Pending orders can be confirmed.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
       
            String sql = "UPDATE Orders SET OrderStatus = 'Confirmed' WHERE OrderID = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, orderId);
            pst.executeUpdate();
            loadIncomingOrders();
            JOptionPane.showMessageDialog(this, "Order Confirmed! Waiting for Admin to Ship.");
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

 // STATS//
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    
    private void updateStats(JPanel panel) {
        panel.removeAll();
        panel.add(new JLabel("=== Seller Statistics ==="));
        panel.add(Box.createVerticalStrut(20));
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            String sql = "SELECT SUM(TotalAmount) FROM Orders WHERE SellerID = ? AND OrderStatus != 'Cancelled'";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, sellerId);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) panel.add(new JLabel("Total Revenue: " + rs.getDouble(1) + " TL"));
            
            String sql2 = "SELECT p.ProductName, SUM(oi.Quantity) as Qty FROM OrderItems oi " +
                          "JOIN Products p ON oi.ProductID = p.ProductID JOIN Catalogs c ON p.CatalogID = c.CatalogID " +
                          "WHERE c.SellerID = ? GROUP BY p.ProductName ORDER BY Qty DESC LIMIT 1";
            PreparedStatement pst2 = conn.prepareStatement(sql2);
            pst2.setInt(1, sellerId);
            ResultSet rs2 = pst2.executeQuery();
            if(rs2.next()) panel.add(new JLabel("Best Seller: " + rs2.getString(1) + " (" + rs2.getInt(2) + " units)"));
            
        } catch (SQLException ex) { ex.printStackTrace(); }
        panel.revalidate();
        panel.repaint();
    }
}