package model;

/**
 * Rappresenta un campo base. I campi base sono comuni a tutte le categorie
 * e immutabili dopo la creazione (nome, tipo e obbligatorietà non modificabili).
 *
 * Dalla specifica: i campi base sono definiti nella sezione GENERALITÀ
 * e alcuni sono obbligatori, altri facoltativi (es. Ora, Quota individuale,
 * Data conclusiva sono facoltativi).
 *
 * CORREZIONE: aggiunto costruttore a 3 parametri per permettere di specificare
 * l'obbligatorietà, visto che non tutti i campi base sono obbligatori.
 */
public class BaseField extends Field {

    private static final long serialVersionUID = 1L;

    /**
     * Costruttore con obbligatorietà esplicita.
     * Usato da ConfigurationService per creare i campi base con il corretto
     * valore di mandatory per ciascuno.
     *
     * @param name      nome del campo
     * @param type      tipo di dato
     * @param mandatory true se obbligatorio, false se facoltativo
     */
    public BaseField(String name, FieldType type, boolean mandatory) {
        super(name, type, mandatory);
    }

    /**
     * Costruttore legacy che forza mandatory=true.
     * Mantenuto per compatibilità — i campi base storicamente erano tutti obbligatori.
     *
     * @param name nome del campo
     * @param type tipo di dato
     */
    public BaseField(String name, FieldType type) {
        super(name, type, true);
    }

    /**
     * I campi base non possono cambiare la loro obbligatorietà dopo la creazione.
     * @throws UnsupportedOperationException sempre
     */
    @Override
    public void setMandatory(boolean mandatory) {
        throw new UnsupportedOperationException(
            "I campi base sono immutabili: l'obbligatorietà non può essere modificata.");
    }

    @Override
    public String getKindLabel() { return "Base"; }
}