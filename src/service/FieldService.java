package service;

import exception.DuplicateFieldException;
import exception.FieldNotFoundException;
import model.*;

import java.util.List;

/**
 * FieldService — owns all business rules for common fields and specific fields.
 *
 * Common fields:  shared by every category, managed globally.
 * Specific fields: belong to one category, managed per-category.
 *
 * Enforces:
 *  - name uniqueness within common fields
 *  - name uniqueness within specific fields of the same category
 *  - existence checks before removal / mandatory-toggle
 *
 * Invariant: state != null
 */
public class FieldService {

    private final ApplicationState state;

    /**
     * @param state the application state (non-null)
     * @pre state != null
     */
    public FieldService(ApplicationState state) {
        assert state != null : "FieldService requires a non-null ApplicationState";
        this.state = state;
    }

    // ================================================================
    // COMMON FIELDS
    // ================================================================

    /** @return read-only list of common fields */
    public List<CommonField> getCommonFields() {
        return state.getCommonFields();
    }

    /**
     * Adds a new common field.
     *
     * @param name      field name (non-null, non-blank)
     * @param type      data type (non-null)
     * @param mandatory whether the field is mandatory
     * @throws DuplicateFieldException if a common field with the same name exists
     * @pre name != null && !name.isBlank() && type != null
     * @post getCommonFields() contains a field with the given name
     */
    public void addCommonField(String name, FieldType type, boolean mandatory)
            throws DuplicateFieldException {

        assert name != null && !name.isBlank() && type != null;

        if (commonFieldExists(name)) {
            throw new DuplicateFieldException(name, "campi comuni");
        }

        state.getCommonFieldsMutable().add(new CommonField(name, type, mandatory));

        assert commonFieldExists(name) : "Post-condition: field must exist after add";
    }

    /**
     * Removes a common field by name.
     *
     * @param name field name to remove
     * @throws FieldNotFoundException if no common field with that name exists
     * @pre name != null
     * @post !commonFieldExists(name)
     */
    public void removeCommonField(String name) throws FieldNotFoundException {
        assert name != null;
        boolean removed = state.getCommonFieldsMutable()
                .removeIf(f -> f.getName().equalsIgnoreCase(name));
        if (!removed) throw new FieldNotFoundException(name);

        assert !commonFieldExists(name) : "Post-condition: field must not exist after removal";
    }

    /**
     * Changes the mandatory status of a common field.
     *
     * @param name      field name
     * @param mandatory new mandatory status
     * @throws FieldNotFoundException if no common field with that name exists
     * @pre name != null
     * @post getCommonField(name).isMandatory() == mandatory
     */
    public void setCommonFieldMandatory(String name, boolean mandatory)
            throws FieldNotFoundException {
        assert name != null;
        CommonField field = findCommonField(name);
        field.setMandatory(mandatory);
    }

    /** @return true if a common field with the given name exists */
    public boolean commonFieldExists(String name) {
        return state.getCommonFields().stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(name));
    }

    /** Returns a common field by name or throws FieldNotFoundException. */
    public CommonField getCommonField(String name) throws FieldNotFoundException {
        return findCommonField(name);
    }

    private CommonField findCommonField(String name) throws FieldNotFoundException {
        return state.getCommonFields().stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new FieldNotFoundException(name));
    }

    // ================================================================
    // SPECIFIC FIELDS  (operate on a given Category)
    // ================================================================

    /**
     * Adds a specific field to a category.
     *
     * @param category  the target category (non-null)
     * @param name      field name
     * @param type      data type
     * @param mandatory whether the field is mandatory
     * @throws DuplicateFieldException if a specific field with the same name
     *                                 already exists in the category
     * @pre category != null && name != null && !name.isBlank() && type != null
     * @post category.hasSpecificField(name)
     */
    public void addSpecificField(Category category,
                                 String name,
                                 FieldType type,
                                 boolean mandatory)
            throws DuplicateFieldException {

        assert category != null && name != null && !name.isBlank() && type != null;

        if (category.hasSpecificField(name)) {
            throw new DuplicateFieldException(name,
                    "campi specifici di '" + category.getName() + "'");
        }

        boolean added = category.addSpecificField(new SpecificField(name, type, mandatory));
        // addSpecificField returns false only on duplicate, already checked above
        assert added : "Unexpected duplicate despite prior check";
        assert category.hasSpecificField(name) : "Post-condition: specific field must exist";
    }

    /**
     * Removes a specific field from a category.
     *
     * @param category  the target category
     * @param name      field name to remove
     * @throws FieldNotFoundException if no specific field with that name exists
     * @pre category != null && name != null
     * @post !category.hasSpecificField(name)
     */
    public void removeSpecificField(Category category, String name)
            throws FieldNotFoundException {

        assert category != null && name != null;

        boolean removed = category.removeSpecificField(name);
        if (!removed) throw new FieldNotFoundException(name);

        assert !category.hasSpecificField(name) : "Post-condition: field must not exist";
    }

    /**
     * Changes the mandatory status of a specific field.
     *
     * @param category  the target category
     * @param name      field name
     * @param mandatory new mandatory status
     * @throws FieldNotFoundException if no specific field with that name exists
     */
    public void setSpecificFieldMandatory(Category category,
                                          String name,
                                          boolean mandatory)
            throws FieldNotFoundException {

        assert category != null && name != null;

        SpecificField field = category.getSpecificField(name);
        if (field == null) throw new FieldNotFoundException(name);
        field.setMandatory(mandatory);
    }
}