package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class AdminRentPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JButton rentButton;
    private JButton reservationButton;
    private JButton deleteButton;
    private JCheckBox allCheckBox;
    private JCheckBox checkBox1;
    private JCheckBox checkBox2;
    private JCheckBox checkBox3;
    private JCheckBox checkBox4;
    private JCheckBox checkBox5;


    public AdminRentPanel() {
        setLayout(new BorderLayout());
        initComponents();
//        loadItemList();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        allCheckBox = new JCheckBox("전체", true);
        checkBox1 = new JCheckBox("대여신청", true);
        checkBox2 = new JCheckBox("대여중", true);
        checkBox3 = new JCheckBox("반납", true);
        checkBox4 = new JCheckBox("연체중", true);
        checkBox5 = new JCheckBox("연체반납", true);

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

        ItemListener itemSyncListener = e -> {
            boolean allChecked = checkBox1.isSelected()
                                 && checkBox2.isSelected()
                                 && checkBox3.isSelected()
                                 && checkBox4.isSelected()
                                 && checkBox5.isSelected();
            allCheckBox.setSelected(allChecked);
        };

        ItemListener reloadListener = e -> loadItemList();
        checkBox1.addItemListener(reloadListener);
        checkBox2.addItemListener(reloadListener);
        checkBox3.addItemListener(reloadListener);
        checkBox4.addItemListener(reloadListener);
        checkBox5.addItemListener(reloadListener);
        allCheckBox.addActionListener(e -> loadItemList());  // 클릭 시 테이블 갱신

        allCheckBox.addActionListener(checkAllListener);
        checkBox1.addItemListener(itemSyncListener);
        checkBox2.addItemListener(itemSyncListener);
        checkBox3.addItemListener(itemSyncListener);
        checkBox4.addItemListener(itemSyncListener);
        checkBox5.addItemListener(itemSyncListener);
        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "분류", "대여물품", "대여자", "대여일", "반납일", "대여상태", "경과일수"};
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
    }

    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            List<String> selectedStatuses = new ArrayList<>();
            if (checkBox1.isSelected()) selectedStatuses.add("대여신청");
            if (checkBox2.isSelected()) selectedStatuses.add("대여중");
            if (checkBox3.isSelected()) selectedStatuses.add("반납");
            if (checkBox4.isSelected()) selectedStatuses.add("연체중");
            if (checkBox5.isSelected()) selectedStatuses.add("연체반납");

            conn = DatabaseManager.getInstance().getConnection();

            String inClause = String.join(",", Collections.nCopies(selectedStatuses.size(), "?"));
            String sql = "SELECT rent_id, user_name, item_name, borrow_date, return_date, rent_status " +
                    "FROM VIEW_RENT_DETAIL " +
                    "WHERE rent_status IN (" + inClause + ") " +
                    "ORDER BY borrow_date DESC";

            stmt = conn.prepareStatement(sql);

            for (int i = 0; i < selectedStatuses.size(); i++) {
                stmt.setString(i + 1, selectedStatuses.get(i));
            }

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
