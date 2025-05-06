package DB2025Team04.db;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/DB2025Team04";
    private static final String USER = "DB2025Team04";
    private static final String PASSWORD = "DB2025Team04"; // Change as needed

    private static DatabaseManager instance;

    // Add static initializer to load the JDBC driver
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading MySQL JDBC driver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASSWORD);
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("serverTimezone", "UTC");

        return DriverManager.getConnection(DB_URL, props);
    }

    public void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Transaction example
    public boolean executeTransaction(String... queries) {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            for (String query : queries) {
                stmt.executeUpdate(query);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, stmt, null);
        }
    }}
