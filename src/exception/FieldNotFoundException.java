// exception/FieldNotFoundException.java
package exception;

public class FieldNotFoundException extends DomainException {
	private static final long serialVersionUID = 1L;

	public FieldNotFoundException(String name) {
        super("Campo non trovato: '" + name + "'.");
    }
}