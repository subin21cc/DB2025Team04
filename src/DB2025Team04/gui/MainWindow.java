package DB2025Team04.gui;

import DB2025Team04.db.DatabaseManager;
import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
메인 윈도우 클래스
 - 로그인 후 사용자/관리자에 따라 탭을 다르게 구성
 - 상단에 사용자 정보 및 로그아웃 버튼 표시
 - 메뉴바에서 비밀번호 변경, 도움말, 새로고침 등 제공
 */
public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane; // 탭 패널
    // 각 패널(탭) 선언
    private ItemListPanel itemListPanel;
    private MyRentStatusPanel myRentStatusPanel;
    private MyReservationPanel myReservationPanel;
    private MyOverduePanel myOverduePanel;
    private AdminOutPanel adminOutPanel;
    private AdminRentPanel adminRentPanel;
    private AdminUserPanel adminUserPanel;
    private AdminReservationPanel adminReservationPanel;

    /*
    생성자: 메인 윈도우 UI 초기화
    */
    public MainWindow() {
        setTitle("이화 물품 대여 서비스");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initComponents();
    }

    /*
    UI 컴포넌트 및 탭, 메뉴바 구성
    */
    private void initComponents() {
        // 상단 사용자 정보 및 로그아웃 버튼
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JLabel userInfoLabel = new JLabel(
                "사용자: " + SessionManager.getInstance().getUserId() + " (" +
                        SessionManager.getInstance().getUserName() + " :" +
                        (SessionManager.getInstance().isAdmin() ? "관리자" : "일반 사용자") + ")");
        JButton logoutButton = new JButton("로그아웃");
        logoutButton.addActionListener(e -> {
            // 로그아웃 시 세션 초기화 및 로그인 창으로 이동
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginWindow().setVisible(true);
        });

        topPanel.add(userInfoLabel);
        topPanel.add(logoutButton);

        add(topPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();

        // 관리자/일반 사용자에 따라 탭 구성 분기
        if (SessionManager.getInstance().isAdmin()) {
            // 관리자용 탭 패널 생성 및 추가
            itemListPanel = new ItemListPanel();
            adminOutPanel = new AdminOutPanel();
            adminRentPanel = new AdminRentPanel();
            adminUserPanel = new AdminUserPanel();
            adminReservationPanel = new AdminReservationPanel();

            tabbedPane.addTab("대여 물품 관리", new ImageIcon(), itemListPanel, "대여 물품을 관리합니다");
            tabbedPane.addTab("출고예정", new ImageIcon(), adminOutPanel, "출고예정인 물품을 보여줍니다");
            tabbedPane.addTab("대여 현황", new ImageIcon(), adminRentPanel, "대여 물품의 반납을 처리합니다.");
            tabbedPane.addTab("예약 현황", new ImageIcon(), adminReservationPanel, "예약 내역을 관리합니다.");
            tabbedPane.addTab("사용자 관리", new ImageIcon(), adminUserPanel, "사용자 정보를 관리합니다.");

            // 탭 변경 시 각 패널의 데이터 갱신
            tabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int selectedIndex = tabbedPane.getSelectedIndex();
                    // 선택된 탭에 따라 적절한 panel의 loadItemList() 메소드 호출
                    switch (selectedIndex) {
                        case 0: // 대여 물품 관리 탭
                            itemListPanel.loadItemList();
                            break;
                        case 1: // 출고예정 탭
                            adminOutPanel.loadItemList();
                            break;
                        case 2: // 대여 현황 탭
                            adminRentPanel.loadItemList();
                            break;
                        case 3: // 예약 현황 탭
                            adminReservationPanel.loadItemList();
                            break;
                    }
                }
            });
        } else {
            // 일반 사용자용 탭 패널 생성 및 추가
            itemListPanel = new ItemListPanel();
            myRentStatusPanel = new MyRentStatusPanel();
            myReservationPanel = new MyReservationPanel();
            myOverduePanel = new MyOverduePanel();

            tabbedPane.addTab("대여 물품 목록", new ImageIcon(), itemListPanel, "대여 물품을 보여줍니다");
            tabbedPane.addTab("내 대여 현황", new ImageIcon(), myRentStatusPanel, "내가 대여한 물품 및 현황을 보여줍니다");
            tabbedPane.addTab("내 예약 현황", new ImageIcon(), myReservationPanel, "나의 예약 현황을 보여줍니다");
            tabbedPane.addTab("내 연체 현황", new ImageIcon(), myOverduePanel, "내가 연체한 물품 및 현황을 보여줍니다");

            // 탭 변경 시 각 패널의 데이터 갱신
            tabbedPane.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int selectedIndex = tabbedPane.getSelectedIndex();
                    switch (selectedIndex) {
                        case 0: // 대여 물품 목록 탭
                            itemListPanel.loadItemList();
                            break;
                        case 1: // 내 대여 현황 탭
                            myRentStatusPanel.loadItemList();
                            break;
                        case 2: // 내 예약 현황
                            myReservationPanel.loadReservationList();
                            break;

                    }
                }
            });
        }
        add(tabbedPane, BorderLayout.CENTER);

        // 메뉴바 생성 및 설정
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
    }

    /*
    현재 선택된 탭의 데이터를 새로고침하는 메서드
    */
    private void refreshCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        if (SessionManager.getInstance().isAdmin()) { //관리자 화면 새로고침
            switch (selectedIndex) {
                case 0: // 대여 물품 관리
                    itemListPanel.loadItemList();
                    break;
                case 1: // 출고 예정
                    adminOutPanel.loadItemList();
                    break;
                case 2: // 대여 현황
                    adminRentPanel.loadItemList();
                    break;
                case 3: // 사용자 관리
                    adminUserPanel.loadUserList();
                    break;
                default:
                    break;
            }
        } else { //사용자 화면 새로고침
            switch (selectedIndex) {
                case 0: // 대여 물품 목록
                    itemListPanel.loadItemList();
                    break;
                case 1: // 내 대여 현황
                    myRentStatusPanel.loadItemList();
                    break;
                case 2: // 내 예약 현황
                    myReservationPanel.loadReservationList();
                    break;
                case 3: // 내 연체 현황
                    myOverduePanel.loadOverdueList();
                    break;
                default:
                    break;
            }
        }
    }

    /*
    메뉴바 생성
     - 내정보(비밀번호 변경), 도움말(대여/예약 안내, 대여 로그), 새로고침 메뉴 포함
    */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 내정보 메뉴
        JMenu myInfo = new JMenu("내정보");
        JMenuItem editPwItem = new JMenuItem("비밀번호 변경");
        editPwItem.addActionListener(e -> {
            // 비밀번호 변경 대화상자 생성 및 표시
            UserPasswordDialog passwordDialog = new UserPasswordDialog(this, "비밀번호 변경");
            passwordDialog.setVisible(true);

            // 대화상자가 확인 버튼으로 닫힌 경우 처리
            if (passwordDialog.isConfirmed()) {
                // 비밀번호 변경 처리
                changePassword(
                    SessionManager.getInstance().getUserId(),
                    passwordDialog.getCurrentPassword(),
                    passwordDialog.getNewPassword()
                );
            }
        });
        myInfo.add(editPwItem);

        // 도움말 메뉴
        JMenu help = new JMenu("도움말");
        JMenuItem rentHelp = new JMenuItem("대여 안내");
        JMenuItem reserveHelp = new JMenuItem("예약 안내");

        // 대여 안내 클릭 시 안내 메시지 표시
        rentHelp.addActionListener(e -> {
            JTextArea textArea = new JTextArea(15, 35);
            textArea.setText("대여 방법:\n" +
                    "1. 대여할 물품을 선택하세요.\n" +
                    "2. '대여신청' 버튼을 클릭하여 대여를 신청하세요.\n" +
                    "3. 관리자에게 대여신청한 물품을 수령하세요.\n\n" +
                    "주의 사항: \n" +
                    "1. 대여는 대여가능수량이 1개 이상인 물품만 가능합니다.\n" +
                    " (대여가능수량이 0인 물품은 예약해 주세요.)\n" +
                    "2. 대여 기간은 최대 7일입니다.\n" +
                    "3. 대여 물품은 반드시 반납 기한 내에 반납해야 합니다.\n" +
                    "4. 연체 시 연체 기간만큼 대여가 제한됩니다.\n" +
                    "5. 대여신청 후 다음 날 자정까지 물품을 수령하지 않으면\n    대여신청이 자동으로 취소됩니다.\n" +
                    "6. 동일 물품은 중복 대여가 불가능합니다.");
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "대여 안내",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        // 예약 안내 클릭 시 안내 메시지 표시
        reserveHelp.addActionListener(e -> {
            JTextArea textArea = new JTextArea(15, 35);
            textArea.setText("예약 방법:\n" +
                    "1. 예약할 물품을 선택하세요.\n" +
                    "2. '예약' 버튼을 클릭하여 예약을 진행하세요.\n" +
                    "3. 예약 상태는 '내 예약 현황'에서 확인할 수 있습니다.\n\n" +
                    "주의 사항:\n" +
                    "1. 예약은 대여가능수량이 0개인 물품에 대해서만 가능합니다.\n" +
                    "2. 예약 후 '대여신청'으로 상태가 변경되면 관리자에게 물품을 수령하세요.\n" +
                    "3. '대여신청'으로 상태 변경 후 다음 날 자정까지 물품을 수령하지 않으면\n" +
                            "    예약이 자동으로 취소됩니다.\n"
                    );
            textArea.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(textArea);

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "예약 안내",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        help.add(rentHelp);
        help.add(reserveHelp);

        // 대여 로그 보기 메뉴
        JMenuItem viewLog = new JMenuItem("대여 로그 보기");
        viewLog.addActionListener(e -> {
            // 대여 로그를 보여주는 대화상자 생성 및 표시
            RentLogDialog rentLogDialog = new RentLogDialog(this, "대여 로그");
            rentLogDialog.setVisible(true);
        });
        help.add(viewLog);

        // 새로고침 메뉴
        JMenu refreshItem = new JMenu("새로고침");
        refreshItem.addActionListener(e -> refreshCurrentTab());

        menuBar.add(myInfo);
        menuBar.add(help);
        menuBar.add(refreshItem);

        return menuBar;
    }

    // 사용자 비밀번호를 변경하는 메서드
    // userId: 사용자 ID
    // currentPassword: 현재 비밀번호
    // newPassword: 새 비밀번호
    private void changePassword(int userId, String currentPassword, String newPassword) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();

            // 1. 현재 비밀번호가 맞는지 확인
            String checkSql = "SELECT * FROM DB2025_USER WHERE user_id = ? AND user_pw = SHA2(?, 256)";
            stmt = conn.prepareStatement(checkSql);
            stmt.setInt(1, userId);
            stmt.setString(2, currentPassword);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // 현재 비밀번호가 맞으면 새 비밀번호로 업데이트
                stmt.close(); // 이전 문장 닫기

                String updateSql = "UPDATE DB2025_USER SET user_pw = SHA2(?, 256) WHERE user_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setString(1, newPassword);
                stmt.setInt(2, userId);

                int result = stmt.executeUpdate();

                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 변경되었습니다.",
                                                 "비밀번호 변경 완료", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "비밀번호 변경에 실패했습니다.",
                                                 "오류", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // 현재 비밀번호가 틀린 경우
                JOptionPane.showMessageDialog(this, "현재 비밀번호가 일치하지 않습니다.",
                                             "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "비밀번호 변경 중 오류가 발생했습니다: " + ex.getMessage(),
                                         "오류", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            // DB 연결 해제
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}