package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;
import DB2025Team04.gui.ItemDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

/*
대여 물품 목록 패널 클래스
- 검색, 대여/예약(사용자), 추가/수정/삭제(관리자) 기능 제공
*/
public class ItemListPanel extends JPanel {
    private JTable itemTable; // 물품 목록 테이블
    private DefaultTableModel tableModel; // 테이블 데이터 모델
    private JButton rentButton; // 대여 신청 버튼 (사용자)
    private JButton reservationButton; // 예약 버튼 (사용자)
    private JTextField searchField; // 검색어 입력 필드
    private JComboBox<String> searchTypeCombo; // 검색 항목 선택 콤보박스
    private boolean isSearching = false; // 검색 상태 여부
    private JButton adminAddButton; // 물품 추가 버튼 (관리자)
    private JButton adminRemoveButton; // 물품 삭제 버튼 (관리자)
    private JButton adminEditButton; // 물품 수정 버튼 (관리자)

    /*
    패널 생성자. 레이아웃 설정 및 UI 초기화, 물품 목록 로드
    */
    public ItemListPanel() {
        setLayout(new BorderLayout());
        initComponents();
        loadItemList(); // itemList는 항상 load해야 함
    }

    /*
    UI 컴포넌트(검색, 테이블, 버튼 등) 초기화 및 이벤트 리스너 등록
    */
    private void initComponents() {
        // 검색 패널 생성
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchTypeCombo = new JComboBox<>(new String[] {"분류", "물품이름"});
        JButton searchButton = new JButton("검색");
        searchButton.addActionListener(e -> searchItems());
        JButton resetButton = new JButton("검색 초기화");
        resetButton.addActionListener(e->resetSearch());

        searchPanel.add(new JLabel("검색 항목:"));
        searchPanel.add(searchTypeCombo);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        // 물품 목록 테이블 및 모델 생성
        String[] columns = {"물품ID", "분류", "물품이름", "전체수량", "대여가능수량"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing
            }
        };
        itemTable = new JTable(tableModel);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(itemTable);

