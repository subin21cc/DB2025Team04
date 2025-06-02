package DB2025Team04.util;

// 세션 관리 클래스
public class SessionManager {
    private static SessionManager instance; // 싱글톤 인스턴스
    private int userId; // 사용자 ID
    private boolean isAdmin; // 관리자 여부
    private String userName; // 사용자 이름

    private SessionManager() {
        // private 생성자로 외부에서 인스턴스 생성 방지
    }

    // 싱글톤 인스턴스 반환 메소드
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    // 현재 로그인한 사용자 ID를 반환
    public int getUserId() {
        return userId;
    }
    // 현재 사용자가 관리자 권한을 가지고 있는지 여부를 반환
    public boolean isAdmin() {
        return isAdmin;
    }
    // 로그인 시 사용자 ID와 관리자 여부 설정
    public void setUserId(int userId, boolean isAdmin) {
        this.userId = userId;
        this.isAdmin = isAdmin;
    }
    // 로그아웃 시 세션 정보 초기화
    public void clearSession() {
        this.userId = 0;
        this.isAdmin = false;
    }
    // 현재 로그인한 사용자의 이름을 반환
    public String getUserName() {
        return userName;
    }
    // 로그인 시 사용자 이름 설정
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
