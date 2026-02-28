// exception/BaseFieldsAlreadyInitializedException.java
package exception;

public class BaseFieldsAlreadyInitializedException extends DomainException {
	private static final long serialVersionUID = 1L;

	public BaseFieldsAlreadyInitializedException() {
        super("I campi base sono già stati inizializzati e sono immutabili.");
    }
}