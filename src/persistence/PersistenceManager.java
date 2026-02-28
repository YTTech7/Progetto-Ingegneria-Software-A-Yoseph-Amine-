package persistence;

import model.ApplicationState;

import java.io.*;

/**
 * Handles persistent storage of ApplicationState using Java serialization.
 * The file-based approach is chosen for simplicity; future versions can
 * replace this with a JSON or DBMS implementation without changing the
 * rest of the application.
 *
 * Invariant: DATA_FILE path is always the same across calls.
 */
public class PersistenceManager {

    private static final String DATA_FILE = "appstate.dat";

    /**
     * Saves the current ApplicationState to disk.
     * @param state the state to save (non-null)
     * @pre state != null
     * @post the state is persisted on disk and can be loaded in future runs
     * @throws IOException if serialization or I/O fails
     */
    public void save(ApplicationState state) throws IOException {
        assert state != null : "State to save must not be null";
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(DATA_FILE)))) {
            oos.writeObject(state);
        }
    }

    /**
     * Loads the ApplicationState from disk.
     * @return the loaded state, or null if no saved state exists
     * @post if file exists: return value != null and is consistent
     * @throws IOException if deserialization or I/O fails
     * @throws ClassNotFoundException if the serialized class is not found
     */
    public ApplicationState load() throws IOException, ClassNotFoundException {
        File file = new File(DATA_FILE);
        if (!file.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (ApplicationState) ois.readObject();
        }
    }

    /**
     * @return true if a previously saved state file exists on disk
     */
    public boolean hasSavedState() {
        return new File(DATA_FILE).exists();
    }

    /**
     * Deletes the saved state file (useful for testing / reset).
     * @return true if the file was deleted, false if it did not exist
     */
    public boolean deleteSavedState() {
        File file = new File(DATA_FILE);
        return file.exists() && file.delete();
    }
}