package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class AdminOutPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton doneButton;
    private JButton cancelButton;

    public AdminOutPanel() {
        setLayout(new BorderLayout());
        initComponents();
//        loadItemList();
    }

    private void initComponents() {
        // Table
        String[] columns = {"ID", "분류", "이름", "대여자", "대여일"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER);

        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {  // 선택된 행이 있는 경우
                    doneButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    doneButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        doneButton = new JButton("출고 완료");
        cancelButton = new JButton("출고 취소");

        // 초기 상태에서는 버튼 비활성화
        doneButton.setEnabled(false);
        cancelButton.setEnabled(false);

        buttonPanel.add(doneButton);
        buttonPanel.add(cancelButton);

        doneButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int rentId = (int) tableModel.getValueAt(selectedRow, 0);
                // 출고처리
                if (DatabaseManager.getInstance().processOutDone(rentId)) {
                    JOptionPane.showMessageDialog(this, "출고가 완료되었습니다.");
                    loadItemList(); // Refresh the item list
                } else {
                    JOptionPane.showMessageDialog(this, "출고 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "출고할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int rentId = (int) tableModel.getValueAt(selectedRow, 0);
                // 출고취소 -> 예약 삭제
                if (DatabaseManager.getInstance().processDeleteRent(rentId)) {
                    JOptionPane.showMessageDialog(this, "출고가 취소되었습니다.");
                    loadItemList(); // Refresh the item list
                } else {
                    JOptionPane.showMessageDialog(this, "출고 취소 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "취소할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT r.rent_id, category, item_name, borrow_date, u.user_name " +
                    "FROM DB2025_ITEMS i, DB2025_RENT r, DB2025_USER u " +
                    "WHERE i.item_id = r.item_id AND r.user_id = u.user_id AND r.rent_status='대여신청' " +
                    "ORDER BY borrow_date DESC";
            // rent_status 부분 인덱스 사용
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("rent_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(rs.getString("user_name"));
                row.add(dateFormat.format(rs.getDate("borrow_date")));
                String returnDate;
                try {
                    returnDate = dateFormat.format(rs.getString("return_date"));
                } catch (Exception e) {
                    returnDate = "-";
                }
                row.add(returnDate);

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading my rent status: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

}
