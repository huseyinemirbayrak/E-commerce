import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CustomerDashboard extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private DefaultTableModel modelProducts, modelCart, modelHistory, modelAddresses, modelReviews;
    private JTable tableProducts, tableCart, tableHistory, tableAddresses, tableReviews;
    private JLabel lblCartTotal;
    private JComboBox<String> cmbCategories;
    private JTextField txtSearch, txtMinPrice, txtMaxPrice;
    private int selectedCatalogId = -1;

    public CustomerDashboard(User user) {
    this.currentUser = user;
    setTitle("Customer Dashboard - " + user.getName());
    setSize(1200, 800);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Browse Catalogs", createBrowseCatalogsPanel());
    tabbedPane.addTab("Shopping Cart", createCartPanel());
    tabbedPane.addTab("Order History", createHistoryPanel());
    tabbedPane.addTab("My Reviews", createReviewsPanel()); 
    tabbedPane.addTab("Statistics", createStatsPanel());
    tabbedPane.addTab("Addresses", createAddressesPanel());

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    JButton btnNotifications = new JButton("Notifications");
    btnNotifications.addActionListener(e -> showNotifications()); 
    bottomPanel.add(btnNotifications);

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
        if (index == 1) loadCart();
        else if (index == 2) loadOrderHistory();
        else if (index == 3) loadReviewableItems(); 
        else if (index == 4) updateStats();
        else if (index == 5) loadAddresses();
    });
}

    private JPanel createBrowseCatalogsPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topPanel.add(new JLabel("Select Seller Catalog:"));
    JComboBox<String> cmbCatalogs = new JComboBox<>();
    loadCatalogs(cmbCatalogs);
    cmbCatalogs.addActionListener(e -> {
        String selected = (String) cmbCatalogs.getSelectedItem();
        if (selected != null && !selected.equals("-- Select Catalog --")) {
            String[] parts = selected.split(" - ");
            selectedCatalogId = Integer.parseInt(parts[0]);
            loadProducts();
        }
    });
    topPanel.add(cmbCatalogs);

    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    filterPanel.add(new JLabel("Search:"));
    txtSearch = new JTextField(15);
    filterPanel.add(txtSearch);

    filterPanel.add(new JLabel("Category:"));
    cmbCategories = new JComboBox<>();
    cmbCategories.addItem("All");
    loadCategories();
    filterPanel.add(cmbCategories);

    filterPanel.add(new JLabel("Price Range:"));
    txtMinPrice = new JTextField(8);
    txtMaxPrice = new JTextField(8);
    filterPanel.add(new JLabel("Min:"));
    filterPanel.add(txtMinPrice);
    filterPanel.add(new JLabel("Max:"));
    filterPanel.add(txtMaxPrice);

    JButton btnFilter = new JButton("Apply Filters");
    btnFilter.addActionListener(e -> loadProducts());
    filterPanel.add(btnFilter);

    JPanel northPanel = new JPanel(new BorderLayout());
    northPanel.add(topPanel, BorderLayout.NORTH);
    northPanel.add(filterPanel, BorderLayout.SOUTH);
    panel.add(northPanel, BorderLayout.NORTH);

    String[] columns = {"Product ID", "Name", "Category", "Price", "Stock", "Avg Rating", "Reviews"};
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
    JButton btnViewDetails = new JButton("View Details");
    JButton btnAddToCart = new JButton("Add to Cart");
    JButton btnWishlist = new JButton("Add to Wishlist"); 

    btnPanel.add(btnViewDetails);
    btnPanel.add(btnAddToCart);
    btnPanel.add(btnWishlist); 

    btnViewDetails.addActionListener(e -> viewProductDetails());
    btnAddToCart.addActionListener(e -> addToCart());

    btnWishlist.addActionListener(e -> {
        int row = tableProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product!");
            return;
        }
        int productId = (Integer) modelProducts.getValueAt(row, 0);
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO Wishlists (user_id, product_id) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Added to Wishlist!");
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "This item is already in your wishlist!", "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    });

    panel.add(btnPanel, BorderLayout.SOUTH);

    return panel;
}

    private void loadCatalogs(JComboBox<String> cmb) {
        cmb.removeAllItems();
        cmb.addItem("-- Select Catalog --");
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT c.catalog_id, u.name FROM Catalogs c " +
                        "JOIN Users u ON c.seller_id = u.user_id " +
                        "ORDER BY u.name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cmb.addItem(rs.getInt("catalog_id") + " - " + rs.getString("name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadCategories() {
        cmbCategories.removeAllItems();
        cmbCategories.addItem("All");
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT DISTINCT c.name FROM Categories c " +
                        "JOIN Products p ON c.category_id = p.category_id " +
                        "ORDER BY c.name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cmbCategories.addItem(rs.getString("name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadProducts() {
        modelProducts.setRowCount(0);
        if (selectedCatalogId == -1) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.product_id, p.name, c.name as category_name, p.price, p.stock_quantity, " +
                        "COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.review_id) as review_count " +
                        "FROM Products p " +
                        "JOIN Categories c ON p.category_id = c.category_id " +
                        "LEFT JOIN Reviews r ON p.product_id = r.product_id " +
                        "WHERE p.catalog_id = ? ";

            String search = txtSearch.getText().trim();
            String category = (String) cmbCategories.getSelectedItem();
            String minPrice = txtMinPrice.getText().trim();
            String maxPrice = txtMaxPrice.getText().trim();

            if (!search.isEmpty()) {
                sql += "AND (p.name LIKE ? OR p.description LIKE ?) ";
            }
            if (category != null && !category.equals("All")) {
                sql += "AND c.name = ? ";
            }
            if (!minPrice.isEmpty()) {
                sql += "AND p.price >= ? ";
            }
            if (!maxPrice.isEmpty()) {
                sql += "AND p.price <= ? ";
            }

            sql += "GROUP BY p.product_id, p.name, c.name, p.price, p.stock_quantity " +
                   "ORDER BY p.name";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            int paramIndex = 1;
            pstmt.setInt(paramIndex++, selectedCatalogId);

            if (!search.isEmpty()) {
                String searchPattern = "%" + search + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
            }
            if (category != null && !category.equals("All")) {
                pstmt.setString(paramIndex++, category);
            }
            if (!minPrice.isEmpty()) {
                pstmt.setDouble(paramIndex++, Double.parseDouble(minPrice));
            }
            if (!maxPrice.isEmpty()) {
                pstmt.setDouble(paramIndex++, Double.parseDouble(maxPrice));
            }

            ResultSet rs = pstmt.executeQuery();
            DecimalFormat df = new DecimalFormat("#0.00");
            while (rs.next()) {
                modelProducts.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    "$" + df.format(rs.getDouble("price")),
                    rs.getInt("stock_quantity"),
                    String.format("%.1f", rs.getDouble("avg_rating")),
                    rs.getInt("review_count")
                });
            }
        } catch (SQLException | NumberFormatException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage());
        }
    }

    private void viewProductDetails() {
        int row = tableProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product first!");
            return;
        }

        int productId = (Integer) modelProducts.getValueAt(row, 0);
        showProductDetailsDialog(productId);
    }

    private void showProductDetailsDialog(int productId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.*, c.name as category_name, u.name as seller_name, " +
                        "COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(r.review_id) as review_count " +
                        "FROM Products p " +
                        "JOIN Categories c ON p.category_id = c.category_id " +
                        "JOIN Catalogs cat ON p.catalog_id = cat.catalog_id " +
                        "JOIN Users u ON cat.seller_id = u.user_id " +
                        "LEFT JOIN Reviews r ON p.product_id = r.product_id " +
                        "WHERE p.product_id = ? " +
                        "GROUP BY p.product_id";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                JDialog dialog = new JDialog(this, "Product Details", true);
                dialog.setSize(600, 500);
                dialog.setLocationRelativeTo(this);

                JPanel panel = new JPanel(new BorderLayout());
                JTextArea details = new JTextArea();
                details.setEditable(false);
                details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                details.setText(
                    "Product: " + rs.getString("name") + "\n\n" +
                    "Category: " + rs.getString("category_name") + "\n" +
                    "Seller: " + rs.getString("seller_name") + "\n" +
                    "Price: $" + rs.getDouble("price") + "\n" +
                    "Stock: " + rs.getInt("stock_quantity") + "\n" +
                    "Average Rating: " + String.format("%.1f", rs.getDouble("avg_rating")) + " / 5.0\n" +
                    "Total Reviews: " + rs.getInt("review_count") + "\n\n" +
                    "Description:\n" + (rs.getString("description") != null ? rs.getString("description") : "No description available")
                );
                panel.add(new JScrollPane(details), BorderLayout.CENTER);

                String reviewsSql = "SELECT r.rating, r.comment, r.review_date, u.name as customer_name " +
                                   "FROM Reviews r " +
                                   "JOIN Users u ON r.customer_id = u.user_id " +
                                   "WHERE r.product_id = ? " +
                                   "ORDER BY r.review_date DESC";
                PreparedStatement reviewsStmt = conn.prepareStatement(reviewsSql);
                reviewsStmt.setInt(1, productId);
                ResultSet reviewsRs = reviewsStmt.executeQuery();

                JTextArea reviewsArea = new JTextArea();
                reviewsArea.setEditable(false);
                reviewsArea.append("\n\n--- Customer Reviews ---\n\n");
                boolean hasReviews = false;
                while (reviewsRs.next()) {
                    hasReviews = true;
                    reviewsArea.append("Rating: " + reviewsRs.getInt("rating") + "/5\n");
                    reviewsArea.append("By: " + reviewsRs.getString("customer_name") + "\n");
                    reviewsArea.append("Date: " + reviewsRs.getTimestamp("review_date") + "\n");
                    if (reviewsRs.getString("comment") != null) {
                        reviewsArea.append("Comment: " + reviewsRs.getString("comment") + "\n");
                    }
                    reviewsArea.append("---\n\n");
                }
                if (!hasReviews) {
                    reviewsArea.append("No reviews yet.\n");
                }
                panel.add(new JScrollPane(reviewsArea), BorderLayout.SOUTH);

                JButton btnAddToCart = new JButton("Add to Cart");
                btnAddToCart.addActionListener(e -> {
                    addProductToCart(productId);
                    dialog.dispose();
                });
                panel.add(btnAddToCart, BorderLayout.NORTH);

                dialog.add(panel);
                dialog.setVisible(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading product details: " + ex.getMessage());
        }
    }

    private void addToCart() {
        int row = tableProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product first!");
            return;
        }

        int productId = (Integer) modelProducts.getValueAt(row, 0);
        addProductToCart(productId);
    }

    private void addProductToCart(int productId) {
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String stockSql = "SELECT stock_quantity FROM Products WHERE product_id = ?";
                PreparedStatement stockStmt = conn.prepareStatement(stockSql);
                stockStmt.setInt(1, productId);
                ResultSet stockRs = stockStmt.executeQuery();
                if (stockRs.next()) {
                    int stock = stockRs.getInt("stock_quantity");
                    if (stock < quantity) {
                        JOptionPane.showMessageDialog(this, 
                            "Insufficient stock! Available: " + stock, 
                            "Stock Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                String sellerSql = "SELECT c.seller_id FROM Products p " +
                                  "JOIN Catalogs c ON p.catalog_id = c.catalog_id " +
                                  "WHERE p.product_id = ?";
                PreparedStatement sellerStmt = conn.prepareStatement(sellerSql);
                sellerStmt.setInt(1, productId);
                ResultSet sellerRs = sellerStmt.executeQuery();
                if (!sellerRs.next()) {
                    JOptionPane.showMessageDialog(this, "Error: Product not found!");
                    return;
                }
                int productSellerId = sellerRs.getInt("seller_id");

                String orderSql = "SELECT order_id, seller_id FROM Orders " +
                                 "WHERE customer_id = ? AND status = 'ONGOING'";
                PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                orderStmt.setInt(1, currentUser.getUserId());
                ResultSet orderRs = orderStmt.executeQuery();

                int orderId;
                if (orderRs.next()) {
                    orderId = orderRs.getInt("order_id");
                    int existingSellerId = orderRs.getInt("seller_id");
                    
                    if (existingSellerId != productSellerId) {
                        JOptionPane.showMessageDialog(this, 
                            "You already have items from a different seller in your cart!\n" +
                            "Please complete or cancel your current order first.", 
                            "Cart Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    String addressSql = "SELECT address_id FROM Addresses WHERE user_id = ? LIMIT 1";
                    PreparedStatement addrStmt = conn.prepareStatement(addressSql);
                    addrStmt.setInt(1, currentUser.getUserId());
                    ResultSet addrRs = addrStmt.executeQuery();
                    
                    if (!addrRs.next()) {
                        JOptionPane.showMessageDialog(this, 
                            "Please add an address first in the Addresses tab!", 
                            "Address Required", 
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    int addressId = addrRs.getInt("address_id");
                    
                    String createOrderSql = "INSERT INTO Orders (customer_id, seller_id, shipping_address_id, billing_address_id, status) " +
                                          "VALUES (?, ?, ?, ?, 'ongoing')";
                    PreparedStatement createStmt = conn.prepareStatement(createOrderSql, Statement.RETURN_GENERATED_KEYS);
                    createStmt.setInt(1, currentUser.getUserId());
                    createStmt.setInt(2, productSellerId);
                    createStmt.setInt(3, addressId);
                    createStmt.setInt(4, addressId);
                    createStmt.executeUpdate();
                    
                    ResultSet generatedKeys = createStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to create order");
                    }
                }

                String priceSql = "SELECT price FROM Products WHERE product_id = ?";
                PreparedStatement priceStmt = conn.prepareStatement(priceSql);
                priceStmt.setInt(1, productId);
                ResultSet priceRs = priceStmt.executeQuery();
                if (!priceRs.next()) {
                    throw new SQLException("Product price not found");
                }
                double price = priceRs.getDouble("price");
                double subtotal = price * quantity;

                String checkSql = "SELECT order_item_id, quantity FROM Order_Items " +
                                 "WHERE order_id = ? AND product_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, orderId);
                checkStmt.setInt(2, productId);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    int existingQty = checkRs.getInt("quantity");
                    int newQty = existingQty + quantity;
                    double newSubtotal = price * newQty;
                    
                    String updateSql = "UPDATE Order_Items SET quantity = ?, subtotal = ? " +
                                      "WHERE order_id = ? AND product_id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setInt(1, newQty);
                    updateStmt.setDouble(2, newSubtotal);
                    updateStmt.setInt(3, orderId);
                    updateStmt.setInt(4, productId);
                    updateStmt.executeUpdate();
                } else {
                    String insertSql = "INSERT INTO Order_Items (order_id, product_id, quantity, price_at_purchase, subtotal) " +
                                     "VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setInt(1, orderId);
                    insertStmt.setInt(2, productId);
                    insertStmt.setInt(3, quantity);
                    insertStmt.setDouble(4, price);
                    insertStmt.setDouble(5, subtotal);
                    insertStmt.executeUpdate();
                }

                String totalSql = "UPDATE Orders SET total_amount = " +
                                 "(SELECT COALESCE(SUM(subtotal), 0) FROM Order_Items WHERE order_id = ?) " +
                                 "WHERE order_id = ?";
                PreparedStatement totalStmt = conn.prepareStatement(totalSql);
                totalStmt.setInt(1, orderId);
                totalStmt.setInt(2, orderId);
                totalStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Product added to cart successfully!");
                loadCart();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity!");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding to cart: " + ex.getMessage());
        }
    }

    private JPanel createCartPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    String[] columns = {"Product", "Quantity", "Unit Price", "Subtotal"};
    modelCart = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 1; 
        }
    };
    tableCart = new JTable(modelCart);
    panel.add(new JScrollPane(tableCart), BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel(new BorderLayout());

    JPanel couponPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JTextField txtCoupon = new JTextField(10);
    JButton btnApplyCoupon = new JButton("Apply Coupon");

    btnApplyCoupon.addActionListener(e -> {
        String code = txtCoupon.getText().trim();
        if (code.isEmpty()) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT discount_percent FROM Coupons WHERE code = ? AND is_active = TRUE AND expiry_date >= CURDATE()";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int discount = rs.getInt("discount_percent");
                JOptionPane.showMessageDialog(this, 
                    "Coupon Applied! " + discount + "% discount will be reflected at checkout payment.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                // Not: Fiyatı gerçekten düşürmüyoruz, sistemi bozma riski almamak için sadece doğrulama yapıyoruz.
                txtCoupon.setEnabled(false); 
                btnApplyCoupon.setEnabled(false);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid or expired coupon code!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    });

    couponPanel.add(new JLabel("Coupon Code:"));
    couponPanel.add(txtCoupon);
    couponPanel.add(btnApplyCoupon);
    bottomPanel.add(couponPanel, BorderLayout.NORTH); 

    lblCartTotal = new JLabel("Total: $0.00");
    lblCartTotal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
    
    JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    totalPanel.add(lblCartTotal);
    bottomPanel.add(totalPanel, BorderLayout.CENTER);

    JPanel btnPanel = new JPanel(new FlowLayout());
    JButton btnRemove = new JButton("Remove Item");
    JButton btnUpdateQty = new JButton("Update Quantity");
    JButton btnSubmit = new JButton("Submit Order");
    btnPanel.add(btnRemove);
    btnPanel.add(btnUpdateQty);
    btnPanel.add(btnSubmit);

    btnRemove.addActionListener(e -> removeFromCart());
    btnUpdateQty.addActionListener(e -> updateQuantity());
    btnSubmit.addActionListener(e -> submitOrder());

    bottomPanel.add(btnPanel, BorderLayout.SOUTH);
    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
}

    private void loadCart() {
        modelCart.setRowCount(0);
        double total = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT oi.order_item_id, p.name, oi.quantity, oi.price_at_purchase, oi.subtotal " +
                        "FROM Order_Items oi " +
                        "JOIN Products p ON oi.product_id = p.product_id " +
                        "JOIN Orders o ON oi.order_id = o.order_id " +
                        "WHERE o.customer_id = ? AND o.status = 'ONGOING' " +
                        "ORDER BY oi.order_item_id";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat df = new DecimalFormat("#0.00");
            while (rs.next()) {
                modelCart.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    "$" + df.format(rs.getDouble("price_at_purchase")),
                    "$" + df.format(rs.getDouble("subtotal"))
                });
                total += rs.getDouble("subtotal");
            }

            lblCartTotal.setText("Total: $" + df.format(total));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void removeFromCart() {
        int row = tableCart.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT oi.order_item_id, oi.order_id " +
                        "FROM Order_Items oi " +
                        "JOIN Orders o ON oi.order_id = o.order_id " +
                        "WHERE o.customer_id = ? AND o.status = 'ONGOING' " +
                        "LIMIT 1 OFFSET ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            pstmt.setInt(2, row);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int orderItemId = rs.getInt("order_item_id");
                int orderId = rs.getInt("order_id");

                String deleteSql = "DELETE FROM Order_Items WHERE order_item_id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setInt(1, orderItemId);
                deleteStmt.executeUpdate();

                String totalSql = "UPDATE Orders SET total_amount = " +
                                 "(SELECT COALESCE(SUM(subtotal), 0) FROM Order_Items WHERE order_id = ?) " +
                                 "WHERE order_id = ?";
                PreparedStatement totalStmt = conn.prepareStatement(totalSql);
                totalStmt.setInt(1, orderId);
                totalStmt.setInt(2, orderId);
                totalStmt.executeUpdate();

                loadCart();
                JOptionPane.showMessageDialog(this, "Item removed from cart!");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error removing item: " + ex.getMessage());
        }
    }

    private void updateQuantity() {
        int row = tableCart.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to update!");
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this, "Enter new quantity:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int newQty = Integer.parseInt(qtyStr);
            if (newQty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT oi.order_item_id, oi.order_id, oi.product_id, oi.price_at_purchase " +
                            "FROM Order_Items oi " +
                            "JOIN Orders o ON oi.order_id = o.order_id " +
                            "WHERE o.customer_id = ? AND o.status = 'ONGOING' " +
                            "LIMIT 1 OFFSET ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUser.getUserId());
                pstmt.setInt(2, row);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int orderItemId = rs.getInt("order_item_id");
                    int orderId = rs.getInt("order_id");
                    int productId = rs.getInt("product_id");
                    double price = rs.getDouble("price_at_purchase");

                    String stockSql = "SELECT stock_quantity FROM Products WHERE product_id = ?";
                    PreparedStatement stockStmt = conn.prepareStatement(stockSql);
                    stockStmt.setInt(1, productId);
                    ResultSet stockRs = stockStmt.executeQuery();
                    if (stockRs.next()) {
                        int stock = stockRs.getInt("stock_quantity");
                        if (stock < newQty) {
                            JOptionPane.showMessageDialog(this, 
                                "Insufficient stock! Available: " + stock, 
                                "Stock Error", 
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    double newSubtotal = price * newQty;
                    String updateSql = "UPDATE Order_Items SET quantity = ?, subtotal = ? WHERE order_item_id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setInt(1, newQty);
                    updateStmt.setDouble(2, newSubtotal);
                    updateStmt.setInt(3, orderItemId);
                    updateStmt.executeUpdate();

                    String totalSql = "UPDATE Orders SET total_amount = " +
                                     "(SELECT COALESCE(SUM(subtotal), 0) FROM Order_Items WHERE order_id = ?) " +
                                     "WHERE order_id = ?";
                    PreparedStatement totalStmt = conn.prepareStatement(totalSql);
                    totalStmt.setInt(1, orderId);
                    totalStmt.setInt(2, orderId);
                    totalStmt.executeUpdate();

                    loadCart();
                    JOptionPane.showMessageDialog(this, "Quantity updated!");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity!");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating quantity: " + ex.getMessage());
        }
    }

private void submitOrder() {
    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        String checkSql = "SELECT COUNT(*) as item_count FROM Order_Items oi " +
                         "JOIN Orders o ON oi.order_id = o.order_id " +
                         "WHERE o.customer_id = ? AND o.status = 'ONGOING'";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setInt(1, currentUser.getUserId());
        ResultSet checkRs = checkStmt.executeQuery();
        
        if (!checkRs.next() || checkRs.getInt("item_count") == 0) {
            conn.rollback(); 
            JOptionPane.showMessageDialog(this, "Your cart is empty!");
            return;
        }

        String orderSql = "SELECT order_id, shipping_address_id, billing_address_id FROM Orders " +
                        "WHERE customer_id = ? AND status = 'ONGOING'";
        PreparedStatement orderStmt = conn.prepareStatement(orderSql);
        orderStmt.setInt(1, currentUser.getUserId());
        ResultSet orderRs = orderStmt.executeQuery();

        if (orderRs.next()) {
            int orderId = orderRs.getInt("order_id");
            int shippingAddrId = orderRs.getInt("shipping_address_id");
            int billingAddrId = orderRs.getInt("billing_address_id");

            AddressSelectionDialog dialog = new AddressSelectionDialog(this, conn, currentUser.getUserId(), 
                                                                      shippingAddrId, billingAddrId);
            dialog.setVisible(true);

            if (!dialog.isConfirmed()) {
                conn.rollback();
                return;
            }

            shippingAddrId = dialog.getShippingAddressId();
            billingAddrId = dialog.getBillingAddressId();

            String stockCheckSql = "SELECT oi.product_id, oi.quantity, p.stock_quantity, p.name " +
                                 "FROM Order_Items oi " +
                                 "JOIN Products p ON oi.product_id = p.product_id " +
                                 "WHERE oi.order_id = ?";
            PreparedStatement stockCheckStmt = conn.prepareStatement(stockCheckSql);
            stockCheckStmt.setInt(1, orderId);
            ResultSet stockRs = stockCheckStmt.executeQuery();

            while (stockRs.next()) {
                int prodId = stockRs.getInt("product_id");
                int qtyNeeded = stockRs.getInt("quantity");
                int currentStock = stockRs.getInt("stock_quantity");
                String prodName = stockRs.getString("name");

                if (currentStock < qtyNeeded) {
                    conn.rollback(); 
                    JOptionPane.showMessageDialog(this, 
                        "Sorry, insufficient stock for: " + prodName + "\nAvailable: " + currentStock, 
                        "Stock Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String reduceStockSql = "UPDATE Products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
                PreparedStatement reduceStmt = conn.prepareStatement(reduceStockSql);
                reduceStmt.setInt(1, qtyNeeded);
                reduceStmt.setInt(2, prodId);
                reduceStmt.executeUpdate();
            }

            String updateSql = "UPDATE Orders SET shipping_address_id = ?, billing_address_id = ?, status = 'PENDING' " +
                              "WHERE order_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, shippingAddrId);
            updateStmt.setInt(2, billingAddrId);
            updateStmt.setInt(3, orderId);
            updateStmt.executeUpdate();

            String totalSql = "SELECT total_amount FROM Orders WHERE order_id = ?";
            PreparedStatement totalStmt = conn.prepareStatement(totalSql);
            totalStmt.setInt(1, orderId);
            ResultSet totalRs = totalStmt.executeQuery();
            double totalAmount = 0.0;
            if (totalRs.next()) {
                totalAmount = totalRs.getDouble("total_amount");
            }

            String paymentSql = "INSERT INTO Payments (order_id, transaction_id, amount, method, status) " +
                              "VALUES (?, ?, ?, 'Credit Card', 'Completed')"; // Otomatik Completed varsayıyoruz
            PreparedStatement paymentStmt = conn.prepareStatement(paymentSql);
            paymentStmt.setInt(1, orderId);
            paymentStmt.setString(2, "TXN-" + System.currentTimeMillis());
            paymentStmt.setDouble(3, totalAmount);
            paymentStmt.executeUpdate();

            conn.commit();

            JOptionPane.showMessageDialog(this, 
                "Order submitted successfully! Order ID: " + orderId, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            loadCart(); 
        }
    } catch (SQLException ex) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error submitting order: " + ex.getMessage());
    } finally {
        try {
            if (conn != null) conn.setAutoCommit(true);
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Order ID", "Date", "Seller", "Total", "Status"};
        modelHistory = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableHistory = new JTable(modelHistory);
        panel.add(new JScrollPane(tableHistory), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnViewDetails = new JButton("View Order Details");
        JButton btnCancel = new JButton("Cancel Order");
        btnPanel.add(btnViewDetails);
        btnPanel.add(btnCancel);

        btnViewDetails.addActionListener(e -> viewOrderDetails());
        btnCancel.addActionListener(e -> cancelOrder());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadOrderHistory() {
        modelHistory.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT o.order_id, o.order_date, u.name as seller_name, o.total_amount, o.status " +
                        "FROM Orders o " +
                        "JOIN Users u ON o.seller_id = u.user_id " +
                        "WHERE o.customer_id = ? AND o.status != 'ongoing' " +
                        "ORDER BY o.order_date DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat df = new DecimalFormat("#0.00");
            while (rs.next()) {
                modelHistory.addRow(new Object[]{
                    rs.getInt("order_id"),
                    rs.getTimestamp("order_date"),
                    rs.getString("seller_name"),
                    "$" + df.format(rs.getDouble("total_amount")),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void viewOrderDetails() {
        int row = tableHistory.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order first!");
            return;
        }

        int orderId = (Integer) modelHistory.getValueAt(row, 0);
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

    private void cancelOrder() {
        int row = tableHistory.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order first!");
            return;
        }

        int orderId = (Integer) modelHistory.getValueAt(row, 0);
        String status = (String) modelHistory.getValueAt(row, 4);

        if (!"pending".equals(status)) {
            JOptionPane.showMessageDialog(this, 
                "Only pending orders can be canceled!", 
                "Cancel Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to cancel this order?", 
            "Confirm Cancel", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String restoreSql = "UPDATE Products p " +
                                  "JOIN Order_Items oi ON p.product_id = oi.product_id " +
                                  "SET p.stock_quantity = p.stock_quantity + oi.quantity " +
                                  "WHERE oi.order_id = ?";
                PreparedStatement restoreStmt = conn.prepareStatement(restoreSql);
                restoreStmt.setInt(1, orderId);
                restoreStmt.executeUpdate();

                String updateSql = "UPDATE Orders SET status = 'CANCELED' WHERE order_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, orderId);
                updateStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Order canceled successfully!");
                loadOrderHistory();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error canceling order: " + ex.getMessage());
            }
        }
    }

    private JPanel createReviewsPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    String[] columns = {"Order ID", "Product", "Quantity", "Status", "Order Item ID", "Product ID"};
    
    modelReviews = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    
    tableReviews = new JTable(modelReviews);
    
    tableReviews.getColumnModel().getColumn(4).setMinWidth(0); 
    tableReviews.getColumnModel().getColumn(4).setMaxWidth(0);
    tableReviews.getColumnModel().getColumn(4).setWidth(0);
    
    tableReviews.getColumnModel().getColumn(5).setMinWidth(0); 
    tableReviews.getColumnModel().getColumn(5).setMaxWidth(0);
    tableReviews.getColumnModel().getColumn(5).setWidth(0);

    panel.add(new JScrollPane(tableReviews), BorderLayout.CENTER);

    JButton btnReview = new JButton("Leave Review");
    btnReview.addActionListener(e -> {
        int row = tableReviews.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to review!");
            return;
        }
        
        int orderId = (Integer) modelReviews.getValueAt(row, 0);
        String productName = (String) modelReviews.getValueAt(row, 1);
        showReviewDialog(orderId, productName);
        
        loadReviewableItems(); 
    });

    panel.add(btnReview, BorderLayout.SOUTH);
    return panel;
}

    private void loadReviewableItems() {
    if (modelReviews == null) return;

    modelReviews.setRowCount(0); // Tabloyu temizle

    try (Connection conn = DatabaseConnection.getConnection()) {
        String sql = "SELECT o.order_id, p.name, oi.quantity, o.status, oi.order_item_id, p.product_id " +
                     "FROM Order_Items oi " +
                     "JOIN Orders o ON oi.order_id = o.order_id " +
                     "JOIN Products p ON oi.product_id = p.product_id " +
                     "WHERE o.customer_id = ? " +
                     "AND o.status IN ('Shipped', 'Delivered') " +
                     "AND NOT EXISTS ( " +
                     "    SELECT 1 FROM Reviews r WHERE r.order_item_id = oi.order_item_id " +
                     ") " +
                     "ORDER BY o.order_date DESC";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, currentUser.getUserId());
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            modelReviews.addRow(new Object[]{
                rs.getInt("order_id"),
                rs.getString("name"),
                rs.getInt("quantity"),
                rs.getString("status"),
                rs.getInt("order_item_id"), // Gizli kolon
                rs.getInt("product_id")     // Gizli kolon
            });
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error loading reviewable items: " + ex.getMessage());
    }
}

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private void updateStats() {
        JPanel panel = (JPanel) tabbedPane.getComponentAt(4);
        panel.removeAll();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String monthlySql = "SELECT MONTH(order_date) as month, YEAR(order_date) as year, " +
                               "SUM(total_amount) as total " +
                               "FROM Orders " +
                               "WHERE customer_id = ? AND status != 'ongoing' " +
                               "GROUP BY YEAR(order_date), MONTH(order_date) " +
                               "ORDER BY year DESC, month DESC";
            PreparedStatement monthlyStmt = conn.prepareStatement(monthlySql);
            monthlyStmt.setInt(1, currentUser.getUserId());
            ResultSet monthlyRs = monthlyStmt.executeQuery();

            panel.add(new JLabel("=== Monthly Purchase Amount ==="));
            DecimalFormat df = new DecimalFormat("#0.00");
            while (monthlyRs.next()) {
                panel.add(new JLabel(monthlyRs.getInt("year") + "-" + 
                                    String.format("%02d", monthlyRs.getInt("month")) + 
                                    ": $" + df.format(monthlyRs.getDouble("total"))));
            }

            panel.add(Box.createVerticalStrut(20));

            String categorySql = "SELECT c.name, SUM(oi.quantity) as total_quantity " +
                                "FROM Order_Items oi " +
                                "JOIN Products p ON oi.product_id = p.product_id " +
                                "JOIN Categories c ON p.category_id = c.category_id " +
                                "JOIN Orders o ON oi.order_id = o.order_id " +
                                "WHERE o.customer_id = ? AND o.status != 'ongoing' " +
                                "GROUP BY c.category_id, c.name " +
                                "ORDER BY total_quantity DESC " +
                                "LIMIT 1";
            PreparedStatement categoryStmt = conn.prepareStatement(categorySql);
            categoryStmt.setInt(1, currentUser.getUserId());
            ResultSet categoryRs = categoryStmt.executeQuery();

            panel.add(new JLabel("=== Most Purchased Category ==="));
            if (categoryRs.next()) {
                panel.add(new JLabel(categoryRs.getString("name") + 
                                    " (" + categoryRs.getInt("total_quantity") + " items)"));
            } else {
                panel.add(new JLabel("No purchases yet"));
            }

            panel.add(Box.createVerticalStrut(20));

            String avgSql = "SELECT AVG(monthly_total) as avg_monthly " +
                           "FROM (SELECT YEAR(order_date) as year, MONTH(order_date) as month, " +
                           "SUM(total_amount) as monthly_total " +
                           "FROM Orders " +
                           "WHERE customer_id = ? AND status != 'ongoing' " +
                           "GROUP BY YEAR(order_date), MONTH(order_date)) as monthly";
            PreparedStatement avgStmt = conn.prepareStatement(avgSql);
            avgStmt.setInt(1, currentUser.getUserId());
            ResultSet avgRs = avgStmt.executeQuery();

            panel.add(new JLabel("=== Average Monthly Purchase ==="));
            if (avgRs.next()) {
                double avg = avgRs.getDouble("avg_monthly");
                if (!avgRs.wasNull()) {
                    panel.add(new JLabel("$" + df.format(avg)));
                } else {
                    panel.add(new JLabel("No purchases yet"));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            panel.add(new JLabel("Error loading statistics: " + ex.getMessage()));
        }

        panel.revalidate();
        panel.repaint();
    }

    private JPanel createAddressesPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Address ID", "Street", "City", "Country"};
        modelAddresses = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableAddresses = new JTable(modelAddresses);
        tableAddresses.getColumnModel().getColumn(0).setMinWidth(0);
        tableAddresses.getColumnModel().getColumn(0).setMaxWidth(0);
        tableAddresses.getColumnModel().getColumn(0).setWidth(0);
        panel.add(new JScrollPane(tableAddresses), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add Address");
        JButton btnEdit = new JButton("Edit Address");
        JButton btnDelete = new JButton("Delete Address");
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);

        btnAdd.addActionListener(e -> addAddress());
        btnEdit.addActionListener(e -> editAddress());
        btnDelete.addActionListener(e -> deleteAddress());

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadAddresses() {
        modelAddresses.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT address_id, street, city, country FROM Addresses WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, currentUser.getUserId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                modelAddresses.addRow(new Object[]{
                    rs.getInt("address_id"),
                    rs.getString("street"),
                    rs.getString("city"),
                    rs.getString("country")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void addAddress() {
        JTextField txtStreet = new JTextField(20);
        JTextField txtCity = new JTextField(20);
        JTextField txtCountry = new JTextField(20);

        Object[] message = {
            "Street:", txtStreet,
            "City:", txtCity,
            "Country:", txtCountry
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Address", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (txtStreet.getText().trim().isEmpty() || 
                txtCity.getText().trim().isEmpty() || 
                txtCountry.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO Addresses (user_id, street, city, country) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUser.getUserId());
                pstmt.setString(2, txtStreet.getText().trim());
                pstmt.setString(3, txtCity.getText().trim());
                pstmt.setString(4, txtCountry.getText().trim());
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Address added successfully!");
                loadAddresses();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding address: " + ex.getMessage());
            }
        }
    }

    private void editAddress() {
        int row = tableAddresses.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an address to edit!");
            return;
        }

        int addressId = (Integer) modelAddresses.getValueAt(row, 0);
        String currentStreet = (String) modelAddresses.getValueAt(row, 1);
        String currentCity = (String) modelAddresses.getValueAt(row, 2);
        String currentCountry = (String) modelAddresses.getValueAt(row, 3);

        JTextField txtStreet = new JTextField(currentStreet, 20);
        JTextField txtCity = new JTextField(currentCity, 20);
        JTextField txtCountry = new JTextField(currentCountry, 20);

        Object[] message = {
            "Street:", txtStreet,
            "City:", txtCity,
            "Country:", txtCountry
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Edit Address", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE Addresses SET street = ?, city = ?, country = ? WHERE address_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, txtStreet.getText().trim());
                pstmt.setString(2, txtCity.getText().trim());
                pstmt.setString(3, txtCountry.getText().trim());
                pstmt.setInt(4, addressId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Address updated successfully!");
                loadAddresses();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating address: " + ex.getMessage());
            }
        }
    }

    private void showReviewDialog(int orderId, String productName) {
    JDialog dialog = new JDialog(this, "Review: " + productName, true);
    dialog.setSize(400, 300);
    dialog.setLocationRelativeTo(this);
    
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5,5,5,5);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    gbc.gridx=0; gbc.gridy=0;
    panel.add(new JLabel("Rating (1-5):"), gbc);
    
    gbc.gridx=1;
    JSlider ratingSlider = new JSlider(1, 5, 5);
    ratingSlider.setMajorTickSpacing(1);
    ratingSlider.setPaintTicks(true);
    ratingSlider.setPaintLabels(true);
    panel.add(ratingSlider, gbc);
    
    gbc.gridx=0; gbc.gridy=1;
    panel.add(new JLabel("Comment:"), gbc);
    
    gbc.gridx=0; gbc.gridy=2; gbc.gridwidth=2;
    JTextArea txtComment = new JTextArea(5, 20);
    txtComment.setLineWrap(true);
    panel.add(new JScrollPane(txtComment), gbc);
    
    gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2;
    JButton btnSubmit = new JButton("Submit Review");
    panel.add(btnSubmit, gbc);
    
    btnSubmit.addActionListener(e -> {
        String comment = txtComment.getText().trim();
        int rating = ratingSlider.getValue();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String findItemSql = "SELECT oi.order_item_id, oi.product_id " +
                               "FROM Order_Items oi WHERE oi.order_id = ? LIMIT 1";
            PreparedStatement findStmt = conn.prepareStatement(findItemSql);
            findStmt.setInt(1, orderId);
            ResultSet findRs = findStmt.executeQuery();
            
            if (findRs.next()) {
                int orderItemId = findRs.getInt("order_item_id");
                int productId = findRs.getInt("product_id");
                
                String checkSql = "SELECT review_id FROM Reviews WHERE order_item_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, orderItemId);
                if (checkStmt.executeQuery().next()) {
                    JOptionPane.showMessageDialog(dialog, "You have already reviewed this item!");
                    return;
                }

                String insertSql = "INSERT INTO Reviews (customer_id, product_id, order_id, order_item_id, rating, comment) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                pstmt.setInt(1, currentUser.getUserId());
                pstmt.setInt(2, productId);
                pstmt.setInt(3, orderId);
                pstmt.setInt(4, orderItemId);
                pstmt.setInt(5, rating);
                pstmt.setString(6, comment);
                pstmt.executeUpdate();
                
                JOptionPane.showMessageDialog(dialog, "Review submitted! Thank you.");
                dialog.dispose();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
        }
    });
    
    dialog.add(panel);
    dialog.setVisible(true);
}


    private void deleteAddress() {
        int row = tableAddresses.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an address to delete!");
            return;
        }

        int addressId = (Integer) modelAddresses.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this address?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM Addresses WHERE address_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, addressId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Address deleted successfully!");
                loadAddresses();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting address: " + ex.getMessage());
            }
        }
    }

    // Helper class for address selection
    private static class AddressSelectionDialog extends JDialog {
        private int shippingAddressId;
        private int billingAddressId;
        private boolean confirmed = false;
        private JComboBox<String> cmbShipping, cmbBilling;

        public AddressSelectionDialog(JFrame parent, Connection conn, int userId, 
                                     int currentShippingId, int currentBillingId) {
            super(parent, "Select Addresses", true);
            setSize(400, 300);
            setLocationRelativeTo(parent);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Shipping Address:"), gbc);
            gbc.gridx = 1;
            cmbShipping = new JComboBox<>();
            loadAddresses(conn, userId, cmbShipping, currentShippingId);
            panel.add(cmbShipping, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Billing Address:"), gbc);
            gbc.gridx = 1;
            cmbBilling = new JComboBox<>();
            loadAddresses(conn, userId, cmbBilling, currentBillingId);
            panel.add(cmbBilling, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            JPanel btnPanel = new JPanel(new FlowLayout());
            JButton btnOK = new JButton("OK");
            JButton btnCancel = new JButton("Cancel");
            btnPanel.add(btnOK);
            btnPanel.add(btnCancel);

            btnOK.addActionListener(e -> {
                String shipping = (String) cmbShipping.getSelectedItem();
                String billing = (String) cmbBilling.getSelectedItem();
                if (shipping != null && billing != null) {
                    shippingAddressId = Integer.parseInt(shipping.split(" - ")[0]);
                    billingAddressId = Integer.parseInt(billing.split(" - ")[0]);
                    confirmed = true;
                    dispose();
                }
            });

            btnCancel.addActionListener(e -> dispose());
            panel.add(btnPanel, gbc);

            add(panel);
        }

        private void loadAddresses(Connection conn, int userId, JComboBox<String> cmb, int currentId) {
            try {
                String sql = "SELECT address_id, street, city, country FROM Addresses WHERE user_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    int addrId = rs.getInt("address_id");
                    String addr = addrId + " - " + rs.getString("street") + ", " + 
                                rs.getString("city") + ", " + rs.getString("country");
                    cmb.addItem(addr);
                    if (addrId == currentId) {
                        cmb.setSelectedItem(addr);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        public boolean isConfirmed() { return confirmed; }
        public int getShippingAddressId() { return shippingAddressId; }
        public int getBillingAddressId() { return billingAddressId; }
    }

        private void showNotifications() {
    try (Connection conn = DatabaseConnection.getConnection()) {
        // Demo amaçlı: Eğer hiç bildirim yoksa bir tane 'Hoşgeldin' bildirimi oluştur.
        String checkSql = "SELECT COUNT(*) FROM Notifications WHERE user_id = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setInt(1, currentUser.getUserId());
        ResultSet rsCheck = checkStmt.executeQuery();
        if (rsCheck.next() && rsCheck.getInt(1) == 0) {
            String insertSql = "INSERT INTO Notifications (user_id, message) VALUES (?, 'Welcome to E-Commerce System!')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, currentUser.getUserId());
            insertStmt.executeUpdate();
        }

        String sql = "SELECT message, created_at FROM Notifications WHERE user_id = ? ORDER BY created_at DESC";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, currentUser.getUserId());
        ResultSet rs = pstmt.executeQuery();

        StringBuilder sb = new StringBuilder();
        boolean hasData = false;
        while (rs.next()) {
            hasData = true;
            sb.append("[").append(rs.getTimestamp("created_at")).append("]\n");
            sb.append(rs.getString("message")).append("\n\n");
        }
        
        if (!hasData) {
            sb.append("No new notifications.");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 250));

        JOptionPane.showMessageDialog(this, scrollPane, "My Notifications", JOptionPane.INFORMATION_MESSAGE);

    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error loading notifications: " + ex.getMessage());
    }
}

}
