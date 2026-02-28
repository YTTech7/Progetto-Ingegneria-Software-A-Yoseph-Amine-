package exception;

public class AuthenticationException extends DomainException{
	private static final long serialVersionUID = 1L;

	public AuthenticationException() {
        super("Credenziali non valide.");
    }
}
