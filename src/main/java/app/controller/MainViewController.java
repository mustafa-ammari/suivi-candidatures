package app.controller;

import app.PdfViewerPane;
import app.model.Candidature;
import app.model.DocumentFile;
import app.model.Profil;
import app.model.StatutCandidature;
import app.persistence.CandidatureEditData;
import app.persistence.ProfilDAO;
import app.service.CandidatureService;
import app.service.DatabaseService;
import app.service.FileSystemService;
import app.service.PdfImportService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainViewController {

    @FXML private TableView<Candidature> table;
    @FXML private BorderPane rootPane;
    @FXML private TableColumn<Candidature, Number> colIndex;
    @FXML private TableColumn<Candidature, LocalDate> colDate;
    @FXML private TableColumn<Candidature, String> colEntreprise;
    @FXML private TableColumn<Candidature, String> colPoste;
    @FXML private TableColumn<Candidature, String> colStatut;
    @FXML private TableColumn<Candidature, String> colProfil;

    @FXML private TableColumn<Candidature, String> colNotes;
    @FXML private TableColumn<Candidature, LocalDate> colRelance;
    @FXML private TableColumn<Candidature, String> colElapsed;

    @FXML private VBox pdfViewerContainer;

    @FXML private TextField searchField;
    @FXML private ChoiceBox<StatutCandidature> statutFilter;
    @FXML private ChoiceBox<String> moisFilter;
    @FXML private CheckBox pdfFilter;
    @FXML private CheckBox responseFilter;
    @FXML private ToggleButton themeBtn;

    @FXML private Button addBtn;
    @FXML public Button profilBtn;
    @FXML public Button rapportBtn;
    @FXML private Button statBtn;
    @FXML private Button dashBtn;

    private MainController controller;
    private PdfViewerPane pdfViewerPane;

    private FilteredList<Candidature> filteredCandidatures;
    private SortedList<Candidature> sortedCandidatures;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private final Pattern dateTimePattern = Pattern.compile(
            "(\\d{1,2}\\s(?:janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)\\s\\d{4}\\sà\\s\\d{1,2}:\\d{2})",
            Pattern.CASE_INSENSITIVE);

    @FXML
    public void initialize() {

        FileSystemService.getBaseCandidatureFolder();

        initTableColumns();
        initContextMenu();

        controller = new MainController(table);
        controller.loadCandidaturesFromDB();

        filteredCandidatures = new FilteredList<>(controller.getCandidatures(), c -> true);
        sortedCandidatures = new SortedList<>(filteredCandidatures);
        sortedCandidatures.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedCandidatures);

        if (!sortedCandidatures.isEmpty()) {
            Platform.runLater(() -> {
                table.getSelectionModel().selectFirst();
                table.scrollTo(0);
            });
        }

        pdfViewerPane = new PdfViewerPane(null, controller);
        pdfViewerContainer.getChildren().add(pdfViewerPane);


        initFilters();
        initActions();
        initSelectionListener();

//        controller.getCandidatures().forEach(c ->
//                System.out.println(STR."\{c.getEntreprise()} -> \{c.getProfil() != null ? c.getProfil().getNom() : "AUCUN PROFIL"}")
//        );

        setupPdfViewerContextMenu();

        Platform.runLater(this::setupDragAndDrop);
    }

    private void setupDragAndDrop() {
            // Drag & Drop pour importer un PDF sur la candidature sélectionnée
            rootPane.setOnDragOver(event -> {
                Candidature selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });

            rootPane.setOnDragDropped(event -> {
                Candidature selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                var db = event.getDragboard();
                if (!db.hasFiles()) return;

                rootPane.getScene().setCursor(Cursor.WAIT);

                for (File f : db.getFiles()) {
                    new Thread(() -> {
                        LocalDateTime pdfDate = null;

                        // ===== Lecture PDF et extraction date (comme importPdf) =====
                        try (PDDocument document = PDDocument.load(f)) {
                            PDFTextStripper stripper = new PDFTextStripper();
                            String text = stripper.getText(document);
                            Matcher matcher = dateTimePattern.matcher(text);
                            while (matcher.find()) {
                                String dateStr = matcher.group(1);
                                try {
                                    pdfDate = LocalDateTime.parse(dateStr, formatter);
                                    break; // première date trouvée
                                } catch (Exception ignored) {}
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        if (pdfDate == null) pdfDate = LocalDateTime.now();

                        try {
                            // ===== Créer dossier candidature si absent =====
                            Path dossier = selected.getDossier();
                            if (dossier == null || !Files.exists(dossier)) {
                                String safeName = (selected.getEntreprise() + "_" + selected.getPoste())
                                        .replaceAll("\\W+", "_");
                                dossier = FileSystemService.createCandidatureFolder(safeName);
                                selected.setDossier(dossier);
                                DatabaseService.saveCandidature(selected);
                            }

                            // ===== Importer PDF =====
                            PdfImportService pdfImportService = new PdfImportService();
                            DocumentFile doc = pdfImportService.importer(f.toPath(), dossier, pdfDate);

                            if (!selected.getDocuments().contains(doc)) {
                                selected.getDocuments().add(doc);
                            }

                            // ===== Renommer dossier selon PDF le plus ancien =====
                            Path newFolder = FileSystemService.renameCandidatureFolderWithOldestPdfDate(selected);
                            selected.setDossier(newFolder);

                            // ===== Déplacer fichiers si nécessaire =====
                            for (DocumentFile d : selected.getDocuments()) {
                                Path oldPath = d.getFichier();
                                Path newPath = newFolder.resolve(oldPath.getFileName());
                                if (!oldPath.equals(newPath) && Files.exists(oldPath)) {
                                    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                                    d.setFichier(newPath);
                                }
                            }

                            // ===== Mettre à jour date de la candidature =====
                            selected.getDocuments().stream()
                                    .map(DocumentFile::getDateMail)
                                    .filter(Objects::nonNull)
                                    .min(LocalDateTime::compareTo)
                                    .ifPresent(dtt -> selected.setDateEnvoi(dtt.toLocalDate()));

                            // ===== Sauvegarde =====
                            DatabaseService.saveDocuments(selected);
                            DatabaseService.saveCandidature(selected);

                            // ===== Rafraîchir interface =====
                            Platform.runLater(() -> {
                                table.refresh();
                                updatePdfList(selected);
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> {
                                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'import du PDF").showAndWait();
                            });
                        } finally {
                            Platform.runLater(() -> rootPane.getScene().setCursor(Cursor.DEFAULT));
                        }

                    }, "pdf-drag-drop-thread").start();
                }

                event.setDropCompleted(true);
                event.consume();
            });
    }

    private void importPdfFromFile(Candidature c, Path pdfPath) {
        // exactement le même code que importPdf, mais sans FileChooser
        new Thread(() -> {
            try {
                PdfImportService pdfImportService = new PdfImportService();
                DocumentFile doc = pdfImportService.importer(pdfPath, c.getDossier(), LocalDateTime.now());

                if (!c.getDocuments().contains(doc)) c.getDocuments().add(doc);

                Path newFolder = FileSystemService.renameCandidatureFolderWithOldestPdfDate(c);
                c.setDossier(newFolder);

                DatabaseService.saveDocuments(c);
                DatabaseService.saveCandidature(c);

                Platform.runLater(() -> updatePdfList(c));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
                        "Erreur lors de l'import du PDF : " + pdfPath.getFileName()).showAndWait());
            }
        }).start();
    }

    private void initContextMenu() {

        MenuItem addItem = new MenuItem("Nouvelle candidature");
        addItem.setOnAction(e -> {
            try {
                onAddCandidature();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        MenuItem editItem = new MenuItem("Modifier");
        editItem.setOnAction(e -> {
            try {
                onEditCandidature();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(e -> onDeleteCandidature());

        ContextMenu menu = new ContextMenu(addItem, editItem, deleteItem);

        // Optionnel mais recommandé : désactiver selon sélection
        menu.setOnShowing(e -> {
            boolean selected = table.getSelectionModel().getSelectedItem() != null;
            editItem.setDisable(!selected);
            deleteItem.setDisable(!selected);
        });

        table.setContextMenu(menu);
    }

    private void onEditCandidature() throws Exception {
        Candidature selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editCandidature(selected);
        }
    }

    private void onAddCandidature() throws Exception {
        Stage stage = (Stage) table.getScene().getWindow();
        createCandidature(stage);
    }

    private void onDeleteCandidature() {

        Candidature selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer cette candidature ?",
                ButtonType.OK,
                ButtonType.CANCEL
        );

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    controller.deleteCandidature(selected);
                } catch (Exception e) {
                    showError("Erreur lors de la suppression", e);
                }
            }
        });
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void initTableColumns() {

        table.setEditable(true);

        colIndex.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(table.getItems().indexOf(c.getValue()) + 1)
        );

        colDate.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getDateEnvoi())
        );
        colDate.setSortType(TableColumn.SortType.DESCENDING);

        colEntreprise.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEntreprise())
        );

        colPoste.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPoste())
        );

        colStatut.setCellValueFactory(c -> {
            StatutCandidature s = c.getValue().getStatut();
            return new SimpleStringProperty(s != null ? s.getLabel() : "");
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("non-retenu");
                } else {
                    setText(item);
                    if ("Non retenu".equals(item)) {
                        if (!getStyleClass().contains("non-retenu")) {
                            getStyleClass().add("non-retenu");
                        }
                    } else {
                        getStyleClass().remove("non-retenu");
                    }
                }
            }
        });

        colProfil.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getProfil() != null
                                ? c.getValue().getProfil().getNom()
                                : ""
                )
        );
