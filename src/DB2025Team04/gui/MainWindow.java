package DB2025Team04.gui;

import DB2025Team04.util.SessionManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private ItemListPanel itemListPanel;
    private MyRentStatusPanel myRentStatusPanel;
    private MyReservationPanel myReservationPanel;
    private MyOverduePanel myOverduePanel;
    private AdminOutPanel adminOutPanel;
    private AdminRentPanel adminRentPanel;
    private AdminUserPanel adminUserPanel;

    public MainWindow() {
        setTitle("이화 물품 대여 서비스");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JLabel userInfoLabel = new JLabel(
                "사용자: " + SessionManager.getInstance().getUserId() + " (" +
                        SessionManager.getInstance().getUserName() + " :" +
                        (SessionManager.getInstance().isAdmin() ? "관리자" : "일반 사용자") + ")");
        JButton logoutButton = new JButton("로그아웃");
        logoutButton.addActionListener(e -> {
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginWindow().setVisible(true);
        });

        topPanel.add(userInfoLabel);
        topPanel.add(logoutButton);

        add(topPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();

        if (SessionManager.getInstance().isAdmin()) {
            itemListPanel = new ItemListPanel();
            adminOutPanel = new AdminOutPanel();
            adminRentPanel = new AdminRentPanel();
            adminUserPanel = new AdminUserPanel();

            tabbedPane.addTab("대여 물품 관리", new ImageIcon(), itemListPanel, "대여 물품을 관리합니다");
            tabbedPane.addTab("출고예정", new ImageIcon(), adminOutPanel, "출고예정인 물품을 보여줍니다");
            tabbedPane.addTab("대여 현황", new ImageIcon(), adminRentPanel, "대여 물품의 반납을 처리합니다.");
            tabbedPane.addTab("사용자 관리", new ImageIcon(), adminUserPanel, "사용자 정보를 관리합니다.");

            // 탭 변경 이벤트 리스너 추가
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
                    }
                }
            });
        } else {
            itemListPanel = new ItemListPanel();
            myRentStatusPanel = new MyRentStatusPanel();
            myReservationPanel = new MyReservationPanel();
            myOverduePanel = new MyOverduePanel();

            tabbedPane.addTab("대여 물품 목록", new ImageIcon(), itemListPanel, "대여 물품을 보여줍니다");
            tabbedPane.addTab("내 대여 현황", new ImageIcon(), myRentStatusPanel, "내가 대여한 물품 및 현황을 보여줍니다");
            tabbedPane.addTab("내 예약 현황", new ImageIcon(), myReservationPanel, "나의 예약 현황을 보여줍니다");
            tabbedPane.addTab("내 연체 현황", new ImageIcon(), myOverduePanel, "내가 연체한 물품 및 현황을 보여줍니다");

            // 일반 사용자 모드에서도 탭 변경 시 데이터를 갱신하는 리스너 추가
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
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        JLabel statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel);

        add(statusPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu myInfo = new JMenu("내정보");
        JMenuItem editPwItem = new JMenuItem("비밀번호 변경");
        editPwItem.addActionListener(e -> System.exit(0));
        myInfo.add(editPwItem);

        JMenu help = new JMenu("도움말");
        JMenuItem rentHelp = new JMenuItem("대여 안내");
        JMenuItem reserveHelp = new JMenuItem("예약 안내");

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

        JMenu refresh = new JMenu("새로고침");

        menuBar.add(myInfo);
        menuBar.add(help);
        menuBar.add(refresh);

        return menuBar;
    }
}