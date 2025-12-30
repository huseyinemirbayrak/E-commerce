import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class SellerDashboard extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private DefaultTableModel modelProducts, modelOrders, modelReviews;
    private JTable tableProducts, tableOrders, tableReviews;
    private int catalogId = -1;

    public SellerDashboard(User user) {
        this.currentUser = user;
        setTitle("Seller Dashboard - " + user.getName());
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT catalog_id FROM Catalogs WHERE seller_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                catalogId = rs.getInt("catalog_id");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("My Catalog", createCatalogOverviewPanel());
        tabbedPane.addTab("Product Management", createProductPanel());
        tabbedPane.addTab("Inventory Management", createInventoryPanel());
        tabbedPane.addTab("Order Management", createOrderPanel());
        tabbedPane.addTab("Reviews", createReviewsPanel());
        tabbedPane.addTab("Statistics", createStatsPanel());

JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
JButton btnLogout = new JButton("Logout");
btnLogout.setForeground(Color.RED);

btnLogout.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(this, 
        "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        dispose(); 
        new LoginFrame().setVisible(true);
    }
});

bottomPanel.add(btnLogout);

add(tabbedPane, BorderLayout.CENTER);  
add(bottomPanel, BorderLayout.SOUTH);  


        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index == 1) loadProducts();
            else if (index == 2) loadProducts(); 
            else if (index == 3) loadOrders();
            else if (index == 4) loadReviews();
            else if (index == 5) updateStats();
        });
    }

    private JPanel createCatalogOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        try (Connection conn = DatabaseConnection.getConnection()) {
            String countSql = "SELECT COUNT(*) as product_count FROM Products WHERE catalog_id = ?";
            PreparedStatement countStmt = conn.prepareStatement(countSql);
            countStmt.setInt(1, catalogId);
            ResultSet countRs = countStmt.executeQuery();
            int productCount = 0;
            if (countRs.next()) {
                productCount = countRs.getInt("product_count");
            }

            String valueSql = "SELECT SUM(price * stock_quantity) as total_value FROM Products WHERE catalog_id = ?";
            PreparedStatement valueStmt = conn.prepareStatement(valueSql);
            valueStmt.setInt(1, catalogId);
            ResultSet valueRs = valueStmt.executeQuery();
            double totalValue = 0.0;
            if (valueRs.next()) {
                totalValue = valueRs.getDouble("total_value");
            }

            DecimalFormat df = new DecimalFormat("#0.00");
            panel.add(new JLabel("=== My Catalog Overview ==="));
            panel.add(Box.createVerticalStrut(20));
            panel.add(new JLabel("Catalog ID: " + catalogId));
            panel.add(new JLabel("Total Products: " + productCount));
            panel.add(new JLabel("Total Inventory Value: $" + df.format(totalValue)));

        } catch (SQLException ex) {
            ex.printStackTrace();
            panel.add(new JLabel("Error loading catalog overview: " + ex.getMessage()));
        }

        return panel;
    }

    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Product ID", "Name", "Category", "Price", "Stock", "Description"};
        modelProducts = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableProducts = new JTable(modelProducts);
        tableProducts.getColumnModel().getColumn(0).setMinWidth(0);
        tableProducts.getColumnModel().getColumn(0).setMaxWidth(0);
        tableProducts.getColumnModel().getColumn(0).setWidth(0);
        panel.add(new JScrollPane(tableProducts), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add Product");
        JButton btnEdit = new JButton("Edit Product");
        JButton btnDelete = new JButton("Delete Product");
        JButton btnRefresh = new JButton("Refresh");
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        btnAdd.addActionListener(e -> addProduct());
        btnEdit.addActionListener(e -> editProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnRefresh.addActionListener(e -> loadProducts());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadProducts() {
        modelProducts.setRowCount(0);
        if (catalogId == -1) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.product_id, p.name, c.name as category_name, p.price, p.stock_quantity, p.description " +
                        "FROM Products p " +
                        "JOIN Categories c ON p.category_id = c.category_id " +
                        "WHERE p.catalog_id = ? " +
                        "ORDER BY p.name";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, catalogId);
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat df = new DecimalFormat("#0.00");
            while (rs.next()) {
                String description = rs.getString("description");
                if (description != null && description.length() > 50) {
                    description = description.substring(0, 50) + "...";
                }
                modelProducts.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    "$" + df.format(rs.getDouble("price")),
                    rs.getInt("stock_quantity"),
                    description != null ? description : ""
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage());
        }
    }

    private void addProduct() {
        JTextField txtName = new JTextField(20);
        JTextArea txtDescription = new JTextArea(5, 20);
        txtDescription.setLineWrap(true);
        JTextField txtPrice = new JTextField(10);
        JTextField txtStock = new JTextField(10);
        JComboBox<String> cmbCategory = new JComboBox<>();
        loadCategories(cmbCategory);

        Object[] message = {
            "Product Name:", txtName,
            "Category:", cmbCategory,
            "Description:", new JScrollPane(txtDescription),
            "Price:", txtPrice,
            "Initial Stock:", txtStock
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (txtName.getText().trim().isEmpty() || 
                txtPrice.getText().trim().isEmpty() || 
                txtStock.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name, Price, and Stock are required!");
                return;
            }

            try {
                double price = Double.parseDouble(txtPrice.getText().trim());
                int stock = Integer.parseInt(txtStock.getText().trim());

                if (price < 0 || stock < 0) {
                    JOptionPane.showMessageDialog(this, "Price and stock must be >= 0!");
                    return;
                }

                String selectedCategory = (String) cmbCategory.getSelectedItem();
                if (selectedCategory == null) {
                    JOptionPane.showMessageDialog(this, "Please select a category!");
                    return;
                }

                int categoryId = getCategoryId(selectedCategory);
                if (categoryId == -1) {
                    JOptionPane.showMessageDialog(this, "Invalid category!");
                    return;
                }

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO Products (catalog_id, category_id, name, description, price, stock_quantity) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, catalogId);
                    pstmt.setInt(2, categoryId);
                    pstmt.setString(3, txtName.getText().trim());
                    pstmt.setString(4, txtDescription.getText().trim());
                    pstmt.setDouble(5, price);
                    pstmt.setInt(6, stock);
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Product added successfully!");
                    loadProducts();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding product: " + ex.getMessage());
            }
        }
    }

    private void editProduct() {
        int row = tableProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit!");
            return;
        }

        int productId = (Integer) modelProducts.getValueAt(row, 0);
        String currentName = (String) modelProducts.getValueAt(row, 1);
        String currentCategory = (String) modelProducts.getValueAt(row, 2);
        String currentPrice = ((String) modelProducts.getValueAt(row, 3)).substring(1); // Remove $
        String currentStock = modelProducts.getValueAt(row, 4).toString();

        // Get full description
        String currentDescription = "";
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT description FROM Products WHERE product_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentDescription = rs.getString("description") != null ? rs.getString("description") : "";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        JTextField txtName = new JTextField(currentName, 20);
        JTextArea txtDescription = new JTextArea(currentDescription, 5, 20);
        txtDescription.setLineWrap(true);
        JTextField txtPrice = new JTextField(currentPrice, 10);
        JComboBox<String> cmbCategory = new JComboBox<>();
        loadCategories(cmbCategory);
        cmbCategory.setSelectedItem(currentCategory);

        Object[] message = {
            "Product Name:", txtName,
            "Category:", cmbCategory,
            "Description:", new JScrollPane(txtDescription),
            "Price:", txtPrice
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(txtPrice.getText().trim());
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Price must be >= 0!");
                    return;
                }

                String selectedCategory = (String) cmbCategory.getSelectedItem();
                int categoryId = getCategoryId(selectedCategory);

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE Products SET name = ?, category_id = ?, description = ?, price = ? " +
                                "WHERE product_id = ? AND catalog_id = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, txtName.getText().trim());
                    pstmt.setInt(2, categoryId);
                    pstmt.setString(3, txtDescription.getText().trim());
                    pstmt.setDouble(4, price);
                    pstmt.setInt(5, productId);
                    pstmt.setInt(6, catalogId);
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Product updated successfully!");
                    loadProducts();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating product: " + ex.getMessage());
            }
        }
    }

    private void deleteProduct() {
        int row = tableProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete!");
            return;
        }

        int productId = (Integer) modelProducts.getValueAt(row, 0);
        String productName = (String) modelProducts.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete '" + productName + "'?\n" +
            "This will fail if the product appears in any orders.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check if product is in any orders
                String checkSql = "SELECT COUNT(*) as count FROM Order_Items WHERE product_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, productId);
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next() && checkRs.getInt("count") > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete product! It appears in existing orders.\n" +
                        "The database constraint prevents deletion.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "DELETE FROM Products WHERE product_id = ? AND catalog_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, productId);
                pstmt.setInt(2, catalogId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Product deleted successfully!");
                loadProducts();
            } catch (SQLException ex) {
                ex.printStackTrace();
                if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("RESTRICT")) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete product! It is referenced in existing orders.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error deleting product: " + ex.getMessage());
                }
            }
        }
    }

    // Inventory Management Panel
    private JPanel createInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Product ID", "Name", "Current Stock"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnRestock = new JButton("Restock Product");
        btnPanel.add(btnRestock);

        btnRestock.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product to restock!");
                return;
            }

            int productId = (Integer) model.getValueAt(row, 0);
            int currentStock = (Integer) model.getValueAt(row, 2);
            String productName = (String) model.getValueAt(row, 1);

            String qtyStr = JOptionPane.showInputDialog(this, 
                "Current stock: " + currentStock + "\nEnter quantity to add:", 
                "Restock " + productName);

            if (qtyStr != null && !qtyStr.trim().isEmpty()) {
                try {
                    int addQty = Integer.parseInt(qtyStr.trim());
                    if (addQty <= 0) {
                        JOptionPane.showMessageDialog(this, "Quantity must be greater than 0!");
                        return;
                    }

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "UPDATE Products SET stock_quantity = stock_quantity + ? WHERE product_id = ? AND catalog_id = ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setInt(1, addQty);
                        pstmt.setInt(2, productId);
                        pstmt.setInt(3, catalogId);
                        pstmt.executeUpdate();

                        JOptionPane.showMessageDialog(this, 
                            "Stock updated! Added " + addQty + " units.\n" +
                            "New stock: " + (currentStock + addQty), 
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);

                        loadProducts();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity!");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error updating stock: " + ex.getMessage());
                }
            }
        });

        // Load products for inventory view
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT product_id, name, stock_quantity FROM Products WHERE catalog_id = ? ORDER BY name";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, catalogId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getInt("stock_quantity")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Order Management Panel
    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Order ID", "Customer", "Date", "Total", "Status"};
        modelOrders = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableOrders = new JTable(modelOrders);
        panel.add(new JScrollPane(tableOrders), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnViewDetails = new JButton("View Order Details");
        JButton btnUpdateStatus = new JButton("Update Order Status");
        btnPanel.add(btnViewDetails);
        btnPanel.add(btnUpdateStatus);

        btnViewDetails.addActionListener(e -> viewOrderDetails());
        btnUpdateStatus.addActionListener(e -> updateOrderStatus());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadOrders() {
        modelOrders.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT o.order_id, u.name as customer_name, o.order_date, o.total_amount, o.status " +
                        "FROM Orders o " +
                        "JOIN Users u ON o.customer_id = u.user_id " +
                        "WHERE o.seller_id = ? " +
                        "ORDER BY o.order_date DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat df = new DecimalFormat("#0.00");
            while (rs.next()) {
                modelOrders.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getTimestamp("order_date"),
                    "$" + df.format(rs.getDouble("total_amount")),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void viewOrderDetails() {
        int row = tableOrders.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order first!");
            return;
        }

        int orderId = (Integer) modelOrders.getValueAt(row, 0);
        showOrderDetailsDialog(orderId);
    }

    private void showOrderDetailsDialog(int orderId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT oi.product_id, p.name, oi.quantity, oi.price_at_purchase, oi.subtotal " +
                        "FROM Order_Items oi " +
                        "JOIN Products p ON oi.product_id = p.product_id " +
                        "WHERE oi.order_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            JDialog dialog = new JDialog(this, "Order Details - Order #" + orderId, true);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);

            DefaultTableModel model = new DefaultTableModel(
                new String[]{"Product", "Quantity", "Unit Price", "Subtotal"}, 0
            );
            JTable table = new JTable(model);
            DecimalFormat df = new DecimalFormat("#0.00");
            double total = 0.0;

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    "$" + df.format(rs.getDouble("price_at_purchase")),
                    "$" + df.format(rs.getDouble("subtotal"))
                });
                total += rs.getDouble("subtotal");
            }

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            JLabel totalLabel = new JLabel("Total: $" + df.format(total));
            totalLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            panel.add(totalLabel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading order details: " + ex.getMessage());
        }
    }

    private void updateOrderStatus() {
        int row = tableOrders.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order first!");
            return;
        }

        int orderId = (Integer) modelOrders.getValueAt(row, 0);
        String currentStatus = (String) modelOrders.getValueAt(row, 4);

        String[] statuses = {"pending", "paid", "shipped", "delivered"};
        JComboBox<String> cmbStatus = new JComboBox<>(statuses);
        cmbStatus.setSelectedItem(currentStatus);

        Object[] message = {
            "Select new status:", cmbStatus
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Update Order Status", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newStatus = (String) cmbStatus.getSelectedItem();
            
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Validate: Can only update orders for own products
                String validateSql = "SELECT COUNT(*) as count FROM Orders WHERE order_id = ? AND seller_id = ?";
                PreparedStatement validateStmt = conn.prepareStatement(validateSql);
                validateStmt.setInt(1, orderId);
                validateStmt.setInt(2, currentUser.getUserId());
                ResultSet validateRs = validateStmt.executeQuery();
                
                if (!validateRs.next() || validateRs.getInt("count") == 0) {
                    JOptionPane.showMessageDialog(this, "You can only update orders for your own products!");
                    return;
                }

                String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, newStatus);
                updateStmt.setInt(2, orderId);
                updateStmt.executeUpdate();

                if ("shipped".equals(newStatus)) {
                    String shipmentSql = "INSERT INTO Shipments (order_id, tracking_number, shipped_date, status) " +
                                        "VALUES (?, ?, CURDATE(), 'in_transit') " +
                                        "ON DUPLICATE KEY UPDATE shipped_date = CURDATE(), status = 'in_transit'";
                    PreparedStatement shipmentStmt = conn.prepareStatement(shipmentSql);
                    shipmentStmt.setInt(1, orderId);
                    shipmentStmt.setString(2, "TRACK-" + System.currentTimeMillis());
                    shipmentStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Order status updated successfully!");
                loadOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating order status: " + ex.getMessage());
            }
        }
    }

    // Reviews Panel
    private JPanel createReviewsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Review ID", "Product", "Customer", "Rating", "Comment", "Date"};
        modelReviews = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableReviews = new JTable(modelReviews);
        panel.add(new JScrollPane(tableReviews), BorderLayout.CENTER);

        JLabel lblNote = new JLabel("Note: Reviews are read-only. You cannot modify or delete customer reviews.");
        lblNote.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        panel.add(lblNote, BorderLayout.SOUTH);

        return panel;
    }

    private void loadReviews() {
        modelReviews.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT r.review_id, p.name as product_name, u.name as customer_name, " +
                        "r.rating, r.comment, r.review_date " +
                        "FROM Reviews r " +
                        "JOIN Products p ON r.product_id = p.product_id " +
                        "JOIN Users u ON r.customer_id = u.user_id " +
                        "WHERE p.catalog_id = ? " +
                        "ORDER BY r.review_date DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, catalogId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String comment = rs.getString("comment");
                if (comment != null && comment.length() > 50) {
                    comment = comment.substring(0, 50) + "...";
                }
                modelReviews.addRow(new Object[]{
                    rs.getInt("review_id"),
                    rs.getString("product_name"),
                    rs.getString("customer_name"),
                    rs.getInt("rating") + "/5",
                    comment != null ? comment : "",
                    rs.getTimestamp("review_date")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Statistics Panel
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }

    private void updateStats() {
        JPanel panel = (JPanel) tabbedPane.getComponentAt(5);
        panel.removeAll();

        try (Connection conn = DatabaseConnection.getConnection()) {
            DecimalFormat df = new DecimalFormat("#0.00");

            // Total Revenue Per Month
            panel.add(new JLabel("=== Total Revenue Per Month ==="));
            String monthlySql = "SELECT MONTH(order_date) as month, YEAR(order_date) as year, " +
                               "SUM(total_amount) as total " +
                               "FROM Orders " +
                               "WHERE seller_id = ? " +
                               "GROUP BY YEAR(order_date), MONTH(order_date) " +
                               "ORDER BY year DESC, month DESC";
            PreparedStatement monthlyStmt = conn.prepareStatement(monthlySql);
            monthlyStmt.setInt(1, currentUser.getUserId());
            ResultSet monthlyRs = monthlyStmt.executeQuery();

            while (monthlyRs.next()) {
                panel.add(new JLabel(monthlyRs.getInt("year") + "-" + 
                                    String.format("%02d", monthlyRs.getInt("month")) + 
                                    ": $" + df.format(monthlyRs.getDouble("total"))));
            }

            panel.add(Box.createVerticalStrut(20));

            // Best-Selling Products
            panel.add(new JLabel("=== Best-Selling Products ==="));
            String bestSql = "SELECT p.product_id, p.name, SUM(oi.quantity) as total_sold, " +
                            "SUM(oi.subtotal) as total_revenue " +
                            "FROM Order_Items oi " +
                            "JOIN Products p ON oi.product_id = p.product_id " +
                            "JOIN Catalogs c ON p.catalog_id = c.catalog_id " +
                            "WHERE c.seller_id = ? " +
                            "GROUP BY p.product_id, p.name " +
                            "ORDER BY total_sold DESC " +
                            "LIMIT 5";
            PreparedStatement bestStmt = conn.prepareStatement(bestSql);
            bestStmt.setInt(1, currentUser.getUserId());
            ResultSet bestRs = bestStmt.executeQuery();

            while (bestRs.next()) {
                panel.add(new JLabel(bestRs.getString("name") + 
                                    " - Sold: " + bestRs.getInt("total_sold") + 
                                    " units, Revenue: $" + df.format(bestRs.getDouble("total_revenue"))));
            }

            panel.add(Box.createVerticalStrut(20));

            // Most Rated Products
            panel.add(new JLabel("=== Most Rated Products ==="));
            String ratedSql = "SELECT p.product_id, p.name, COUNT(r.review_id) as review_count " +
                              "FROM Products p " +
                              "JOIN Reviews r ON p.product_id = r.product_id " +
                              "WHERE p.catalog_id = ? " +
                              "GROUP BY p.product_id, p.name " +
                              "ORDER BY review_count DESC " +
                              "LIMIT 5";
            PreparedStatement ratedStmt = conn.prepareStatement(ratedSql);
            ratedStmt.setInt(1, catalogId);
            ResultSet ratedRs = ratedStmt.executeQuery();

            while (ratedRs.next()) {
                panel.add(new JLabel(ratedRs.getString("name") + 
                                    " - " + ratedRs.getInt("review_count") + " reviews"));
            }

            panel.add(Box.createVerticalStrut(20));

            panel.add(new JLabel("=== Average Order Value ==="));
            String avgSql = "SELECT AVG(total_amount) as avg_value FROM Orders WHERE seller_id = ?";
            PreparedStatement avgStmt = conn.prepareStatement(avgSql);
            avgStmt.setInt(1, currentUser.getUserId());
            ResultSet avgRs = avgStmt.executeQuery();

            if (avgRs.next()) {
                double avg = avgRs.getDouble("avg_value");
                if (!avgRs.wasNull()) {
                    panel.add(new JLabel("$" + df.format(avg)));
                } else {
                    panel.add(new JLabel("No orders yet"));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            panel.add(new JLabel("Error loading statistics: " + ex.getMessage()));
        }

        panel.revalidate();
        panel.repaint();
    }

    // Helper methods
    private void loadCategories(JComboBox<String> cmb) {
        cmb.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT name FROM Categories ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cmb.addItem(rs.getString("name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int getCategoryId(String categoryName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT category_id FROM Categories WHERE name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("category_id");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }
}
