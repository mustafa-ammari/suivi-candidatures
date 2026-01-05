package app.service;

import app.model.Candidature;
import app.model.Profil;
import app.model.StatutCandidature;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    public static void exportCandidatures(
            List<Candidature> candidatures,
            LocalDate start,
            LocalDate end,
            File output
    ) throws Exception {

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float y = 750;

            // ===== TITRE =====
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.newLineAtOffset(50, y);
            cs.showText("Rapport des candidatures");
            cs.endText();

            y -= 30;

            // ===== PERIODE =====
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, y);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            cs.showText("Période : " + start.format(formatter) + " -> " + end.format(formatter));
            cs.endText();

            y -= 30;

            // ===== TABLE HEADER =====
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            drawRow(cs, y,
                    "","Date", "Entreprise", "Poste");
            y -= 15;

            cs.setFont(PDType1Font.HELVETICA, 10);

            // ===== DATA =====
            int index = 1;
            for (Candidature c : candidatures) {

                if (y < 50) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = 750;
                }

                drawRow(
                        cs, y,
                        String.valueOf(index),
                        safe(c.getDateEnvoi()),
                        safe(c.getEntreprise()),
                        safe(c.getPoste())
//                        safe(c.getStatut())
//                        c.getProfil() != null ? c.getProfil().getNom() : ""
                );

                y -= 15;
                index++;
            }

            cs.close();
            doc.save(output);
        }
    }

    private static void drawRow(
            PDPageContentStream cs,
            float y,
            String... cols
    ) throws Exception {

        float x = 50;
        float[] widths = {30, 70, 180, 240};

        for (int i = 0; i < cols.length; i++) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 10); // <-- définir la police avant showText
            cs.newLineAtOffset(x, y);
            cs.showText(cols[i]);
            cs.endText();
            x += widths[i];
        }
    }

    private static String safe(Object o) {
        if (o == null) return "";
        String s = o.toString();
        // Supprime les caractères invisibles ou non supportés par PDFBox
        s = s.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        // Remplace les flèches → par ->
        s = s.replace("→", "->");
        // Tu peux ajouter d'autres remplacements si nécessaire
        return s;
    }

}
