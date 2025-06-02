package DB2025Team04;

import DB2025Team04.gui.LoginWindow;
import javax.swing.*;

// Main 클래스: 프로그램 시작점
public class Main {
    public static void main(String[] args) {
        // Swing UI 초기화를 위한 스레드 안전한 실행
        SwingUtilities.invokeLater(() -> {
            try {
                // 시스템 기본 Look and Feel 적용 (윈도우 스타일 등)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // 로그인 창 생성 및 표시
                new LoginWindow().setVisible(true);
            } catch (Exception e) {
                // Look and Feel 설정 실패 등 예외 발생 시 스택 트레이스 출력
                e.printStackTrace();
            }
        });
    }
}