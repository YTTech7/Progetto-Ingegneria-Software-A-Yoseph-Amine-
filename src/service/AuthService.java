package service;

import exception.AuthenticationException;
import exception.DuplicateUsernameException;
import model.ApplicationState;
import model.Configurator;

/**
 * AuthService — owns all authentication and registration business rules.
 *
 * CORREZIONE V2:
 *   createPendingConfigurator() non è più vincolato a isFirstEverLaunch().
 *   Le credenziali predefinite servono ogni volta che si vuole aggiungere
 *   un nuovo configuratore al gruppo, non solo al primissimo avvio.
 *   Questo rispetta la specifica: "comunicate a ciascun nuovo configuratore
 *   autorizzato a registrarsi".
 *
 * Invariant: state != null
 */
public class AuthService {

    private final ApplicationState state;

    /** @pre state != null */
    public AuthService(ApplicationState state) {
        assert state != null;
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /**
     * @return true se non esiste ancora nessun configuratore registrato.
     *         Usato solo per mostrare il messaggio di benvenuto al primo avvio.
     *         NON usato per decidere se accettare le credenziali predefinite.
     */
    public boolean isFirstEverLaunch() {
        return state.getConfigurators().isEmpty();
    }

    /**
     * Controlla se le credenziali inserite sono quelle predefinite di sistema.
     * @pre username != null && password != null
     */
    public boolean isDefaultCredentials(String username, String password) {
        assert username != null && password != null;
        return ApplicationState.DEFAULT_USERNAME.equals(username)
            && ApplicationState.DEFAULT_PASSWORD.equals(password);
    }

    /** @return true se lo username è già usato da un altro configuratore */
    public boolean isUsernameTaken(String username) {
        assert username != null;
        return state.getConfigurators().stream()
                .anyMatch(c -> c.getUsername().equalsIgnoreCase(username));
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Crea un nuovo configuratore pendente che dovrà completare la registrazione.
     *
     * CORREZIONE: rimosso il vincolo isFirstEverLaunch().
     * Le credenziali predefinite possono essere usate in qualsiasi momento
     * per aggiungere un nuovo configuratore al gruppo, non solo al primo avvio.
     *
     * @return il nuovo Configurator con firstLogin=true
     * @post il configuratore è in ApplicationState ma ha ancora firstLogin=true
     * @post !isFirstEverLaunch()
     */
    public Configurator createPendingConfigurator() {
        // Username temporaneo univoco — verrà sostituito durante la registrazione
        String tempUsername = ApplicationState.DEFAULT_USERNAME
                + "_pending_" + System.currentTimeMillis();

        Configurator c = new Configurator(
                tempUsername,
                ApplicationState.DEFAULT_PASSWORD);
        state.addConfigurator(c);

        assert !isFirstEverLaunch();
        return c;
    }

    /**
     * Completa la registrazione assegnando credenziali personali.
     * Verifica l'unicità dello username tra tutti i configuratori esistenti
     * (escludendo il configuratore pending corrente).
     *
     * @throws DuplicateUsernameException se newUsername è già usato
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

        // Unicità: esclude il configuratore pending corrente dal controllo
        boolean taken = state.getConfigurators().stream()
                .filter(c -> c != configurator)
                .anyMatch(c -> c.getUsername().equalsIgnoreCase(newUsername));

        if (taken) throw new DuplicateUsernameException(newUsername);

        configurator.setPersonalCredentials(newUsername, newPassword);

        assert !configurator.isFirstLogin();
    }

    /**
     * Autentica un configuratore con username e password.
     *
     * @throws AuthenticationException se nessun configuratore corrisponde
     * @pre username != null && password != null
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