package DB2025Team04.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserPasswordDialog extends JDialog {
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private boolean isConfirmed = false;
    
    public UserPasswordDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        initComponents();
        setupDialog();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // 입력 필드 패널
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 현재 비밀번호
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("현재 비밀번호:"), gbc);
        
        gbc.gridx = 1;
        currentPasswordField = new JPasswordField(20);
        inputPanel.add(currentPasswordField, gbc);
        
        // 새 비밀번호
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("새 비밀번호:"), gbc);
        
        gbc.gridx = 1;
        newPasswordField = new JPasswordField(20);
        inputPanel.add(newPasswordField, gbc);
        
        // 비밀번호 확인
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("비밀번호 확인:"), gbc);
        
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        inputPanel.add(confirmPasswordField, gbc);
        
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
        setSize(400, 200);
        setLocationRelativeTo(getOwner());
        setResizable(false);
    }
    
    private boolean validateInput() {
        // 현재 비밀번호 검증
        if (currentPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "현재 비밀번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 새 비밀번호 검증
        if (newPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "새 비밀번호를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 비밀번호 확인 검증
        if (confirmPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "비밀번호 확인을 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // 새 비밀번호와 비밀번호 확인이 일치하는지 검증
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return isConfirmed;
    }
    
    public String getCurrentPassword() {
        return new String(currentPasswordField.getPassword());
    }
    
    public String getNewPassword() {
        return new String(newPasswordField.getPassword());
    }
}