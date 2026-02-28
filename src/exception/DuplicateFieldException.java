// exception/DuplicateFieldException.java
package exception;

public class DuplicateFieldException extends DomainException {
	private static final long serialVersionUID = 1L;

	public DuplicateFieldException(String name, String scope) {
        super("Campo '" + name + "' già presente in: " + scope + ".");
    }
}