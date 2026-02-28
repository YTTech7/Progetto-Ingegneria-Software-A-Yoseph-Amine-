package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplicationState implements Serializable {
	private static final long serialVersionUID = 2L;
    private static transient ApplicationState instance;
    
    
    // attibuti della classe
    private final List<Configurator> configurators  = new ArrayList<>();
    private final List<BaseField>    baseFields     = new ArrayList<>();
    private final List<CommonField>  commonFields   = new ArrayList<>();
    private final List<Category>     categories     = new ArrayList<>();
    private boolean baseFieldsLocked = false;
    
    // solo per il primo accesso 
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "adminYA";
    
    private ApplicationState() {}
    
    // ----------------------------------------------------------------
    // Singleton plumbing (cre + deserialization support)
    // ----------------------------------------------------------------

    public static ApplicationState getInstance() {
        if (instance == null) instance = new ApplicationState();
        return instance;
    }

    /** Called by PersistenceManager after deserialization. */
    public static void setInstance(ApplicationState loaded) {
        assert loaded != null;
        instance = loaded;
    }

    // ----------------------------------------------------------------
    // Configurators — raw list access
    // ----------------------------------------------------------------

    /** Returns a live (modifiable) view — only Services should mutate it. */
    public List<Configurator> getConfigurators() { return configurators; }

    /** Read-only snapshot for everyone else. */
    public List<Configurator> getConfiguratorsView() {
        return Collections.unmodifiableList(configurators);
    }

    // ----------------------------------------------------------------
    // Base fields
    // ----------------------------------------------------------------

    public List<BaseField> getBaseFields() {
        return Collections.unmodifiableList(baseFields);
    }

    /** Package-visible mutator used only by ConfigurationService. */
    public List<BaseField> getBaseFieldsMutable() { return baseFields; }

    public boolean isBaseFieldsLocked() { return baseFieldsLocked; }

    public void setBaseFieldsLocked(boolean locked) { this.baseFieldsLocked = locked; }

    // ----------------------------------------------------------------
    // Common fields
    // ----------------------------------------------------------------

    public List<CommonField> getCommonFields() {
        return Collections.unmodifiableList(commonFields);
    }

    public List<CommonField> getCommonFieldsMutable() { return commonFields; }

    // ----------------------------------------------------------------
    // Categories
    // ----------------------------------------------------------------

    public List<Category> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public List<Category> getCategoriesMutable() { return categories; }
    
}
