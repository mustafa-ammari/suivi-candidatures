package app.persistence;

import app.model.Profil;
import app.model.StatutCandidature;

import java.time.LocalDate;

public record CandidatureEditData(
        LocalDate dateEnvoi,
        String entreprise,
        String poste,
        StatutCandidature statut,
        Profil profil
) {}

