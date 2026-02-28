package exception;

/**
 * Base class for all domain exceptions in this application.
 * Using checked exceptions so the compiler forces callers to handle them.
 */
public class DomainException extends Exception {
	private static final long serialVersionUID = 1L;

	public DomainException(String message) { super(message); }
}