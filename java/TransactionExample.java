import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Example utility class demonstrating transaction management for critical operations.
 * This shows how to properly handle multi-step database operations atomically.
 */
public class TransactionExample {
    
    /**
     * Example: Complete order checkout with payment verification, stock deduction, and shipment creation.
     * All operations must succeed together or all are rolled back.
     */
    public static boolean checkoutOrder(int orderId) throws SQLException, BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);  // Start transaction
            
            // Step 1: Verify payment COMPLETED
            String checkPaymentSql = """
                SELECT COALESCE(SUM(amount), 0) as PAID_amount
                FROM Payments 
                WHERE order_id = ? AND status = 'COMPLETED'
                """;
            PreparedStatement checkStmt = conn.prepareStatement(checkPaymentSql);
            checkStmt.setInt(1, orderId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                conn.rollback();
                throw new BusinessException("Payment verification failed");
            }
            
            double PAIDAmount = rs.getDouble("PAID_amount");
            
            // Get order total
            String orderSql = "SELECT total_amount, status FROM Orders WHERE order_id = ?";
            PreparedStatement orderStmt = conn.prepareStatement(orderSql);
            orderStmt.setInt(1, orderId);
            ResultSet orderRs = orderStmt.executeQuery();
            
            if (!orderRs.next()) {
                conn.rollback();
                throw new BusinessException("Order not found");
            }
            
            double orderTotal = orderRs.getDouble("total_amount");
            String currentStatus = orderRs.getString("status");
            
            if (!"PENDING".equals(currentStatus)) {
                conn.rollback();
                throw new BusinessException("Order is not in PENDING status");
            }
            
            if (PAIDAmount < orderTotal) {
                conn.rollback();
                throw new BusinessException("Payment insufficient. PAID: $" + PAIDAmount + ", Required: $" + orderTotal);
            }
            
            // Step 2: Verify stock availability before deducting
            String stockCheckSql = """
                SELECT oi.product_id, oi.quantity, p.stock_quantity, p.name
                FROM OrderItems oi
                JOIN Products p ON oi.product_id = p.product_id
                WHERE oi.order_id = ?
                """;
            PreparedStatement stockCheckStmt = conn.prepareStatement(stockCheckSql);
            stockCheckStmt.setInt(1, orderId);
            ResultSet stockRs = stockCheckStmt.executeQuery();
            
            while (stockRs.next()) {
                int requiredQty = stockRs.getInt("quantity");
                int availableStock = stockRs.getInt("stock_quantity");
                String productName = stockRs.getString("name");
                
                if (availableStock < requiredQty) {
                    conn.rollback();
                    throw new BusinessException(
                        "Insufficient stock for " + productName + 
                        ". Available: " + availableStock + ", Required: " + requiredQty);
                }
            }
            
            // Step 3: Update order status to PAID
            String updateOrderSql = "UPDATE Orders SET status = 'PAID' WHERE order_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateOrderSql);
            updateStmt.setInt(1, orderId);
            updateStmt.executeUpdate();
            
            // Step 4: Deduct stock quantities
            String deductStockSql = """
                UPDATE Products p
                JOIN OrderItems oi ON p.product_id = oi.product_id
                SET p.stock_quantity = p.stock_quantity - oi.quantity
                WHERE oi.order_id = ?
                """;
            PreparedStatement stockStmt = conn.prepareStatement(deductStockSql);
            stockStmt.setInt(1, orderId);
            int rowsAffected = stockStmt.executeUpdate();
            
            if (rowsAffected == 0) {
                conn.rollback();
                throw new BusinessException("Failed to update stock quantities");
            }
            
            // Step 5: Create shipment record
            String shipmentSql = """
                INSERT INTO Shipments (order_id, status, tracking_number)
                VALUES (?, 'Preparing', ?)
                """;
            PreparedStatement shipmentStmt = conn.prepareStatement(shipmentSql);
            shipmentStmt.setInt(1, orderId);
            shipmentStmt.setString(2, "TRACK-" + System.currentTimeMillis());
            shipmentStmt.executeUpdate();
            
            conn.commit();  // Success - commit all changes
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();  // Error - rollback all changes
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            throw e;
        } catch (BusinessException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            throw e;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Example: Cancel order with stock restoration and refund.
     */
    public static void cancelOrder(int orderId, String reason) throws SQLException, BusinessException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Step 1: Check if order can be CANCELED
            String checkSql = "SELECT status, total_amount FROM Orders WHERE order_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, orderId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                conn.rollback();
                throw new BusinessException("Order not found");
            }
            
            String status = rs.getString("status");
            double totalAmount = rs.getDouble("total_amount");
            
            if ("DELIVERED".equals(status)) {
                conn.rollback();
                throw new BusinessException("Cannot cancel DELIVERED order");
            }
            
            if ("CANCELED".equals(status)) {
                conn.rollback();
                throw new BusinessException("Order is already CANCELED");
            }
            
            // Step 2: Restore stock quantities (only if order was PAID)
            if (!"PENDING".equals(status)) {
                String restoreStockSql = """
                    UPDATE Products p
                    JOIN OrderItems oi ON p.product_id = oi.product_id
                    SET p.stock_quantity = p.stock_quantity + oi.quantity
                    WHERE oi.order_id = ?
                    """;
                PreparedStatement restoreStmt = conn.prepareStatement(restoreStockSql);
                restoreStmt.setInt(1, orderId);
                restoreStmt.executeUpdate();
            }
            
            // Step 3: Update order status
            String cancelSql = """
                UPDATE Orders 
                SET status = 'CANCELED', 
                    notes = CONCAT(COALESCE(notes, ''), '\nCANCELED: ', ?)
                WHERE order_id = ?
                """;
            PreparedStatement cancelStmt = conn.prepareStatement(cancelSql);
            cancelStmt.setString(1, reason);
            cancelStmt.setInt(2, orderId);
            cancelStmt.executeUpdate();
            
            // Step 4: Create refund payment (if order was PAID)
            if (!"PENDING".equals(status)) {
                String refundSql = """
                    INSERT INTO Payments (order_id, payment_date, amount, method, status, transaction_id)
                    VALUES (?, NOW(), ?, 'Refund', 'COMPLETED', ?)
                    """;
                PreparedStatement refundStmt = conn.prepareStatement(refundSql);
                refundStmt.setInt(1, orderId);
                refundStmt.setDouble(2, -totalAmount);  // Negative amount for refund
                refundStmt.setString(3, "REFUND-" + System.currentTimeMillis());
                refundStmt.executeUpdate();
            }
            
            conn.commit();
            
        } catch (SQLException | BusinessException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

