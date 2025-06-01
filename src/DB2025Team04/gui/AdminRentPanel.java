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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * 관리자 대여 현황/반납처리 패널 클래스
 * - 대여 상세/사용자 요약 보기, 대여상태별 필터, 반납처리 기능 제공
 */
public class AdminRentPanel extends JPanel {
    private JTable itemTable; // 대여 목록을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블에 표시될 데이터 모델
    // 상세/요약 보기 전환 라디오 버튼
    private JRadioButton detailRadio;
    private JRadioButton userSummaryRadio;
    // 대여상태별 필터 체크박스
    private JButton returnButton;
    private JCheckBox allCheckBox;
    private JCheckBox checkBox1;
    private JCheckBox checkBox2;
    private JCheckBox checkBox3;
    private JCheckBox checkBox4;
    private JCheckBox checkBox5;

    /**
     * 패널 생성자. 레이아웃 설정 및 UI 컴포넌트 초기화
     */
    public AdminRentPanel() {
        setLayout(new BorderLayout());
        initComponents(); // UI 컴포넌트 초기화
    }

    /**
     * UI 컴포넌트(라디오, 체크박스, 테이블, 버튼 등) 초기화 및 이벤트 리스너 등록
     */
    private void initComponents() {
        // 상단 패널(라디오, 필터) 생성
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));

        // 상세/요약 보기 라디오 버튼 패널
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // 대여상태 필터 체크박스 패널
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 상세/요약 보기 라디오 버튼 생성 및 그룹화
        detailRadio = new JRadioButton("상세 보기", true); // 기본 선택
        userSummaryRadio = new JRadioButton("사용자 요약");
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(detailRadio);
        viewGroup.add(userSummaryRadio);

        radioPanel.add(detailRadio);
        radioPanel.add(userSummaryRadio);

        // 대여상태별 필터 체크박스 생성(전체, 각 상태)
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

        // '전체' 체크박스 클릭 시 모든 상태 체크/해제
        ActionListener checkAllListener = e -> {
            boolean isSelected = allCheckBox.isSelected();
            checkBox1.setSelected(isSelected);
            checkBox2.setSelected(isSelected);
            checkBox3.setSelected(isSelected);
            checkBox4.setSelected(isSelected);
            checkBox5.setSelected(isSelected);
        };

        // 개별 체크박스 상태 변경 시 '전체' 체크박스 동기화
        ItemListener itemSyncListener = e -> {
            boolean allChecked = checkBox1.isSelected()
                                 && checkBox2.isSelected()
                                 && checkBox3.isSelected()
                                 && checkBox4.isSelected()
                                 && checkBox5.isSelected();
            allCheckBox.setSelected(allChecked);
        };

        // 필터 변경 시 목록 새로고침(상세 보기일 때만)
        ItemListener reloadListener = e -> {
            if (detailRadio.isSelected()) loadItemList();
        };

        // 체크박스 이벤트 리스너 등록
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

        // 체크박스 이벤트 리스너 등록
        detailRadio.addActionListener(e -> {
            loadItemList(); // 상세 목록 로드
            filterPanel.setVisible(true); // 필터 보이기
        });
        userSummaryRadio.addActionListener(e -> {
            loadUserOverview(); // 사용자 요약 로드
            filterPanel.setVisible(false); // 필터 숨기기
        });

        // 상단 패널에 라디오, 필터 추가
        topPanel.add(radioPanel);
        topPanel.add(filterPanel);
        add(topPanel, BorderLayout.NORTH);

        // 대여 목록 테이블 및 모델 생성
        String[] columns = {"물품ID", "분류", "대여물품", "대여자", "대여일", "반납일", "대여상태", "경과일수"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 셀 편집 비활성화
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 테이블 행 선택 시 반납처리 버튼 활성화 조건
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = itemTable.getSelectedRow();
                // 상세보기 + 대여중/연체중 상태만 버튼 활성화
                returnButton.setEnabled(selectedRow != -1 && detailRadio.isSelected() &&
                        ("대여중".equals(tableModel.getValueAt(selectedRow, 6)) ||
                        "연체중".equals(tableModel.getValueAt(selectedRow, 6))));
            }
        });

        // 반납처리 버튼 및 패널 생성
        JPanel buttonPanel = new JPanel();
        returnButton = new JButton("반납처리");
        returnButton.setEnabled(false); // 초기 상태에서는 버튼 비활성화
        buttonPanel.add(returnButton);

        // 반납처리 버튼 클릭 시 동작
        returnButton.addActionListener(e -> {
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                int rentId = (int) tableModel.getValueAt(selectedRow, 0);
                String status = (String) tableModel.getValueAt(selectedRow, 6);
                // 대여중/연체중만 반납처리 가능
                if ("대여중".equals(status) || "연체중".equals(status)) {
                    int option = JOptionPane.showConfirmDialog(this,
                            "반납처리 하시겠습니까?", "반납처리 확인",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        // DB 반납처리
                        if (DatabaseManager.getInstance().processReturn(rentId)) {
                            JOptionPane.showMessageDialog(this, "반납처리가 완료되었습니다.");
                            loadItemList(); // 목록 새로고침
                        } else {
                            JOptionPane.showMessageDialog(this, "반납 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    // 대여중/연체중이 아닌 경우 경고
                    JOptionPane.showMessageDialog(this, "대여중 또는 연체중인 물품만 반납처리 가능합니다.", "경고", JOptionPane.WARNING_MESSAGE);
                }


            }
        });

        // 대여중/연체중이 아닌 경우 경고
        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 대여상태별 필터에 따라 대여 상세 목록을 DB에서 불러와 테이블에 표시
     */
    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 선택된 대여상태 목록 생성
            List<String> selectedStatuses = new ArrayList<>();
            if (checkBox1.isSelected()) selectedStatuses.add("대여신청");
            if (checkBox2.isSelected()) selectedStatuses.add("대여중");
            if (checkBox3.isSelected()) selectedStatuses.add("반납완료");
            if (checkBox4.isSelected()) selectedStatuses.add("연체중");
            if (checkBox5.isSelected()) selectedStatuses.add("연체반납");

            // 아무 상태도 선택 안 했으면 테이블 비움
            if (selectedStatuses.isEmpty()) {
                tableModel.setRowCount(0);
                return;
            }
            conn = DatabaseManager.getInstance().getConnection();

            String sql;
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

            rs = stmt.executeQuery();

            String[] columns = {"대여ID", "분류", "대여물품", "대여자", "대여일", "반납일", "대여상태", "경과일수"};
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
                if ("연체중".equals(rs.getString("rent_status"))) {
                    LocalDate borrowDate = rs.getDate("borrow_date").toLocalDate();
                    LocalDate returnDateLocal = rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : LocalDate.now();
                    long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(borrowDate, returnDateLocal);
                    row.add(daysElapsed);
                } else {
                    row.add("-");
                }

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
            // idx_user_status_due, idx_rent_status 인덱스 사용
            // DB2025_VIEW_USER_RENTAL_OVERVIEW 뷰 사용
            String sql = "SELECT user_id, user_name, active_rental_count, overdue_count, rented_items " +
                    "FROM DB2025_VIEW_USER_RENTAL_OVERVIEW " +
                    "ORDER BY overdue_count DESC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            String[] columns = {"사용자ID", "대여자", "대여중/연체중", "연체건수", "대여 중인 물품"};
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
