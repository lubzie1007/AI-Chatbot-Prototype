package com.studymate.student_assistant.service;

import com.studymate.student_assistant.model.CvDraft;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class CvPdfService {

    public byte[] generate(CvDraft cv) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            float pageWidth = page.getMediaBox().getWidth();
            float contentWidth = pageWidth - 2 * margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ---- Name ----
                cs.beginText();
                cs.setFont(bold, 22);
                cs.newLineAtOffset(margin, y);
                cs.showText(safe(cv.getFullName()));
                cs.endText();
                y -= 30;

                // ---- Contact ----
                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(safe(cv.getContact()));
                cs.endText();
                y -= 30;

                // ---- Divider ----
                cs.setLineWidth(0.8f);
                cs.moveTo(margin, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();
                y -= 25;

                // ---- Sections ----
                y = section(cs, bold, regular, "EDUCATION",
                        "School: " + safe(cv.getSchool()), margin, y, contentWidth);

                y = section(cs, bold, regular, "MATRIC SUBJECTS",
                        safe(cv.getSubjects()), margin, y, contentWidth);

                y = section(cs, bold, regular, "EXPERIENCE",
                        safe(cv.getExperience()), margin, y, contentWidth);

                y = section(cs, bold, regular, "SKILLS",
                        safe(cv.getSkills()), margin, y, contentWidth);

                y = section(cs, bold, regular, "ACTIVITIES & ACHIEVEMENTS",
                        safe(cv.getActivities()), margin, y, contentWidth);
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private float section(PDPageContentStream cs,
                          PDFont bold, PDFont regular,
                          String heading, String body,
                          float margin, float y, float contentWidth) throws IOException {

        cs.beginText();
        cs.setFont(bold, 12);
        cs.newLineAtOffset(margin, y);
        cs.showText(heading);
        cs.endText();
        y -= 16;

        // Wrap body into lines
        for (String paragraph : body.split("\n")) {
            for (String line : wrap(paragraph, regular, 11, contentWidth)) {
                cs.beginText();
                cs.setFont(regular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(line);
                cs.endText();
                y -= 14;
            }
        }
        return y - 12; // space between sections
    }

    private java.util.List<String> wrap(String text, PDFont font,
                                         float fontSize, float maxWidth) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("—");
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * fontSize;
            if (width > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    /** PDFBox Standard14Fonts can't render some Unicode chars. Strip them. */
    private String safe(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 32 && c <= 126) sb.append(c);
            else if (c == '\n')       sb.append(' ');
        }
        return sb.toString();
    }
}
