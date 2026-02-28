package service;

import exception.BaseFieldsAlreadyInitializedException;
import model.*;

import java.util.List;

/**
 * ConfigurationService — owns the one-time initialisation of base fields.
 *
 * Base fields are defined by the specification (8 fixed fields) and must be
 * set exactly once for the entire lifetime of the application.
 * After initialisation they are immutable.
 *
 * Invariant: state != null
 */
public class ConfigurationService {

    private final ApplicationState state;

    /**
     * @param state the application state (non-null)
     * @pre state != null
     */
    public ConfigurationService(ApplicationState state) {
        assert state != null : "ConfigurationService requires a non-null ApplicationState";
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /** @return true if base fields have already been initialised */
    public boolean areBaseFieldsInitialised() {
        return state.isBaseFieldsLocked();
    }

    /** @return the (unmodifiable) list of base fields */
    public List<BaseField> getBaseFields() {
        return state.getBaseFields();
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Initialises the eight predefined base fields and locks them permanently.
     * Must be called exactly once, during the very first session.
     *
     * The eight base fields (from the specification):
     *   Titolo, Numero di partecipanti, Termine ultimo di iscrizione,
     *   Luogo, Data, Ora, Quota individuale, Data conclusiva.
     *
     * @throws BaseFieldsAlreadyInitializedException if called more than once
     * @pre !areBaseFieldsInitialised()
     * @post areBaseFieldsInitialised()
     * @post getBaseFields().size() == 8
     */
    public void initBaseFields() throws BaseFieldsAlreadyInitializedException {
        if (areBaseFieldsInitialised()) {
            throw new BaseFieldsAlreadyInitializedException();
        }

        List<BaseField> mutable = state.getBaseFieldsMutable();
        mutable.clear();
        mutable.add(new BaseField("Titolo",                          FieldType.STRING));
        mutable.add(new BaseField("Numero di partecipanti",          FieldType.INTEGER));
        mutable.add(new BaseField("Termine ultimo di iscrizione",    FieldType.DATE));
        mutable.add(new BaseField("Luogo",                           FieldType.STRING));
        mutable.add(new BaseField("Data",                            FieldType.DATE));
        mutable.add(new BaseField("Ora",                             FieldType.TIME));
        mutable.add(new BaseField("Quota individuale",               FieldType.DECIMAL));
        mutable.add(new BaseField("Data conclusiva",                 FieldType.DATE));

        state.setBaseFieldsLocked(true);

        assert areBaseFieldsInitialised()      : "Post-condition: must be locked";
        assert getBaseFields().size() == 8     : "Post-condition: exactly 8 base fields";
    }
}