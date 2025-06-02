package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Vector;

/*
관리자 예약 현황 조회 패널 클래스
검색(아이디/이름/물품/날짜) 기능과 예약 목록 테이블 제공
*/
public class AdminReservationPanel extends JPanel {
    private JTable itemTable; // 예약 목록을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블 데이터 모델
    private JTextField searchField; // 통합 검색어 입력 필드
    private JComboBox<String> searchTypeCombo; // 검색 항목 선택 콤보박스
    private JTextField dateSearchField; // 예약일 검색 입력 필드

    /*
    패널 생성자. 레이아웃 설정 및 UI 초기화
    */
    public AdminReservationPanel() {
        setLayout(new BorderLayout());
        initComponents();
    }

    /*
    UI 컴포넌트(검색, 테이블 등) 초기화 및 이벤트 리스너 등록
    */
    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));

        // 검색 패널(검색 항목, 검색어, 버튼)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // 예약일 검색 패널
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

        // 검색 버튼: 조건에 맞는 예약 목록 조회
        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> {
            loadItemList();
        });

        // 검색 초기화 버튼: 입력값 초기화 및 전체 목록 조회
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

        // 예약 목록 테이블 생성
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

    /*
    검색 조건(아이디/이름/물품/날짜)에 따라 예약 목록을 DB에서 조회하여 테이블에 표시
    */
    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            // 기본 쿼리(예약 뷰)
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

            // 검색어가 있을 때 검색 항목별 조건 추가
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

            // 파라미터 바인딩
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

            // 결과를 테이블에 추가
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
