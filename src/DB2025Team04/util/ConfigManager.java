package DB2025Team04.util;

public class ConfigManager {
    private static ConfigManager instance;
    final public static int MAX_RENT_DAYS = 7; // 최대 대여일수

    private ConfigManager() {}

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
}
