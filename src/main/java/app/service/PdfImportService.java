package app.service;

import app.model.DocumentFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class PdfImportService {


    public static DocumentFile importer(Path pdfPath, Path dossierCandidature, LocalDateTime datePdfTime) throws IOException {
        if (pdfPath == null || dossierCandidature == null || datePdfTime == null)
            throw new IllegalArgumentException("Arguments null interdits");

        if (!Files.exists(dossierCandidature)) {
            Files.createDirectories(dossierCandidature);
        }

        Path targetPath = dossierCandidature.resolve(pdfPath.getFileName());
        Files.move(pdfPath, targetPath);

        DocumentFile doc = new DocumentFile();
        doc.setFichier(targetPath);
        doc.setNom(targetPath.getFileName().toString());
        doc.setDateMail(datePdfTime);

        return doc;
    }
}
