package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ItemListPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;

    public ItemListPanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadItemList();
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

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadItemList() {
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
            JOptionPane.showMessageDialog(this, "Error loading movies: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

}
