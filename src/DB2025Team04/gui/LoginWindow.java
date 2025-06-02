package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
로그인 창 클래스
사용자가 아이디와 비밀번호를 입력하여 로그인할 수 있는 GUI를 제공
일반 사용자와 관리자를 선택할 수 있는 라디오 버튼 포함
*/
public class LoginWindow extends JFrame {
    private JTextField idField; // 아이디 입력 필드
    private JPasswordField passwordField; // 비밀번호 입력 필드
    private JRadioButton normalUserRadio; // 일반 사용자 라디오 버튼
    private JRadioButton adminRadio; // 관리자 라디오 버튼

    /*
    생성자: 로그인 창을 초기화하고 UI를 설정합니다.
    */
    public LoginWindow() {
        // 로그인 시 자동 처리 작업 실행 (예: 만료 처리 등)
        DatabaseManager.getInstance().processAutoTask();

        setTitle("로그인");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 250);
        setLocationRelativeTo(null);
        
        // 메인 패널
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 라디오 버튼으로 사용자 타입 선택
        ButtonGroup userTypeGroup = new ButtonGroup();
        normalUserRadio = new JRadioButton("일반 사용자", true); // true로 설정하여 기본 선택
        adminRadio = new JRadioButton("관리자");

        userTypeGroup.add(normalUserRadio);
        userTypeGroup.add(adminRadio);

        // 라디오 버튼을 담을 패널
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        radioPanel.add(normalUserRadio);
        radioPanel.add(adminRadio);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(radioPanel, gbc);
        
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
        
        // 로그인 버튼
        JButton loginButton = new JButton("로그인");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(loginButton, gbc);

        // 엔터키로 로그인 가능하도록 기본 버튼 지정
        getRootPane().setDefaultButton(loginButton);

        // 로그인 버튼 이벤트
        loginButton.addActionListener(e -> {
            int userTypeIndex = adminRadio.isSelected() ? 1 : 0; // 관리자면 1, 일반 사용자면 0
            String id = idField.getText();
            String password = new String(passwordField.getPassword());

            // 로그인 검증
            if (validateLogin(id, password, userTypeIndex)) {
                int userId;
                try {
                    userId = Integer.parseInt(id);
                } catch (NumberFormatException ex) {
                    userId = 0;
                }

                // 세션에 사용자 정보 저장
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

        // 도움말 버튼
        JButton helpButton = new JButton("도움말");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.SOUTHEAST; // 우측 하단 정렬
        mainPanel.add(helpButton, gbc);
        // 도움말 버튼 클릭 시 안내 메시지 표시
        helpButton.addActionListener(e -> {
            JTextArea textArea = new JTextArea(10, 30);
            textArea.setText("\n1. 아이디와 비밀번호를 입력하고 로그인 버튼을 클릭하세요.\n\n" +
                    "2. 회원가입이 필요한 경우 관리자에게 신청하세요.\n\n" +
                    "3. 초기 비밀번호는 아이디(학번)와 동일합니다.\n\n" +
                    "4. 비밀번호는 '내정보' -> '비밀번호 변경'에서 변경할 수 있습니다.");
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "도움말", JOptionPane.INFORMATION_MESSAGE);
        });


        add(mainPanel);
    }

    /*
    로그인 검증 메서드
    - DB에서 아이디/비밀번호 일치 여부 확인
    - 관리자 로그인 시 관리자 테이블 추가 확인
    */
    private boolean validateLogin(String id, String password, int userTypeIndex) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            // 사용자 인증 쿼리 (비밀번호는 SHA-256 해시)
            String sql = "SELECT * FROM DB2025_USER WHERE user_id = ? AND user_pw = sha2(?, 256)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            if (rs.next()) {
                // 사용자 인증 성공
                String userName = rs.getString("user_name");
                SessionManager.getInstance().setUserName(userName);
                if (userTypeIndex == 1) {
                    // 관리자 권한 확인
                    sql = "SELECT * FROM DB2025_ADMIN WHERE user_id = ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, id);
                    rs = stmt.executeQuery();
                    if (!rs.next()) {
                        // 관리자 권한 없음
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
            // DB 연결 해제
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