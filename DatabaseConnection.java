import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbHelper {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root"; 
    private static final String PASS = "YOUR_PASSWORD"; // <--- BURAYA KENDİ ŞİFRENİ YAZ !!!!!!!!!!!!!!!!!!!!!!!!!!!

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found!", e);
            }
        }
        return connection;
    }
}