package DB2025Team04;

import DB2025Team04.gui.MainWindow;
import DB2025Team04.gui.LoginWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new LoginWindow().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}