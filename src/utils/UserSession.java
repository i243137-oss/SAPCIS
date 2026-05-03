package utils;

import model.User;

/**
 * =============================================================================
 *  UserSession
 * -----------------------------------------------------------------------------
 *  Pure Java Singleton that holds the currently-authenticated user for the
 *  lifetime of the JavaFX application. After a successful login the
 *  AuthController populates this singleton; every downstream screen
 *  (StudentDashboard, TeacherDashboard, AdminNavHub, ...) reads the current
 *  user from here instead of re-prompting for an ID.
 *
 *  Thread-safety: guarded by a synchronized lazy-init block. Usage from the
 *  JavaFX Application Thread is the norm; the synchronization protects
 *  against background JDBC threads that may set/clear the user as well.
 * =============================================================================
 */
public final class UserSession {

    private static volatile UserSession instance;

    private User currentUser;

    // Private — use getInstance().
    private UserSession() { }

    /**
     * Double-checked locking singleton accessor.
     */
    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------
    //  Current user
    // ------------------------------------------------------------------

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Returns true when a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Clears the session. Called by logout flows.
     */
    public void clear() {
        this.currentUser = null;
    }
}
