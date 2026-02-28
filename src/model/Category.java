package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a category of initiatives (e.g. "sport", "arte", "gite").
 *
 * Invariant: name != null && !name.isBlank()
 * Invariant: no two specific fields share the same name (case-insensitive)
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<SpecificField> specificFields;

    /**
     * Creates a new category with no specific fields.
     * @param name category name (non-null, non-blank, must be unique system-wide)
     * @pre name != null && !name.isBlank()
     */
    public Category(String name) {
        assert name != null && !name.isBlank() : "Category name must be non-null and non-blank";
        this.name = name.trim();
        this.specificFields = new ArrayList<>();
    }

    /** @return the category name */
    public String getName() { return name; }

    /**
     * Returns an unmodifiable view of the specific fields.
     * @return list of specific fields
     */
    public List<SpecificField> getSpecificFields() {
        return Collections.unmodifiableList(specificFields);
    }

    /**
     * Adds a specific field to this category.
     * @param field the field to add
     * @return true if added, false if a field with the same name already exists
     * @pre field != null
     * @post if return true: getSpecificFields().contains(field)
     */
    public boolean addSpecificField(SpecificField field) {
        assert field != null : "Field must not be null";
        if (hasSpecificField(field.getName())) return false;
        specificFields.add(field);
        return true;
    }

    /**
     * Removes a specific field by name.
     * @param fieldName name of the field to remove
     * @return true if removed, false if not found
     * @pre fieldName != null
     * @post !hasSpecificField(fieldName)
     */
    public boolean removeSpecificField(String fieldName) {
        assert fieldName != null : "Field name must not be null";
        return specificFields.removeIf(f -> f.getName().equalsIgnoreCase(fieldName));
    }

    /**
     * Checks if a specific field with the given name exists.
     * @param fieldName field name to look up
     * @return true if found
     */
    public boolean hasSpecificField(String fieldName) {
        return specificFields.stream()
                .anyMatch(f -> f.getName().equalsIgnoreCase(fieldName));
    }

    /**
     * Returns a specific field by name, or null if not found.
     * @param fieldName field name to look up
     * @return the field, or null
     */
    public SpecificField getSpecificField(String fieldName) {
        return specificFields.stream()
                .filter(f -> f.getName().equalsIgnoreCase(fieldName))
                .findFirst().orElse(null);
    }

    /** Invariant check */
    public boolean repOk() {
        if (name == null || name.isBlank()) return false;
        // no duplicate names in specific fields
        long distinctNames = specificFields.stream()
                .map(f -> f.getName().toLowerCase())
                .distinct().count();
        return distinctNames == specificFields.size();
    }

    @Override
    public String toString() {
        return name;
    }
}