import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.DecimalFormat;

public class AdminDashboard extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private DefaultTableModel modelUsers, modelCategories, modelShipments;
    private JTable tableUsers, tableCategories, tableShipments;

    public AdminDashboard(User user) {
        this.currentUser = user;
        setTitle("Admin Dashboard - " + user.getName());
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("User Management", createUserPanel());
        tabbedPane.addTab("Category Management", createCategoryPanel());
        tabbedPane.addTab("Shipment Management", createShipmentPanel());
        tabbedPane.addTab("System Statistics", createStatsPanel());

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
            if (index == 0) loadUsers();
            else if (index == 1) loadCategories();
            else if (index == 2) loadShipments();
            else if (index == 3) updateStats();
        });
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"User ID", "Name", "Email", "Role"};
        modelUsers = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableUsers = new JTable(modelUsers);
        tableUsers.getColumnModel().getColumn(0).setMinWidth(0);
        tableUsers.getColumnModel().getColumn(0).setMaxWidth(0);
        tableUsers.getColumnModel().getColumn(0).setWidth(0);
        panel.add(new JScrollPane(tableUsers), BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Role:"));
        JComboBox<String> cmbRoleFilter = new JComboBox<>(new String[]{"All", "Customer", "Seller", "Administrator"});
        filterPanel.add(cmbRoleFilter);
        JButton btnFilter = new JButton("Apply Filter");
        btnFilter.addActionListener(e -> loadUsers((String) cmbRoleFilter.getSelectedItem()));
        filterPanel.add(btnFilter);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add User");
        JButton btnEdit = new JButton("Edit User");
        JButton btnDelete = new JButton("Delete User");
        JButton btnRefresh = new JButton("Refresh");
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        btnAdd.addActionListener(e -> addUser());
        btnEdit.addActionListener(e -> editUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnRefresh.addActionListener(e -> loadUsers());

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(filterPanel, BorderLayout.NORTH);
        southPanel.add(btnPanel, BorderLayout.SOUTH);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadUsers() {
        loadUsers("All");
    }

    private void loadUsers(String roleFilter) {
        modelUsers.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT user_id, name, email, role FROM Users";
            if (!"All".equals(roleFilter)) {
                sql += " WHERE role = ?";
            }
            sql += " ORDER BY name";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if (!"All".equals(roleFilter)) {
                pstmt.setString(1, roleFilter);
            }
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                modelUsers.addRow(new Object[]{
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
        }
    }

    private void addUser() {
        JTextField txtName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        String[] roles = {"Customer", "Seller", "Administrator"};
        JComboBox<String> cmbRole = new JComboBox<>(roles);

        Object[] message = {
            "Name:", txtName,
            "Email:", txtEmail,
            "Password:", txtPassword,
            "Role:", cmbRole
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add User", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (txtName.getText().trim().isEmpty() || 
                txtEmail.getText().trim().isEmpty() || 
                new String(txtPassword.getPassword()).isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
                return;
            }

            if (!txtEmail.getText().contains("@")) {
                JOptionPane.showMessageDialog(this, "Invalid email format!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check email uniqueness
                String checkSql = "SELECT user_id FROM Users WHERE email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, txtEmail.getText().trim());
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next()) {
                    JOptionPane.showMessageDialog(this, 
                        "Email already exists! Please use a different email.", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO Users (name, email, password, role) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, txtName.getText().trim());
                pstmt.setString(2, txtEmail.getText().trim());
                pstmt.setString(3, new String(txtPassword.getPassword()));
                pstmt.setString(4, (String) cmbRole.getSelectedItem());
                pstmt.executeUpdate();

                // If seller, create catalog
                if ("Seller".equals(cmbRole.getSelectedItem())) {
                    String catalogSql = "INSERT INTO Catalogs (seller_id) VALUES (?)";
                    PreparedStatement catalogStmt = conn.prepareStatement(catalogSql);
                    catalogStmt.setInt(1, getUserIdByEmail(conn, txtEmail.getText().trim()));
                    catalogStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "User added successfully!");
                loadUsers();
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, 
                    "Email already exists! Please use a different email.", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding user: " + ex.getMessage());
            }
        }
    }

    private void editUser() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit!");
            return;
        }

        int userId = (Integer) modelUsers.getValueAt(row, 0);
        String currentName = (String) modelUsers.getValueAt(row, 1);
        String currentEmail = (String) modelUsers.getValueAt(row, 2);
        String currentRole = (String) modelUsers.getValueAt(row, 3);

        JTextField txtName = new JTextField(currentName, 20);
        JTextField txtEmail = new JTextField(currentEmail, 20);
        String[] roles = {"Customer", "Seller", "Administrator"};
        JComboBox<String> cmbRole = new JComboBox<>(roles);
        cmbRole.setSelectedItem(currentRole);

        Object[] message = {
            "Name:", txtName,
            "Email:", txtEmail,
            "Role:", cmbRole
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit User", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check email uniqueness if changed
                if (!txtEmail.getText().trim().equals(currentEmail)) {
                    String checkSql = "SELECT user_id FROM Users WHERE email = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                    checkStmt.setString(1, txtEmail.getText().trim());
                    ResultSet checkRs = checkStmt.executeQuery();
                    
                    if (checkRs.next()) {
                        JOptionPane.showMessageDialog(this, 
                            "Email already exists! Please use a different email.", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                String sql = "UPDATE Users SET name = ?, email = ?, role = ? WHERE user_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, txtName.getText().trim());
                pstmt.setString(2, txtEmail.getText().trim());
                pstmt.setString(3, (String) cmbRole.getSelectedItem());
                pstmt.setInt(4, userId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "User updated successfully!");
                loadUsers();
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, 
                    "Email already exists! Please use a different email.", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating user: " + ex.getMessage());
            }
        }
    }

    private void deleteUser() {
        int row = tableUsers.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!");
            return;
        }

        int userId = (Integer) modelUsers.getValueAt(row, 0);
        String userName = (String) modelUsers.getValueAt(row, 1);

        if (userId == currentUser.getUserId()) {
            JOptionPane.showMessageDialog(this, "You cannot delete yourself!", 
                "Delete Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete user '" + userName + "'?\n" +
            "This will fail if the user has active orders.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check for active orders
                String checkSql = "SELECT COUNT(*) as count FROM Orders WHERE customer_id = ? OR seller_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, userId);
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next() && checkRs.getInt("count") > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete user! User has active orders.\n" +
                        "The database constraint prevents deletion.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "DELETE FROM Users WHERE user_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "User deleted successfully!");
                loadUsers();
            } catch (SQLException ex) {
                ex.printStackTrace();
                if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("RESTRICT")) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete user! User is referenced in existing orders.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage());
                }
            }
        }
    }

    // Category Management Panel
    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Category ID", "Name", "Parent Category"};
        modelCategories = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableCategories = new JTable(modelCategories);
        tableCategories.getColumnModel().getColumn(0).setMinWidth(0);
        tableCategories.getColumnModel().getColumn(0).setMaxWidth(0);
        tableCategories.getColumnModel().getColumn(0).setWidth(0);
        panel.add(new JScrollPane(tableCategories), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add Category");
        JButton btnEdit = new JButton("Edit Category");
        JButton btnDelete = new JButton("Delete Category");
        JButton btnRefresh = new JButton("Refresh");
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        btnAdd.addActionListener(e -> addCategory());
        btnEdit.addActionListener(e -> editCategory());
        btnDelete.addActionListener(e -> deleteCategory());
        btnRefresh.addActionListener(e -> loadCategories());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadCategories() {
        modelCategories.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT c.category_id, c.name, " +
                        "COALESCE(p.name, 'None') as parent_name " +
                        "FROM Categories c " +
                        "LEFT JOIN Categories p ON c.parent_category_id = p.category_id " +
                        "ORDER BY c.name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                modelCategories.addRow(new Object[]{
                    rs.getInt("category_id"),
                    rs.getString("name"),
                    rs.getString("parent_name")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading categories: " + ex.getMessage());
        }
    }

    private void addCategory() {
        JTextField txtName = new JTextField(20);
        JComboBox<String> cmbParent = new JComboBox<>();
        cmbParent.addItem("None");
        loadCategoriesForCombo(cmbParent);

        Object[] message = {
            "Category Name:", txtName,
            "Parent Category (optional):", cmbParent
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Category", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (txtName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Category name is required!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check name uniqueness
                String checkSql = "SELECT category_id FROM Categories WHERE name = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, txtName.getText().trim());
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next()) {
                    JOptionPane.showMessageDialog(this, 
                        "Category name already exists!", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String selectedParent = (String) cmbParent.getSelectedItem();
                String sql;
                PreparedStatement pstmt;

                if ("None".equals(selectedParent) || selectedParent == null) {
                    sql = "INSERT INTO Categories (name, created_by) VALUES (?, ?)";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, txtName.getText().trim());
                    pstmt.setInt(2, currentUser.getUserId());
                } else {
                    int parentId = getCategoryIdByName(conn, selectedParent);
                    sql = "INSERT INTO Categories (name, parent_category_id, created_by) VALUES (?, ?, ?)";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, txtName.getText().trim());
                    pstmt.setInt(2, parentId);
                    pstmt.setInt(3, currentUser.getUserId());
                }
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Category added successfully!");
                loadCategories();
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, 
                    "Category name already exists!", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding category: " + ex.getMessage());
            }
        }
    }

    private void editCategory() {
        int row = tableCategories.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to edit!");
            return;
        }

        int categoryId = (Integer) modelCategories.getValueAt(row, 0);
        String currentName = (String) modelCategories.getValueAt(row, 1);
        String currentParent = (String) modelCategories.getValueAt(row, 2);

        JTextField txtName = new JTextField(currentName, 20);
        JComboBox<String> cmbParent = new JComboBox<>();
        cmbParent.addItem("None");
        loadCategoriesForCombo(cmbParent);
        if (!"None".equals(currentParent)) {
            cmbParent.setSelectedItem(currentParent);
        }

        Object[] message = {
            "Category Name:", txtName,
            "Parent Category:", cmbParent
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Category", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check name uniqueness if changed
                if (!txtName.getText().trim().equals(currentName)) {
                    String checkSql = "SELECT category_id FROM Categories WHERE name = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                    checkStmt.setString(1, txtName.getText().trim());
                    ResultSet checkRs = checkStmt.executeQuery();
                    
                    if (checkRs.next()) {
                        JOptionPane.showMessageDialog(this, 
                            "Category name already exists!", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                String selectedParent = (String) cmbParent.getSelectedItem();
                String sql;
                PreparedStatement pstmt;

                if ("None".equals(selectedParent) || selectedParent == null) {
                    sql = "UPDATE Categories SET name = ?, parent_category_id = NULL WHERE category_id = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, txtName.getText().trim());
                    pstmt.setInt(2, categoryId);
                } else {
                    int parentId = getCategoryIdByName(conn, selectedParent);
                    if (parentId == categoryId) {
                        JOptionPane.showMessageDialog(this, 
                            "A category cannot be its own parent!", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sql = "UPDATE Categories SET name = ?, parent_category_id = ? WHERE category_id = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, txtName.getText().trim());
                    pstmt.setInt(2, parentId);
                    pstmt.setInt(3, categoryId);
                }
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Category updated successfully!");
                loadCategories();
            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(this, 
                    "Category name already exists!", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating category: " + ex.getMessage());
            }
        }
    }

    private void deleteCategory() {
        int row = tableCategories.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a category to delete!");
            return;
        }

        int categoryId = (Integer) modelCategories.getValueAt(row, 0);
        String categoryName = (String) modelCategories.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete category '" + categoryName + "'?\n" +
            "This will fail if the category has products or subcategories.", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Check if category has products
                String checkSql = "SELECT COUNT(*) as count FROM Products WHERE category_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, categoryId);
                ResultSet checkRs = checkStmt.executeQuery();
                
                if (checkRs.next() && checkRs.getInt("count") > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete category! It has products.\n" +
                        "The database constraint prevents deletion.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "DELETE FROM Categories WHERE category_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, categoryId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Category deleted successfully!");
                loadCategories();
            } catch (SQLException ex) {
                ex.printStackTrace();
                if (ex.getMessage().contains("foreign key") || ex.getMessage().contains("RESTRICT")) {
                    JOptionPane.showMessageDialog(this, 
                        "Cannot delete category! It is referenced by products or has subcategories.", 
                        "Delete Error", 
                        JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error deleting category: " + ex.getMessage());
                }
            }
        }
    }

    // Shipment Management Panel
    private JPanel createShipmentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Order ID", "Customer", "Seller", "Tracking Number", "Status", "Shipped Date"};
        modelShipments = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableShipments = new JTable(modelShipments);
        panel.add(new JScrollPane(tableShipments), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnApprove = new JButton("Approve/Update Shipment");
        JButton btnRefresh = new JButton("Refresh");
        btnPanel.add(btnApprove);
        btnPanel.add(btnRefresh);

        btnApprove.addActionListener(e -> updateShipment());
        btnRefresh.addActionListener(e -> loadShipments());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadShipments() {
        modelShipments.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT s.order_id, " +
                        "cu.name as customer_name, su.name as seller_name, " +
                        "s.tracking_number, s.status, s.shipped_date " +
                        "FROM Shipments s " +
                        "JOIN Orders o ON s.order_id = o.order_id " +
                        "JOIN Users cu ON o.customer_id = cu.user_id " +
                        "JOIN Users su ON o.seller_id = su.user_id " +
                        "ORDER BY s.order_id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                modelShipments.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("seller_name"),
                    rs.getString("tracking_number") != null ? rs.getString("tracking_number") : "N/A",
                    rs.getString("status"),
                    rs.getDate("shipped_date") != null ? rs.getDate("shipped_date") : "N/A"
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading shipments: " + ex.getMessage());
        }
    }

    private void updateShipment() {
        int row = tableShipments.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a shipment to update!");
            return;
        }

        int orderId = (Integer) modelShipments.getValueAt(row, 0);

        // Check if payment is completed
        try (Connection conn = DatabaseConnection.getConnection()) {
            String paymentSql = "SELECT status FROM Payments WHERE order_id = ? AND status = 'completed'";
            PreparedStatement paymentStmt = conn.prepareStatement(paymentSql);
            paymentStmt.setInt(1, orderId);
            ResultSet paymentRs = paymentStmt.executeQuery();

            if (!paymentRs.next()) {
                JOptionPane.showMessageDialog(this, 
                    "Cannot approve shipment! Payment must be completed first.", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] statuses = {"pending", "in_transit", "delivered", "failed"};
            JComboBox<String> cmbStatus = new JComboBox<>(statuses);
            JTextField txtTracking = new JTextField(20);
            JTextField txtDeliveryDate = new JTextField(10);

            Object[] message = {
                "Status:", cmbStatus,
                "Tracking Number:", txtTracking,
                "Delivery Date (YYYY-MM-DD):", txtDeliveryDate
            };

            int option = JOptionPane.showConfirmDialog(this, message, "Update Shipment", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String newStatus = (String) cmbStatus.getSelectedItem();
                String trackingNumber = txtTracking.getText().trim();
                String deliveryDate = txtDeliveryDate.getText().trim();

                String updateSql = "UPDATE Shipments SET status = ?, tracking_number = ?";
                if (!deliveryDate.isEmpty()) {
                    updateSql += ", delivery_date = ?";
                }
                if ("delivered".equals(newStatus)) {
                    updateSql += ", delivery_date = CURDATE()";
                }
                updateSql += " WHERE order_id = ?";

                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                int paramIndex = 1;
                updateStmt.setString(paramIndex++, newStatus);
                updateStmt.setString(paramIndex++, trackingNumber.isEmpty() ? null : trackingNumber);
                if (!deliveryDate.isEmpty() && !"delivered".equals(newStatus)) {
                    updateStmt.setString(paramIndex++, deliveryDate);
                }
                updateStmt.setInt(paramIndex, orderId);
                updateStmt.executeUpdate();

                // If status is delivered, update order status
                if ("delivered".equals(newStatus)) {
                    String orderSql = "UPDATE Orders SET status = 'delivered' WHERE order_id = ?";
                    PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                    orderStmt.setInt(1, orderId);
                    orderStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Shipment updated successfully!");
                loadShipments();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating shipment: " + ex.getMessage());
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
        JPanel panel = (JPanel) tabbedPane.getComponentAt(3);
        panel.removeAll();

        try (Connection conn = DatabaseConnection.getConnection()) {
            DecimalFormat df = new DecimalFormat("#0.00");

            // Total Sales
            panel.add(new JLabel("=== Total Sales ==="));
            String totalSql = "SELECT SUM(total_amount) as total FROM Orders " +
                             "WHERE status IN ('paid', 'shipped', 'delivered')";
            Statement totalStmt = conn.createStatement();
            ResultSet totalRs = totalStmt.executeQuery(totalSql);
            if (totalRs.next()) {
                double total = totalRs.getDouble("total");
                if (!totalRs.wasNull()) {
                    panel.add(new JLabel("$" + df.format(total)));
                } else {
                    panel.add(new JLabel("No sales yet"));
                }
            }

            panel.add(Box.createVerticalStrut(20));

            // Top-Selling Categories
            panel.add(new JLabel("=== Top-Selling Categories ==="));
            String categorySql = "SELECT c.name, SUM(oi.quantity) as total_quantity " +
                                "FROM Order_Items oi " +
                                "JOIN Products p ON oi.product_id = p.product_id " +
                                "JOIN Categories c ON p.category_id = c.category_id " +
                                "JOIN Orders o ON oi.order_id = o.order_id " +
                                "WHERE o.status IN ('paid', 'shipped', 'delivered') " +
                                "GROUP BY c.category_id, c.name " +
                                "ORDER BY total_quantity DESC " +
                                "LIMIT 5";
            Statement categoryStmt = conn.createStatement();
            ResultSet categoryRs = categoryStmt.executeQuery(categorySql);
            while (categoryRs.next()) {
                panel.add(new JLabel(categoryRs.getString("name") + 
                                    " - " + categoryRs.getInt("total_quantity") + " items sold"));
            }

            panel.add(Box.createVerticalStrut(20));

            // Top Sellers
            panel.add(new JLabel("=== Top Sellers ==="));
            String sellerSql = "SELECT u.user_id, u.name, COUNT(DISTINCT o.order_id) as order_count, " +
                              "SUM(o.total_amount) as total_revenue " +
                              "FROM Users u " +
                              "JOIN Orders o ON u.user_id = o.seller_id " +
                              "WHERE o.status IN ('paid', 'shipped', 'delivered') " +
                              "GROUP BY u.user_id, u.name " +
                              "ORDER BY total_revenue DESC " +
                              "LIMIT 10";
            Statement sellerStmt = conn.createStatement();
            ResultSet sellerRs = sellerStmt.executeQuery(sellerSql);
            while (sellerRs.next()) {
                panel.add(new JLabel(sellerRs.getString("name") + 
                                    " - Orders: " + sellerRs.getInt("order_count") + 
                                    ", Revenue: $" + df.format(sellerRs.getDouble("total_revenue"))));
            }

            panel.add(Box.createVerticalStrut(20));

            // Most Popular Items
            panel.add(new JLabel("=== Most Popular Items ==="));
            String popularSql = "SELECT p.product_id, p.name, COUNT(DISTINCT oi.order_id) as order_count " +
                              "FROM Products p " +
                              "JOIN Order_Items oi ON p.product_id = oi.product_id " +
                              "GROUP BY p.product_id, p.name " +
                              "ORDER BY order_count DESC " +
                              "LIMIT 5";
            Statement popularStmt = conn.createStatement();
            ResultSet popularRs = popularStmt.executeQuery(popularSql);
            while (popularRs.next()) {
                panel.add(new JLabel(popularRs.getString("name") + 
                                    " - Ordered " + popularRs.getInt("order_count") + " times"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            panel.add(new JLabel("Error loading statistics: " + ex.getMessage()));
        }

        panel.revalidate();
        panel.repaint();
    }

    // Helper methods
    private int getUserIdByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT user_id FROM Users WHERE email = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, email);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("user_id");
        }
        return -1;
    }

    private void loadCategoriesForCombo(JComboBox<String> cmb) {
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

    private int getCategoryIdByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT category_id FROM Categories WHERE name = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("category_id");
        }
        return -1;
    }
}
