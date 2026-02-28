package model;

import java.io.Serializable;

/**
 * Represents a system configurator (back-end user).
 *
 * Invariant: username != null && !username.isBlank()
 * Invariant: password != null && !password.isBlank()
 */
public class Configurator implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private boolean firstLogin;

    /**
     * Creates a new configurator who has not yet completed first-login registration.
     * @param username preliminary username (will be replaced during first login)
     * @param password preliminary password (will be replaced during first login)
     * @pre username != null && !username.isBlank()
     * @pre password != null && !password.isBlank()
     */
    public Configurator(String username, String password) {
        assert username != null && !username.isBlank() : "Username must be non-null and non-blank";
        assert password != null && !password.isBlank() : "Password must be non-null and non-blank";
        this.username = username.trim();
        this.password = password;
        this.firstLogin = true;
    }

    /** @return the username */
    public String getUsername() { return username; }

    /** @return true if first-login registration is not yet completed */
    public boolean isFirstLogin() { return firstLogin; }

    /**
     * Verifies the provided credentials.
     * @param username username to check
     * @param password password to check
     * @return true if both match
     * @pre username != null && password != null
     */
    public boolean authenticate(String username, String password) {
        assert username != null && password != null : "Credentials must not be null";
        return this.username.equals(username.trim()) && this.password.equals(password);
    }

    /**
     * Sets personal credentials after first login. Marks first login as complete.
     * @param newUsername new personal username (non-null, non-blank)
     * @param newPassword new personal password (non-null, non-blank)
     * @pre newUsername != null && !newUsername.isBlank()
     * @pre newPassword != null && !newPassword.isBlank()
     * @post !isFirstLogin()
     */
    public void setPersonalCredentials(String newUsername, String newPassword) {
        assert newUsername != null && !newUsername.isBlank() : "New username must be non-null and non-blank";
        assert newPassword != null && !newPassword.isBlank() : "New password must be non-null and non-blank";
        this.username = newUsername.trim();
        this.password = newPassword;
        this.firstLogin = false;
        assert !isFirstLogin() : "Post-condition failed: firstLogin should be false";
    }

    /** Invariant check */
    public boolean repOk() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return username;
    }
}