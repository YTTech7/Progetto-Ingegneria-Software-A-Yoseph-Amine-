package model;

import java.io.Serializable;

/**
 * Abstract base class for all fields (base, common, specific).
 *
 * Invariant: name != null && !name.isBlank() && type != null
 */
public abstract class Field implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final FieldType type;
    private boolean mandatory;

    /**
     * @param name      unique name for the field (non-null, non-blank)
     * @param type      data type of the field (non-null)
     * @param mandatory whether filling this field is required
     * @pre name != null && !name.isBlank()
     * @pre type != null
     */
    protected Field(String name, FieldType type, boolean mandatory) {
        assert name != null && !name.isBlank() : "Field name must be non-null and non-blank";
        assert type != null : "Field type must be non-null";
        this.name = name.trim();
        this.type = type;
        this.mandatory = mandatory;
    }

    /** @return the field name */
    public String getName() { return name; }

    /** @return the field data type */
    public FieldType getType() { return type; }

    /** @return true if the field is mandatory */
    public boolean isMandatory() { return mandatory; }

    /**
     * Changes the mandatory status of this field.
     * @param mandatory new mandatory status
     * @post isMandatory() == mandatory
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return a human-readable label for the field kind (base/common/specific)
     */
    public abstract String getKindLabel();

    @Override
    public String toString() {
        return String.format("%-30s | %-15s | %-10s | %s",
                name, type.getDisplayName(),
                mandatory ? "Obbligatorio" : "Facoltativo",
                getKindLabel());
    }

    /** Invariant check */
    protected boolean repOk() {
        return name != null && !name.isBlank() && type != null;
    }
}
