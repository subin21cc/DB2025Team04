package DB2025Team04.gui;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.SpinnerNumberModel;

import DB2025Team04.db.DatabaseManager;

/*
물품 추가/수정 다이얼로그 클래스
물품의 분류, 이름, 전체수량, 대여가능수량을 입력받아 DB에 추가 또는 수정
*/
public class ItemDialog extends JDialog {
    private JTextField categoryField; // 분류 입력 필드
    private JTextField nameField; // 이름 입력 필드
    private JSpinner totalQuantitySpinner; // 전체수량 입력
    private JSpinner availableQuantitySpinner; // 대여가능수량 입력
    private boolean isConfirmed = false; // 확인 버튼 클릭 여부
    private int itemId = -1; // 새 필드: 수정 시 사용할 아이템 ID
    private boolean isEditMode = false; // 수정 모드 여부

    // 신규 등록용 생성자
    public ItemDialog(Window parent, String title) {
        this(parent, title, -1, null, null, 1, 1);
    }

    // 수정용 생성자 추가
    public ItemDialog(Window parent, String title, int itemId, String category, String name, int totalQuantity, int availableQuantity) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        setSize(500, 300);
        setLocationRelativeTo(parent);

        this.itemId = itemId;
        this.isEditMode = (itemId > 0);

        // 전체 레이아웃 설정
        setLayout(new BorderLayout());

        // 메인 입력 패널
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 분류 입력
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("분류"), gbc);

        categoryField = new JTextField(20);
        if (category != null) categoryField.setText(category);
        gbc.gridx = 1;
        mainPanel.add(categoryField, gbc);

        // 이름 입력
        gbc.gridy = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("이름"), gbc);

        nameField = new JTextField(20);
        if (name != null) nameField.setText(name);
        gbc.gridx = 1;
        mainPanel.add(nameField, gbc);

        // 전체수량 입력
        gbc.gridy = 2;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("전체수량"), gbc);

        SpinnerNumberModel totalModel = new SpinnerNumberModel(totalQuantity, 1, 100, 1);
        totalQuantitySpinner = new JSpinner(totalModel);
        gbc.gridx = 1;
        mainPanel.add(totalQuantitySpinner, gbc);

        // 대여가능수량 입력
        gbc.gridy = 3;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("대여가능수량"), gbc);

        SpinnerNumberModel availableModel = new SpinnerNumberModel(
                availableQuantity, 0, Math.max(totalQuantity, availableQuantity), 1);
        availableQuantitySpinner = new JSpinner(availableModel);
        gbc.gridx = 1;
        mainPanel.add(availableQuantitySpinner, gbc);

        // 전체수량 변경 시 대여가능수량의 최대값도 변경
        totalQuantitySpinner.addChangeListener(e -> {
            int totalValue = (int) totalQuantitySpinner.getValue();
            int availableValue = (int) availableQuantitySpinner.getValue();

            // 대여가능수량의 모델 업데이트
            SpinnerNumberModel availableModelUpdated = new SpinnerNumberModel(
                    Math.min(availableValue, totalValue), // 현재값과 전체수량 중 작은 값
                    0, // 최소값
                    totalValue, // 최대값은 전체수량
                    1 // 증가값
            );
            availableQuantitySpinner.setModel(availableModelUpdated);
        });

        // 버튼 패널 (확인/취소)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 확인 버튼
        JButton okButton = new JButton("확인");
        buttonPanel.add(okButton);

        // 확인 버튼을 기본 버튼으로 설정
        getRootPane().setDefaultButton(okButton);

        // 확인 버튼 클릭 시 입력값 검증 및 DB 처리
        okButton.addActionListener(e -> {
            if (validateInputs()) {
                boolean success;
                if (isEditMode) {
                    // 수정 모드 : DB 업데이트
                    success = updateItemInDatabase();
                } else {
                    // 신규 등록 모드 : DB에 새 물품 추가
                    success = addItemToDatabase();
                }

                if (success) {
                    isConfirmed = true;
                    dispose();
                }
            }
        });

        // 취소 버튼
        JButton cancelButton = new JButton("취소");
        buttonPanel.add(cancelButton);

        // 취소 버튼 클릭 시 다이얼로그 닫기
        cancelButton.addActionListener(e -> {
            dispose();
        });

        // 메인 패널과 버튼 패널 배치
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // 입력값 검증 (빈칸, 수량 범위 등)
    private boolean validateInputs() {
        // 분류 확인
        if (categoryField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "분류를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            categoryField.requestFocus();
            return false;
        }

        // 이름 확인
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }

        // 수량 확인
        int totalQuantity = (int) totalQuantitySpinner.getValue();
        int availableQuantity = (int) availableQuantitySpinner.getValue();

        if (availableQuantity > totalQuantity) {
            JOptionPane.showMessageDialog(this, "대여가능수량은 전체수량보다 클 수 없습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            availableQuantitySpinner.requestFocus();
            return false;
        }

        return true;
    }

    // DB에 물품 정보 수정
    private boolean updateItemInDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            // 아이템 수정
            String updateSql = "UPDATE DB2025_ITEMS SET item_name = ?, quantity = ?, available_quantity = ?, category = ? WHERE item_id = ?";
            stmt = conn.prepareStatement(updateSql);

            stmt.setString(1, nameField.getText().trim());
            stmt.setInt(2, (int) totalQuantitySpinner.getValue());
            stmt.setInt(3, (int) availableQuantitySpinner.getValue());
            stmt.setString(4, categoryField.getText().trim());
            stmt.setInt(5, itemId);

            int result = stmt.executeUpdate();

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "물품이 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "물품 수정에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, null);
        }
    }

    // DB에 새 물품 추가
    private boolean addItemToDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            // 먼저 가장 큰 item_id 값을 조회하여 새 ID 생성
            String maxIdSql = "SELECT MAX(item_id) FROM DB2025_ITEMS";
            stmt = conn.prepareStatement(maxIdSql);
            rs = stmt.executeQuery();

            int newItemId = 2001; // 기본 시작 ID
            if (rs.next() && rs.getObject(1) != null) {
                newItemId = rs.getInt(1) + 1;
            }

            // 새 아이템 추가
            String insertSql = "INSERT INTO DB2025_ITEMS (item_id, item_name, quantity, available_quantity, category) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(insertSql);
            stmt.setInt(1, newItemId);
            stmt.setString(2, nameField.getText().trim());
            stmt.setInt(3, (int) totalQuantitySpinner.getValue());
            stmt.setInt(4, (int) availableQuantitySpinner.getValue());
            stmt.setString(5, categoryField.getText().trim());

            int result = stmt.executeUpdate();

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "물품이 성공적으로 추가되었습니다.", "추가 완료", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "물품 추가에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "데이터베이스 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            DatabaseManager.getInstance().closeResources(conn, stmt, rs);
        }
    }

    // 다이얼로그가 확인되었는지 여부 반환
    public boolean isConfirmed() {
        return isConfirmed;
    }
}