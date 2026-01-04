package app.controller;

import app.model.Candidature;
import app.model.Profil;
import app.persistence.ProfilDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ProfilViewController {

    @FXML
    private TableView<Profil> profilTable;
    @FXML private TableColumn<Profil, String> colNom;
    @FXML private TableColumn<Profil, String> colDomaine;
    @FXML private TableColumn<Profil, String> colNiveau;

    @FXML private Button newProfilBtn;
    @FXML private Button editProfilBtn;
    @FXML private Button deleteProfilBtn;

    @FXML private Label lblNom;
    @FXML private Label lblDomaine;
    @FXML private Label lblNiveau;
    @FXML private Label lblCompetences;
    @FXML private Label lblMotsCles;

    @FXML private ListView<String> candidatureList;

    @FXML
    public void initialize() throws Exception {

        colNom.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getNom()));
        colDomaine.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDomaine()));
        colNiveau.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getNiveau()));

        profilTable.setItems(FXCollections.observableArrayList(ProfilDAO.findAll()));

        profilTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, p) -> {
                    try {
                        updateDetail(p);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        newProfilBtn.setOnAction(e -> {
            try {
                createProfil();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        editProfilBtn.setOnAction(e -> {
            try {
                editProfil();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        deleteProfilBtn.setOnAction(e -> {
            try {
                deleteProfil();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void updateDetail(Profil p) throws Exception {
        boolean hasSelection = p != null;
        editProfilBtn.setDisable(!hasSelection);
        deleteProfilBtn.setDisable(!hasSelection);

        if (!hasSelection) return;

        lblNom.setText("Nom : " + p.getNom());
        lblDomaine.setText("Domaine : " + p.getDomaine());
        lblNiveau.setText("Niveau : " + p.getNiveau());
        lblCompetences.setText("Compétences : " + p.getCompetences());
        lblMotsCles.setText("Mots-clés : " + p.getMotsCles());

//        candidatureList.setItems(
//                FXCollections.observableArrayList(
//                        ProfilDAO.findCandidaturesByProfil(p)
//                )
//        );

        List<String> postes = ProfilDAO.findPostesByProfil(p);
        candidatureList.getItems().setAll(postes);  // Remplit le ListView avec les postes
    }

    private void createProfil() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/profil_form.fxml"));
        Stage modal = new Stage();
        modal.setScene(new Scene(loader.load()));
        modal.setTitle("Nouveau Profil");
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setMinWidth(400);
        modal.setMinHeight(450);

        // Récupère le controller pour lui passer le stage (optionnel)
        ProfilFormController controller = loader.getController();
        controller.setStage(modal);

        modal.showAndWait();

        refresh(); // après fermeture du modal
    }


    private void editProfil() throws Exception {
        Profil p = profilTable.getSelectionModel().getSelectedItem();
        if (p == null) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/profil_form.fxml"));
        Stage modal = new Stage();
        modal.setScene(new Scene(loader.load()));
        modal.setTitle("Modifier Profil");
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setMinWidth(400);
        modal.setMinHeight(450);

        ProfilFormController controller = loader.getController();
        controller.setStage(modal);

        // Passe le profil à éditer
        controller.setProfil(p);

        modal.showAndWait();

        refresh(); // après fermeture du modal
    }


    private void deleteProfil() throws Exception {
        Profil p = profilTable.getSelectionModel().getSelectedItem();
        if (p == null) return;

        if (ProfilDAO.hasCandidatures(p)) {
            new Alert(Alert.AlertType.WARNING,
                    "Impossible de supprimer : des candidatures sont associées").showAndWait();
            return;
        }

        ProfilDAO.delete(p);
        refresh();
    }

    private void refresh() throws Exception {
        profilTable.setItems(FXCollections.observableArrayList(ProfilDAO.findAll()));
    }
}
