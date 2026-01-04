package app.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:suivi-candidature.db";

    static {
        init();
    }

    public static Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private static void init() {
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {

            // Table candidature
            st.execute("""
                CREATE TABLE IF NOT EXISTS candidature (
                    id TEXT PRIMARY KEY,
                    entreprise TEXT NOT NULL,
                    poste TEXT NOT NULL,
                    date_envoi TEXT,
                    statut TEXT,
                    dossier TEXT,
                    notes TEXT,
                    date_relance TEXT
                )
            """);

            // Table document
            st.execute("""
                CREATE TABLE IF NOT EXISTS document (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    candidature_id TEXT NOT NULL,
                    nom TEXT,
                    chemin TEXT,
                    date_mail TEXT,
                    type TEXT,
                    FOREIGN KEY (candidature_id) REFERENCES candidature(id) ON DELETE CASCADE
                )
            """);

            // Table profil
                        st.execute("""
                CREATE TABLE IF NOT EXISTS profil (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nom TEXT NOT NULL,
                    cv_path TEXT NOT NULL,
                    lm_path TEXT NOT NULL,
                    domaine TEXT,
                    niveau TEXT,
                    competences TEXT,
                    mots_cles TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Ajout du lien profil → candidature (à faire une seule fois)
            try {
                st.execute("""
                ALTER TABLE candidature
                ADD COLUMN profil_id INTEGER
                REFERENCES profil(id)
                """);
            } catch (Exception ignored) {
                // colonne déjà existante → OK
            }

        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }
}