//        colNotes.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().getNotes())
//        );
//        colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
//        colNotes.setOnEditCommit(e -> {
//            e.getRowValue().setNotes(e.getNewValue());
//            try {
//                controller.createCandidature(e.getRowValue());
//            } catch (Exception ex) {
//                throw new RuntimeException(ex);
//            }
//        });

//        colRelance.setCellValueFactory(c ->
//                new SimpleObjectProperty<>(c.getValue().getDateRelance())
//        );
//        colRelance.setCellFactory(col -> new DatePickerTableCell<>());
//        colRelance.setOnEditCommit(e -> {
//            e.getRowValue().setDateRelance(e.getNewValue());
////            try {
////                controller.createCandidature(e.getRowValue());
////            } catch (Exception ex) {
////                throw new RuntimeException(ex);
////            }
//        });

//        colElapsed.setCellValueFactory(c -> {
//            LocalDate d = c.getValue().getDateEnvoi();
//            long days = d != null
//                    ? java.time.temporal.ChronoUnit.DAYS.between(d, LocalDate.now())
//                    : 0;
//            return new SimpleStringProperty(days + " jours");
//        });
    }

    private void initFilters() {
        moisFilter.getItems().add("Tous");
        for (int i = 1; i <= 12; i++) moisFilter.getItems().add(String.valueOf(i));
        moisFilter.setValue("Tous");

        statutFilter.getItems().add(null); // pour correspondre à "Tous"
        statutFilter.getItems().addAll(StatutCandidature.values());
        statutFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatutCandidature s) {
                return s == null ? "Tous" : s.getLabel();
            }

            @Override
            public StatutCandidature fromString(String string) {
                return Arrays.stream(StatutCandidature.values())
                        .filter(st -> st.getLabel().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        statutFilter.setValue(null); // sélection par défaut = Tous

        Runnable update = this::applyFilters;

        searchField.textProperty().addListener((o,a,b)->update.run());
        statutFilter.valueProperty().addListener((o,a,b)->update.run());
        moisFilter.valueProperty().addListener((o,a,b)->update.run());
//        pdfFilter.selectedProperty().addListener((o,a,b)->update.run());
//        responseFilter.selectedProperty().addListener((o,a,b)->update.run());
    }

    private void initActions() {

        addBtn.setOnAction(e -> {
            try {
                createCandidature((Stage) table.getScene().getWindow());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        profilBtn.setOnAction(e -> openProfilManagement());
        rapportBtn.setOnAction(e -> openRapportCandidatures());
        statBtn.setOnAction(e -> showStatWindow());
        dashBtn.setOnAction(e -> showDashboard());
        themeBtn.setOnAction(e -> {
            Scene scene = table.getScene();
            boolean dark = themeBtn.isSelected();
            applyTheme(scene, dark);
        });

        Platform.runLater(() -> {
            if (table.getScene() != null) {
                table.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.DELETE) {
                        Candidature c = table.getSelectionModel().getSelectedItem();
                        DocumentFile selectedDoc = pdfViewerPane.getSelectedPdf();
                        if (c != null && selectedDoc != null) {
                            deletePdf(c, selectedDoc); // exactement comme ton menu contextuel
                        }
                    }
                });
            }
        });
    }

    private void openProfilManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/profil_view.fxml")
            );
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Gestion des profils");
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setWidth(1600);
            stage.setHeight(1000);
            stage.setMinWidth(1200);
            stage.setMinHeight(800);

            stage.show();
            Platform.runLater(stage::centerOnScreen);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openRapportCandidatures() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/rapport_candidatures.fxml")
            );
            Stage stage = new Stage();
            stage.setTitle("Rapport des candidatures");
            stage.setScene(new Scene(loader.load(), 1400, 900));
            stage.initModality(Modality.NONE);

            stage.show();
            Platform.runLater(stage::centerOnScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deletePdf(Candidature c, DocumentFile doc) {
        try {
            // Supprime le fichier du disque
            Files.deleteIfExists(doc.getFichier());

            // Supprime de la candidature
            c.getDocuments().remove(doc);

            // Sauvegarde en base
            DatabaseService.saveDocuments(c);
            DatabaseService.saveCandidature(c);

            // Rafraîchit interface
            pdfViewerPane.setPdfList(c.getDocuments().stream()
                    .filter(d -> "PDF".equalsIgnoreCase(d.getType()))
                    .toList());

            table.refresh();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de supprimer le PDF").showAndWait();
        }
    }

    private void initSelectionListener() {
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> updatePdfList(n));
    }

    private void applyFilters() {
        filteredCandidatures.setPredicate(c -> {
            if (c == null) return false;

            String txt = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

            boolean matchSearch =
                    txt.isEmpty()
                            || (c.getEntreprise() != null && c.getEntreprise().toLowerCase().contains(txt))
                            || (c.getPoste() != null && c.getPoste().toLowerCase().contains(txt));

            boolean matchStatut =
                    statutFilter.getValue() == null
                            || c.getStatut() == statutFilter.getValue();

            boolean matchMonth =
                    "Tous".equals(moisFilter.getValue())
                            || (c.getDateEnvoi() != null &&
                            c.getDateEnvoi().getMonthValue() == Integer.parseInt(moisFilter.getValue()));

//            boolean matchPdf =
//                    !pdfFilter.isSelected()
//                            || !c.getDocuments().isEmpty();
//
//            boolean matchResponse =
//                    !responseFilter.isSelected()
//                            || c.getStatut() != StatutCandidature.EN_ATTENTE;

//            return matchSearch && matchStatut && matchMonth && matchPdf && matchResponse;
            return matchSearch && matchStatut && matchMonth;
        });
    }

    private void createCandidature(Stage stage) throws Exception {
        showCreateCandidatureDialog(stage).ifPresent(c -> {
            controller.createCandidature(c); // <-- ici on appelle la méthode du MainController
            handleCandidatureCreated(c);     // mise à jour interface
        });
    }

    private void handleCandidatureCreated(Candidature c) {

        if (!controller.getCandidatures().contains(c)) {
            controller.getCandidatures().add(c);
        }

        table.getSortOrder().setAll(colDate);
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        table.sort();

        table.getSelectionModel().select(c);
        table.scrollTo(c);
    }

    private Optional<Candidature> showCreateCandidatureDialog(Stage owner) throws Exception {

        Dialog<Candidature> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle candidature");
        dialog.initOwner(owner);

        ComboBox<Profil> comboProfil = new ComboBox<>();
        comboProfil.getItems().setAll(ProfilDAO.findAll());

        ButtonType createBtnType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        TextField entrepriseField = new TextField();
        entrepriseField.setPromptText("Entreprise");

        TextField posteField = new TextField();
        posteField.setPromptText("Poste");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Entreprise"), 0, 0);
        grid.add(entrepriseField, 1, 0);
        grid.add(new Label("Poste"), 0, 1);
        grid.add(posteField, 1, 1);
        grid.add(new Label("Date d'envoi"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Profil"), 0, 3);
        grid.add(comboProfil, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node createBtn = dialog.getDialogPane().lookupButton(createBtnType);
        createBtn.disableProperty().bind(
                entrepriseField.textProperty().isEmpty()
                        .or(posteField.textProperty().isEmpty())
        );


        dialog.setResultConverter(bt -> {
            if (bt == createBtnType) {

                Candidature c = new Candidature(
                        entrepriseField.getText().trim(),
                        posteField.getText().trim(),
                        datePicker.getValue(),
                        StatutCandidature.EN_ATTENTE
                );

                c.setProfil(comboProfil.getValue()); // 👈 ICI, ET SEULEMENT ICI

                return c;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void editCandidature(Candidature c) throws Exception {

        showEditCandidatureDialog(c).ifPresent(data -> {
            try {
                controller.updateCandidature(c, data);
                handleCandidatureUpdated(c);
                updatePdfList(c);             // rafraîchir liste PDF
            } catch (Exception e) {
                showError("Erreur lors de la mise à jour de la candidature", e);
            }
        });
    }

    private Optional<CandidatureEditData> showEditCandidatureDialog(Candidature c) throws Exception {

        Dialog<CandidatureEditData> dialog = new Dialog<>();
        dialog.setTitle("Modifier candidature");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(c.getDateEnvoi());
        TextField entrepriseField = new TextField(c.getEntreprise());
        TextField posteField = new TextField(c.getPoste());
        ChoiceBox<StatutCandidature> statutChoice =
                new ChoiceBox<>(FXCollections.observableArrayList(StatutCandidature.values()));
        statutChoice.setValue(c.getStatut());

        ComboBox<Profil> profilCombo = new ComboBox<>();
        profilCombo.setItems(FXCollections.observableArrayList(ProfilDAO.findAll()));
        profilCombo.setValue(c.getProfil()); // profil actuel

        VBox content = new VBox(10,
                new Label("Date de candidature"), datePicker,
                new Label("Entreprise"), entrepriseField,
                new Label("Poste"), posteField,
                new Label("Statut"), statutChoice,
                new Label("Profil"), profilCombo
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            return new CandidatureEditData(
                    datePicker.getValue(),
                    entrepriseField.getText(),
                    posteField.getText(),
                    statutChoice.getValue(),
                    profilCombo.getValue()
            );
        });

        return dialog.showAndWait();
    }

    private void handleCandidatureUpdated(Candidature c) {

        // 1️⃣ Rafraîchir la table
        table.refresh();

        // 2️⃣ Re-trier
        table.getSortOrder().setAll(colDate);
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        table.sort();

        // 3️⃣ Re-sélectionner
        table.getSelectionModel().select(c);
        table.scrollTo(c);

        // 4️⃣ Rafraîchir le viewer PDF (INDISPENSABLE)
        updatePdfList(c);
    }

    private void showStatWindow() {
        CandidatureService service = new CandidatureService(table.getItems());

        Stage statStage = new Stage();
        statStage.setTitle("Rapport Statistiques Candidatures");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        VBox topBox = new VBox(10);
        Label totalLabel = new Label("Total candidatures : " + service.getTotal());
        long reponses = service.countByStatut(StatutCandidature.RETENU)
                + service.countByStatut(StatutCandidature.NON_RETENU);
        Label reponseLabel = new Label("Réponses reçues : " + reponses);
        topBox.getChildren().addAll(totalLabel, reponseLabel);

        PieChart pieChart = new PieChart();
        pieChart.getData().addAll(
                new PieChart.Data("Acceptées", service.countByStatut(StatutCandidature.RETENU)),
                new PieChart.Data("Refusées", service.countByStatut(StatutCandidature.NON_RETENU)),
                new PieChart.Data("En attente", service.countByStatut(StatutCandidature.EN_ATTENTE))
        );

        // Permet au PieChart de prendre tout l’espace disponible
        root.setTop(topBox);
        root.setCenter(pieChart);

        Scene scene = new Scene(root, 800, 600); // taille initiale plus grande
        statStage.setScene(scene);
        statStage.setMinWidth(600);
        statStage.setMinHeight(500);
        statStage.setResizable(true);

        statStage.show();
        statStage.centerOnScreen();
    }

    private void showDashboard() {
        Stage dashStage = new Stage();
        dashStage.setTitle("Dashboard des candidatures");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // ===== Calcul des indicateurs =====
        int total = controller.getCandidatures().size();
        long totalReponses = controller.getCandidatures().stream()
                .filter(c ->  c.getStatut() == StatutCandidature.RETENU || c.getStatut() == StatutCandidature.NON_RETENU)
                .count();
        double avgDays = controller.getCandidatures().stream()
                .filter(c -> c.getDateEnvoi() != null)
                .mapToLong(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getDateEnvoi(), LocalDate.now()))
                .average().orElse(0);

        long distinctEntreprises = controller.getCandidatures().stream()
                .map(Candidature::getEntreprise)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Entreprise avec max candidatures
        String maxEnt = controller.getCandidatures().stream()
                .filter(c -> c.getEntreprise() != null)
                .collect(Collectors.groupingBy(Candidature::getEntreprise, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // PDF et réponses
        long candidaturesWithPdf = controller.getCandidatures().stream()
                .filter(c -> c.getDocuments().stream().anyMatch(d -> "PDF".equals(d.getType())))
                .count();
        long candidaturesWithoutPdf = total - candidaturesWithPdf;
        long candidaturesWithoutResponse = controller.getCandidatures().stream()
                .filter(c -> c.getStatut() == StatutCandidature.EN_ATTENTE)
                .count();

        // ===== Bloc 1 : total, réponses, délai =====
        VBox block1 = new VBox(10,
                new Label("Total candidatures : " + total),
                new Label("Réponses reçues : " + totalReponses),
                new Label(String.format("Délai moyen de réponse : %.1f jours", avgDays))
        );

        // ===== Bloc 2 : entreprises =====
        VBox block2 = new VBox(10,
                new Label("Nombre d'entreprises : " + distinctEntreprises),
                new Label("Entreprise avec le plus de candidatures : " + maxEnt)
        );

        // ===== Bloc 3 : PDF et réponses =====
        VBox block3 = new VBox(10,
                new Label("Candidatures avec PDF : " + candidaturesWithPdf),
                new Label("Candidatures sans PDF : " + candidaturesWithoutPdf),
                new Label("Candidatures sans réponse : " + candidaturesWithoutResponse)
        );

        HBox topBox = new HBox(50, block1, block2, block3);
        topBox.setPadding(new Insets(15));
        root.setTop(topBox);

        // ===== Charts =====
        BarChart<String, Number> statutChart = createBarChart("Statut", "Nombre", "Répartition des candidatures par statut");
        BarChart<String, Number> monthChart = createBarChart("Mois", "Nombre", "Candidatures envoyées par mois");
        BarChart<String, Number> entChart = createBarChart("Entreprise", "Nombre", "Candidatures par entreprise");

        // Remplissage des charts (idem version précédente)
        XYChart.Series<String, Number> statutSeries = new XYChart.Series<>();
        statutSeries.setName("Candidatures");
        for (StatutCandidature s : StatutCandidature.values()) {
            long count = controller.getCandidatures().stream()
                    .filter(c -> c.getStatut() == s)
                    .count();
            statutSeries.getData().add(new XYChart.Data<>(s.getLabel(), count));
        }
        statutChart.getData().add(statutSeries);

        XYChart.Series<String, Number> monthSeries = new XYChart.Series<>();
        monthSeries.setName("Envoyées");
        for (int m = 1; m <= 12; m++) {
            int finalM = m;
            long count = controller.getCandidatures().stream()
                    .filter(c -> c.getDateEnvoi() != null && c.getDateEnvoi().getMonthValue() == finalM)
                    .count();
            monthSeries.getData().add(new XYChart.Data<>(String.valueOf(m), count));
        }
        monthChart.getData().add(monthSeries);

        XYChart.Series<String, Number> entSeries = new XYChart.Series<>();
        entSeries.setName("Candidatures");
        controller.getCandidatures().stream()
                .map(Candidature::getEntreprise)
                .distinct()
                .sorted()
                .forEach(ent -> {
                    long count = controller.getCandidatures().stream()
                            .filter(c -> ent.equals(c.getEntreprise()))
                            .count();
                    entSeries.getData().add(new XYChart.Data<>(ent, count));
                });
        entChart.getData().add(entSeries);

        VBox chartsBox = new VBox(30, statutChart, monthChart, entChart);
        chartsBox.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(chartsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 1600, 1000);
        dashStage.setScene(scene);
        dashStage.setMinWidth(1200);
        dashStage.setMinHeight(800);
        dashStage.show();
        dashStage.centerOnScreen();
    }

    private BarChart<String, Number> createBarChart(String xLabel, String yLabel, String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(xLabel);
        yAxis.setLabel(yLabel);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCategoryGap(25);
        chart.setBarGap(5);
        chart.setPrefHeight(350);
        chart.setAnimated(false);
        chart.setMaxWidth(Double.MAX_VALUE);
        return chart;
    }

    private void setupPdfViewerContextMenu() {


        ListView<DocumentFile> listView = pdfViewerPane.getPdfListView();

        MenuItem importPdf = new MenuItem("Importer PDF");
        MenuItem changeDate = new MenuItem("Modifier date");
        MenuItem deleteDoc = new MenuItem("Supprimer");

        importPdf.setOnAction(e -> {
            Stage stage = (Stage) table.getScene().getWindow();
            importPdf(stage);
        });
        deleteDoc.setOnAction(e -> {
            DocumentFile doc = listView.getSelectionModel().getSelectedItem();
            if (doc == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce document ?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try { Files.deleteIfExists(doc.getFichier()); } catch (IOException ignored) {}
                    Candidature c = table.getSelectionModel().getSelectedItem();
                    if (c != null) {
                        c.getDocuments().remove(doc);
                        try {
                            controller.saveCandidature(c);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                        updatePdfList(c);
                    }
                }
            });
        });

        changeDate.setOnAction(e -> {
            DocumentFile doc = listView.getSelectionModel().getSelectedItem();
            if (doc == null) return;

            Dialog<LocalDateTime> dialog = new Dialog<>();
            dialog.setTitle("Modifier la date du document");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            DatePicker datePicker = new DatePicker(doc.getDateMail() != null ? doc.getDateMail().toLocalDate() : LocalDate.now());
            Spinner<Integer> hourSpinner = new Spinner<>(0, 23, doc.getDateMail() != null ? doc.getDateMail().getHour() : 12);
            Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, doc.getDateMail() != null ? doc.getDateMail().getMinute() : 0);
            VBox content = new VBox(10, new Label("Date"), datePicker, new Label("Heure"), new HBox(5, hourSpinner, new Label(":"), minuteSpinner));
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) return LocalDateTime.of(datePicker.getValue(), LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
                return null;
            });

            dialog.showAndWait().ifPresent(newDate -> {
                doc.setDateMail(newDate);
                try {
                    Candidature c = table.getSelectionModel().getSelectedItem();
                    controller.saveCandidature(c);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                Candidature c = table.getSelectionModel().getSelectedItem();
                if (c != null) updatePdfList(c);
            });
        });


        listView.setContextMenu(new ContextMenu(importPdf, changeDate, deleteDoc));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentFile doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) setText(null);
                else {
                    String dateStr = doc.getDateMail() != null ? doc.getDateMail().format(dateFormatter) : "Date inconnue";
                    setText(dateStr + " - " + doc.getFichier().getFileName());
                }
            }
        });
        listView.setPrefHeight(100);
    }

    private void importPdf(Stage stage) {
        Candidature c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;

        stage.getScene().setCursor(Cursor.WAIT);

        new Thread(() -> {
            LocalDateTime dt = null;

            // ===== Extraire la date du PDF =====
            try (PDDocument document = PDDocument.load(f)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                Matcher matcher = dateTimePattern.matcher(text);
                while (matcher.find()) {
                    String dateStr = matcher.group(1);
                    try {
                        dt = LocalDateTime.parse(dateStr, formatter);
                        break; // on prend la première date trouvée
                    } catch (Exception ignored) {}
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (dt == null) dt = LocalDateTime.now(); // date par défaut si non détectée

            Path oldFolder = c.getDossier();

            try {
                // ===== Créer dossier candidature si absent =====
                if (oldFolder == null || !Files.exists(oldFolder)) {
                    String safeName = (c.getEntreprise() + "_" + c.getPoste()).replaceAll("\\W+", "_");
                    oldFolder = FileSystemService.createCandidatureFolder(safeName);
                    c.setDossier(oldFolder);
                    DatabaseService.saveCandidature(c);
                }

                // ===== Importer le PDF =====
                PdfImportService pdfImportService = new PdfImportService();
                DocumentFile doc = pdfImportService.importer(f.toPath(), oldFolder, dt);

                // ===== Ajouter à la candidature si nouveau =====
                if (!c.getDocuments().contains(doc)) {
                    c.getDocuments().add(doc);
                }

                // ===== Renommer le dossier selon la date du PDF la plus ancienne =====
                Path newFolder = FileSystemService.renameCandidatureFolderWithOldestPdfDate(c);

                // ===== Mettre à jour les chemins des documents =====
                for (DocumentFile d : c.getDocuments()) {
                    Path oldPath = d.getFichier();
                    Path newPath = newFolder.resolve(oldPath.getFileName());
                    if (!oldPath.equals(newPath) && Files.exists(oldPath)) {
                        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                        d.setFichier(newPath);
                    }
                }

                // ===== METTRE À JOUR LA DATE DE LA CANDIDATURE =====
                Optional<LocalDateTime> oldestPdfDateTime = c.getDocuments().stream()
                        .map(DocumentFile::getDateMail)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo);

                oldestPdfDateTime.ifPresent(dtt -> c.setDateEnvoi(dtt.toLocalDate()));

                c.setDossier(newFolder);

                // ===== Sauvegarde en base =====
                DatabaseService.saveDocuments(c);
                DatabaseService.saveCandidature(c);

                // ===== Rafraîchir interface =====
                Platform.runLater(() -> {
                    table.refresh();
                    updatePdfList(c); // mettre à jour le viewer
                });

            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de l'import et du déplacement du PDF").showAndWait();
                });
            } finally {
                Platform.runLater(() -> stage.getScene().setCursor(Cursor.DEFAULT));
            }

        }, "pdf-import-thread").start();
    }

    void updatePdfList(Candidature c) {

        if (c == null) {
            pdfViewerPane.clear();
            return;
        }

        // Filtre tous les PDFs (ignore la casse et les nulls)
        List<DocumentFile> pdfs = c.getDocuments().stream()
                .filter(d -> d.getType() != null && d.getType().equalsIgnoreCase("PDF"))
                .sorted(Comparator.comparing(
                        DocumentFile::getDateMail,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )) // Plus ancien en premier
                .toList();

//        System.out.println("PDFs chargés pour " + c.getEntreprise() + " : " + pdfs.size());

//        c.getDocuments().forEach(d ->
//                System.out.println(
//                        System.identityHashCode(d) + " | " + d.getFichierPath()
//                )
//        );

        // Mettre à jour la liste dans le viewer
        pdfViewerPane.setPdfList(pdfs);

        if (!pdfs.isEmpty()) {
            // Sélection automatique du PDF le plus ancien
            pdfViewerPane.selectPdf(pdfs.get(0));
        } else {
            pdfViewerPane.clear();
        }
    }

    private void applyTheme(Scene scene, boolean dark) {
        scene.getStylesheets().clear(); // on supprime la feuille existante
        if (dark) {
            scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());
        }
    }
}