        // 테이블 행 선택 시 버튼 활성화/비활성화 처리
        itemTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {  // 이벤트가 조정 중이 아닐 때만 처리
                int selectedRow = itemTable.getSelectedRow();
                boolean hasSelection = selectedRow != -1;
                
                if (SessionManager.getInstance().isAdmin()) {
                    // 관리자 모드: 수정/삭제 버튼 활성화
                    if (adminEditButton != null) adminEditButton.setEnabled(hasSelection);
                    if (adminRemoveButton != null) adminRemoveButton.setEnabled(hasSelection);
                } else {
                    // 사용자 모드: 대여/예약 버튼 활성화 조건
                    if (hasSelection) {
                        // 선택된 행이 있는 경우
                        int availableQuantity = (int) tableModel.getValueAt(selectedRow, 4);
                        
                        // 대여가능수량에 따라 버튼 활성화/비활성화
                        if (rentButton != null) rentButton.setEnabled(availableQuantity > 0);
                        if (reservationButton != null) reservationButton.setEnabled(availableQuantity <= 0);
                    } else {
                        // 선택된 행이 없는 경우 모두 비활성화
                        if (rentButton != null) rentButton.setEnabled(false);
                        if (reservationButton != null) reservationButton.setEnabled(false);
                    }
                }
            }
        });

        // 버튼 패널 생성 및 권한별 버튼 배치
        JPanel buttonPanel = new JPanel();
        if (SessionManager.getInstance().isAdmin()) {
            // 관리자 모드: 추가/수정/삭제 버튼
            adminAddButton = new JButton("추가");
            adminEditButton = new JButton("수정");
            adminRemoveButton = new JButton("삭제");

            // 버튼의 초기 상태
            adminAddButton.setEnabled(true);
            adminEditButton.setEnabled(false);
            adminRemoveButton.setEnabled(false);

            buttonPanel.add(adminAddButton);
            buttonPanel.add(adminEditButton);
            buttonPanel.add(adminRemoveButton);

            // 추가 버튼 클릭 시 다이얼로그 표시 및 DB 추가
            adminAddButton.addActionListener(e -> {
                ItemDialog itemDialog = new ItemDialog(SwingUtilities.getWindowAncestor(this), "대여 물품 관리 - 추가");
                itemDialog.setVisible(true);
                
                // 다이얼로그가 확인 버튼으로 닫힌 경우에만 아이템 목록 새로고침
                if (itemDialog.isConfirmed()) {
                    loadItemList();
                }
            });

            // 수정 버튼 클릭 시 다이얼로그 표시 및 DB 수정
            adminEditButton.addActionListener(e -> {
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {
                    // 선택된 행의 데이터 가져오기
                    int itemId = (int) tableModel.getValueAt(selectedRow, 0);
                    String category = (String) tableModel.getValueAt(selectedRow, 1);
                    String name = (String) tableModel.getValueAt(selectedRow, 2);
                    int totalQuantity = (int) tableModel.getValueAt(selectedRow, 3);
                    int availableQuantity = (int) tableModel.getValueAt(selectedRow, 4);
                    
                    // 수정 다이얼로그 열기
                    ItemDialog itemDialog = new ItemDialog(
                        SwingUtilities.getWindowAncestor(this), 
                        "대여 물품 관리 - 수정",
                        itemId, category, name, totalQuantity, availableQuantity
                    );
                    itemDialog.setVisible(true);
                    
                    // 다이얼로그가 확인 버튼으로 닫힌 경우에만 아이템 목록 새로고침
                    if (itemDialog.isConfirmed()) {
                        loadItemList();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "수정할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
                }
            });

            // 삭제 버튼 클릭 시 확인 후 DB 삭제
            adminRemoveButton.addActionListener(e -> {
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {
                    // 선택된 행의 데이터 가져오기
                    int itemId = (int) tableModel.getValueAt(selectedRow, 0);
                    String category = (String) tableModel.getValueAt(selectedRow, 1);
                    String name = (String) tableModel.getValueAt(selectedRow, 2);
                    
                    // 삭제 확인 대화상자
                    int option = JOptionPane.showConfirmDialog(
                        this,
                        String.format("선택한 물품을 삭제할까요?\n분류: %s\n이름: %s", category, name),
                        "물품 삭제 확인",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    
                    if (option == JOptionPane.YES_OPTION) {
                        // 물품 삭제 처리
                        boolean success = DatabaseManager.getInstance().deleteItem(itemId);
                        if (success) {
                            JOptionPane.showMessageDialog(this, "물품이 성공적으로 삭제되었습니다.", "삭제 완료", JOptionPane.INFORMATION_MESSAGE);
                            loadItemList(); // 목록 새로고침
                        } else {
                            JOptionPane.showMessageDialog(this, 
                                "물품 삭제에 실패했습니다.\n대여 중인 물품은 삭제할 수 없습니다.", 
                                "오류", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "삭제할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
                }
            });
        } else {
            // 사용자 모드: 대여/예약 버튼
            rentButton = new JButton("대여신청");
            reservationButton = new JButton("예약");

            // 초기 상태에서는 버튼 비활성화
            rentButton.setEnabled(false);
            reservationButton.setEnabled(false);

            buttonPanel.add(rentButton);
            buttonPanel.add(reservationButton);

            // 대여신청 버튼 클릭 시 대여 처리
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

            // 예약 버튼 클릭 시 예약 처리
            reservationButton.addActionListener(e -> {
                int selectedRow = itemTable.getSelectedRow();
                if (selectedRow != -1) {
                    int itemId = (int) tableModel.getValueAt(selectedRow, 0);
                    String itemName = (String) tableModel.getValueAt(selectedRow, 2);
                    int userId = SessionManager.getInstance().getUserId();
                    
                    // 예약 처리
                    int result = DatabaseManager.getInstance().processReservation(itemId, userId);
                    
                    switch (result) {
                        case 0:
                            JOptionPane.showMessageDialog(this, 
                                String.format("'%s' 물품이 성공적으로 예약되었습니다.", itemName),
                                "예약 완료", JOptionPane.INFORMATION_MESSAGE);
                            loadItemList(); // 목록 새로고침
                            break;
                        case 1:
                            JOptionPane.showMessageDialog(this, 
                                "대여 가능한 수량이 있는 물품은 예약할 수 없습니다.\n대여 버튼을 이용해주세요.",
                                "예약 불가", JOptionPane.WARNING_MESSAGE);
                            break;
                        case 2:
                            JOptionPane.showMessageDialog(this, 
                                "이미 예약한 상태입니다.",
                                "예약 불가", JOptionPane.WARNING_MESSAGE);
                            break;
                        case 3:
                            JOptionPane.showMessageDialog(this, 
                                "연체 중인 상태에서는 예약할 수 없습니다.\n연체된 물품을 먼저 반납해주세요.",
                                "예약 불가", JOptionPane.WARNING_MESSAGE);
                            break;
                        default:
                            JOptionPane.showMessageDialog(this, 
                                "예약 처리 중 오류가 발생했습니다.",
                                "오류", JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "예약할 물품을 선택하세요.", "경고", JOptionPane.WARNING_MESSAGE);
                }
            });
        }

        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // 물품 목록을 DB에서 로드하여 테이블에 표시
    public void loadItemList() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            String sql = "SELECT item_id, item_name, quantity, available_quantity, category " +
                    "FROM DB2025_ITEMS ";
            boolean hasCondition = false;
            if (isSearching) {
                switch (searchTypeCombo.getSelectedIndex()) {
                    case 0:
                        sql += "WHERE category LIKE ? ";
                        hasCondition = true;
                        break;
                    case 1:
                        sql += "WHERE item_name LIKE ? ";
                        hasCondition = true;
                        break;
                }
            }
            sql += "ORDER BY category, item_name";

            stmt = conn.prepareStatement(sql);
            if (hasCondition) {
                stmt.setString(1, "%" + searchField.getText().trim() + "%");
            }

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

    // 검색 버튼 클릭 시 검색어에 따라 물품 목록 필터링
    private void searchItems() {
        isSearching = true;
        loadItemList();
    }

    // 검색 초기화 버튼 클릭 시 검색 상태를 초기화하고 전체 목록 로드
    private void resetSearch() {
        isSearching = false;
        searchField.setText("");
        loadItemList();
    }

}