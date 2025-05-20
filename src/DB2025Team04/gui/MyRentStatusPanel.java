package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class MyRentStatusPanel extends JPanel {
    private JTable myRentTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    public MyRentStatusPanel() {
        setLayout(new BorderLayout());
        initComponents();
//        loadMyRentStatus();
    }

    private void initComponents() {
        String[] columnNames = {"ID", "분류", "이름", "대여일", "반납일"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing
            }
        };
        myRentTable = new JTable(tableModel);
        myRentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(myRentTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);

    }

    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT i.item_id, category, item_name, borrow_date, return_date " +
                    "FROM DB2025_ITEMS i, DB2025_RENT r " +
                    "WHERE r.user_id = ? AND i.item_id = r.item_id " +
                    "ORDER BY borrow_date DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, SessionManager.getInstance().getUserId());
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("item_id"));
                row.add(rs.getString("category"));
                row.add(rs.getString("item_name"));
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
