package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

// 내 대여 현황을 보여주는 패널 클래스
public class MyRentStatusPanel extends JPanel {
    private JTable myRentTable; // 대여 현황을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블에 표시될 데이터 모델

    // 생성자: 패널 레이아웃 및 컴포넌트 초기화
    public MyRentStatusPanel() {
        setLayout(new BorderLayout());
        initComponents();
    }

    // 테이블 및 스크롤 패널 등 UI 컴포넌트 초기화
    private void initComponents() {
        // 테이블 컬럼명 정의
        String[] columnNames = {"대여ID", "물품ID", "분류", "이름", "대여일", "반납일", "대여상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRentTable = new JTable(tableModel);
        myRentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(myRentTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    // DB에서 내 대여 현황을 조회하여 테이블에 표시
    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            // DB2025_VIEW_USER_RENT_STATUS 뷰 사용
            // 대여 현황 조회 쿼리
            String sql = "SELECT rent_id, item_id, category, item_name, borrow_date, return_date, rent_status " +
                    "FROM DB2025_VIEW_USER_RENT_STATUS " +
                    "WHERE user_id = ? " +
                    "ORDER BY borrow_date DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // 결과를 테이블에 한 행씩 추가
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("rent_id"));
                row.add(rs.getInt("item_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(dateFormat.format(rs.getDate("borrow_date")));
                // 반납일은 null일 수 있으므로 예외 처리
                String returnDate;
                try {
                    returnDate = dateFormat.format(rs.getDate("return_date"));
                } catch (Exception e) {
                    returnDate = "-";
                }
                row.add(returnDate);
                row.add(rs.getString("rent_status"));

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "내 대여 현황 로딩 실패: " + e.getMessage(),
                    "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
}
