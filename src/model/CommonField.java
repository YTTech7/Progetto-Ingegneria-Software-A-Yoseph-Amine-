package model;

/**
 * Represents a common field. Common fields are shared by all categories
 * and can be freely added, removed, or modified by configurators.
 */
public class CommonField extends Field {

    private static final long serialVersionUID = 1L;

    /**
     * @param name      field name
     * @param type      field data type
     * @param mandatory whether the field is mandatory
     * @pre name != null && !name.isBlank()
     * @pre type != null
     */
    public CommonField(String name, FieldType type, boolean mandatory) {
        super(name, type, mandatory);
    }

    @Override
    public String getKindLabel() { return "Comune"; }
}