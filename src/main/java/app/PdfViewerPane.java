package app;

import app.controller.MainController;
import app.model.Candidature;
import app.model.DocumentFile;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PdfViewerPane extends BorderPane {

    private final MainController controller;
    private final AtomicLong renderVersion = new AtomicLong();

    private DocumentFile currentDocumentFile;
    private Path currentPdfPath;
    private Candidature currentCandidature;

    private final ImageView imageView = new ImageView();
    @Getter
    private final ListView<DocumentFile> pdfListView = new ListView<>();

    private int currentPage = 0;
    private int pageCount = 0;

    private ObjectProperty<DocumentFile> selectedPdf = new SimpleObjectProperty<>();

    public ObjectProperty<DocumentFile> selectedPdfProperty() {
        return selectedPdf;
    }

    public PdfViewerPane(List<DocumentFile> pdfList, MainController controller) {
        this.controller = controller;

        // ========================= IMAGE VIEW =========================
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.fitWidthProperty().bind(widthProperty().subtract(20));

        ScrollPane pdfScrollPane = new ScrollPane(imageView);
        pdfScrollPane.setFitToWidth(true);
        pdfScrollPane.setFitToHeight(true);
        pdfScrollPane.setPannable(true);
        setCenter(pdfScrollPane);

        // ========================= LISTE PDF =========================
        pdfListView.setPrefHeight(200);
        pdfListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String dateStr = item.getDateMail() != null
                            ? item.getDateMail().toString()
                            : "Date inconnue";
                    setText(dateStr + " - " + item.getFichier().getFileName());
                }
            }
        });

        pdfListView.getSelectionModel().selectedItemProperty().addListener((obs, old, doc) -> {
            if (doc != null) {
                currentDocumentFile = doc;
                currentPdfPath = doc.getFichier();
                currentPage = 0;
                openPdf(currentPdfPath);
            }
        });

        setTop(pdfListView);
        BorderPane.setMargin(pdfListView, new Insets(5));

        // ========================= NAVIGATION =========================
        Button prev = new Button("Précédent");
        Button next = new Button("Suivant");

        prev.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                renderPage();
            }
        });

        next.setOnAction(e -> {
            if (currentPage < pageCount - 1) {
                currentPage++;
                renderPage();
            }
        });

        ToolBar toolbar = new ToolBar(prev, next);
        setBottom(toolbar);

        if (pdfList != null) setPdfList(pdfList);
    }

    public DocumentFile getSelectedPdf() {
        return selectedPdf.get(); // selectedPdf est ton ObjectProperty<DocumentFile>
    }

    // ========================= SET LIST =========================
    public void setPdfList(List<DocumentFile> list) {
        pdfListView.getItems().setAll(list);
    }


    // ========================= OUVERTURE PDF =========================
    private synchronized void openPdf(Path path) {
        if (path == null) return;

        currentPdfPath = path;
        currentPage = 0;

        try (PDDocument doc = PDDocument.load(path.toFile())) {
            pageCount = doc.getNumberOfPages();
        } catch (Exception e) {
            pageCount = 0;
            e.printStackTrace();
            return;
        }

        renderPage();
    }


    // ========================= RENDU PAGE =========================
    private void renderPage() {
        if (currentPdfPath == null || currentPage < 0 || currentPage >= pageCount) return;

        long version = renderVersion.incrementAndGet();

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                try (PDDocument doc = PDDocument.load(currentPdfPath.toFile())) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    BufferedImage img = renderer.renderImageWithDPI(currentPage, 150);
                    return SwingFXUtils.toFXImage(img, null);
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (renderVersion.get() == version) {
                imageView.setImage(task.getValue());
            }
        });

        new Thread(task, "pdf-render-" + version).start();
    }


    // ========================= FERMETURE PDF =========================
    private void closePdf() {
        renderVersion.incrementAndGet();
        imageView.setImage(null);
    }

    public void selectPdf(DocumentFile doc) {
        if (doc == null) return;
        pdfListView.getSelectionModel().select(doc);
        pdfListView.scrollTo(doc);
        selectedPdf.set(doc);
    }

    public void clear() {
        pdfListView.getItems().clear();
        imageView.setImage(null);
    }
}
