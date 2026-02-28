package service;

import exception.CategoryNotFoundException;
import exception.DuplicateCategoryException;
import model.ApplicationState;
import model.Category;

import java.util.List;

/**
 * CategoryService — owns all business rules for the category lifecycle.
 *
 * Enforces:
 *  - name uniqueness across all categories
 *  - existence checks before editing / removing
 *
 * Note: this service does NOT manage specific fields — that is FieldService's job.
 * The two services collaborate when the controller needs both (e.g. create category
 * then add specific fields).
 *
 * Invariant: state != null
 */
public class CategoryService {

    private final ApplicationState state;

    /**
     * @param state the application state (non-null)
     * @pre state != null
     */
    public CategoryService(ApplicationState state) {
        assert state != null : "CategoryService requires a non-null ApplicationState";
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /** @return read-only list of all categories */
    public List<Category> getCategories() {
        return state.getCategories();
    }

    /** @return true if a category with the given name exists (case-insensitive) */
    public boolean categoryExists(String name) {
        assert name != null;
        return state.getCategories().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    /**
     * Retrieves a category by name.
     *
     * @param name category name
     * @return the matching Category
     * @throws CategoryNotFoundException if not found
     * @pre name != null
     */
    public Category getCategory(String name) throws CategoryNotFoundException {
        assert name != null;
        return state.getCategories().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new CategoryNotFoundException(name));
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Creates and registers a new category with no specific fields.
     *
     * Specific fields are added separately via FieldService — keeping
     * creation and field management as independent concerns.
     *
     * @param name category name (non-null, non-blank)
     * @return the newly created Category
     * @throws DuplicateCategoryException if a category with the same name exists
     * @pre name != null && !name.isBlank()
     * @post categoryExists(name)
     */
    public Category addCategory(String name) throws DuplicateCategoryException {
        assert name != null && !name.isBlank() : "Category name must be non-null and non-blank";

        // ---- Business rule: uniqueness ----
        if (categoryExists(name)) {
            throw new DuplicateCategoryException(name);
        }

        Category newCategory = new Category(name);
        state.getCategoriesMutable().add(newCategory);

        assert categoryExists(name) : "Post-condition: category must exist after creation";
        return newCategory;
    }

    /**
     * Removes a category (and implicitly all its specific fields, since they are
     * stored inside the Category object).
     *
     * Per specification: removal has no effect on already-published proposals
     * (not relevant in V1, but the rule is documented here for future versions).
     *
     * @param name category name to remove
     * @throws CategoryNotFoundException if no category with that name exists
     * @pre name != null
     * @post !categoryExists(name)
     */
    public void removeCategory(String name) throws CategoryNotFoundException {
        assert name != null;

        boolean removed = state.getCategoriesMutable()
                .removeIf(c -> c.getName().equalsIgnoreCase(name));

        if (!removed) throw new CategoryNotFoundException(name);

        assert !categoryExists(name) : "Post-condition: category must not exist after removal";
    }
}