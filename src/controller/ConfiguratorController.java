package controller;

import exception.*;
import model.Category;
import model.CommonField;
import model.Configurator;
import model.FieldType;
import persistence.PersistenceManager;
import service.*;
import view.ConsoleUI;

import java.io.IOException;
import java.util.List;

/**
 * ConfiguratorController — orchestrates user interactions for the configurator role.
 *
 * Dependency rule:
 *   Controller → Service (only)
 *   Controller does NOT import or reference ApplicationState directly.
 *
 * The controller's sole jobs are:
 *  1. Drive the UI flow (read input, show output)
 *  2. Call the appropriate Service method
 *  3. Catch domain exceptions and translate them into user-friendly messages
 *  4. Trigger persistence after every successful state mutation
 *
 * Invariant: all injected dependencies are non-null.
 */
public class ConfiguratorController {

    // ---- dependencies (injected via constructor) ----
    private final ConsoleUI             ui;
    private final AuthService           authService;
    private final ConfigurationService  configService;
    private final FieldService          fieldService;
    private final CategoryService       categoryService;
    private final PersistenceManager    pm;

    // ---- session state ----
    private Configurator currentConfigurator;

    /**
     * @pre all parameters non-null
     */
    public ConfiguratorController(ConsoleUI ui,
                                   AuthService authService,
                                   ConfigurationService configService,
                                   FieldService fieldService,
                                   CategoryService categoryService,
                                   PersistenceManager pm) {
        assert ui != null && authService != null && configService != null
            && fieldService != null && categoryService != null && pm != null;
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
     * Handles the full login flow.
     * @return true if the configurator is authenticated and ready to operate
     * @post if return true: currentConfigurator != null
     */
    public boolean login() {
        ui.printTitle("Accesso Configuratore");

        if (authService.isFirstEverLaunch()) {
            ui.printInfo("Primo avvio — usa le credenziali predefinite:");
            ui.printInfo("  utente: " + model.ApplicationState.DEFAULT_USERNAME
                       + "  password: " + model.ApplicationState.DEFAULT_PASSWORD);
            ui.printBlank();
        }

        String username = ui.readString("Username");
        String password = ui.readString("Password");

        // First-ever launch: accept default credentials and open registration
        if (authService.isFirstEverLaunch()
                && authService.isDefaultCredentials(username, password)) {
            ui.printSuccess("Credenziali predefinite accettate.");
            Configurator pending = authService.createPendingConfigurator();
            return runRegistration(pending);
        }

        // Normal authentication
        try {
            Configurator c = authService.authenticate(username, password);
            if (c.isFirstLogin()) {
                ui.printWarning("Registrazione incompleta. Scegli le credenziali personali.");
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

    /** Collects and sets personal credentials for a pending configurator. */
    private boolean runRegistration(Configurator pending) {
        ui.printSection("Scegli le tue credenziali personali");
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
                ui.printSuccess("Registrazione completata. Benvenuto, " + pending.getUsername() + "!");
                currentConfigurator = pending;
                return true;
            } catch (DuplicateUsernameException e) {
                ui.printError(e.getMessage());
                // loop: ask again
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

    /** Initialises base fields on first login; skips silently on subsequent ones. */
    public void initBaseFieldsIfNeeded() {
        if (configService.areBaseFieldsInitialised()) return;
        ui.printSection("Inizializzazione campi base (una tantum)");
        try {
            configService.initBaseFields();
            save();
            ui.printSuccess("Otto campi base inizializzati e bloccati definitivamente.");
        } catch (BaseFieldsAlreadyInitializedException e) {
            ui.printInfo(e.getMessage()); // should never happen here, but safe
        }
    }

    // ================================================================
    // COMMON FIELDS  (UC-04)
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
                "Modifica obbligatorietà di un campo comune"
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
        String name      = ui.readString("Nome del campo");
        FieldType type   = ui.readFieldType();
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.addCommonField(name, type, mandatory);
            save();
            ui.printSuccess("Campo comune '" + name + "' aggiunto.");
        } catch (DuplicateFieldException e) {
            ui.printError(e.getMessage());
        }
    }

    private void handleRemoveCommonField() {
        ui.printSection("Rimuovi campo comune");
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("Nessun campo comune presente.");
            return;
        }
        printCommonFields();
        String name = ui.readString("Nome del campo da rimuovere");
        if (!ui.readConfirm("Confermi la rimozione di '" + name + "'?")) {
            ui.printInfo("Annullato.");
            return;
        }
        try {
            fieldService.removeCommonField(name);
            save();
            ui.printSuccess("Campo comune '" + name + "' rimosso.");
        } catch (FieldNotFoundException e) {
            ui.printError(e.getMessage());
        }
    }

    private void handleToggleCommonField() {
        ui.printSection("Modifica obbligatorietà campo comune");
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("Nessun campo comune presente.");
            return;
        }
        printCommonFields();
        String name       = ui.readString("Nome del campo da modificare");
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.setCommonFieldMandatory(name, mandatory);
            save();
            ui.printSuccess("Campo '" + name + "' aggiornato: "
                    + (mandatory ? "Obbligatorio" : "Facoltativo") + ".");
        } catch (FieldNotFoundException e) {
            ui.printError(e.getMessage());
        }
    }

