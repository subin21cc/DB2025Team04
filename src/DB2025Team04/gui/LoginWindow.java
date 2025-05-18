package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginWindow extends JFrame {
    private JTextField idField;
    private JPasswordField passwordField;
    private JComboBox<String> userTypeCombo;
    
    public LoginWindow() {
        setTitle("로그인");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        
        // 메인 패널
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 사용자 타입 선택
        userTypeCombo = new JComboBox<>(new String[]{"일반 사용자", "관리자"});
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(userTypeCombo, gbc);
        
        // ID 라벨과 필드
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("아이디:"), gbc);
        
        idField = new JTextField(15);
        gbc.gridx = 1;
        mainPanel.add(idField, gbc);
        
        // 비밀번호 라벨과 필드
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("비밀번호:"), gbc);
        
        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        // test를 위해서 id, password를 미리 설정
        idField.setText("2025001");
        passwordField.setText("pw1234");
        
        // 로그인 버튼
        JButton loginButton = new JButton("로그인");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(loginButton, gbc);

        // 로그인 버튼을 기본 버튼으로 설정
        getRootPane().setDefaultButton(loginButton);

        // 로그인 버튼 이벤트
        loginButton.addActionListener(e -> {
            int userTypeIndex = userTypeCombo.getSelectedIndex(); // 0: 일반 사용자, 1: 관리자
            String id = idField.getText();
            String password = new String(passwordField.getPassword());
            
            // TODO: 여기에 로그인 검증 로직 추가
            if (validateLogin(id, password, userTypeIndex)) {
                int userId;
                try {
                    userId = Integer.parseInt(id);
                } catch (NumberFormatException ex) {
                    userId = 0;
                }

                SessionManager.getInstance().setUserId(userId, userTypeIndex == 1);
                dispose(); // 로그인 창 닫기
                new MainWindow().setVisible(true); // 메인 창 열기
            } else {
                JOptionPane.showMessageDialog(this,
                    "로그인 실패: 아이디나 비밀번호를 확인해주세요.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        add(mainPanel);
    }
    
    private boolean validateLogin(String id, String password, int userTypeIndex) {
        // TODO: 실제 데이터베이스 검증 로직 구현
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT * FROM DB2025_USER WHERE user_id = ? AND user_pw = sha2(?, 256)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            if (rs.next()) {
                // 패스워드 확인 성공
                if (userTypeIndex == 1) {
                    // 관리자 로그인 확인
                    sql = "SELECT * FROM DB2025_ADMIN WHERE user_id = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, id);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        // 관리자 로그인 성공
                    } else {
                        // 관리자 로그인 실패
                        JOptionPane.showMessageDialog(this,
                            "관리자 권한이 필요합니다.",
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                return true;
            } else {
                // 로그인 실패
                return false;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "데이터베이스 연결 오류: " + e.getMessage(),
                "오류",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        // 임시로 항상 true 반환
//        return true;
    }
}