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
    private JRadioButton detailRadio;
    private JRadioButton userSummaryRadio;
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
        topPanel.setLayout(new GridLayout(2, 1) );

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        detailRadio = new JRadioButton("상세 보기", true);
        userSummaryRadio = new JRadioButton("사용자 요약");
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(detailRadio);
        viewGroup.add(userSummaryRadio);

        radioPanel.add(detailRadio);
        radioPanel.add(userSummaryRadio);

        allCheckBox = new JCheckBox("전체", true);
        checkBox1 = new JCheckBox("대여신청", true);
        checkBox2 = new JCheckBox("대여중", true);
        checkBox3 = new JCheckBox("반납완료", true);
        checkBox4 = new JCheckBox("연체중", true);
        checkBox5 = new JCheckBox("연체반납", true);

        filterPanel.add(allCheckBox);
        filterPanel.add(checkBox1);
        filterPanel.add(checkBox2);
        filterPanel.add(checkBox3);
        filterPanel.add(checkBox4);
        filterPanel.add(checkBox5);

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

        ItemListener reloadListener = e -> {
            if (detailRadio.isSelected()) loadItemList();
        };

        checkBox1.addItemListener(reloadListener);
        checkBox2.addItemListener(reloadListener);
        checkBox3.addItemListener(reloadListener);
        checkBox4.addItemListener(reloadListener);
        checkBox5.addItemListener(reloadListener);
        allCheckBox.addActionListener(e -> loadItemList());

        allCheckBox.addActionListener(checkAllListener);
        checkBox1.addItemListener(itemSyncListener);
        checkBox2.addItemListener(itemSyncListener);
        checkBox3.addItemListener(itemSyncListener);
        checkBox4.addItemListener(itemSyncListener);
        checkBox5.addItemListener(itemSyncListener);

        detailRadio.addActionListener(e -> {
            loadItemList();
            filterPanel.setVisible(true);
        });
        userSummaryRadio.addActionListener(e -> {
            loadUserOverview();
            filterPanel.setVisible(false);
        });

        topPanel.add(radioPanel);
        topPanel.add(filterPanel);
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
            if (checkBox3.isSelected()) selectedStatuses.add("반납완료");
            if (checkBox4.isSelected()) selectedStatuses.add("연체중");
            if (checkBox5.isSelected()) selectedStatuses.add("연체반납");

            if (selectedStatuses.isEmpty()) {
                tableModel.setRowCount(0);
                return;
            }
            conn = DatabaseManager.getInstance().getConnection();

            String sql;
            if (selectedStatuses.isEmpty()) {
                sql = "SELECT r.rent_id, i.category, i.item_name, u.user_name, " +
                        "r.borrow_date, r.return_date, r.rent_status " +
                        "FROM DB2025_RENT r " +
                        "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                        "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                        "ORDER BY r.borrow_date DESC";
                stmt = conn.prepareStatement(sql);
            } else {
                String inClause = String.join(",", Collections.nCopies(selectedStatuses.size(), "?"));
                sql = "SELECT r.rent_id, i.category, i.item_name, u.user_name, " +
                        "r.borrow_date, r.return_date, r.rent_status " +
                        "FROM DB2025_RENT r " +
                        "JOIN DB2025_ITEMS i ON r.item_id = i.item_id " +
                        "JOIN DB2025_USER u ON r.user_id = u.user_id " +
                        "WHERE r.rent_status IN (" + inClause + ") " +
                        "ORDER BY r.borrow_date DESC";
                stmt = conn.prepareStatement(sql);
                for (int i = 0; i < selectedStatuses.size(); i++) {
                    stmt.setString(i + 1, selectedStatuses.get(i));
                }
            }

            rs = stmt.executeQuery();

            String[] columns = {"ID", "분류", "대여물품", "대여자", "대여일", "반납일", "대여상태", "경과일수"};
            tableModel.setColumnIdentifiers(columns);
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

    public void loadUserOverview() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT user_id, user_name, active_rental_count, overdue_count, rented_items " +
                    "FROM VIEW_USER_RENTAL_OVERVIEW " +
                    "ORDER BY overdue_count DESC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            String[] columns = {"ID", "대여자", "대여중/연체중", "연체건수", "대여 중인 물품"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);


            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                row.add(rs.getString("user_name"));
                row.add(rs.getInt("active_rental_count"));
                row.add(rs.getInt("overdue_count"));
                row.add(rs.getString("rented_items") != null ? rs.getString("rented_items") : "-");
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "사용자 요약 정보 로딩 실패: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
}
