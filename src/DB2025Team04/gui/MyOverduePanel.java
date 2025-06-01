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

public class MyOverduePanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;

    public MyOverduePanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadOverdueList();
    }

    private void initComponents() {
        // Table
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
    
    public void loadOverdueList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            
            // DB2025_RENT 테이블에서 연체 정보 조회
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
            JOptionPane.showMessageDialog(this, 
                "연체 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(),
                "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

    // 패널이 표시될 때마다 데이터 새로고침
    public void refresh() {
        loadOverdueList();
    }
}