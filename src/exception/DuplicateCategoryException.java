// exception/DuplicateCategoryException.java
package exception;

public class DuplicateCategoryException extends DomainException {
	private static final long serialVersionUID = 1L;

	public DuplicateCategoryException(String name) {
        super("Categoria già esistente: '" + name + "'.");
    }
}