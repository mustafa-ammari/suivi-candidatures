package app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Profil {

    private int id;
    private String nom;
    private String cvPath;
    private String lmPath;
    private String domaine;
    private String niveau;
    private String competences;
    private String motsCles;

    public Profil(int profilId, String pNom) {
        this.id = profilId;
        this.nom = pNom;
    }


    @Override
    public String toString() {
        return nom;
    }
}
