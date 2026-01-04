package app.model;

public enum StatutCandidature {
    EN_ATTENTE("En attente"),
    NON_RETENU("Non retenu"),
    RETENU("Retenu"),
    ACCEPTE("Accepté");

    private final String label;

    StatutCandidature(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static StatutCandidature fromLabel(String label) {
        if (label == null) return EN_ATTENTE; // fallback
        String clean = label.trim().toLowerCase().replace("_", " ");
        for (StatutCandidature s : values()) {
            if (s.label.toLowerCase().equals(clean)) return s;
        }
        throw new IllegalArgumentException("Statut inconnu : " + label);
    }


    @Override
    public String toString() {
        return label;
    }
}
