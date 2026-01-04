package app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFile {

    @JsonIgnore
    private Path fichier;

    @JsonProperty("fichier")
    private String fichierPath;

    private LocalDateTime dateMail;
    private String nom;

    /* ===================== SYNC ===================== */

    public void syncPath() {
        if (fichier != null) {
            this.fichierPath = fichier
                    .toAbsolutePath()
                    .normalize()
                    .toString();
        }
    }

    public void restorePath() {
        if (fichierPath != null) {
            this.fichier = Path.of(fichierPath)
                    .toAbsolutePath()
                    .normalize();
        }
    }

    /* ===================== TYPE ===================== */

    @JsonIgnore
    public String getType() {
        if (fichier == null) return "AUTRE";

        String name = fichier.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) return "PDF";
        if (name.endsWith(".eml")) return "EML";
        return "AUTRE";
    }

    /* ===================== IDENTITÉ ===================== */

    @JsonIgnore
    private Path identityPath() {
        if (fichier != null) {
            return fichier.toAbsolutePath().normalize();
        }
        if (fichierPath != null) {
            return Path.of(fichierPath).toAbsolutePath().normalize();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentFile other)) return false;

        Path p1 = this.identityPath();
        Path p2 = other.identityPath();

        if (p1 == null || p2 == null) return false;
        return p1.equals(p2);
    }

    @Override
    public int hashCode() {
        Path p = identityPath();
        return p == null ? 0 : p.hashCode();
    }

    @Override
    public String toString() {
        return nom;
    }
}
