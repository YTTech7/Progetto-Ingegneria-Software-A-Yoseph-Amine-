package service;

import exception.BaseFieldsAlreadyInitializedException;
import model.*;

import java.util.List;

/**
 * ConfigurationService — gestisce l'inizializzazione una-tantum dei campi base.
 *
 * I campi base sono definiti dalla sezione GENERALITÀ della specifica:
 *   Titolo, Numero di partecipanti, Termine ultimo di iscrizione,
 *   Luogo, Data, Ora, Quota individuale, Data conclusiva.
 *
 * Vengono inizializzati automaticamente al primo avvio e mostrati
 * al configuratore per conferma. Una volta creati sono immutabili.
 *
 * Invariant: state != null
 */
public class ConfigurationService {

    private final ApplicationState state;

    /** @pre state != null */
    public ConfigurationService(ApplicationState state) {
        assert state != null;
        this.state = state;
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    /** @return true se i campi base sono già stati inizializzati */
    public boolean areBaseFieldsInitialised() {
        return state.isBaseFieldsLocked();
    }

    /** @return lista immutabile dei campi base */
    public List<BaseField> getBaseFields() {
        return state.getBaseFields();
    }

    // ----------------------------------------------------------------
    // Commands
    // ----------------------------------------------------------------

    /**
     * Inizializza gli 8 campi base definiti dalla specifica e li blocca.
     *
     * Deve essere chiamato esattamente una volta, durante il primo avvio.
     * Il Controller mostra i campi al configuratore PRIMA di chiamare
     * questo metodo, in modo che l'utente sappia cosa sta per essere fissato.
     *
     * @throws BaseFieldsAlreadyInitializedException se chiamato più volte
     * @pre !areBaseFieldsInitialised()
     * @post areBaseFieldsInitialised()
     * @post getBaseFields().size() == 8
     */
    public void initBaseFields(List<BaseField> fields)
            throws BaseFieldsAlreadyInitializedException {

        assert fields != null && !fields.isEmpty() : "fields must be non-null and non-empty";

        if (areBaseFieldsInitialised()) {
            throw new BaseFieldsAlreadyInitializedException();
        }

        state.getBaseFieldsMutable().addAll(fields);
        state.setBaseFieldsLocked(true);

        assert areBaseFieldsInitialised();
        assert getBaseFields().size() == fields.size();
    }
}