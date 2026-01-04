package app.controller;

import app.model.Profil;
import app.persistence.ProfilDAO;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.File;

public class ProfilFormController {

    @FXML private TextField tfNom;
    @FXML private TextField tfCV;
    @FXML private TextField tfLM;
    @FXML private TextField tfDomaine;
    @FXML private TextField tfNiveau;
    @FXML private TextField tfCompetences;
    @FXML private TextField tfMotsCles;

    @Setter
    private Stage stage; // optionnel, si tu veux fermer le modal après sauvegarde
    private Profil profil;


    @FXML
    private void onChooseCV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir le CV (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showOpenDialog(tfNom.getScene().getWindow());
        if (file != null) tfCV.setText(file.getAbsolutePath());
    }

    @FXML
    private void onChooseLM() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir la lettre de motivation (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showOpenDialog(tfNom.getScene().getWindow());
        if (file != null) tfLM.setText(file.getAbsolutePath());
    }


    @FXML
    private void onSave() throws Exception {
        if (profil == null) {
            // Création
            profil = new Profil();
        }

        // Remplir le profil avec les valeurs du formulaire
        profil.setNom(tfNom.getText());
        profil.setCvPath(tfCV.getText());
        profil.setLmPath(tfLM.getText());
        profil.setDomaine(tfDomaine.getText());
        profil.setNiveau(tfNiveau.getText());
        profil.setCompetences(tfCompetences.getText());
        profil.setMotsCles(tfMotsCles.getText());

        if (profil.getId() == 0) {
            // Nouveau profil
            ProfilDAO.insert(profil);
        } else {
            // Profil existant -> update
            ProfilDAO.update(profil);
        }

        stage.close();
    }


    public void setProfil(Profil profil) {
        this.profil = profil;
        if (profil != null) {
            // remplir les champs du formulaire avec les données existantes
            tfNom.setText(profil.getNom());
            tfCV.setText(profil.getCvPath());
            tfLM.setText(profil.getLmPath());
            tfDomaine.setText(profil.getDomaine());
            tfNiveau.setText(profil.getNiveau());
            tfCompetences.setText(profil.getCompetences());
            tfMotsCles.setText(profil.getMotsCles());
        }
    }
}
