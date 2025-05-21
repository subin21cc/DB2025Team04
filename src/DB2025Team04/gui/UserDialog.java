package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserDialog extends JDialog {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JTextField nameField;
    private JTextField departmentField;
    private JTextField phoneField;
    private JComboBox<String> statusCombo;
    private JCheckBox adminCheckBox;
    
    private boolean isEditing;
    private boolean isConfirmed = false;
    
    // 사용자 추가 시 사용하는 생성자
    public UserDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.isEditing = false;
        initComponents();
        setupDialog();
    }
    
    // 사용자 수정 시 사용하는 생성자
    public UserDialog(Window owner, String title, int userId, String name, String department, 
                      String phone, String status, boolean isAdmin) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.isEditing = true;
        initComponents();
        
        // 기존 데이터 채우기
        userIdField.setText(String.valueOf(userId));
        userIdField.setEditable(false); // ID는 수정 불가
        passwordField.setText(""); // 비밀번호는 빈칸으로 시작 (변경하지 않으려면 비워둠)
        nameField.setText(name);
        departmentField.setText(department);
        phoneField.setText(phone);
        statusCombo.setSelectedItem(status);
        adminCheckBox.setSelected(isAdmin);
        
        setupDialog();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // 입력 필드 패널
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 사용자 ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("사용자 ID:"), gbc);
        
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        inputPanel.add(userIdField, gbc);
        
        // 비밀번호
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("비밀번호:"), gbc);
        
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        inputPanel.add(passwordField, gbc);
        
        if (isEditing) {
            inputPanel.add(new JLabel("(변경 시에만 입력)"), gbc);
        }
        
        // 이름
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("이름:"), gbc);
        
        gbc.gridx = 1;
        nameField = new JTextField(20);
        inputPanel.add(nameField, gbc);
        
        // 부서
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("부서:"), gbc);
        
        gbc.gridx = 1;
        departmentField = new JTextField(20);
        inputPanel.add(departmentField, gbc);
        
        // 전화번호
        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("전화번호:"), gbc);
        
        gbc.gridx = 1;
        phoneField = new JTextField(20);
        inputPanel.add(phoneField, gbc);
        
        // 상태
        gbc.gridx = 0;
        gbc.gridy = 5;
        inputPanel.add(new JLabel("상태:"), gbc);
        
        gbc.gridx = 1;
        statusCombo = new JComboBox<>(new String[]{"대여가능", "대여불가"});
        inputPanel.add(statusCombo, gbc);
        
        // 관리자 권한
        gbc.gridx = 0;
        gbc.gridy = 6;
        inputPanel.add(new JLabel("관리자 권한:"), gbc);
        
        gbc.gridx = 1;
        adminCheckBox = new JCheckBox("관리자 권한 부여");
        inputPanel.add(adminCheckBox, gbc);
        
        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton("확인");
        JButton cancelButton = new JButton("취소");
        
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateInput()) {
                    isConfirmed = true;
                    dispose();
                }
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isConfirmed = false;
                dispose();
            }
        });
        
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        
        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupDialog() {
        setSize(400, 350);
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }
    
    private boolean validateInput() {
        // 사용자 ID 검증
        if (userIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "사용자 ID를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        try {
            Integer.parseInt(userIdField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "사용자 ID는 숫자만 입력 가능합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 비밀번호 검증 (추가 시에는 필수, 수정 시에는 선택)
        if (!isEditing && passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "비밀번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 이름 검증
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 부서 검증
        if (departmentField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "부서를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 전화번호 검증
        if (phoneField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "전화번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        String phonePattern = "^\\d{2,3}-\\d{3,4}-\\d{4}$"; // 전화번호 형식 검증
        if (!phoneField.getText().matches(phonePattern)) {
            JOptionPane.showMessageDialog(this, "올바른 전화번호 형식이 아닙니다.\n예: 010-1234-5678", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    public int getUserId() {
        return Integer.parseInt(userIdField.getText().trim());
    }
    
    public String getPassword() {
        return new String(passwordField.getPassword());
    }
    
    public String getName() {
        return nameField.getText().trim();
    }
    
    public String getDepartment() {
        return departmentField.getText().trim();
    }
    
    public String getPhone() {
        return phoneField.getText().trim();
    }
    
    public String getStatus() {
        return (String) statusCombo.getSelectedItem();
    }
    
    public boolean isAdmin() {
        return adminCheckBox.isSelected();
    }
}