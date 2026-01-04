package app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidature {

    private String id;
    private String entreprise;
    private String poste;
    private LocalDate dateEnvoi;
    private StatutCandidature statut;
    private Path dossier;
    private List<DocumentFile> documents = new ArrayList<>();
    private String notes;
    private LocalDate dateRelance;
    private Profil profil;

    public Candidature(String entreprise, String poste) {
        this.id = UUID.randomUUID().toString();
        this.entreprise = entreprise;
        this.poste = poste;
        this.statut = StatutCandidature.EN_ATTENTE;
    }

    public Candidature(String text, String text1, LocalDate value) {
    }

    public Candidature(String entreprise, String poste, LocalDate dateEnvoi, StatutCandidature statut) {
        this.entreprise = entreprise;
        this.poste = poste;
        this.dateEnvoi = dateEnvoi;
        this.statut = statut;
    }

}
//        BorderPane root = new BorderPane();
//        root.setCenter(splitPane);
//
//        // Drag & Drop pour importer directement un PDF sur la candidature sélectionnée
//        // ========================= DRAG & DROP PDF =========================
//        root.setOnDragOver(event -> {
//            Candidature selected = table.getSelectionModel().getSelectedItem();
//            if (selected != null && event.getDragboard().hasFiles()) {
//                event.acceptTransferModes(TransferMode.COPY);
//            }
//            event.consume();
//        });
//
//        root.setOnDragDropped(event -> {
//            Candidature selected = table.getSelectionModel().getSelectedItem();
//            if (selected == null) return;
//
//            event.setDropCompleted(false);
//            var db = event.getDragboard();
//            if (!db.hasFiles()) return;
//
//            root.getScene().setCursor(Cursor.WAIT);
//
//            for (File f : db.getFiles()) {
//                new Thread(() -> {
//                    LocalDateTime pdfDate = null;
//                    boolean found = false;
//
//                    // Lecture PDF + extraction date (comme importPdf)
//                    try (PDDocument document = PDDocument.load(f)) {
//                        PDFTextStripper stripper = new PDFTextStripper();
//                        String text = stripper.getText(document);
//
//                        Matcher matcher = dateTimePattern.matcher(text);
//                        while (matcher.find()) {
//                            String dateStr = matcher.group(1);
//                            try {
//                                pdfDate = LocalDateTime.parse(dateStr, formatter);
//                                found = true;
//                                break; // première date trouvée
//                            } catch (Exception ignored) {}
//                        }
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//
//                    DocumentFile doc = null;
//                    try {
//                        doc = PdfImportService.importer(f.toPath(), selected.getDossier(), pdfDate);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    DocumentFile finalDoc = doc;
//                    boolean finalFound = found;
//
//                    Platform.runLater(() -> {
//                        root.getScene().setCursor(Cursor.DEFAULT);
//
//                        if (finalDoc != null && !selected.getDocuments().contains(finalDoc)) {
//                            selected.getDocuments().add(finalDoc);
//
//                            // 🔹 Renommer le dossier automatiquement selon le PDF le plus ancien
//                            try {
//                                FileSystemService.renameCandidatureFolderWithOldestPdfDate(selected);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                new Alert(Alert.AlertType.ERROR, "Impossible de renommer le dossier de la candidature").showAndWait();
//                            }
//
//                            controller.saveDocuments(selected); // persiste les PDF
//                            try {
//                                controller.save();                  // persiste la candidature
//                            } catch (Exception e) {
//                                throw new RuntimeException(e);
//                            }
//                            updatePdfList(selected);
//                        }
//
//
//                        if (!finalFound) {
//                            new Alert(Alert.AlertType.INFORMATION,
//                                    "Aucune date détectée dans le PDF : " + f.getName())
//                                    .showAndWait();
//                        }
//                    });
//
//                }, "pdf-drag-drop-thread").start();
//            }
//
//            event.setDropCompleted(true);
//            event.consume();
//        });