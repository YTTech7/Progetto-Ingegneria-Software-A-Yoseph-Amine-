package view;

import model.FieldType;

import java.util.Scanner;

/**
 * Utility class providing formatted console output and safe input reading.
 * Centralises all I/O so that replacing the console with a GUI in future
 * versions requires modifying only this layer.
 */
public class ConsoleUI {

    private final Scanner scanner;

    private static final String SEPARATOR =
            "════════════════════════════════════════════════════════════";
    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────────";

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    // ----------------------------------------------------------------
    // OUTPUT HELPERS
    // ----------------------------------------------------------------

    public void printSeparator() { System.out.println(SEPARATOR); }
    public void printThinSeparator() { System.out.println(THIN_SEP); }
    public void printBlank() { System.out.println(); }

    public void printTitle(String text) {
        printSeparator();
        System.out.println("  " + text.toUpperCase());
        printSeparator();
    }

    public void printSection(String text) {
        printThinSeparator();
        System.out.println("  » " + text);
        printThinSeparator();
    }

    public void printInfo(String msg)    { System.out.println("  [i] " + msg); }
    public void printSuccess(String msg) { System.out.println("  [✓] " + msg); }
    public void printError(String msg)   { System.out.println("  [✗] " + msg); }
    public void printWarning(String msg) { System.out.println("  [!] " + msg); }
    public void print(String msg)        { System.out.println("      " + msg); }

    public void printMenu(String... options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("    [%d] %s%n", i + 1, options[i]);
        }
        System.out.println("    [0] Indietro / Esci");
    }

    // ----------------------------------------------------------------
    // INPUT HELPERS
    // ----------------------------------------------------------------

    /**
     * Prompts for a non-blank string. Keeps asking until one is provided.
     */
    public String readString(String prompt) {
        while (true) {
            System.out.print("  > " + prompt + ": ");
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            printError("Il valore non può essere vuoto. Riprova.");
        }
    }

    /**
     * Prompts for a string that may be blank (returns empty string if blank).
     */
    public String readOptionalString(String prompt) {
        System.out.print("  > " + prompt + " (invio per saltare): ");
        return scanner.nextLine().trim();
    }

    /**
     * Prompts for an integer in [min, max]. Keeps asking until valid.
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print("  > " + prompt + " (" + min + "-" + max + "): ");
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                printError("Inserire un numero tra " + min + " e " + max + ".");
            } catch (NumberFormatException e) {
                printError("Inserire un numero intero valido.");
            }
        }
    }

    /**
     * Prompts for Y/N confirmation. Returns true for Y.
     */
    public boolean readConfirm(String prompt) {
        while (true) {
            System.out.print("  > " + prompt + " (s/n): ");
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.equals("s") || line.equals("si") || line.equals("y") || line.equals("yes")) return true;
            if (line.equals("n") || line.equals("no")) return false;
            printError("Rispondere con 's' oppure 'n'.");
        }
    }

    /**
     * Asks user to pick a FieldType from the available options.
     */
    public FieldType readFieldType() {
        printSection("Scegli il tipo di dato del campo");
        FieldType[] types = FieldType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("    [%d] %s%n", i + 1, types[i].getDisplayName());
        }
        int choice = readInt("Tipo", 1, types.length);
        return types[choice - 1];
    }

    /**
     * Asks user to pick mandatory status.
     */
    public boolean readMandatory() {
        System.out.println("    [1] Obbligatorio");
        System.out.println("    [2] Facoltativo");
        int choice = readInt("Obbligatorietà", 1, 2);
        return choice == 1;
    }

    public Scanner getScanner() { return scanner; }
}
