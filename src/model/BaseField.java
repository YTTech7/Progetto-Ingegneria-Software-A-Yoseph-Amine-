package model;

/**
 * Represents a base field. Base fields are mandatory, shared by all categories,
 * and immutable after creation (name, type and mandatory status cannot be changed).
 *
 * Invariant: always mandatory (mandatory == true)
 */
public class BaseField extends Field {

    private static final long serialVersionUID = 1L;

    /**
     * @param name  field name
     * @param type  field data type
     * @pre name != null && !name.isBlank()
     * @pre type != null
     */
    public BaseField(String name, FieldType type) {
        super(name, type, true); // base fields are always mandatory
    }

    /**
     * Base fields cannot change their mandatory status.
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setMandatory(boolean mandatory) {
        throw new UnsupportedOperationException("I campi base sono sempre obbligatori e immutabili.");
    }

    @Override
    public String getKindLabel() { return "Base"; }
}
