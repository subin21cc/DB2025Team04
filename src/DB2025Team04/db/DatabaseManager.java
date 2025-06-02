package DB2025Team04.db;

import DB2025Team04.util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

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
        // DB URL에 서버 타임존을 시스템 로컬 타임존으로 설정하는 파라미터 추가
        String timeZoneParam = "?serverTimezone=" + TimeZone.getDefault().getID();
        
        // 기존 URL에 파라미터가 있는지 확인하여 적절히 연결 문자 추가
        String connectionUrl = DB_URL;
        if (!DB_URL.contains("?")) {
            connectionUrl += timeZoneParam;
        } else if (!DB_URL.contains("serverTimezone")) {
            connectionUrl += "&serverTimezone=" + TimeZone.getDefault().getID();
        }
        
        return DriverManager.getConnection(connectionUrl, USER, PASSWORD);
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
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 0. 대여 가능 여부 확인
            // idx_item_user_status 인덱스 사용
            String checkSql = "SELECT count(*) FROM DB2025_RENT USE INDEX (idx_item_user_status) "
                + "WHERE item_id = ? AND user_id = ? AND rent_status in ('대여중', '대여신청', '연체중')";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // 이미 대여중인 물품
            }

            // 물품 및 사용자 정보 조회
            String getInfoSql = "SELECT i.item_name, i.category, i.available_quantity, i.quantity, " +
                           "u.user_name, u.user_dep " +
                           "FROM DB2025_ITEMS i, DB2025_USER u " +
                           "WHERE i.item_id = ? AND u.user_id = ?";
            stmt = conn.prepareStatement(getInfoSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                conn.rollback(); // 실패 시 롤백
                return false; // 물품 또는 사용자 정보가 없음
            }

            String itemName = rs.getString("item_name");
            String category = rs.getString("category");
            String userName = rs.getString("user_name");
            String userDep = rs.getString("user_dep");
            int availableQuantity = rs.getInt("available_quantity");
            int totalQuantity = rs.getInt("quantity");

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
                    // 3. 대여 기록의 ID 가져오기
                    int rentId = 0;
                    sql = "SELECT LAST_INSERT_ID() as rent_id";
                    stmt = conn.prepareStatement(sql);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        rentId = rs.getInt("rent_id");
                    }

                    // 4. 대여 로그 추가
                    sql = "INSERT INTO DB2025_RENT_LOG " +
                            "(rent_id, item_id, user_id, previous_status, current_status, " +
                            "log_date, borrow_date, due_date, note, " +
                            "item_name, item_category, user_name, user_dep, " +
                            "available_quantity, total_quantity, operation_type, created_by) " +
                            "VALUES (?, ?, ?, NULL, '대여신청', CURRENT_TIMESTAMP, NOW(), " +
                            "DATE_ADD(NOW(), INTERVAL 7 DAY), '신규 대여 신청', ?, ?, ?, ?, ?, ?, '생성', ?)";

                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, rentId);
                    stmt.setInt(2, itemId);
                    stmt.setInt(3, userId);
                    stmt.setString(4, itemName);
                    stmt.setString(5, category);
                    stmt.setString(6, userName);
                    stmt.setString(7, userDep);
                    stmt.setInt(8, availableQuantity - 1); // 감소된 수량 반영
                    stmt.setInt(9, totalQuantity);
                    stmt.setString(10, "사용자"); // 사용자가 직접 대여한 경우

                    stmt.executeUpdate();
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
            closeResources(conn, stmt, rs);
        }
    }

    // 출고 완료 처리
    public boolean processOutDone(int rentId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작
        
            // 관련 정보 조회
            String infoSql = "SELECT r.item_id, r.user_id, r.borrow_date, r.due_date, " +
                         "i.item_name, i.category, i.available_quantity, i.quantity, " +
                         "u.user_name, u.user_dep " +
                         "FROM DB2025_RENT r " +
                         "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                         "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                         "WHERE r.rent_id = ? AND r.rent_status = '대여신청'";
        
            stmt = conn.prepareStatement(infoSql);
            stmt.setInt(1, rentId);
            rs = stmt.executeQuery();
        
            if (!rs.next()) {
                conn.rollback(); // 실패시 롤백
                return false; // 해당 대여 정보가 없거나 이미 처리됨
            }

            int itemId = rs.getInt("item_id");
            int userId = rs.getInt("user_id");
            Date borrowDate = rs.getDate("borrow_date");
            Date dueDate = rs.getDate("due_date");
            String itemName = rs.getString("item_name");
            String category = rs.getString("category");
            String userName = rs.getString("user_name");
            String userDep = rs.getString("user_dep");
            int availableQuantity = rs.getInt("available_quantity");
            int totalQuantity = rs.getInt("quantity");

            // 현재 관리자 정보 가져오기
            int adminId = SessionManager.getInstance().getUserId();
            String adminName = "";

            String adminSql = "SELECT user_name FROM DB2025_USER WHERE user_id = ?";
            stmt = conn.prepareStatement(adminSql);
            stmt.setInt(1, adminId);
            ResultSet adminRs = stmt.executeQuery();
            if (adminRs.next()) {
                adminName = adminRs.getString("user_name");
            }
            adminRs.close();

            // 1. 대여 기록 업데이트
            String sql = "UPDATE DB2025_RENT SET rent_status = '대여중' WHERE rent_id = ? AND rent_status = '대여신청'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rentId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // 2. 대여 로그 추가
                sql = "INSERT INTO DB2025_RENT_LOG " +
                        "(rent_id, item_id, user_id, admin_id, previous_status, current_status, " +
                        "log_date, borrow_date, due_date, note, " +
                        "item_name, item_category, user_name, user_dep, admin_name, " +
                        "available_quantity, total_quantity, operation_type, created_by) " +
                        "VALUES (?, ?, ?, ?, '대여신청', '대여중', CURRENT_TIMESTAMP, ?, ?, '출고 완료 처리', " +
                        "?, ?, ?, ?, ?, ?, ?, '수정', ?)";

                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, rentId);
                stmt.setInt(2, itemId);
                stmt.setInt(3, userId);
                stmt.setInt(4, adminId);
                stmt.setDate(5, borrowDate);
                stmt.setDate(6, dueDate);
                stmt.setString(7, itemName);
                stmt.setString(8, category);
                stmt.setString(9, userName);
                stmt.setString(10, userDep);
                stmt.setString(11, adminName);
                stmt.setInt(12, availableQuantity);
                stmt.setInt(13, totalQuantity);
                stmt.setString(14, adminName);

                stmt.executeUpdate();
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
            closeResources(conn, stmt, rs);
        }
    }

    // 출고 취소 처리 -> 대여기록 삭제
    public boolean processDeleteRent(int rentId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작
        
            // 삭제할 대여 정보 조회
            String infoSql = "SELECT r.item_id, r.user_id, r.borrow_date, r.rent_status, " +
                         "i.item_name, i.category, i.available_quantity, i.quantity, " +
                         "u.user_name, u.user_dep " +
                         "FROM DB2025_RENT r " +
                         "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                         "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                         "WHERE r.rent_id = ?";

            stmt = conn.prepareStatement(infoSql);
            stmt.setInt(1, rentId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                conn.rollback(); // 실패 시 롤백
                return false; // 해당 대여 정보가 없음
            }

            int itemId = rs.getInt("item_id");
            int userId = rs.getInt("user_id");
            Date borrowDate = rs.getDate("borrow_date");
            String rentStatus = rs.getString("rent_status");
            String itemName = rs.getString("item_name");
            String category = rs.getString("category");
            String userName = rs.getString("user_name");
            String userDep = rs.getString("user_dep");
            int availableQuantity = rs.getInt("available_quantity");
            int totalQuantity = rs.getInt("quantity");

            // 현재 관리자 정보 가져오기
            int adminId = SessionManager.getInstance().getUserId();
            String adminName = "";

            String adminSql = "SELECT user_name FROM DB2025_USER WHERE user_id = ?";
            stmt = conn.prepareStatement(adminSql);
            stmt.setInt(1, adminId);
            ResultSet adminRs = stmt.executeQuery();
            if (adminRs.next()) {
                adminName = adminRs.getString("user_name");
            }
            adminRs.close();

            // 1. 대여 기록 삭제
            String sql = "DELETE FROM DB2025_RENT WHERE rent_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rentId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // 2. 물품 수량 증가
                sql = "UPDATE DB2025_ITEMS SET available_quantity = available_quantity + 1 WHERE item_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, itemId);
                int rowsAffected2 = stmt.executeUpdate();

                if (rowsAffected2 > 0) {
                    // 3. 대여 로그 추가
                    sql = "INSERT INTO DB2025_RENT_LOG " +
                            "(rent_id, item_id, user_id, admin_id, previous_status, current_status, " +
                            "log_date, borrow_date, note, " +
                            "item_name, item_category, user_name, user_dep, admin_name, " +
                            "available_quantity, total_quantity, operation_type, created_by) " +
                            "VALUES (?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, ?, '출고 취소 및 대여 기록 삭제', " +
                            "?, ?, ?, ?, ?, ?, ?, '삭제', ?)";

                    stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, rentId);
                    stmt.setInt(2, itemId);
                    stmt.setInt(3, userId);
                    stmt.setInt(4, adminId);
                    stmt.setString(5, rentStatus);
                    stmt.setDate(6, borrowDate);
                    stmt.setString(7, itemName);
                    stmt.setString(8, category);
                    stmt.setString(9, userName);
                    stmt.setString(10, userDep);
                    stmt.setString(11, adminName);
                    stmt.setInt(12, availableQuantity + 1); // 증가된 수량 반영
                    stmt.setInt(13, totalQuantity);
                    stmt.setString(14, adminName);

                    stmt.executeUpdate();
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
            closeResources(conn, stmt, rs);
        }
    }

    // 물품 삭제 처리
    public boolean deleteItem(int itemId) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            
            // 대여 중인 물품인지 확인
            // idx_item_status 인덱스 사용
            String checkSql = "SELECT COUNT(*) FROM DB2025_RENT USE INDEX (idx_item_status) "
                + "WHERE item_id = ? AND rent_status IN ('대여중', '대여신청', '연체중')";
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

            // 물품 및 사용자 정보 조회
            String getInfoSql = "SELECT i.item_name, i.category, i.available_quantity, i.quantity, " +
                           "u.user_name, u.user_dep " +
                           "FROM DB2025_ITEMS i, DB2025_USER u " +
                           "WHERE i.item_id = ? AND u.user_id = ?";
            stmt = conn.prepareStatement(getInfoSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                conn.rollback(); // 실패 시 롤백
                return 4; // 물품 또는 사용자 정보가 없음
            }

            String itemName = rs.getString("item_name");
            String category = rs.getString("category");
            String userName = rs.getString("user_name");
            String userDep = rs.getString("user_dep");
            int availableQuantity = rs.getInt("available_quantity");
            int totalQuantity = rs.getInt("quantity");

            // 1. 대여가능수량 확인
            if (availableQuantity > 0) {
                conn.rollback(); // 실패 시 롤백
                return 1; // 대여가능수량이 있는 경우 예약 불가
            }

            // 2. 이미 예약한 내역이 있는지 확인
            // idx_item_user 인덱스 사용
            String reservationCheckSql = "SELECT COUNT(*) FROM DB2025_RESERVATION USE INDEX (idx_item_user) "
                + "WHERE item_id = ? AND user_id = ?";
            stmt = conn.prepareStatement(reservationCheckSql);
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback(); // 실패 시 롤백
                return 2; // 이미 예약한 내역이 있음
            }

            // 3. 연체 중인지 확인
            // idx_user_status 인덱스 사용
            String overdueCheckSql = "SELECT COUNT(*) FROM DB2025_USER USE INDEX (idx_user_status) "
                + "WHERE user_id = ? AND user_status = '대여불가'";
            stmt = conn.prepareStatement(overdueCheckSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                conn.rollback(); // 실패 시 롤백
                return 3; // 연체 중인 경우, 예약 불가
            }

            // 4. 예약 추가
            String insertSql = "INSERT INTO DB2025_RESERVATION (user_id, item_id, reserve_date) VALUES (?, ?, CURRENT_DATE())";
            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, userId);
            stmt.setInt(2, itemId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // 5. 예약 ID 가져오기
                int reservationId = 0;
                String idSql = "SELECT LAST_INSERT_ID() as reservation_id";
                stmt = conn.prepareStatement(idSql);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    reservationId = rs.getInt("reservation_id");
                }

                // 6. 예약 로그 추가
                String logSql = "INSERT INTO DB2025_RENT_LOG " +
                        "(item_id, user_id, current_status, log_date, note, " +
                        "item_name, item_category, user_name, user_dep, " +
                        "available_quantity, total_quantity, operation_type, created_by) " +
                        "VALUES (?, ?, '대여가능', CURRENT_TIMESTAMP, '예약 신청', " +
                        "?, ?, ?, ?, ?, ?, '생성', ?)";

                stmt = conn.prepareStatement(logSql);
                stmt.setInt(1, itemId);
                stmt.setInt(2, userId);
                stmt.setString(3, itemName);
                stmt.setString(4, category);
                stmt.setString(5, userName);
                stmt.setString(6, userDep);
                stmt.setInt(7, availableQuantity);
                stmt.setInt(8, totalQuantity);
                stmt.setString(9, "사용자");

                stmt.executeUpdate();
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
            // idx_user_phone 인덱스 사용
            checkSql = "SELECT COUNT(*) FROM DB2025_USER USE INDEX (idx_user_phone) WHERE user_phone = ?";
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

    // 프로그램 수행시 자동으로 처리해야 할 작업 처리
    public void processAutoTask() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // A. 대여 상태 처리 (대여중 -> 연체중)
            // 1. 대여중이면서 대여일자가 7일 지난 대여 찾기
            String findOverdueRentSql =
                    "SELECT r.rent_id, r.item_id, r.user_id, r.borrow_date, r.due_date, r.rent_status, " +
                            "i.item_name, i.category, i.available_quantity, i.quantity, " +
                            "u.user_name, u.user_dep " +
                            "FROM DB2025_RENT r " +
                            "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                            "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                            "WHERE r.rent_status = '대여중' AND DATEDIFF(CURRENT_DATE, r.due_date) > 0";

            stmt = conn.prepareStatement(findOverdueRentSql);
            rs = stmt.executeQuery();

            // 연체된 사용자 ID 목록을 저장하기 위한 리스트
            List<Integer> overdueUserIds = new ArrayList<>();

            while (rs.next()) {
                int rentId = rs.getInt("rent_id");
                int userId = rs.getInt("user_id");
                int itemId = rs.getInt("item_id");
                String itemName = rs.getString("item_name");
                String itemCategory = rs.getString("category");
                String userName = rs.getString("user_name");
                String userDep = rs.getString("user_dep");
                int availableQuantity = rs.getInt("available_quantity");
                int totalQuantity = rs.getInt("quantity");
                Date borrowDate = rs.getDate("borrow_date");
                Date dueDate = rs.getDate("due_date");
                String previousStatus = rs.getString("rent_status");

                // 연체 사용자 ID 추가
                if (!overdueUserIds.contains(userId)) {
                    overdueUserIds.add(userId);
                }

                // 3. 대여 상태를 '연체중'으로 변경
                String updateRentSql = "UPDATE DB2025_RENT SET rent_status = '연체중' WHERE rent_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateRentSql);
                updateStmt.setInt(1, rentId);
                updateStmt.executeUpdate();
                updateStmt.close();

                // 대여 로그 추가
                int overdueDays = (int) ((new java.util.Date().getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));

                String logSql =
                        "INSERT INTO DB2025_RENT_LOG " +
                                "(rent_id, item_id, user_id, previous_status, current_status, " +
                                "log_date, borrow_date, due_date, overdue_days, note, " +
                                "item_name, item_category, user_name, user_dep, " +
                                "available_quantity, total_quantity, operation_type, created_by) " +
                                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, rentId);
                logStmt.setInt(2, itemId);
                logStmt.setInt(3, userId);
                logStmt.setString(4, previousStatus);
                logStmt.setString(5, "연체중");
                logStmt.setDate(6, borrowDate);
                logStmt.setDate(7, dueDate);
                logStmt.setInt(8, overdueDays);
                logStmt.setString(9, "자동 연체 처리");
                logStmt.setString(10, itemName);
                logStmt.setString(11, itemCategory);
                logStmt.setString(12, userName);
                logStmt.setString(13, userDep);
                logStmt.setInt(14, availableQuantity);
                logStmt.setInt(15, totalQuantity);
                logStmt.setString(16, "시스템처리");
                logStmt.setString(17, "SYSTEM");
                logStmt.executeUpdate();
                logStmt.close();
            }

            // 2a. 연체된 사용자의 상태를 '대여불가'로 변경
            if (!overdueUserIds.isEmpty()) {
                for (Integer userId : overdueUserIds) {
                    // 사용자 정보 조회
                    String getUserSql = "SELECT user_name, user_dep, user_status FROM DB2025_USER WHERE user_id = ? AND user_status != '대여불가'";
                    PreparedStatement getUserStmt = conn.prepareStatement(getUserSql);
                    getUserStmt.setInt(1, userId);
                    ResultSet userRs = getUserStmt.executeQuery();

                    if (userRs.next()) {
                        String userName = userRs.getString("user_name");
                        String userDep = userRs.getString("user_dep");
                        String previousStatus = userRs.getString("user_status");

                        // 사용자 상태 업데이트
                        String updateUserSql =
                                "UPDATE DB2025_USER SET user_status = '대여불가', user_restrict_date = DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) " +
                                        "WHERE user_id = ?";
                        PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                        updateUserStmt.setInt(1, userId);
                        updateUserStmt.executeUpdate();
                        updateUserStmt.close();

                        // 사용자 상태 변경 로그 추가
                        String logSql =
                                "INSERT INTO DB2025_RENT_LOG " +
                                        "(user_id, previous_status, current_status, " +
                                        "log_date, note, user_name, user_dep, operation_type, created_by) " +
                                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";

                        PreparedStatement logStmt = conn.prepareStatement(logSql);
                        logStmt.setInt(1, userId);
                        logStmt.setString(2, previousStatus);
                        logStmt.setString(3, "대여불가");
                        logStmt.setString(4, "연체로 인한 자동 대여제한");
                        logStmt.setString(5, userName);
                        logStmt.setString(6, userDep);
                        logStmt.setString(7, "시스템처리");
                        logStmt.setString(8, "SYSTEM");
                        logStmt.executeUpdate();
                        logStmt.close();
                    }
                    userRs.close();
                    getUserStmt.close();
                }

                // 2b. 연체된 사용자의 예약을 모두 취소
                for (Integer userId : overdueUserIds) {
                    // 예약 목록 조회
                    String getReservationsSql =
                            "SELECT r.reservation_id, r.item_id, i.item_name, i.category, u.user_name, u.user_dep " +
                                    "FROM DB2025_RESERVATION r " +
                                    "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                                    "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                                    "WHERE r.user_id = ?";

                    PreparedStatement getReservationsStmt = conn.prepareStatement(getReservationsSql);
                    getReservationsStmt.setInt(1, userId);
                    ResultSet reservationRs = getReservationsStmt.executeQuery();

                    while (reservationRs.next()) {
                        int reservationId = reservationRs.getInt("reservation_id");
                        int itemId = reservationRs.getInt("item_id");
                        String itemName = reservationRs.getString("item_name");
                        String itemCategory = reservationRs.getString("category");
                        String userName = reservationRs.getString("user_name");
                        String userDep = reservationRs.getString("user_dep");

                        // 예약 삭제 로그 추가
                        String logSql =
                                "INSERT INTO DB2025_RENT_LOG " +
                                        "(rent_id, item_id, user_id, log_date, note, " +
                                        "item_name, item_category, user_name, user_dep, operation_type, created_by, current_status) " +
                                        "VALUES (0, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?)";

                        PreparedStatement logStmt = conn.prepareStatement(logSql);
                        logStmt.setInt(1, itemId);
                        logStmt.setInt(2, userId);
                        logStmt.setString(3, "연체로 인한 자동 예약 취소");
                        logStmt.setString(4, itemName);
                        logStmt.setString(5, itemCategory);
                        logStmt.setString(6, userName);
                        logStmt.setString(7, userDep);
                        logStmt.setString(8, "삭제");
                        logStmt.setString(9, "SYSTEM");
                        logStmt.setString(10, "예약취소");
                        logStmt.executeUpdate();
                        logStmt.close();
                    }
                    reservationRs.close();
                    getReservationsStmt.close();

                    // 예약 삭제
                    String deleteReservationsSql = "DELETE FROM DB2025_RESERVATION WHERE user_id = ?";
                    PreparedStatement deleteStmt = conn.prepareStatement(deleteReservationsSql);
                    deleteStmt.setInt(1, userId);
                    deleteStmt.executeUpdate();
                    deleteStmt.close();
                }
            }

            // B. 사용자 제한 복구 처리
            // 1. 대여불가인 사용자 중 제한 기간이 지난 사용자 찾기
            String findRestrictedUsersSql =
                    "SELECT user_id, user_name, user_dep FROM DB2025_USER " +
                            "WHERE user_status = '대여불가' AND user_restrict_date IS NOT NULL AND user_restrict_date < CURRENT_DATE";

            if (stmt != null) stmt.close();
            stmt = conn.prepareStatement(findRestrictedUsersSql);
            if (rs != null) rs.close();
            rs = stmt.executeQuery();

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String userName = rs.getString("user_name");
                String userDep = rs.getString("user_dep");

                // 사용자 상태를 '대여가능'으로 변경
                String updateUserSql = "UPDATE DB2025_USER SET user_status = '대여가능', user_restrict_date = NULL WHERE user_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateUserSql);
                updateStmt.setInt(1, userId);
                updateStmt.executeUpdate();
                updateStmt.close();

                // 대여 제한 해제 로그 추가
                String logSql =
                        "INSERT INTO DB2025_RENT_LOG " +
                                "(rent_id, user_id, previous_status, current_status, " +
                                "log_date, note, user_name, user_dep, operation_type, created_by) " +
                                "VALUES (0, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";

                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, userId);
                logStmt.setString(2, "대여불가");
                logStmt.setString(3, "대여가능");
                logStmt.setString(4, "대여제한 기간 만료로 인한 자동 제한 해제");
                logStmt.setString(5, userName);
                logStmt.setString(6, userDep);
                logStmt.setString(7, "시스템처리");
                logStmt.setString(8, "SYSTEM");
                logStmt.executeUpdate();
                logStmt.close();
            }

            conn.commit(); // 모든 작업 성공 시 커밋
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // 예외 발생 시 롤백
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // 반납처리 (대여중일때는 반납완료 처리, 연체중일때는 연체반납 처리)
    // 반납완료 처리: 예약된 물품이 있는 경우 예약자에게 대여신청 처리(대여가능 수량: 그대로 0), 예약자가 없는 경우 반납완료 처리 (대여가능 수량: +1)
    // 연체반납 처리: 연체된 기간만큼 대여자의 제한 기간 추가, 기존에 제한기간이 있을 경우에는 그만큼 더해서 추가
    // 모든 update, insert, delete 경우에 대여 로그에 기록 추가 (현재의 관리자 ID로 기록)
    public boolean processReturn(int rentId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 현재 대여 정보 조회
            String getRentSql =
                    "SELECT r.rent_id, r.item_id, r.user_id, r.borrow_date, r.due_date, r.rent_status, " +
                            "i.item_name, i.category, i.available_quantity, i.quantity, " +
                            "u.user_name, u.user_dep, u.user_restrict_date " +
                            "FROM DB2025_RENT r " +
                            "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                            "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                            "WHERE r.rent_id = ? AND r.rent_status IN ('대여중', '연체중')";

            stmt = conn.prepareStatement(getRentSql);
            stmt.setInt(1, rentId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int itemId = rs.getInt("item_id");
                int userId = rs.getInt("user_id");
                Date borrowDate = rs.getDate("borrow_date");
                Date dueDate = rs.getDate("due_date");
                String rentStatus = rs.getString("rent_status");
                String itemName = rs.getString("item_name");
                String itemCategory = rs.getString("category");
                String userName = rs.getString("user_name");
                String userDep = rs.getString("user_dep");
                int availableQuantity = rs.getInt("available_quantity");
                int totalQuantity = rs.getInt("quantity");
                Date userRestrictDate = rs.getDate("user_restrict_date");

                // 현재 날짜 설정
                java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());

                // 현재 로그인한 관리자 ID
                int adminId = SessionManager.getInstance().getUserId();

                // 관리자 정보 조회
                String adminName = "";
                String getAdminSql = "SELECT user_name FROM DB2025_USER WHERE user_id = ?";
                PreparedStatement adminStmt = conn.prepareStatement(getAdminSql);
                adminStmt.setInt(1, adminId);
                ResultSet adminRs = adminStmt.executeQuery();
                if (adminRs.next()) {
                    adminName = adminRs.getString("user_name");
                }
                adminRs.close();
                adminStmt.close();

                // 2. 대여 상태에 따른 처리
                String newStatus;
                String note;
                int overdueDays = 0;

                if ("연체중".equals(rentStatus)) {
                    newStatus = "연체반납";

                    // 연체일 계산 (현재 날짜 - 반납예정일)
                    overdueDays = (int)((currentDate.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));

                    // 연체 기간만큼 제한 기간 추가
                    Date newRestrictDate;
                    if (userRestrictDate != null && userRestrictDate.after(currentDate)) {
                        // 기존 제한 기간이 있으면 그 기간에 연체일 추가
                        newRestrictDate = new java.sql.Date(userRestrictDate.getTime() +
                                (overdueDays * 24L * 60L * 60L * 1000L));
                    } else {
                        // 기존 제한 기간이 없으면 현재 날짜 + 연체일
                        newRestrictDate = new java.sql.Date(currentDate.getTime() +
                                (overdueDays * 24L * 60L * 60L * 1000L));
                    }

                    // 사용자 상태 업데이트
                    String updateUserSql =
                            "UPDATE DB2025_USER SET user_status = '대여불가', user_restrict_date = ? WHERE user_id = ?";
                    PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                    updateUserStmt.setDate(1, newRestrictDate);
                    updateUserStmt.setInt(2, userId);
                    updateUserStmt.executeUpdate();
                    updateUserStmt.close();

                    note = "연체 반납 처리 (연체일: " + overdueDays + "일)";
                } else {
                    newStatus = "반납완료";
                    note = "정상 반납 처리";
                }

                // 3. 대여 상태 업데이트
                String updateRentSql =
                        "UPDATE DB2025_RENT SET rent_status = ?, return_date = CURRENT_DATE WHERE rent_id = ?";
                PreparedStatement updateRentStmt = conn.prepareStatement(updateRentSql);
                updateRentStmt.setString(1, newStatus);
                updateRentStmt.setInt(2, rentId);
                updateRentStmt.executeUpdate();
                updateRentStmt.close();

                // 4. 예약 확인 및 처리
                String getReservationSql =
                        "SELECT r.reservation_id, r.user_id, u.user_name, u.user_dep, u.user_status " +
                                "FROM DB2025_RESERVATION r " +
                                "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                                "WHERE r.item_id = ? ORDER BY r.reserve_date ASC LIMIT 1";

                PreparedStatement getReservationStmt = conn.prepareStatement(getReservationSql);
                getReservationStmt.setInt(1, itemId);
                ResultSet reservationRs = getReservationStmt.executeQuery();

                boolean hasReservation = reservationRs.next();
                boolean availabilityIncreased = false;

                if (hasReservation) {
                    int reservationId = reservationRs.getInt("reservation_id");
                    int reserveUserId = reservationRs.getInt("user_id");
                    String reserveUserName = reservationRs.getString("user_name");
                    String reserveUserDep = reservationRs.getString("user_dep");
                    String reserveUserStatus = reservationRs.getString("user_status");

                    // 예약자가 대여 가능한 상태인지 확인
                    if ("대여가능".equals(reserveUserStatus)) {
                        // 예약 삭제
                        String deleteReservationSql = "DELETE FROM DB2025_RESERVATION WHERE reservation_id = ?";
                        PreparedStatement deleteReservationStmt = conn.prepareStatement(deleteReservationSql);
                        deleteReservationStmt.setInt(1, reservationId);
                        deleteReservationStmt.executeUpdate();
                        deleteReservationStmt.close();

                        // 예약자에게 대여신청 처리
                        String insertRentSql =
                                "INSERT INTO DB2025_RENT (item_id, user_id, borrow_date, rent_status) " +
                                        "VALUES (?, ?, CURRENT_DATE, '대여신청')";
                        PreparedStatement insertRentStmt = conn.prepareStatement(insertRentSql);
                        insertRentStmt.setInt(1, itemId);
                        insertRentStmt.setInt(2, reserveUserId);
                        insertRentStmt.executeUpdate();
                        insertRentStmt.close();

                        // 예약 처리 로그 추가
                        String reserveLogSql =
                                "INSERT INTO DB2025_RENT_LOG " +
                                        "(rent_id, item_id, user_id, admin_id, previous_status, current_status, " +
                                        "log_date, borrow_date, due_date, note, " +
                                        "item_name, item_category, user_name, user_dep, admin_name, " +
                                        "available_quantity, total_quantity, operation_type, created_by) " +
                                        "VALUES (0, ?, ?, ?, NULL, '대여신청', CURRENT_TIMESTAMP, CURRENT_DATE, " +
                                        "DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                        PreparedStatement reserveLogStmt = conn.prepareStatement(reserveLogSql);
                        reserveLogStmt.setInt(1, itemId);
                        reserveLogStmt.setInt(2, reserveUserId);
                        reserveLogStmt.setInt(3, adminId);
                        reserveLogStmt.setString(4, "예약자 자동 대여 처리");
                        reserveLogStmt.setString(5, itemName);
                        reserveLogStmt.setString(6, itemCategory);
                        reserveLogStmt.setString(7, reserveUserName);
                        reserveLogStmt.setString(8, reserveUserDep);
                        reserveLogStmt.setString(9, adminName);
                        reserveLogStmt.setInt(10, availableQuantity);
                        reserveLogStmt.setInt(11, totalQuantity);
                        reserveLogStmt.setString(12, "시스템처리");
                        reserveLogStmt.setString(13, adminName);
                        reserveLogStmt.executeUpdate();
                        reserveLogStmt.close();
                    } else {
                        // 예약자가 대여 불가능한 상태인 경우, 예약 취소 및 대여가능 수량 증가
                        String deleteReservationSql = "DELETE FROM DB2025_RESERVATION WHERE reservation_id = ?";
                        PreparedStatement deleteReservationStmt = conn.prepareStatement(deleteReservationSql);
                        deleteReservationStmt.setInt(1, reservationId);
                        deleteReservationStmt.executeUpdate();
                        deleteReservationStmt.close();

                        // 대여가능 수량 증가
                        String updateItemSql =
                                "UPDATE DB2025_ITEMS SET available_quantity = available_quantity + 1 WHERE item_id = ?";
                        PreparedStatement updateItemStmt = conn.prepareStatement(updateItemSql);
                        updateItemStmt.setInt(1, itemId);
                        updateItemStmt.executeUpdate();
                        updateItemStmt.close();

                        availabilityIncreased = true;

                        // 예약 취소 로그 추가
                        String cancelLogSql =
                                "INSERT INTO DB2025_RENT_LOG " +
                                        "(item_id, user_id, admin_id, log_date, note, " +
                                        "item_name, item_category, user_name, user_dep, admin_name, " +
                                        "operation_type, created_by) " +
                                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?)";

                        PreparedStatement cancelLogStmt = conn.prepareStatement(cancelLogSql);
                        cancelLogStmt.setInt(1, itemId);
                        cancelLogStmt.setInt(2, reserveUserId);
                        cancelLogStmt.setInt(3, adminId);
                        cancelLogStmt.setString(4, "예약자 대여 불가로 인한 예약 취소");
                        cancelLogStmt.setString(5, itemName);
                        cancelLogStmt.setString(6, itemCategory);
                        cancelLogStmt.setString(7, reserveUserName);
                        cancelLogStmt.setString(8, reserveUserDep);
                        cancelLogStmt.setString(9, adminName);
                        cancelLogStmt.setString(10, "삭제");
                        cancelLogStmt.setString(11, adminName);
                        cancelLogStmt.executeUpdate();
                        cancelLogStmt.close();
                    }
                } else {
                    // 예약자가 없는 경우, 대여가능 수량 증가
                    String updateItemSql =
                            "UPDATE DB2025_ITEMS SET available_quantity = available_quantity + 1 WHERE item_id = ?";
                    PreparedStatement updateItemStmt = conn.prepareStatement(updateItemSql);
                    updateItemStmt.setInt(1, itemId);
                    updateItemStmt.executeUpdate();
                    updateItemStmt.close();

                    availabilityIncreased = true;
                }

                // 5. 반납 처리 로그 추가
                String logSql =
                        "INSERT INTO DB2025_RENT_LOG " +
                                "(rent_id, item_id, user_id, admin_id, previous_status, current_status, " +
                                "log_date, borrow_date, due_date, return_date, overdue_days, note, " +
                                "item_name, item_category, user_name, user_dep, admin_name, " +
                                "available_quantity, total_quantity, operation_type, created_by) " +
                                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement logStmt = conn.prepareStatement(logSql);
                logStmt.setInt(1, rentId);
                logStmt.setInt(2, itemId);
                logStmt.setInt(3, userId);
                logStmt.setInt(4, adminId);
                logStmt.setString(5, rentStatus);
                logStmt.setString(6, newStatus);
                logStmt.setDate(7, borrowDate);
                logStmt.setDate(8, dueDate);
                logStmt.setInt(9, overdueDays);
                logStmt.setString(10, note + (availabilityIncreased ? " (가용 수량 증가)" : " (예약자 대여 처리)"));
                logStmt.setString(11, itemName);
                logStmt.setString(12, itemCategory);
                logStmt.setString(13, userName);
                logStmt.setString(14, userDep);
                logStmt.setString(15, adminName);
                logStmt.setInt(16, availabilityIncreased ? availableQuantity + 1 : availableQuantity);
                logStmt.setInt(17, totalQuantity);
                logStmt.setString(18, "수정");
                logStmt.setString(19, adminName);
                logStmt.executeUpdate();
                logStmt.close();

                conn.commit(); // 모든 작업 성공 시 커밋
            } else {
                conn.rollback(); // 대여 정보가 없거나 이미 반납된 경우 롤백
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
            return false; // 반납 처리 실패
        } finally {
            closeResources(conn, stmt, rs);
        }

        return true; // 반납 처리 성공
    }
}