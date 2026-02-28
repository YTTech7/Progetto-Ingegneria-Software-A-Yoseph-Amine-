package it.unibs.principale;

import controller.ConfiguratorController;
import model.ApplicationState;
import persistence.PersistenceManager;
import service.*;
import view.ConsoleUI;

import java.io.IOException;

/**
 * Main — wires all layers together and starts the application.
 *
 * Dependency graph (all arrows point downward):
 *
 *   Main
 *    └─ ConfiguratorController
 *         ├─ AuthService          ──┐
 *         ├─ ConfigurationService ──┤──> ApplicationState
 *         ├─ FieldService         ──┤
 *         ├─ CategoryService      ──┘
 *         ├─ PersistenceManager   ──>  disk (appstate.dat)
 *         └─ ConsoleUI            ──>  System.in / System.out
 *
 * ApplicationState is created/loaded ONCE here and injected into every Service.
 * No class other than Main and PersistenceManager ever calls
 * ApplicationState.getInstance() or ApplicationState.setInstance().
 */
public class Main {

    public static void main(String[] args) {

        ConsoleUI ui = new ConsoleUI();
        PersistenceManager pm = new PersistenceManager();

        // ---- 1. Load or create ApplicationState ----
        try {
            ApplicationState loaded = pm.load();
            if (loaded != null) {
                ApplicationState.setInstance(loaded);
                ui.printInfo("Stato precedente caricato.");
            } else {
                ui.printInfo("Nessuno stato salvato. Primo avvio.");
            }
        } catch (IOException | ClassNotFoundException e) {
            ui.printError("Impossibile caricare lo stato: " + e.getMessage());
            ui.printWarning("Avvio con stato vuoto.");
        }

        ApplicationState state = ApplicationState.getInstance();

        // ---- 2. Build Services (each gets the single ApplicationState) ----
        AuthService          authService    = new AuthService(state);
        ConfigurationService configService  = new ConfigurationService(state);
        FieldService         fieldService   = new FieldService(state);
        CategoryService      categoryService = new CategoryService(state);

        // ---- 3. Build Controller (gets only Services + UI + PM) ----
        ConfiguratorController controller = new ConfiguratorController(
                ui, authService, configService, fieldService, categoryService, pm);

        // ---- 4. Application loop ----
        boolean running = true;
        while (running) {
            ui.printTitle("Sistema Gestione Iniziative Ricreative — Versione 1");

            if (!controller.login()) {
                ui.printError("Accesso non riuscito.");
                if (!ui.readConfirm("Riprovare?")) running = false;
                continue;
            }

            // Ensure base fields are initialised (idempotent)
            controller.initBaseFieldsIfNeeded();

            // ---- Session menu ----
            boolean sessionActive = true;
            while (sessionActive) {
                ui.printTitle("Menu Principale — Configuratore: "
                        + controller.getCurrentConfigurator().getUsername());
                ui.printMenu(
                    "Gestione Campi Comuni",
                    "Gestione Categorie",
                    "Visualizza tutte le categorie e i campi"
                );
                switch (ui.readInt("Scelta", 0, 3)) {
                    case 1 -> controller.manageCommonFields();
                    case 2 -> controller.manageCategories();
                    case 3 -> controller.viewAllCategories();
                    case 0 -> {
                        controller.logout();
                        sessionActive = false;
                        if (!ui.readConfirm("Accedere con un altro account?")) {
                            running = false;
                        }
                    }
                }
            }
        }

        ui.printTitle("Arrivederci!");
    }
}