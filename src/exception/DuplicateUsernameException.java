package exception;

public class DuplicateUsernameException extends DomainException {
	private static final long serialVersionUID = 1L;

	public DuplicateUsernameException(String username) {
        super("Username già in uso: '" + username + "'.");
    }
}