    // ================================================================
    // CATEGORIES  (UC-05, UC-06, UC-07, UC-08)
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

    /** UC-05 */
    private void handleCreateCategory() {
        ui.printSection("Crea nuova categoria");
        String name = ui.readString("Nome della categoria");
        try {
            Category cat = categoryService.addCategory(name);  // pure domain call
            // Optionally add specific fields right away
            while (ui.readConfirm("Aggiungere un campo specifico?")) {
                handleAddSpecificField(cat);
            }
            save();
            ui.printSuccess("Categoria '" + name + "' creata con "
                    + cat.getSpecificFields().size() + " campo/i specifico/i.");
        } catch (DuplicateCategoryException e) {
            ui.printError(e.getMessage());
        }
    }

    /** UC-06 */
    private void handleEditSpecificFields() {
        Category cat = pickCategory();
        if (cat == null) return;

        boolean back = false;
        while (!back) {
            ui.printBlank();
            ui.printSection("Categoria: " + cat.getName());
            printSpecificFields(cat);
            ui.printBlank();
            ui.printMenu(
                "Aggiungi campo specifico",
                "Rimuovi campo specifico",
                "Modifica obbligatorietà campo specifico"
            );
            switch (ui.readInt("Scelta", 0, 3)) {
                case 1 -> { handleAddSpecificField(cat);    save(); }
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
            ui.printSuccess("Campo specifico '" + name + "' aggiunto.");
        } catch (DuplicateFieldException e) {
            ui.printError(e.getMessage());
        }
    }

    private void handleRemoveSpecificField(Category cat) {
        if (cat.getSpecificFields().isEmpty()) {
            ui.printInfo("Nessun campo specifico presente.");
            return;
        }
        printSpecificFields(cat);
        String name = ui.readString("Nome del campo da rimuovere");
        if (!ui.readConfirm("Confermi?")) { ui.printInfo("Annullato."); return; }
        try {
            fieldService.removeSpecificField(cat, name);
            ui.printSuccess("Campo specifico '" + name + "' rimosso.");
        } catch (FieldNotFoundException e) {
            ui.printError(e.getMessage());
        }
    }

    private void handleToggleSpecificField(Category cat) {
        if (cat.getSpecificFields().isEmpty()) {
            ui.printInfo("Nessun campo specifico presente.");
            return;
        }
        printSpecificFields(cat);
        String name       = ui.readString("Nome del campo da modificare");
        boolean mandatory = ui.readMandatory();
        try {
            fieldService.setSpecificFieldMandatory(cat, name, mandatory);
            ui.printSuccess("Aggiornato.");
        } catch (FieldNotFoundException e) {
            ui.printError(e.getMessage());
        }
    }

    /** UC-07 */
    private void handleRemoveCategory() {
        Category cat = pickCategory();
        if (cat == null) return;
        ui.printWarning("Verranno rimossi anche tutti i campi specifici della categoria.");
        if (!ui.readConfirm("Confermi la rimozione di '" + cat.getName() + "'?")) {
            ui.printInfo("Annullato.");
            return;
        }
        try {
            categoryService.removeCategory(cat.getName());
            save();
            ui.printSuccess("Categoria '" + cat.getName() + "' rimossa.");
        } catch (CategoryNotFoundException e) {
            ui.printError(e.getMessage()); // shouldn't happen since we just picked it
        }
    }

    /** UC-08 */
    public void viewAllCategories() {
        ui.printTitle("Riepilogo Categorie e Campi");

        ui.printSection("Campi Base (immutabili, comuni a tutte le categorie)");
        if (configService.getBaseFields().isEmpty()) {
            ui.printInfo("(non ancora inizializzati)");
        } else {
            printFieldHeader();
            configService.getBaseFields().forEach(f -> ui.print(f.toString()));
        }

        ui.printBlank();
        ui.printSection("Campi Comuni");
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("(nessuno)");
        } else {
            printFieldHeader();
            fieldService.getCommonFields().forEach(f -> ui.print(f.toString()));
        }

        if (categoryService.getCategories().isEmpty()) {
            ui.printBlank();
            ui.printInfo("Nessuna categoria definita.");
            return;
        }

        for (Category cat : categoryService.getCategories()) {
            ui.printBlank();
            ui.printSection("Categoria: " + cat.getName().toUpperCase());
            ui.print("  [Campi base]");
            configService.getBaseFields().forEach(f -> ui.print(f.toString()));
            ui.print("  [Campi comuni]");
            if (fieldService.getCommonFields().isEmpty()) ui.print("  (nessuno)");
            else fieldService.getCommonFields().forEach(f -> ui.print(f.toString()));
            ui.print("  [Campi specifici]");
            if (cat.getSpecificFields().isEmpty()) ui.print("  (nessuno)");
            else cat.getSpecificFields().forEach(f -> ui.print(f.toString()));
        }
        ui.printBlank();
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /** Prompts user to select a category from the list. Returns null on cancel. */
    private Category pickCategory() {
        List<Category> cats = categoryService.getCategories();
        if (cats.isEmpty()) {
            ui.printInfo("Nessuna categoria presente.");
            return null;
        }
        ui.printSection("Seleziona categoria");
        for (int i = 0; i < cats.size(); i++) {
            System.out.printf("    [%d] %s%n", i + 1, cats.get(i).getName());
        }
        System.out.println("    [0] Annulla");
        int choice = ui.readInt("Scelta", 0, cats.size());
        return choice == 0 ? null : cats.get(choice - 1);
    }

    private void printCommonFields() {
        if (fieldService.getCommonFields().isEmpty()) {
            ui.printInfo("(Nessun campo comune)");
        } else {
            printFieldHeader();
            fieldService.getCommonFields().forEach(f -> ui.print(f.toString()));
        }
    }

    private void printSpecificFields(Category cat) {
        if (cat.getSpecificFields().isEmpty()) {
            ui.printInfo("(Nessun campo specifico)");
        } else {
            printFieldHeader();
            cat.getSpecificFields().forEach(f -> ui.print(f.toString()));
        }
    }

    private void printFieldHeader() {
        ui.print(String.format("  %-30s | %-15s | %-12s | %s",
                "Nome", "Tipo", "Obbligatorio", "Tipo campo"));
        ui.print("  " + "-".repeat(74));
    }

    private void save() {
        try {
            // Controller asks PersistenceManager to save the state,
            // but gets the state via the service (not directly).
            // We need the raw state here — this is the ONE allowed coupling
            // between controller and state, only for persistence triggering.
            pm.save(model.ApplicationState.getInstance());
        } catch (IOException e) {
            ui.printError("Errore durante il salvataggio: " + e.getMessage());
        }
    }

    public Configurator getCurrentConfigurator() { return currentConfigurator; }
}