package utils;

import models.NhanVien;

/**
 * Simple session management for the application.
 * Stores the current logged-in user information.
 */
public class Session {
    private static NhanVien currentUser;
    private static boolean isManager;
    
    private Session() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Set the current logged-in user
     * @param user The logged-in user
     */
    public static void setCurrentUser(NhanVien user) {
        currentUser = user;
        // Check if the user is a manager (position ID 1)
        isManager = user != null && user.getMaVT() == 1;
    }
    
    /**
     * Get the current logged-in user
     * @return The current user, or null if no user is logged in
     */
    public static NhanVien getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if the current user is a manager
     * @return true if the user is a manager, false otherwise
     */
    public static boolean isManager() {
        return isManager;
    }
    
    /**
     * Clear the current session (logout)
     */
    public static void clear() {
        currentUser = null;
        isManager = false;
    }
}
