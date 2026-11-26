import java.util.*;

public class StudentProfile {
    private static String currentUsername = "";
    /**
     * Selects the profile when saving and viewing results.
     */
    public static boolean select(String username) {
        if (username == null) {
            return false;
        }
        username = username.trim();
        if (!username.matches("[A-Za-z0-9_]{3,20}")) {
            return false;
        }
        currentUsername = username.toLowerCase(Locale.ROOT);
        return true;
    }
    public static String getUsername() {
        return currentUsername;
    }
    /**
     * Checks whether a profile has been selected.
     */
    public static boolean isSelected() {
        return !currentUsername.isEmpty();
    }
    /**
     * Clears the selected profile so another can be chosen.
     */
    public static void clear() {
        currentUsername = "";
    }
}