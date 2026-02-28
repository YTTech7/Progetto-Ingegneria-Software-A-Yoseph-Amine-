package model;

/**
 * Represents a specific field. Specific fields belong to exactly one category
 * and can be added, removed, or modified per category.
 */
public class SpecificField extends Field {

    private static final long serialVersionUID = 1L;

    /**
     * @param name      field name
     * @param type      field data type
     * @param mandatory whether the field is mandatory
     * @pre name != null && !name.isBlank()
     * @pre type != null
     */
    public SpecificField(String name, FieldType type, boolean mandatory) {
        super(name, type, mandatory);
    }

    @Override
    public String getKindLabel() { return "Specifico"; }
}