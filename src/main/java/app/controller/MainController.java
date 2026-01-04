package app.controller;

import app.model.Candidature;
import app.model.DocumentFile;
import app.persistence.CandidatureEditData;
import app.service.DatabaseService;
import app.service.FileSystemService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    @Getter
    private ObservableList<Candidature> candidatures = FXCollections.observableArrayList();

    private TableView<Candidature> table = new TableView<>();

    public MainController(TableView<Candidature> t) {
        loadCandidaturesFromDB();
        this.table.setItems(candidatures);
    }

    public void loadCandidaturesFromDB() {
        candidatures.clear();
        List<Candidature> all = DatabaseService.loadAllCandidatures();
        candidatures.addAll(all);
        sortByDateDesc();
    }

    public void saveCandidature(Candidature c) {
        DatabaseService.saveCandidature(c);
    }

    private void sortByDateDesc() {
        FXCollections.sort(candidatures, (a, b) -> {
            LocalDate d1 = a.getDateEnvoi();
            LocalDate d2 = b.getDateEnvoi();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1); // Descendant
        });
    }

    public void createCandidature(Candidature c) {
        try {
            // 1️⃣ Créer le dossier avec date + entreprise + poste
            String safeName = (c.getDateEnvoi() + "_" + c.getEntreprise() + "_" + c.getPoste())
                    .replaceAll("\\W+", "_");

            Path folder = FileSystemService.getCandidatureRoot().resolve(safeName);
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
            c.setDossier(folder);

            // 2️⃣ Sauvegarder candidature en base
            DatabaseService.saveCandidature(c);

            // 3️⃣ Rafraîchir la table
            table.getItems().add(c);
            table.getSelectionModel().select(c);
            table.scrollTo(c);

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la création de la candidature").showAndWait();
        }
    }

    public void updateCandidature(Candidature c, CandidatureEditData data) {
        try {
            // 1️⃣ Mettre à jour les champs
            c.setEntreprise(data.entreprise());
            c.setPoste(data.poste());
            c.setDateEnvoi(data.dateEnvoi());
            c.setStatut(data.statut());
            c.setProfil(data.profil());

            // 2️⃣ Renommer le dossier si nécessaire
            Path oldFolder = c.getDossier();

            // Nouveau nom basé sur date + entreprise + poste
            String safeName = (c.getDateEnvoi() + "_" + c.getEntreprise() + "_" + c.getPoste())
                    .replaceAll("\\W+", "_");
            Path newFolder = oldFolder.getParent().resolve(safeName);

            if (!oldFolder.equals(newFolder)) {
                // Crée le dossier si inexistant
                if (!Files.exists(newFolder)) {
                    Files.createDirectories(newFolder);
                }

                // Déplacer tous les fichiers PDF vers le nouveau dossier
                for (DocumentFile doc : c.getDocuments()) {
                    Path oldPath = doc.getFichier();
                    if (oldPath != null && Files.exists(oldPath)) {
                        Path newPath = newFolder.resolve(oldPath.getFileName());
                        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                        doc.setFichier(newPath);
                    }
                }

                // Supprimer l’ancien dossier si vide
                try {
                    Files.deleteIfExists(oldFolder);
                } catch (DirectoryNotEmptyException ignored) {}

                // Mettre à jour le chemin du dossier
                c.setDossier(newFolder);
            }

            // 3️⃣ Sauvegarder candidature et documents en base
            DatabaseService.saveDocuments(c);
            DatabaseService.saveCandidature(c);

            // 4️⃣ Rafraîchir interface
            table.refresh();
            table.getSelectionModel().select(c);
            table.scrollTo(c);

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour de la candidature et du dossier").showAndWait();
        }
    }

    public void deleteCandidature(Candidature c) throws IOException {
        if (c.getDossier() != null && Files.exists(c.getDossier())) {
            FileSystemService.deleteRecursively(c.getDossier());
        }
        DatabaseService.deleteCandidature(c);

        table.getItems().remove(c);

    }

    private void handleCandidatureUpdated(Candidature c) {
    }

    private void refreshTable(Candidature c) {
        table.refresh();
        table.getSortOrder().setAll(table.getColumns().get(0)); // trier par date ou autre
        table.getSelectionModel().select(c);
        table.scrollTo(c);
    }

    private void showError(String message, Exception e) {
        new Alert(Alert.AlertType.ERROR, message + "\n" + e.getMessage()).showAndWait();
    }

    //    public void createCandidature(Candidature c) {
//        String folderName = generateFolderName(c);
//        Path newFolder = FileSystemService.createCandidatureFolder(folderName);
//        c.setDossier(newFolder);
//
//        // Crée dossier
    ////        String safeName = (c.getEntreprise() + "_" + c.getPoste()).replaceAll("\\W+", "_");
    ////        Path folder = FileSystemService.createCandidatureFolder(safeName);
    ////        c.setDossier(folder);
//
//        // Sauvegarde en base
//        DatabaseService.saveCandidature(c);
//
//        // Rafraîchir table
//        table.getItems().add(c);
//        refreshTable(c);
//
//    }

    public static String generateFolderName(Candidature c) {
        String dateStr = "";
        if (c.getDateEnvoi() != null) {
            dateStr = c.getDateEnvoi().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        }

        String entreprise = c.getEntreprise() != null ? c.getEntreprise().replaceAll("\\W+", "_") : "Entreprise";
        String poste = c.getPoste() != null ? c.getPoste().replaceAll("\\W+", "_") : "Poste";

        if (!dateStr.isEmpty()) {
            return dateStr + "_" + entreprise + "_" + poste;
        } else {
            return entreprise + "_" + poste;
        }
    }

    private Path generateCandidatureFolderName(Candidature c) {
        String dateStr = c.getDateEnvoi() != null
                ? c.getDateEnvoi().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));

        String entreprise = c.getEntreprise().replaceAll("[\\\\/:*?\"<>|]", "").trim();
        String poste = c.getPoste().replaceAll("[\\\\/:*?\"<>|]", "").trim();

        String folderName = dateStr + "_" + entreprise + "_" + poste;
        return FileSystemService.getCandidatureRoot().resolve(folderName);
    }

}
