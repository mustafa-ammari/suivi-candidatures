package app.service;

import app.model.Candidature;
import app.persistence.CandidatureDAO;

import java.time.LocalDate;
import java.util.List;

public class RapportCandidatureService {

    public List<Candidature> getByPeriod(
            LocalDate start,
            LocalDate end
    ) throws Exception {

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Période invalide");
        }

        return CandidatureDAO.findByPeriod(start, end);
    }
}
