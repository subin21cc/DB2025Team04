package DB2025Team04.db;

import DB2025Team04.util.SessionManager;

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

            // 0. 대여 가능 여부 확인
            String checkSql = "SELECT count(*) FROM DB2025_RENT WHERE item_id = ? AND user_id = ? AND rent_status in ('대여중', '대여신청', '연체중')";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // 이미 대여중인 물품
            }

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
                // 2. 물품 수량 증가
                sql = "UPDATE DB2025_ITEMS SET available_quantity = available_quantity + 1 " +
                        "WHERE item_id = (SELECT item_id FROM DB2025_RENT WHERE rent_id = ?)";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, rentId);
                int rowsAffected2 = stmt.executeUpdate();
                if (rowsAffected2 > 0) {
                    conn.commit(); // 모든 작업 성공 시 커밋
                    return true;
                }
            }
            conn.rollback(); // 대여 기록 삭제 실패 시 롤백
            return false; // 대여 기록이 없거나 삭제 불가능한 경우
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

    // 물품 삭제 처리
    public boolean deleteItem(int itemId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            
            // 대여 중인 물품인지 확인
            String checkSql = "SELECT COUNT(*) FROM DB2025_RENT WHERE item_id = ? AND rent_status IN ('대여중', '대여신청', '연체중')";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, itemId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // 대여 중인 물품은 삭제할 수 없음
            }
        
            // 물품 삭제
            String deleteSql = "DELETE FROM DB2025_ITEMS WHERE item_id = ?";
            stmt = conn.prepareStatement(deleteSql);
            stmt.setInt(1, itemId);
            int rowsAffected = stmt.executeUpdate();
        
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // 예약 처리
    public int processReservation(int itemId, int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 대여가능수량 확인
            String availableCheckSql = "SELECT available_quantity FROM DB2025_ITEMS WHERE item_id = ?";
            stmt = conn.prepareStatement(availableCheckSql);
            stmt.setInt(1, itemId);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt("available_quantity") > 0) {
                conn.rollback();
                return 1; // 대여가능수량이 있는 경우 예약 불가
            }

            // 2. 이미 예약한 내역이 있는지 확인
            String reservationCheckSql = "SELECT COUNT(*) FROM DB2025_RESERVATION WHERE item_id = ? AND user_id = ?";
            stmt = conn.prepareStatement(reservationCheckSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return 2; // 이미 예약한 내역이, 있음
            }

            // 3. 연체 중인지 확인
            String overdueCheckSql = "SELECT COUNT(*) FROM DB2025_OVERDUES WHERE user_id = ? AND restriction_end > CURRENT_DATE()";
            stmt = conn.prepareStatement(overdueCheckSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return 3; // 연체 중인 경우, 예약 불가
            }

            // 4. 예약 추가
            String insertSql = "INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date) VALUES (?, ?, CURRENT_DATE())";
            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit(); // 트랜잭션 완료
                return 0; // 성공
            } else {
                conn.rollback();
                return 4; // 예약 실패
            }
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return 4; // 기타 오류
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // 사용자 추가
    public boolean addUser(int userId, String password, String name, String department, String phone, String status, boolean isAdmin) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작
            
            // 1. 사용자 ID 중복 체크
            String checkSql = "SELECT COUNT(*) FROM DB2025_USER WHERE user_id = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return false; // 이미 존재하는 ID
            }
            
            // 2. 전화번호 중복 체크
            checkSql = "SELECT COUNT(*) FROM DB2025_USER WHERE user_phone = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, phone);
            rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return false; // 이미 존재하는 전화번호
            }
            
            // 3. 사용자 추가
            String sql = "INSERT INTO DB2025_USER (user_id, user_pw, user_name, user_dep, user_phone, user_status) " +
                         "VALUES (?, SHA2(?, 256), ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, name);
            stmt.setString(4, department);
            stmt.setString(5, phone);
            stmt.setString(6, status);
            
            int rowsAffected = stmt.executeUpdate();
            
            // 4. 관리자로 설정할 경우 관리자 테이블에 추가
            if (rowsAffected > 0 && isAdmin) {
                sql = "INSERT INTO DB2025_ADMIN (user_id) VALUES (?)";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            closeResources(conn, stmt, null);
        }
    }

    // 사용자 수정
    public boolean updateUser(int userId, String password, String name, String department, String phone, String status, boolean isAdmin) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작
            
            // 1. 전화번호 중복 체크 (자기 자신 제외)
            String checkSql = "SELECT COUNT(*) FROM DB2025_USER WHERE user_phone = ? AND user_id != ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, phone);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return false; // 다른 사용자가 사용 중인 전화번호
            }
            
            // 2. 사용자 정보 업데이트
            String sql;
            if (password != null && !password.isEmpty()) {
                // 비밀번호를 변경하는 경우
                sql = "UPDATE DB2025_USER SET user_pw = SHA2(?, 256), user_name = ?, user_dep = ?, " +
                      "user_phone = ?, user_status = ? WHERE user_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, password);
                stmt.setString(2, name);
                stmt.setString(3, department);
                stmt.setString(4, phone);
                stmt.setString(5, status);
                stmt.setInt(6, userId);
            } else {
                // 비밀번호를 변경하지 않는 경우
                sql = "UPDATE DB2025_USER SET user_name = ?, user_dep = ?, " +
                      "user_phone = ?, user_status = ? WHERE user_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, name);
                stmt.setString(2, department);
                stmt.setString(3, phone);
                stmt.setString(4, status);
                stmt.setInt(5, userId);
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // 3. 관리자 권한 체크 및 업데이트
                sql = "SELECT COUNT(*) FROM DB2025_ADMIN WHERE user_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();
                
                boolean currentlyAdmin = false;
                if (rs.next()) {
                    currentlyAdmin = rs.getInt(1) > 0;
                }
                
                // 관리자 권한 상태가 변경된 경우
                if (currentlyAdmin != isAdmin) {
                    if (isAdmin) {
                        // 관리자 권한 추가
                        sql = "INSERT INTO DB2025_ADMIN (user_id) VALUES (?)";
                        stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, userId);
                        stmt.executeUpdate();
                    } else {
                        // 관리자 권한 제거
                        sql = "DELETE FROM DB2025_ADMIN WHERE user_id = ?";
                        stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, userId);
                        stmt.executeUpdate();
                    }
                }
                
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            closeResources(conn, stmt, null);
        }
    }

    // 사용자 삭제
    public boolean deleteUser(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            // 1. 대여 중인 물품이 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM DB2025_RENT WHERE user_id = ? AND rent_status IN ('대여중', '대여신청', '연체중')";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback();
                return false; // 대여 중인 물품이 있어 삭제 불가
            }
            
            // 2. 현재 로그인한 사용자가 아닌지 확인 (자기 자신 삭제 방지)
            if (userId == SessionManager.getInstance().getUserId()) {
                conn.rollback();
                return false; // 현재 로그인한 자기 자신은 삭제 불가
            }
            
            // 3. 사용자 삭제 (관리자 테이블은 CASCADE로 자동 삭제)
            String sql = "DELETE FROM DB2025_USER WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            closeResources(conn, stmt, null);
        }
    }

    // 관리자 여부 확인
    public boolean isAdmin(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            String sql = "SELECT COUNT(*) FROM DB2025_ADMIN WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
}