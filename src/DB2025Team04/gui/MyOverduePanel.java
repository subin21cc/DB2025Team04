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

public class MyOverduePanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton returnButton;

    public MyOverduePanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadOverdueList();
    }

    private void initComponents() {
        // Table
        String[] columns = {"연체ID", "물품분류", "물품명", "원래 반납기한", "연체일수", "제한기간", "상태"};
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

        JPanel buttonPanel = new JPanel();
        returnButton = new JButton("반납 처리");
        returnButton.setEnabled(false); // 초기 상태에서는 버튼 비활성화

        buttonPanel.add(returnButton);
        add(buttonPanel, BorderLayout.SOUTH);

        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {  // 선택된 행이 있는 경우
                    // '연체중' 상태일 경우에만 반납 버튼 활성화
                    String status = (String) tableModel.getValueAt(selectedRow, 6);
                    returnButton.setEnabled(status.equals("연체중"));
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    returnButton.setEnabled(false);
                }
            }
        });
        
        returnButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int overdueId = (int) tableModel.getValueAt(selectedRow, 0);
                String itemName = (String) tableModel.getValueAt(selectedRow, 2);
                
                int option = JOptionPane.showConfirmDialog(this,
                    String.format("'%s' 물품을 반납하시겠습니까?", itemName),
                    "연체 물품 반납",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (option == JOptionPane.YES_OPTION) {
                    if (processReturn(overdueId)) {
                        JOptionPane.showMessageDialog(this, 
                            "물품이 반납되었습니다.\n연체로 인한 대여 제한이 적용됩니다.",
                            "반납 완료", JOptionPane.INFORMATION_MESSAGE);
                        loadOverdueList(); // 목록 새로고침
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "반납 처리 중 오류가 발생했습니다.",
                            "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
    
    public void loadOverdueList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            
            String sql = "SELECT o.overdue_id, i.category, i.item_name, o.original_due, o.overdue_days, " +
                        "o.restriction_end, " +
                        "CASE " +
                        "    WHEN r.rent_status = '연체중' THEN '연체중' " +
                        "    WHEN r.rent_status = '연체완료' THEN '연체완료' " +
                        "    ELSE '처리됨' " +
                        "END AS status " +
                        "FROM DB2025_OVERDUES o " +
                        "JOIN DB2025_ITEMS i ON o.item_id = i.item_id " +
                        "JOIN DB2025_RENT r ON o.item_id = r.item_id AND o.user_id = r.user_id " +
                        "WHERE o.user_id = ? " +
                        "ORDER BY o.restriction_end DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();
            
            tableModel.setRowCount(0); // 기존 데이터 초기화
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("overdue_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(dateFormat.format(rs.getDate("original_due")));
                row.add(rs.getInt("overdue_days"));
                row.add(dateFormat.format(rs.getDate("restriction_end")));
                row.add(rs.getString("status"));
                
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
    
    private boolean processReturn(int overdueId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작
            
            // 1. 연체된 물품 정보 가져오기
            String findSql = "SELECT o.item_id, r.rent_id FROM DB2025_OVERDUES o " +
                           "JOIN DB2025_RENT r ON o.item_id = r.item_id AND o.user_id = r.user_id " + 
                           "WHERE o.overdue_id = ? AND o.user_id = ? AND r.rent_status = '연체중'";
            stmt = conn.prepareStatement(findSql);
            stmt.setInt(1, overdueId);
            stmt.setInt(2, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int itemId = rs.getInt("item_id");
                int rentId = rs.getInt("rent_id");
                
                // 2. 대여 상태 업데이트 (연체중 -> 연체완료)
                String updateSql = "UPDATE DB2025_RENT SET rent_status = '연체완료', return_date = CURRENT_DATE() " +
                                 "WHERE rent_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, rentId);
                stmt.executeUpdate();
                
                // 3. 물품 수량 증가
                String itemSql = "UPDATE DB2025_ITEMS SET available_quantity = available_quantity + 1 " +
                               "WHERE item_id = ?";
                stmt = conn.prepareStatement(itemSql);
                stmt.setInt(1, itemId);
                stmt.executeUpdate();
                
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
                    conn.setAutoCommit(true); // 자동 커밋 모드 복원
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
    
    // 패널이 표시될 때마다 데이터 새로고침
    public void refresh() {
        loadOverdueList();
    }
}