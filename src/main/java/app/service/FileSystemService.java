package app.service;


import app.model.Candidature;
import app.model.DocumentFile;

import java.io.IOException;
import java.nio.file.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

public final class FileSystemService {

    private static final Path ROOT = Paths.get(System.getProperty("user.home"), "Candidatures2026");

    private FileSystemService() {}

    public static Path getCandidatureRoot() {
        try {
            if (!Files.exists(ROOT)) {
                Files.createDirectories(ROOT);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier racine des candidatures", e);
        }
        return ROOT;
    }

    public static Path createCandidatureFolder(String safeName) {
        Path folder = getCandidatureRoot().resolve(safeName);
        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de candidature : " + safeName, e);
        }
        return folder;
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static Path getBaseCandidatureFolder() {
        try {
            Files.createDirectories(ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier base", e);
        }
        return ROOT;
    }

    public static Path renameCandidatureFolderWithOldestPdfDate(Candidature c) throws IOException {
        if (c.getDocuments() == null || c.getDocuments().isEmpty()) return c.getDossier();

        var oldestOpt = c.getDocuments().stream()
                .map(DocumentFile::getDateMail)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo);

        if (oldestOpt.isEmpty()) return c.getDossier();

        String datePrefix = oldestOpt.get()
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        String entreprise = c.getEntreprise().replaceAll("[\\\\/:*?\"<>|]", "").trim();
        String poste = c.getPoste().replaceAll("[\\\\/:*?\"<>|]", "").trim();

        String newFolderName = datePrefix + " " + entreprise + " " + poste;

        Path oldPath = c.getDossier();
        Path newPath = oldPath.getParent().resolve(newFolderName);

        if (Files.exists(newPath)) {
            // Si le nouveau dossier existe déjà, ne rien faire pour éviter écrasement
            return oldPath;
        }

        Files.move(oldPath, newPath);

        // Met à jour les chemins des documents
        for (DocumentFile doc : c.getDocuments()) {
            Path oldFile = doc.getFichier();
            if (oldFile != null) {
                Path newFile = newPath.resolve(oldFile.getFileName());
                doc.setFichier(newFile);
            }
        }

        c.setDossier(newPath);
        return newPath;
    }
}
