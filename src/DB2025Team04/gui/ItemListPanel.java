package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ItemListPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton rentButton;
    private JButton reservationButton;
    private JButton deleteButton;

    public ItemListPanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadItemList(); // itemList는 항상 load해야 함
    }

    private void initComponents() {
        // Table
        String[] columns = {"ID", "분류", "이름", "전체수량", "대여가능수량"};
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
                    // 대여가능수량은 4번 인덱스(5번째 열)에 있음
                    int availableQuantity = (int) tableModel.getValueAt(selectedRow, 4);

                    // 대여가능수량에 따라 버튼 활성화/비활성화
                    if (availableQuantity > 0) {
                        rentButton.setEnabled(true);
                        reservationButton.setEnabled(false);
                    } else {
                        rentButton.setEnabled(false);
                        reservationButton.setEnabled(true);
                    }
                } else {  // 선택된 행이 없는 경우 모두 비활성화
                    rentButton.setEnabled(false);
                    reservationButton.setEnabled(false);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        rentButton = new JButton("대여");
        reservationButton = new JButton("예약");
        deleteButton = new JButton("Delete");

        // 초기 상태에서는 버튼 비활성화
        rentButton.setEnabled(false);
        reservationButton.setEnabled(false);

        buttonPanel.add(rentButton);
        buttonPanel.add(reservationButton);
        buttonPanel.add(deleteButton);

        rentButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int itemId = (int) tableModel.getValueAt(selectedRow, 0);
                // Handle rent action
                if (DatabaseManager.getInstance().processRental(itemId, SessionManager.getInstance().getUserId())) {
                    JOptionPane.showMessageDialog(this, "대여가 완료되었습니다.");
                    loadItemList(); // Refresh the item list
                } else {
                    JOptionPane.showMessageDialog(this, "이미 대여중인 물품을 추가 대여할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "대여할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        reservationButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int itemId = (int) tableModel.getValueAt(selectedRow, 0);
                // Handle reservation action
                JOptionPane.showMessageDialog(this, "예약 버튼 클릭: " + itemId);
            } else {
                JOptionPane.showMessageDialog(this, "예약할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
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

            String sql = "SELECT item_id, item_name, quantity, available_quantity, category " +
                    "FROM DB2025_ITEMS " +
                    "ORDER BY category, item_name";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("item_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(rs.getInt("quantity"));
                row.add(rs.getInt("available_quantity"));

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading item list: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

}
