package app.persistence;

import app.model.Candidature;
import app.model.StatutCandidature;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidatureDAO {

    public static List<Candidature> findByPeriod(
            LocalDate start,
            LocalDate end
    ) throws Exception {

        List<Candidature> list = new ArrayList<>();

        String sql = "SELECT * FROM candidature WHERE date_envoi >= ? AND date_envoi <= ? ORDER BY date_envoi DESC";

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, start.toString());
            ps.setString(2, end.toString());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String dateStr = rs.getString("date_envoi");
                LocalDate dateEnvoi = null;
                if (dateStr != null && !dateStr.isEmpty()) {
                    dateEnvoi = LocalDate.parse(dateStr); // conversion String → LocalDate
                }

                Candidature ca = new Candidature(
                        rs.getString("entreprise"),
                        rs.getString("poste"),
                        dateEnvoi,
                        StatutCandidature.fromLabel(rs.getString("statut")) // ou valueOf selon ton enum
                );
                list.add(ca);
            }
        }
        return list;
    }

    private static Candidature map(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getString("id"));
        c.setEntreprise(rs.getString("entreprise"));
        c.setPoste(rs.getString("poste"));
        c.setDateEnvoi(rs.getDate("date_envoi").toLocalDate());
        c.setStatut(StatutCandidature.valueOf(rs.getString("statut")));
        // charger Profil si besoin
        return c;
    }
}
