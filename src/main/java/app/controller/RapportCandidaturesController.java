package app.controller;

import app.model.Candidature;
import app.persistence.CandidatureDAO;
import app.service.PdfExportService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RapportCandidaturesController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<Candidature> table;
    @FXML private TableColumn<Candidature, LocalDate> dateCol;
    @FXML private TableColumn<Candidature, String> entrepriseCol;
    @FXML private TableColumn<Candidature, String> posteCol;
//    @FXML private TableColumn<Candidature, String> statutCol;
//    @FXML private TableColumn<Candidature, String> profilCol;

    FileChooser chooser = new FileChooser();

    public void initialize() {

        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDateEnvoi()));

        // Optionnel : formatter l'affichage
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            }
        });


        entrepriseCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEntreprise()));
        posteCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPoste()));
//        statutCol.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().getStatut().getLabel()));
//        profilCol.setCellValueFactory(c ->
//                new SimpleStringProperty(
//                        c.getValue().getProfil() != null
//                                ? c.getValue().getProfil().getNom()
//                                : ""
//                ));

        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());

        chooser.setInitialFileName("rapport_candidatures.pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dateCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);      // 15% de la largeur totale
        entrepriseCol.setMaxWidth(1f * Integer.MAX_VALUE * 30); // 25%
        posteCol.setMaxWidth(1f * Integer.MAX_VALUE * 35);      // 25%
//        statutCol.setMaxWidth(1f * Integer.MAX_VALUE * 20);     // 20%

    }

    @FXML
    private void onFilter() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) return;

        try {
            List<Candidature> filtered = CandidatureDAO.findByPeriod(start, end);
            table.setItems(FXCollections.observableArrayList(filtered));
            table.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des candidatures").showAndWait();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onExportPdf() {
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        List<Candidature> list = table.getItems();
        if (list.isEmpty()) return;
        try {
            PdfExportService.exportCandidatures(list,
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    file
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
