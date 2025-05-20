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
    }

    // 대여처리
    public boolean processRental(int itemId, int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 물품 수량 감소
            String sql = "UPDATE DB2025_ITEMS SET available_quantity = available_quantity - 1 " +
                    "WHERE item_id = ? AND available_quantity > 0";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, itemId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // 2. 대여 기록 추가
                sql = "INSERT INTO DB2025_RENT (item_id, user_id, borrow_date) VALUES (?, ?, NOW())";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, itemId);
                stmt.setInt(2, userId);
                int rowAffected2 = stmt.executeUpdate();
                if (rowAffected2 > 0) {
                    conn.commit(); // 모든 작업 성공 시 커밋
                    return true;
                }
            }
            conn.rollback(); // 실패 시 롤백
            return false; // 아이템이 없거나 대여 불가능한 경우
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 예외 발생 시 롤백
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 자동 커밋 모드 복원
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 출고 완료 처리
    public boolean processOutDone(int rentId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 대여 기록 업데이트
            String sql = "UPDATE DB2025_RENT SET rent_status = '대여중' WHERE rent_id = ? AND rent_status = '대여신청'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rentId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit(); // 모든 작업 성공 시 커밋
                return true;
            } else {
                conn.rollback(); // 대여 기록 업데이트 실패 시 롤백
                return false; // 대여 기록이 없거나 상태 변경 불가능한 경우
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 예외 발생 시 롤백
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 자동 커밋 모드 복원
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 출고 취소 처리 -> 대여기록 삭제
    public boolean processDeleteRent(int rentId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 대여 기록 삭제
            String sql = "DELETE FROM DB2025_RENT WHERE rent_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rentId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit(); // 모든 작업 성공 시 커밋
                return true;
            } else {
                conn.rollback(); // 대여 기록 삭제 실패 시 롤백
                return false; // 대여 기록이 없거나 삭제 불가능한 경우
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 예외 발생 시 롤백
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 자동 커밋 모드 복원
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
