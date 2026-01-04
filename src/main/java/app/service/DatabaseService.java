package app.service;

import app.model.Candidature;
import app.model.DocumentFile;
import app.model.Profil;
import app.model.StatutCandidature;
import app.persistence.Database;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class DatabaseService {

    private DatabaseService() {}

    /* ===================== CANDIDATURE ===================== */

    public static List<Candidature> loadAllCandidatures() {
        List<Candidature> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT \n" +
                             "    c.*,\n" +
                             "    p.id            AS p_id,\n" +
                             "    p.nom           AS p_nom,\n" +
                             "    p.cv_path,\n" +
                             "    p.lm_path,\n" +
                             "    p.domaine,\n" +
                             "    p.niveau,\n" +
                             "    p.competences,\n" +
                             "    p.mots_cles\n" +
                             "FROM candidature c\n" +
                             "LEFT JOIN profil p ON c.profil_id = p.id\n")) {
//             ResultSet rs = st.executeQuery("SELECT * FROM candidature")) {

            while (rs.next()) {
                Candidature c = new Candidature();
                c.setId(rs.getString("id"));
                c.setEntreprise(rs.getString("entreprise"));
                c.setPoste(rs.getString("poste"));
                c.setDateEnvoi(rs.getString("date_envoi") != null ? LocalDate.parse(rs.getString("date_envoi")) : null);
                c.setStatut(rs.getString("statut") != null ? StatutCandidature.fromLabel(rs.getString("statut")) : null);
                c.setDossier(rs.getString("dossier") != null ? Path.of(rs.getString("dossier")) : null);
                c.setNotes(rs.getString("notes"));
                c.setDateRelance(rs.getString("date_relance") != null ? LocalDate.parse(rs.getString("date_relance")) : null);
                // Charger les documents liés
                c.setDocuments(loadDocuments(c.getId()));
                Profil profil = null;
                int profilId = rs.getInt("p_id");
                if (!rs.wasNull()) {
                    profil = new Profil(
                            profilId,
                            rs.getString("p_nom"),
                            rs.getString("cv_path"),
                            rs.getString("lm_path"),
                            rs.getString("domaine"),
                            rs.getString("niveau"),
                            rs.getString("competences"),
                            rs.getString("mots_cles")
                    );
                }

                c.setProfil(profil);
                list.add(c);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement candidatures", e);
        }

        // Tri descendant sur la date
        list.sort((a,b) -> {
            if(a.getDateEnvoi() == null && b.getDateEnvoi() == null) return 0;
            if(a.getDateEnvoi() == null) return 1;
            if(b.getDateEnvoi() == null) return -1;
            return b.getDateEnvoi().compareTo(a.getDateEnvoi());
        });

        return list;
    }

    public static void saveCandidature(Candidature c) {
        if (c.getId() == null) c.setId(UUID.randomUUID().toString());

        String sqlInsert = """
            INSERT OR REPLACE INTO candidature
            (id, entreprise, poste, date_envoi, statut, dossier, notes, date_relance, profil_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

            ps.setString(1, c.getId());
            ps.setString(2, c.getEntreprise());
            ps.setString(3, c.getPoste());
            ps.setString(4, c.getDateEnvoi() != null ? c.getDateEnvoi().toString() : null);
            ps.setString(5, c.getStatut() != null ? c.getStatut().getLabel() : null);
            ps.setString(6, c.getDossier() != null ? c.getDossier().toString() : null);
            ps.setString(7, c.getNotes());
            ps.setString(8, c.getDateRelance() != null ? c.getDateRelance().toString() : null);

            // 🔥 LA LIGNE QUI MANQUAIT
            if (c.getProfil() != null) {
                ps.setInt(9, c.getProfil().getId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.executeUpdate();

            // Sauvegarder les documents associés
            saveDocuments(c);

        } catch (Exception e) {
            throw new RuntimeException("Impossible de sauvegarder la candidature", e);
        }
    }

    public static void deleteCandidature(Candidature c) {
        if (c == null || c.getId() == null) return;

        String sqlDeleteDocs = "DELETE FROM document WHERE candidature_id = ?";
        String sqlDeleteCandidature = "DELETE FROM candidature WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            // Supprimer les documents liés
            try (PreparedStatement psDocs = conn.prepareStatement(sqlDeleteDocs)) {
                psDocs.setString(1, c.getId());
                psDocs.executeUpdate();
            }

            // Supprimer la candidature
            try (PreparedStatement psCand = conn.prepareStatement(sqlDeleteCandidature)) {
                psCand.setString(1, c.getId());
                psCand.executeUpdate();
            }

        } catch (Exception e) {
            throw new RuntimeException("Impossible de supprimer la candidature", e);
        }
    }

    /* ===================== DOCUMENTS ===================== */

    private static List<DocumentFile> loadDocuments(String candidatureId) {
        List<DocumentFile> docs = new ArrayList<>();
        String sql = "SELECT * FROM document WHERE candidature_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, candidatureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentFile doc = new DocumentFile();
                    doc.setFichier(Path.of(rs.getString("chemin")));
                    doc.setNom(rs.getString("nom"));
                    doc.setDateMail(rs.getString("date_mail") != null ? LocalDateTime.parse(rs.getString("date_mail")) : null);
                    docs.add(doc);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger les documents", e);
        }
        return docs;
    }

    public static void saveDocuments(Candidature c) {
        if (c.getDocuments() == null || c.getId() == null) return;

        String sqlInsert = """
            INSERT INTO document
            (candidature_id, nom, chemin, date_mail, type)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {

            // Supprimer les doublons existants pour éviter les répétitions
            Set<String> cheminsExistants = loadDocuments(c.getId())
                    .stream()
                    .map(d -> d.getFichier().toAbsolutePath().normalize().toString())
                    .collect(Collectors.toSet());

            for (DocumentFile doc : c.getDocuments()) {
                String docPath = doc.getFichier().toAbsolutePath().normalize().toString();
                if (cheminsExistants.contains(docPath)) continue; // Ignorer doublon

                ps.setString(1, c.getId());
                ps.setString(2, doc.getNom());
                ps.setString(3, docPath);
                ps.setString(4, doc.getDateMail() != null ? doc.getDateMail().toString() : null);
                ps.setString(5, doc.getType());
                ps.addBatch();

                cheminsExistants.add(docPath);
            }

            ps.executeBatch();

        } catch (Exception e) {
            throw new RuntimeException("Impossible de sauvegarder les documents", e);
        }
    }

    /* ===================== MISE À JOUR DATE ===================== */

    /**
     * Met à jour la dateEnvoi d'une candidature si le dossier est renommé
     */
    public static void updateDateEnvoi(Candidature c, LocalDate nouvelleDate) {
        if (c == null || c.getId() == null || nouvelleDate == null) return;

        c.setDateEnvoi(nouvelleDate);

        String sql = "UPDATE candidature SET date_envoi = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nouvelleDate.toString());
            ps.setString(2, c.getId());
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Impossible de mettre à jour la dateEnvoi", e);
        }
    }
}
