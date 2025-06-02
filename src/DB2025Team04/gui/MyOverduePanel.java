package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Date;
import java.util.Calendar;

// 파일에 코드를 이해할 수 있도록 주석달아줘
public class MyOverduePanel extends JPanel {
    private JTable itemTable; // 연체 목록을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블에 표시될 데이터 모델

    // 생성자: 패널 레이아웃 및 컴포넌트 초기화, 연체 목록 로드
    public MyOverduePanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadOverdueList();
    }

    // 테이블 및 스크롤 패널 등 UI 컴포넌트 초기화
    private void initComponents() {
        // 테이블 컬럼명 정의
        String[] columns = {"대여ID", "물품분류", "물품명", "원래 반납기한", "연체일수", "상태"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 방지
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    // DB에서 내 연체 목록을 조회하여 테이블에 표시
    public void loadOverdueList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();

            // 연체중 또는 연체반납 상태의 대여 내역 조회 쿼리
            String sql = "SELECT r.rent_id, i.category, i.item_name, r.due_date, " +
                        "DATEDIFF(CASE WHEN r.return_date IS NULL THEN CURRENT_DATE ELSE r.return_date END, r.due_date) AS overdue_days, " +
                        "r.rent_status " +
                        "FROM DB2025_RENT r " +
                        "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                        "WHERE r.user_id = ? AND r.rent_status IN ('연체중', '연체반납') " +
                        "ORDER BY r.due_date ASC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();
            
            tableModel.setRowCount(0); // 기존 데이터 초기화
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // 결과를 테이블에 한 행씩 추가
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("rent_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(dateFormat.format(rs.getDate("due_date")));
                row.add(rs.getInt("overdue_days"));
                row.add(rs.getString("rent_status"));
                
                tableModel.addRow(row);
            }
            
        } catch (SQLException e) {
            // DB 오류 발생 시 사용자에게 알림
            JOptionPane.showMessageDialog(this, 
                "연체 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(),
                "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            // DB 연결 해제
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

    // 패널이 표시될 때마다 데이터 새로고침
    public void refresh() {
        loadOverdueList();
    }
}