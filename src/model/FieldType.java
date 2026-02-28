package model;

/**
 * Enumeration of allowed data types for a Field.
 */
public enum FieldType {
    STRING("Testo"),
    INTEGER("Numero intero"),
    DECIMAL("Numero decimale"),
    DATE("Data"),
    TIME("Ora"),
    BOOLEAN("Sì/No");

    private final String displayName;

    FieldType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Returns the FieldType matching the given index (1-based) for menu selection.
     * @param index 1-based index
     * @return corresponding FieldType, or null if invalid
     */
    public static FieldType fromIndex(int index) {
        FieldType[] values = FieldType.values();
        if (index < 1 || index > values.length) return null;
        return values[index - 1];
    }
}