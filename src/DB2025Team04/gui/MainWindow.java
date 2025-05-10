package DB2025Team04.gui;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private ItemListPanel itemListPanel;

    public MainWindow() {
        setTitle("이화 물품 대여 서비스");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();

        itemListPanel = new ItemListPanel();

        tabbedPane.addTab("대여 물품 목록", new ImageIcon(), itemListPanel, "대여 물품을 보여줍니다");
        tabbedPane.addTab("내 대여 현황", new ImageIcon(), new ItemListPanel(), "내가 대여한 물품 및 현황을 보여줍니다");
        tabbedPane.addTab("내 연체 현황", new ImageIcon(), new ItemListPanel(), "내가 연체한 물품 및 현황을 보여줍니다");

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
