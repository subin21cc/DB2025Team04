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
    private AdminOutPanel adminOutPanel;
    private AdminRentPanel adminRentPanel;
    private AdminPanel adminPanel;

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
            adminPanel = new AdminPanel();

            tabbedPane.addTab("대여 물품 관리", new ImageIcon(), itemListPanel, "대여 물품을 관리합니다");
            tabbedPane.addTab("출고예정", new ImageIcon(), adminOutPanel, "출고예정인 물품을 보여줍니다");
            tabbedPane.addTab("대여 현황", new ImageIcon(), adminRentPanel, "대여 물품의 반납을 처리합니다.");
            tabbedPane.addTab("관리자 권한", new ImageIcon(), adminPanel, "관리자 권한을 부여하거나 삭제합니다.");
            
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

            tabbedPane.addTab("대여 물품 목록", new ImageIcon(), itemListPanel, "대여 물품을 보여줍니다");
            tabbedPane.addTab("내 대여 현황", new ImageIcon(), myRentStatusPanel, "내가 대여한 물품 및 현황을 보여줍니다");
            tabbedPane.addTab("내 연체 현황", new ImageIcon(), new ItemListPanel(), "내가 연체한 물품 및 현황을 보여줍니다");
            
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

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu databaseMenu = new JMenu("Database");
        JMenuItem backupItem = new JMenuItem("Backup Database");
        JMenuItem restoreItem = new JMenuItem("Restore Database");
        databaseMenu.add(backupItem);
        databaseMenu.add(restoreItem);
        menuBar.add(fileMenu);
        menuBar.add(databaseMenu);

        return menuBar;
    }
}