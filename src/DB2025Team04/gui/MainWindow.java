package DB2025Team04.gui;

import javax.swing.*;

public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;

    public MainWindow() {
        setTitle("이화 물품 대여 서비스");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();

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
