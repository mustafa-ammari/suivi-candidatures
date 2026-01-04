package app.persistence;

import app.model.Candidature;
import app.model.Profil;
import app.model.StatutCandidature;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProfilDAO {

    public static List<Candidature> findCandidaturesByProfil(Profil profil) throws Exception {

        List<Candidature> list = new ArrayList<>();

        if (profil == null || profil.getId() == 0) {
            return list;
        }

        String sql = """
        SELECT *
        FROM candidature
        WHERE profil_id = ?
        ORDER BY date_envoi DESC
    """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, profil.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidature ca = new Candidature();
                    ca.setId(String.valueOf(rs.getInt("id")));
                    ca.setEntreprise(rs.getString("entreprise"));
                    ca.setPoste(rs.getString("poste"));
                    ca.setDateEnvoi(LocalDate.parse(rs.getString("date_envoi")));
                    ca.setStatut(StatutCandidature.fromLabel(rs.getString("statut")));
                    // profil déjà connu, inutile de le recharger
                    ca.setProfil(profil);

                    list.add(ca);
                }
            }
        }

        return list;
    }

    public static List<String> findPostesByProfil(Profil profil) throws Exception {
        List<String> postes = new ArrayList<>();

        String sql = "SELECT poste, entreprise FROM candidature WHERE profil_id = ? ORDER BY date_envoi DESC";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, profil.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    postes.add(rs.getString("entreprise")  + "  -  " +   rs.getString("poste") );
                }
            }
        }

        return postes;
    }

    public static List<Profil> findAll() throws Exception {
        List<Profil> list = new ArrayList<>();

        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM profil ORDER BY nom")) {

            while (rs.next()) {
                Profil p = new Profil();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setCvPath(rs.getString("cv_path"));
                p.setLmPath(rs.getString("lm_path"));
                p.setDomaine(rs.getString("domaine"));
                p.setNiveau(rs.getString("niveau"));
                p.setCompetences(rs.getString("competences"));
                p.setMotsCles(rs.getString("mots_cles"));
                list.add(p);
            }
        }
        return list;
    }

    public static void insert(Profil p) throws Exception {
        String sql = """
            INSERT INTO profil
            (nom, cv_path, lm_path, domaine, niveau, competences, mots_cles)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getCvPath());
            ps.setString(3, p.getLmPath());
            ps.setString(4, p.getDomaine());
            ps.setString(5, p.getNiveau());
            ps.setString(6, p.getCompetences());
            ps.setString(7, p.getMotsCles());

            ps.executeUpdate();
        }
    }

    public static void update(Profil p) throws Exception {
        String sql = """
        UPDATE profil
        SET nom = ?, cv_path = ?, lm_path = ?, domaine = ?, niveau = ?, competences = ?, mots_cles = ?
        WHERE id = ?
    """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getCvPath());
            ps.setString(3, p.getLmPath());
            ps.setString(4, p.getDomaine());
            ps.setString(5, p.getNiveau());
            ps.setString(6, p.getCompetences());
            ps.setString(7, p.getMotsCles());
            ps.setInt(8, p.getId());

            ps.executeUpdate();
        }
    }

    public static boolean hasCandidatures(Profil profil) throws Exception {

        String sql = "SELECT COUNT(*) FROM candidature WHERE profil_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, profil.getId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public static void delete(Profil profil) throws Exception {

        String sql = "DELETE FROM profil WHERE id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, profil.getId());
            ps.executeUpdate();
        }
    }


}
