package controller;

import exception.*;
import model.Category;
import model.Configurator;
import model.FieldType;
import persistence.PersistenceManager;
import service.*;
import view.ConsoleUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ConfiguratorController V2.
 *
 * CORREZIONI rispetto alla versione precedente:
 *
 *   1. login() — le credenziali predefinite aprono SEMPRE una nuova
 *      registrazione, non solo al primissimo avvio. Questo permette
 *      di aggiungere più configuratori al gruppo nel tempo.
 *      (Specifica: "comunicate a ciascun nuovo configuratore autorizzato
 *      a registrarsi")
 *
 *   2. initBaseFieldsIfNeeded() — mostra i campi base al configuratore
 *      prima di confermare, invece di inizializzarli silenziosamente.
 *      (Specifica: "deve essere fornito all'applicazione al suo primo avvio")
 */
public class ConfiguratorController {

    private final ConsoleUI            ui;
    private final AuthService          authService;
    private final ConfigurationService configService;
    private final FieldService         fieldService;
    private final CategoryService      categoryService;
    private final PersistenceManager   pm;

    private Configurator currentConfigurator;

    public ConfiguratorController(ConsoleUI ui,
                                   AuthService authService,
                                   ConfigurationService configService,
                                   FieldService fieldService,
                                   CategoryService categoryService,
                                   PersistenceManager pm) {
        assert ui != null && authService != null && configService != null
            && fieldService != null && categoryService != null;
        this.ui              = ui;
        this.authService     = authService;
        this.configService   = configService;
        this.fieldService    = fieldService;
        this.categoryService = categoryService;
        this.pm              = pm;
    }

    // ================================================================
    // LOGIN / LOGOUT  (UC-01, UC-02)
    // ================================================================

    /**
     * Gestisce il flusso di login.
     *
     * CORREZIONE: le credenziali predefinite aprono sempre una nuova
     * registrazione — non è più condizionale a isFirstEverLaunch().
     * Questo permette al sistema di avere più configuratori nel gruppo.
     *
     * @return true se il configuratore è autenticato e pronto ad operare
     * @post se ritorna true: currentConfigurator != null
     */
    public boolean login() {
        ui.printTitle("Accesso Configuratore");

        // Messaggio di benvenuto solo al primissimo avvio assoluto
        if (authService.isFirstEverLaunch()) {
            ui.printInfo("Primo avvio del sistema.");
            ui.printInfo("Usa le credenziali predefinite per registrarti:");
            ui.printInfo("  username: " + model.ApplicationState.DEFAULT_USERNAME
                       + "  |  password: " + model.ApplicationState.DEFAULT_PASSWORD);
            ui.printBlank();
        }

        String username = ui.readString("Username");
        String password = ui.readString("Password");

        // ── CORREZIONE: credenziali predefinite → sempre nuova registrazione ──
        // Non importa se è il primo avvio o il decimo:
        // le credenziali predefinite identificano sempre un nuovo configuratore
        // che deve scegliere le sue credenziali personali.
        if (authService.isDefaultCredentials(username, password)) {
            ui.printSuccess("Credenziali predefinite riconosciute.");
            ui.printInfo("Devi scegliere le tue credenziali personali.");
            Configurator pending = authService.createPendingConfigurator();
            return runRegistration(pending);
        }

        // ── Autenticazione normale con credenziali personali ──────────────────
        try {
            Configurator c = authService.authenticate(username, password);

            // Registrazione incompleta (interruzione precedente)
            if (c.isFirstLogin()) {
                ui.printWarning("Registrazione precedentemente interrotta. Riprendi.");
                return runRegistration(c);
            }

            currentConfigurator = c;
            ui.printSuccess("Benvenuto, " + c.getUsername() + "!");
            return true;

        } catch (AuthenticationException e) {
            ui.printError(e.getMessage());
            return false;
        }
    }

    /**
     * Raccoglie e imposta le credenziali personali per un configuratore pending.
     * Blocca il configuratore finché la registrazione non è completata.
     * (Specifica: "dovrà immediatamente scegliere credenziali personali,
     * solo a valle di tale scelta egli potrà operare sul back-end")
     */
    private boolean runRegistration(Configurator pending) {
        ui.printSection("Registrazione — scegli le tue credenziali personali");
        while (true) {
            String newUser = ui.readString("Nuovo username");
            String newPass = ui.readString("Nuova password");
            String confirm = ui.readString("Conferma password");

            if (!newPass.equals(confirm)) {
                ui.printError("Le password non coincidono. Riprova.");
                continue;
            }
            try {
                authService.completeRegistration(pending, newUser, newPass);
                save();
                ui.printSuccess("Registrazione completata. Benvenuto, "
                        + pending.getUsername() + "!");
                currentConfigurator = pending;
                return true;
            } catch (DuplicateUsernameException e) {
                ui.printError(e.getMessage());
                // loop: chiede di nuovo
            }
        }
    }

    public void logout() {
        if (currentConfigurator != null) {
            ui.printInfo("Sessione chiusa per: " + currentConfigurator.getUsername());
            currentConfigurator = null;
        }
    }

    // ================================================================
    // BASE FIELDS  (UC-03)
    // ================================================================

    /**
     * Inizializza i campi base se non ancora fatto.
     *
     * CORREZIONE: mostra i campi al configuratore prima di confermare,
     * invece di inizializzarli silenziosamente.
     * (Specifica: "deve essere fornito all'applicazione al suo primo avvio")
     */
    public boolean initBaseFieldsIfNeeded() {
        if (configService.areBaseFieldsInitialised()) return true;

        ui.printTitle("Definizione Campi Base — Primo Avvio");
        ui.printInfo("Il sistema non ha ancora campi base definiti.");
        ui.printInfo("In quanto primo configuratore, devi definirli ora.");
        ui.printInfo("Una volta salvati saranno IMMUTABILI per sempre.");
        ui.printBlank();
        ui.printInfo("I campi base sono comuni a tutte le categorie e a tutte");
        ui.printInfo("le iniziative proposte. Ogni proposta dovrà compilarli.");
        ui.printBlank();
        
        List<model.BaseField> fields = new ArrayList<>();
        
        ui.printSection("Inserisci i campi base uno per uno");
        ui.printInfo("Premi invio senza scrivere nulla per terminare l'inserimento.");
        
        while (true) {
            ui.printBlank();
            ui.print("  Campi inseriti finora: " + fields.size());

            // Chiede il nome — stringa vuota = fine inserimento
            System.out.print("  > Nome del campo (invio per terminare): ");
            String name = ui.getScanner().nextLine().trim();

            if (name.isEmpty()) {
                if (fields.isEmpty()) {
                    ui.printError("Devi inserire almeno un campo base. Riprova.");
                    continue;
                }
                break; // fine inserimento
            }

            // Controlla duplicati
            boolean duplicate = fields.stream()
                    .anyMatch(f -> f.getName().equalsIgnoreCase(name));
            if (duplicate) {
                ui.printError("Campo '" + name + "' già inserito. Scegli un altro nome.");
                continue;
            }

            model.FieldType type    = ui.readFieldType();
            boolean mandatory       = ui.readMandatory();
            fields.add(new model.BaseField(name, type, mandatory));
            ui.printSuccess("'" + name + "' aggiunto.");
        }

        
        // Mostra riepilogo prima di confermare
        ui.printBlank();
        ui.printSection("Riepilogo campi base da salvare (" + fields.size() + " campi)");
        ui.print(String.format("  %-30s | %-15s | %s", "Nome", "Tipo", "Obbligatorio"));
        ui.print("  " + "-".repeat(60));
        for (model.BaseField f : fields) {
            ui.print(String.format("  %-30s | %-15s | %s",
                f.getName(),
                f.getType().getDisplayName(),
                f.isMandatory() ? "Sì" : "No"));
        }
        ui.printBlank();
        ui.printWarning("Attenzione: dopo la conferma questi campi non potranno più essere modificati.");

        if (!ui.readConfirm("Confermi e salvi i campi base?")) {
            ui.printInfo("Operazione annullata. I campi base verranno richiesti al prossimo avvio.");
            return false;
        }

        try {
            configService.initBaseFields(fields);
            save();
            ui.printSuccess(fields.size() + " campi base salvati e bloccati definitivamente.");
            return true;
        } catch (exception.BaseFieldsAlreadyInitializedException e) {
            ui.printInfo(e.getMessage());
            return true;
        }
        
    }
    
    
    
    

    // ================================================================
    // COMMON FIELDS  (UC-04, 05, 06) — invariati
    // ================================================================

    public void manageCommonFields() {
        boolean back = false;
        while (!back) {
            ui.printTitle("Gestione Campi Comuni");
            printCommonFields();
            ui.printBlank();
            ui.printMenu(
                "Aggiungi campo comune",
                "Rimuovi campo comune",
                "Modifica obbligatorietà"
            );
            switch (ui.readInt("Scelta", 0, 3)) {
                case 1 -> handleAddCommonField();
                case 2 -> handleRemoveCommonField();
                case 3 -> handleToggleCommonField();
                case 0 -> back = true;
            }
        }
    }

    private void handleAddCommonField() {
        ui.printSection("Aggiungi campo comune");
        String name       = ui.readString("Nome del campo");
        FieldType type    = ui.readFieldType();
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.addCommonField(name, type, mandatory);
            save();
            ui.printSuccess("Campo comune '" + name + "' aggiunto.");
        } catch (DuplicateFieldException e) { ui.printError(e.getMessage()); }
    }

    private void handleRemoveCommonField() {
        ui.printSection("Rimuovi campo comune");
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("Nessun campo comune presente."); return;
        }
        printCommonFields();
        String name = ui.readString("Nome del campo da rimuovere");
        if (!ui.readConfirm("Confermi la rimozione di '" + name + "'?")) {
            ui.printInfo("Annullato."); return;
        }
        try {
            fieldService.removeCommonField(name);
            save();
            ui.printSuccess("'" + name + "' rimosso.");
        } catch (FieldNotFoundException e) { ui.printError(e.getMessage()); }
    }

    private void handleToggleCommonField() {
        ui.printSection("Modifica obbligatorietà campo comune");
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("Nessun campo comune presente."); return;
        }
        printCommonFields();
        String name       = ui.readString("Nome del campo da modificare");
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.setCommonFieldMandatory(name, mandatory);
            save();
            ui.printSuccess("Campo '" + name + "' aggiornato: "
                    + (mandatory ? "Obbligatorio" : "Facoltativo") + ".");
        } catch (FieldNotFoundException e) { ui.printError(e.getMessage()); }
    }

    // ================================================================
    // CATEGORIES  (UC-07, 08, 09, 10) — invariati
    // ================================================================

    public void manageCategories() {
        boolean back = false;
        while (!back) {
            ui.printTitle("Gestione Categorie");
            ui.printInfo("Categorie presenti: " + categoryService.getCategories().size());
            ui.printBlank();
            ui.printMenu(
                "Crea nuova categoria",
                "Modifica campi specifici di una categoria",
                "Rimuovi categoria",
                "Visualizza categorie e campi"
            );
            switch (ui.readInt("Scelta", 0, 4)) {
                case 1 -> handleCreateCategory();
                case 2 -> handleEditSpecificFields();
                case 3 -> handleRemoveCategory();
                case 4 -> viewAllCategories();
                case 0 -> back = true;
            }
        }
    }

    private void handleCreateCategory() {
        ui.printSection("Crea nuova categoria");
        String name = ui.readString("Nome della categoria");
        try {
            Category cat = categoryService.addCategory(name);
            while (ui.readConfirm("Aggiungere un campo specifico?")) {
                handleAddSpecificField(cat);
            }
            save();
            ui.printSuccess("Categoria '" + name + "' creata con "
                    + cat.getSpecificFields().size() + " campo/i specifico/i.");
        } catch (DuplicateCategoryException e) { ui.printError(e.getMessage()); }
    }

    private void handleEditSpecificFields() {
        Category cat = pickCategory();
        if (cat == null) return;
        boolean back = false;
        while (!back) {
            ui.printSection("Categoria: " + cat.getName());
            printSpecificFields(cat);
            ui.printBlank();
            ui.printMenu(
                "Aggiungi campo specifico",
                "Rimuovi campo specifico",
                "Modifica obbligatorietà campo specifico"
            );
            switch (ui.readInt("Scelta", 0, 3)) {
                case 1 -> { handleAddSpecificField(cat); save(); }
                case 2 -> { handleRemoveSpecificField(cat); save(); }
                case 3 -> { handleToggleSpecificField(cat); save(); }
                case 0 -> back = true;
            }
        }
    }

    private void handleAddSpecificField(Category cat) {
        String name       = ui.readString("Nome del campo specifico");
        FieldType type    = ui.readFieldType();
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.addSpecificField(cat, name, type, mandatory);
            ui.printSuccess("'" + name + "' aggiunto.");
        } catch (DuplicateFieldException e) { ui.printError(e.getMessage()); }
    }

    private void handleRemoveSpecificField(Category cat) {
        if (cat.getSpecificFields().isEmpty()) {
            ui.printInfo("Nessun campo specifico."); return;
        }
        printSpecificFields(cat);
        String name = ui.readString("Nome del campo da rimuovere");
        if (!ui.readConfirm("Confermi?")) { ui.printInfo("Annullato."); return; }
        try {
            fieldService.removeSpecificField(cat, name);
            ui.printSuccess("Rimosso.");
        } catch (FieldNotFoundException e) { ui.printError(e.getMessage()); }
    }

    private void handleToggleSpecificField(Category cat) {
        if (cat.getSpecificFields().isEmpty()) {
            ui.printInfo("Nessun campo specifico."); return;
        }
        printSpecificFields(cat);
        String name       = ui.readString("Nome del campo da modificare");
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.setSpecificFieldMandatory(cat, name, mandatory);
            ui.printSuccess("Aggiornato.");
        } catch (FieldNotFoundException e) { ui.printError(e.getMessage()); }
    }

    private void handleRemoveCategory() {
        Category cat = pickCategory();
        if (cat == null) return;
        ui.printWarning("Verranno rimossi anche tutti i campi specifici della categoria.");
        if (!ui.readConfirm("Confermi la rimozione di '" + cat.getName() + "'?")) {
            ui.printInfo("Annullato."); return;
        }
        try {
            categoryService.removeCategory(cat.getName());
            save();
            ui.printSuccess("Categoria '" + cat.getName() + "' rimossa.");
        } catch (CategoryNotFoundException e) { ui.printError(e.getMessage()); }
    }

    public void viewAllCategories() {
        ui.printTitle("Riepilogo Categorie e Campi");
        ui.printSection("Campi Base (immutabili)");
        if (configService.getBaseFields().isEmpty()) ui.printInfo("(non ancora inizializzati)");
        else { printFieldHeader(); configService.getBaseFields().forEach(f -> ui.print(f.toString())); }
        ui.printBlank();
        ui.printSection("Campi Comuni");
        if (fieldService.getCommonFields().isEmpty()) ui.printInfo("(nessuno)");
        else { printFieldHeader(); fieldService.getCommonFields().forEach(f -> ui.print(f.toString())); }
        if (categoryService.getCategories().isEmpty()) {
            ui.printBlank(); ui.printInfo("Nessuna categoria definita."); return;
        }
        for (Category cat : categoryService.getCategories()) {
            ui.printBlank();
            ui.printSection("Categoria: " + cat.getName().toUpperCase());
            ui.print("  [Base]");
            configService.getBaseFields().forEach(f -> ui.print(f.toString()));
            ui.print("  [Comuni]");
            if (fieldService.getCommonFields().isEmpty()) ui.print("  (nessuno)");
            else fieldService.getCommonFields().forEach(f -> ui.print(f.toString()));
            ui.print("  [Specifici]");
            if (cat.getSpecificFields().isEmpty()) ui.print("  (nessuno)");
            else cat.getSpecificFields().forEach(f -> ui.print(f.toString()));
        }
        ui.printBlank();
    }
   
    // ================================================================
    // HELPERS PRIVATI
    // ================================================================

    private String readFieldValue(String fieldName, FieldType type, boolean mandatory) {
        String hint = " [" + type.getDisplayName()
                    + (mandatory ? ", obbligatorio" : ", facoltativo") + "]";
        return mandatory
                ? ui.readString(fieldName + hint)
                : ui.readOptionalString(fieldName + hint);
    }

    private boolean isBaseFieldName(String name) {
        return List.of("Titolo", "Numero di partecipanti",
                       "Termine ultimo di iscrizione", "Luogo", "Data",
                       "Ora", "Quota individuale", "Data conclusiva").contains(name);
    }

    private Category pickCategory() {
        List<Category> cats = categoryService.getCategories();
        if (cats.isEmpty()) {
            ui.printInfo("Nessuna categoria presente. Creane una prima.");
            return null;
        }
        ui.printSection("Seleziona categoria");
        for (int i = 0; i < cats.size(); i++)
            System.out.printf("    [%d] %s%n", i + 1, cats.get(i).getName());
        System.out.println("    [0] Annulla");
        int choice = ui.readInt("Scelta", 0, cats.size());
        return choice == 0 ? null : cats.get(choice - 1);
    }

    private void printCommonFields() {
        if (fieldService.getCommonFields().isEmpty()) ui.printInfo("(Nessun campo comune)");
        else { printFieldHeader(); fieldService.getCommonFields().forEach(f -> ui.print(f.toString())); }
    }

    private void printSpecificFields(Category cat) {
        if (cat.getSpecificFields().isEmpty()) ui.printInfo("(Nessun campo specifico)");
        else { printFieldHeader(); cat.getSpecificFields().forEach(f -> ui.print(f.toString())); }
    }

    private void printFieldHeader() {
        ui.print(String.format("  %-30s | %-15s | %s", "Nome", "Tipo", "Obbligatorio"));
        ui.print("  " + "-".repeat(60));
    }

    private void save() {
        try { pm.save(model.ApplicationState.getInstance()); }
        catch (IOException e) { ui.printError("Errore salvataggio: " + e.getMessage()); }
    }

    public Configurator getCurrentConfigurator() { return currentConfigurator; }
}