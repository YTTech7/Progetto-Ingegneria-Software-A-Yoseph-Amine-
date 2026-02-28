// exception/CategoryNotFoundException.java
package exception;

public class CategoryNotFoundException extends DomainException {
	private static final long serialVersionUID = 1L;

	public CategoryNotFoundException(String name) {
        super("Categoria non trovata: '" + name + "'.");
    }
}