package DB2025Team04.util;

public class SessionManager {
    private static SessionManager instance;
    private int userId;
    private boolean isAdmin;

    private SessionManager() {
        // private 생성자로 외부에서 인스턴스 생성 방지
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setUserId(int userId, boolean isAdmin) {
        this.userId = userId;
        this.isAdmin = isAdmin;
    }

    public void clearSession() {
        this.userId = 0;
        this.isAdmin = false;
    }
}
