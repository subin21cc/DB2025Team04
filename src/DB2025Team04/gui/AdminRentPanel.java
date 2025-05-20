package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class AdminRentPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton rentButton;
    private JButton reservationButton;
    private JButton deleteButton;

    public AdminRentPanel() {
        setLayout(new BorderLayout());
        initComponents();
//        loadItemList();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JCheckBox allCheckBox = new JCheckBox("전체", true);
        JCheckBox checkBox1 = new JCheckBox("대여신청", true);
        JCheckBox checkBox2 = new JCheckBox("대여중", true);
        JCheckBox checkBox3 = new JCheckBox("반납완료", true);
        JCheckBox checkBox4 = new JCheckBox("연체중", true);
        JCheckBox checkBox5 = new JCheckBox("연체완료", true);

        topPanel.add(allCheckBox);
        topPanel.add(checkBox1);
        topPanel.add(checkBox2);
        topPanel.add(checkBox3);
        topPanel.add(checkBox4);
        topPanel.add(checkBox5);

        ActionListener checkAllListener = e -> {
            boolean isSelected = allCheckBox.isSelected();
            checkBox1.setSelected(isSelected);
            checkBox2.setSelected(isSelected);
            checkBox3.setSelected(isSelected);
            checkBox4.setSelected(isSelected);
            checkBox5.setSelected(isSelected);
        };

        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "분류", "이름", "대여자", "대여일", "반납일", "대여상태", "경과일수"};
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
                    JOptionPane.showMessageDialog(this, "대여 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
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

            String sql = "SELECT r.rent_id, i.category, i.item_name, r.borrow_date, u.user_name, r.rent_status, r.return_date " +
                    "FROM DB2025_ITEMS i, DB2025_RENT r, DB2025_USER u " +
                    "WHERE i.item_id = r.item_id AND r.user_id = u.user_id " +
                    "ORDER BY borrow_date DESC";

            stmt = conn.prepareStatement(sql);
//            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("rent_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
                row.add(rs.getString("user_name"));
                row.add(dateFormat.format(rs.getDate("borrow_date")));
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
            JOptionPane.showMessageDialog(this, "Error loading my rent status: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

}
