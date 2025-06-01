package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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


public class AdminReservationPanel extends JPanel {
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private JTextField dateSearchField;


    public AdminReservationPanel() {
        setLayout(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));

        // 검색 패널
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel dateSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(20);
        searchTypeCombo = new JComboBox<>(new String[] {"ID", "대여자", "대여물품"});

        // 예약일 검색 필드
        dateSearchField = new JTextField("예: 2025-05-17", 20);
        dateSearchField.setForeground(Color.GRAY);
        dateSearchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (dateSearchField.getText().equals("예: 2025-05-17")) {
                    dateSearchField.setText("");
                    dateSearchField.setForeground(Color.BLACK);
                }
            }

            public void focusLost(FocusEvent e) {
                if (dateSearchField.getText().isEmpty()) {
                    dateSearchField.setForeground(Color.GRAY);
                    dateSearchField.setText("예: 2025-05-17");
                }
            }
        });

        // 통합 검색 버튼
        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> {
            loadItemList();
        });

        // 통합 초기화 버튼
        JButton resetButton = new JButton("검색 초기화");
        resetButton.addActionListener(e -> {
            searchField.setText("");
            dateSearchField.setText("예: 2025-05-17");
            dateSearchField.setForeground(Color.GRAY);
            loadItemList();
        });

        // 컴포넌트 배치
        searchPanel.add(new JLabel("검색 항목:"));
        searchPanel.add(searchTypeCombo);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        dateSearchPanel.add(new JLabel("예약일 검색:"));
        dateSearchPanel.add(dateSearchField);

        topPanel.add(searchPanel);
        topPanel.add(dateSearchPanel);
        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"예약ID", "사용자ID", "대여자", "대여물품", "예약일"};
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
            conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT \n" +
                    "    r.reservation_id,\n" +
                    "    r.user_id,\n" +
                    "    u.user_name,\n" +
                    "    i.item_name,\n" +
                    "    r.reserve_date \n" +
                    "FROM DB2025_RESERVATION r\n" +
                    "JOIN DB2025_USER u ON r.user_id = u.user_id\n" +
                    "JOIN DB2025_ITEMS i ON r.item_id = i.item_id\n" +
                    "WHERE 1=1";
            String keyword = searchField.getText().trim();
            String date = dateSearchField.getText().trim();

            if (!keyword.isEmpty()) {
                switch (searchTypeCombo.getSelectedIndex()) {
                    case 0:
                        sql += " AND CAST(user_id AS CHAR) LIKE ?";
                        break;
                    case 1:
                        sql += " AND user_name LIKE ?";
                        break;
                    case 2:
                        sql += " AND item_name LIKE ?";
                        break;
                }
            }
            // 예약일 검색 조건
            if (!date.isEmpty() && !date.equals("예: 2025-05-17")) {
                sql += " AND DATE_FORMAT(reserve_date, '%Y-%m-%d') LIKE ?";
}
            sql += " ORDER BY reservation_id";
            stmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            if (!keyword.isEmpty()) {
                stmt.setString(paramIndex++, "%" + keyword + "%");
            }
            if (!date.isEmpty() && !date.equals("예: 2025-05-17")) {
                stmt.setString(paramIndex, "%" + date + "%");
            }
            rs = stmt.executeQuery();

            tableModel.setRowCount(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("reservation_id"));
                row.add(rs.getInt("user_id"));
                row.add(rs.getString("user_name"));
                row.add(rs.getString("item_name"));
                row.add(dateFormat.format(rs.getDate("reserve_date")));

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading my reservation status: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }
}
