package service;

import exception.AuthenticationException;
import exception.DuplicateUsernameException;
import model.ApplicationState;
import model.Configurator;

/**
 * AuthService — owns all authentication and registration business rules.
 *
 * Responsibilities:
 *  - first-time login with default credentials
 *  - registration of personal credentials
 *  - authentication of subsequent logins
 *  - username uniqueness enforcement
 *
 * Invariant: state != null
 */
public class AuthService {

    private final ApplicationState state;

    /**
     * @param state the application state (non-null)
     * @pre state != null
     */
    public AuthService(ApplicationState state) {
        assert state != null : "AuthService requires a non-null ApplicationState";
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /**
     * @return true if no configurator has been registered yet
     *         (i.e. the application is running for the very first time)
     */
    public boolean isFirstEverLaunch() {
        return state.getConfigurators().isEmpty();
    }

    /**
     * Checks whether the supplied pair matches the system-wide default credentials.
     * @pre username != null && password != null
     */
    public boolean isDefaultCredentials(String username, String password) {
        assert username != null && password != null;
        return ApplicationState.DEFAULT_USERNAME.equals(username)
            && ApplicationState.DEFAULT_PASSWORD.equals(password);
    }

    /**
     * @return true if the given username is already taken by any configurator
     */
    public boolean isUsernameTaken(String username) {
        assert username != null;
        return state.getConfigurators().stream()
                .anyMatch(c -> c.getUsername().equalsIgnoreCase(username));
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Creates a new Configurator entry with default credentials and marks it
     * as pending first-login registration.
     *
     * Called only during the very first launch, before personal credentials
     * have been chosen.
     *
     * @return the newly created (unregistered) Configurator
     * @pre isFirstEverLaunch()
     * @post !isFirstEverLaunch()
     */
    public Configurator createPendingConfigurator() {
        assert isFirstEverLaunch() : "Pre-condition: no configurator must exist yet";
        Configurator c = new Configurator(
                ApplicationState.DEFAULT_USERNAME,
                ApplicationState.DEFAULT_PASSWORD);
        state.getConfigurators().add(c);
        assert !isFirstEverLaunch() : "Post-condition: at least one configurator now exists";
        return c;
    }

    /**
     * Completes registration by assigning personal credentials to a pending
     * configurator. Enforces username uniqueness across the whole system.
     *
     * @param configurator the pending configurator (isFirstLogin() == true)
     * @param newUsername  desired personal username
     * @param newPassword  desired personal password
     * @throws DuplicateUsernameException if newUsername is already taken by
     *                                    another configurator
     * @pre configurator != null && configurator.isFirstLogin()
     * @pre newUsername != null && !newUsername.isBlank()
     * @pre newPassword != null && !newPassword.isBlank()
     * @post !configurator.isFirstLogin()
     */
    public void completeRegistration(Configurator configurator,
                                     String newUsername,
                                     String newPassword)
            throws DuplicateUsernameException {

        assert configurator != null && configurator.isFirstLogin();
        assert newUsername != null && !newUsername.isBlank();
        assert newPassword != null && !newPassword.isBlank();

        // Uniqueness check: exclude the current placeholder entry
        boolean taken = state.getConfigurators().stream()
                .filter(c -> c != configurator)
                .anyMatch(c -> c.getUsername().equalsIgnoreCase(newUsername));

        if (taken) throw new DuplicateUsernameException(newUsername);

        configurator.setPersonalCredentials(newUsername, newPassword);

        assert !configurator.isFirstLogin() : "Post-condition: registration must be complete";
    }

    /**
     * Authenticates a configurator by username + password.
     *
     * @param username supplied username
     * @param password supplied password
     * @return the matching Configurator
     * @throws AuthenticationException if no match is found
     * @pre username != null && password != null
     * @post return value != null && return value authenticates with the given credentials
     */
    public Configurator authenticate(String username, String password)
            throws AuthenticationException {

        assert username != null && password != null;

        return state.getConfigurators().stream()
                .filter(c -> c.authenticate(username, password))
                .findFirst()
                .orElseThrow(AuthenticationException::new);
    }
}