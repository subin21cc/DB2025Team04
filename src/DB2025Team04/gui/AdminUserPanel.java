package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/*
관리자 사용자 관리 패널 클래스
검색, 사용자 추가/수정/삭제 기능 및 사용자 목록 테이블 제공
*/
public class AdminUserPanel extends JPanel {
    private JTable userTable; // 사용자 목록을 보여주는 테이블
    private DefaultTableModel tableModel; // 테이블에 표시될 데이터 모델
    private JTextField searchField; // 검색어 입력 필드
    private JComboBox<String> searchTypeCombo; // 검색 항목 선택 콤보박스
    private boolean isSearching = false; // 검색 상태 여부
    private JButton adminAddButton; // 사용자 추가 버튼
    private JButton adminEditButton; // 사용자 수정 버튼
    private JButton adminRemoveButton; // 사용자 삭제 버튼

    /*
    패널 생성자. 레이아웃 설정 및 UI 초기화, 사용자 목록 로드
    */
    public AdminUserPanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadUserList();
    }

    /*
    UI 컴포넌트(검색, 테이블, 버튼 등) 초기화 및 이벤트 리스너 등록
    */
    private void initComponents() {
        // 검색 패널
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchTypeCombo = new JComboBox<>(new String[]{"사용자ID", "이름", "학과"});
        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> searchUsers());
        JButton resetButton = new JButton("검색 초기화");
        resetButton.addActionListener(e -> resetSearch());

        searchPanel.add(new JLabel("검색 항목:"));
        searchPanel.add(searchTypeCombo);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        // 사용자 목록 테이블 및 모델 생성
        String[] columns = {"사용자ID", "이름", "학과", "전화번호", "상태", "관리자여부"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 방지
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(userTable);

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        adminAddButton = new JButton("추가");
        adminEditButton = new JButton("수정");
        adminRemoveButton = new JButton("삭제");

        // 버튼 초기 상태
        adminAddButton.setEnabled(true);
        adminEditButton.setEnabled(false);
        adminRemoveButton.setEnabled(false);

        buttonPanel.add(adminAddButton);
        buttonPanel.add(adminEditButton);
        buttonPanel.add(adminRemoveButton);

        // 테이블 행 선택 시 수정/삭제 버튼 활성화
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = userTable.getSelectedRow();
                boolean hasSelection = selectedRow != -1;
                
                adminEditButton.setEnabled(hasSelection);
                adminRemoveButton.setEnabled(hasSelection);
            }
        });

        // 사용자 추가 버튼 클릭 시 다이얼로그 표시 및 DB 추가
        adminAddButton.addActionListener(e -> {
            UserDialog userDialog = new UserDialog(SwingUtilities.getWindowAncestor(this), "사용자 관리 - 추가");
            userDialog.setVisible(true);

            if (userDialog.isConfirmed()) {
                boolean success = DatabaseManager.getInstance().addUser(
                    userDialog.getUserId(),
                    userDialog.getPassword(),
                    userDialog.getName(),
                    userDialog.getDepartment(),
                    userDialog.getPhone(),
                    userDialog.getStatus(),
                    userDialog.isAdmin()
                );

                if (success) {
                    JOptionPane.showMessageDialog(this, "사용자가 성공적으로 추가되었습니다.", "추가 완료", JOptionPane.INFORMATION_MESSAGE);
                    loadUserList();
                } else {
                    JOptionPane.showMessageDialog(this, "사용자 추가에 실패했습니다.\n이미 사용 중인 ID나 전화번호일 수 있습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 사용자 수정 버튼 클릭 시 다이얼로그 표시 및 DB 수정
        adminEditButton.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                int userId = (int) tableModel.getValueAt(selectedRow, 0);
                String name = (String) tableModel.getValueAt(selectedRow, 1);
                String department = (String) tableModel.getValueAt(selectedRow, 2);
                String phone = (String) tableModel.getValueAt(selectedRow, 3);
                String status = (String) tableModel.getValueAt(selectedRow, 4);
                boolean isAdmin = ((String) tableModel.getValueAt(selectedRow, 5)).equals("O");

                UserDialog userDialog = new UserDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "사용자 관리 - 수정",
                    userId, name, department, phone, status, isAdmin
                );
                userDialog.setVisible(true);

                if (userDialog.isConfirmed()) {
                    boolean success = DatabaseManager.getInstance().updateUser(
                        userDialog.getUserId(),
                        userDialog.getPassword(),
                        userDialog.getName(),
                        userDialog.getDepartment(),
                        userDialog.getPhone(),
                        userDialog.getStatus(),
                        userDialog.isAdmin()
                    );

                    if (success) {
                        JOptionPane.showMessageDialog(this, "사용자 정보가 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
                        loadUserList();
                    } else {
                        JOptionPane.showMessageDialog(this, "사용자 정보 수정에 실패했습니다.\n이미 사용 중인 전화번호일 수 있습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // 사용자 삭제 버튼 클릭 시 확인 후 DB 삭제
        adminRemoveButton.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                int userId = (int) tableModel.getValueAt(selectedRow, 0);
                String name = (String) tableModel.getValueAt(selectedRow, 1);
                
                // 자기 자신은 삭제할 수 없도록 확인
                if (userId == SessionManager.getInstance().getUserId()) {
                    JOptionPane.showMessageDialog(this, 
                        "현재 로그인한 사용자를 삭제할 수 없습니다.",
                        "삭제 불가", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int option = JOptionPane.showConfirmDialog(
                    this,
                    String.format("선택한 사용자를 삭제할까요?\nID: %d\n이름: %s", userId, name),
                    "사용자 삭제 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (option == JOptionPane.YES_OPTION) {
                    boolean success = DatabaseManager.getInstance().deleteUser(userId);

                    if (success) {
                        JOptionPane.showMessageDialog(this, "사용자가 성공적으로 삭제되었습니다.", "삭제 완료", JOptionPane.INFORMATION_MESSAGE);
                        loadUserList();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "사용자 삭제에 실패했습니다.\n대여 중인 물품이 있거나 현재 로그인한 사용자입니다.", 
                            "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // 패널 조립
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /*
    사용자 목록을 DB에서 조회하여 테이블에 표시
    (검색 모드일 경우 검색 조건 반영)
    */
    public void loadUserList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT u.user_id, u.user_name, u.user_dep, u.user_phone, u.user_status, " +
                        "CASE WHEN a.admin_id IS NOT NULL THEN 'O' ELSE 'X' END AS is_admin " +
                        "FROM DB2025_USER u " +
                        "LEFT JOIN DB2025_ADMIN a ON u.user_id = a.user_id ";

            // 검색 모드일 때 WHERE 조건 추가
            if (isSearching) {
                switch (searchTypeCombo.getSelectedIndex()) {
                    case 0: // ID
                        sql += "WHERE u.user_id LIKE ? ";
                        break;
                    case 1: // 이름
                        sql += "WHERE u.user_name LIKE ? ";
                        break;
                    case 2: // 부서
                        sql += "WHERE u.user_dep LIKE ? ";
                        break;
                }
            }

            sql += "ORDER BY u.user_id";

            stmt = conn.prepareStatement(sql);

            // 검색어가 있을 때 파라미터 설정
            if (isSearching && !searchField.getText().isEmpty()) {
                stmt.setString(1, "%" + searchField.getText() + "%");
            }

            rs = stmt.executeQuery();

            tableModel.setRowCount(0);

            // 결과를 테이블에 추가
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("user_id"));
                row.add(rs.getString("user_name"));
                row.add(rs.getString("user_dep"));
                row.add(rs.getString("user_phone"));
                row.add(rs.getString("user_status"));
                row.add(rs.getString("is_admin"));

                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading user list: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

    /*
   검색 버튼 클릭 시 검색 모드로 전환 후 목록 로드
   */
    private void searchUsers() {
        isSearching = true;
        loadUserList();
    }

    /*
    검색 초기화 버튼 클릭 시 검색 모드 해제 및 전체 목록 로드
    */
    private void resetSearch() {
        isSearching = false;
        searchField.setText("");
        loadUserList();
    }
}