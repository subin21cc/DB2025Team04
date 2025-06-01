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

public class MyReservationPanel extends JPanel {
    private JTable reservationTable;
    private DefaultTableModel tableModel;
    private JButton cancelButton;

    public MyReservationPanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadReservationList();
    }

    private void initComponents() {
        // 테이블 초기화
        String[] columns = {"예약ID", "물품분류", "물품명", "예약일", "상태"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 방지
            }
        };
        reservationTable = new JTable(tableModel);
        reservationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(reservationTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cancelButton = new JButton("예약 취소");
        cancelButton.setEnabled(false);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 테이블 선택 이벤트
        reservationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = reservationTable.getSelectedRow() != -1;
                cancelButton.setEnabled(hasSelection);
            }
        });
        
        // 예약 취소 버튼 이벤트
        cancelButton.addActionListener(e -> {
            int selectedRow = reservationTable.getSelectedRow();
            if (selectedRow != -1) {
                int reservationId = (int) tableModel.getValueAt(selectedRow, 0);
                String itemName = (String) tableModel.getValueAt(selectedRow, 2);
                
                int option = JOptionPane.showConfirmDialog(this,
                    String.format("'%s' 물품의 예약을 취소하시겠습니까?", itemName),
                    "예약 취소 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (option == JOptionPane.YES_OPTION) {
                    if (cancelReservation(reservationId)) {
                        JOptionPane.showMessageDialog(this, 
                            "예약이 취소되었습니다.",
                            "예약 취소 완료", JOptionPane.INFORMATION_MESSAGE);
                        loadReservationList(); // 목록 새로고침
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "예약 취소 중 오류가 발생했습니다.",
                            "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    public void loadReservationList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            
            String sql = "SELECT r.reservation_id, i.category, i.item_name, r.reserve_date, " +
                        "CASE " +
                        "    WHEN i.available_quantity > 0 THEN '대여가능' " +
                        "    ELSE '대기중' " +
                        "END AS status " +
                        "FROM DB2025_RESERVATION r " +
                        "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                        "WHERE r.user_id = ? " +
                        "ORDER BY r.reserve_date DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();
            
            tableModel.setRowCount(0); // 기존 데이터 초기화
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("reservation_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(dateFormat.format(rs.getDate("reserve_date")));
                row.add(rs.getString("status"));
                
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "예약 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(),
                "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
    
    private boolean cancelReservation(int reservationId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = DatabaseManager.getInstance().getConnection();
            
            String sql = "DELETE FROM DB2025_RESERVATION WHERE reservation_id = ? AND user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, reservationId);
            stmt.setInt(2, SessionManager.getInstance().getUserId());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, null);
        }
    }
    
    // 패널이 표시될 때마다 데이터 새로고침
    public void refresh() {
        loadReservationList();
    }
}