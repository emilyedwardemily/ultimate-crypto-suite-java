package academy;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * Professional PDF certificate generator for the Fortress Academy.
 * Renders a deterministic QR-style matrix (seeded by the verification id)
 * plus the full metadata set: verification id, issue date, student, course,
 * score, instructor and status.
 */
public final class CertificateGenerator {

    private CertificateGenerator() { }

    public static void generate(String courseName, String studentName, String verificationId,
                                int score, String instructor, String issueDate, String status,
                                File out) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 54, 54, 54, 54);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
        doc.open();

        float w = doc.getPageSize().getWidth();
        float h = doc.getPageSize().getHeight();

        PdfContentByte cb = writer.getDirectContent();
        BaseColor accent = new BaseColor(0x8B, 0x5C, 0xF6);
        BaseColor gold = new BaseColor(0xD4, 0xAF, 0x37);
        BaseColor dark = new BaseColor(0x11, 0x11, 0x18);

        // Frames
        cb.setColorStroke(accent);
        cb.setLineWidth(2.2f);
        cb.roundRectangle(18, 18, w - 36, h - 36, 18);
        cb.stroke();
        cb.setLineWidth(1f);
        cb.roundRectangle(26, 26, w - 52, h - 52, 14);
        cb.stroke();

        // Corner flourishes
        cb.setColorStroke(gold);
        cb.setLineWidth(2.5f);
        cb.moveTo(30, 84); cb.lineTo(30, 30); cb.lineTo(84, 30); cb.stroke();
        cb.moveTo(w - 84, h - 30); cb.lineTo(w - 30, h - 30); cb.lineTo(w - 30, h - 84); cb.stroke();

        // Header
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 40, accent);
        Paragraph pTitle = new Paragraph("UC-FORTRESS ACADEMY", titleFont);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitle);

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, dark);
        Paragraph pSub = new Paragraph("\nCERTIFICATE OF ACHIEVEMENT", bodyFont);
        pSub.setAlignment(Element.ALIGN_CENTER);
        doc.add(pSub);

        Font midFont = FontFactory.getFont(FontFactory.HELVETICA, 15, new BaseColor(0x40, 0x40, 0x40));
        Paragraph pLine = new Paragraph("\nThis certifies that", midFont);
        pLine.setAlignment(Element.ALIGN_CENTER);
        doc.add(pLine);

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 34, gold);
        Paragraph pName = new Paragraph("\n" + studentName, nameFont);
        pName.setAlignment(Element.ALIGN_CENTER);
        doc.add(pName);

        Font courseFont = FontFactory.getFont(FontFactory.HELVETICA, 17, dark);
        Paragraph pCourse = new Paragraph("\nhas successfully completed the course", courseFont);
        pCourse.setAlignment(Element.ALIGN_CENTER);
        doc.add(pCourse);

        Font cTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new BaseColor(0x8B, 0x5C, 0xF6));
        Paragraph pCourseName = new Paragraph(courseName, cTitleFont);
        pCourseName.setAlignment(Element.ALIGN_CENTER);
        doc.add(pCourseName);

        Paragraph pScore = new Paragraph("Final Score: " + Math.round(score) + "%", courseFont);
        pScore.setAlignment(Element.ALIGN_CENTER);
        doc.add(pScore);

        // QR-style matrix (deterministic from verification id)
        Random r = new Random(verificationId.hashCode());
        int cells = 21;
        float qrSize = 74f;
        float qrX = 46f;
        float qrY = h - 130f;
        cb.setColorFill(dark);
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                boolean inFinder = (row < 7 && col < 7) || (row < 7 && col >= cells - 7) || (row >= cells - 7 && col < 7);
                boolean on;
                if (inFinder) {
                    on = row % 2 == 0 || col % 2 == 0;
                } else {
                    on = r.nextBoolean();
                }
                if (on) {
                    cb.rectangle(qrX + col * (qrSize / cells), qrY - row * (qrSize / cells),
                        qrSize / cells, qrSize / cells);
                }
            }
        }
        cb.fill();

        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new BaseColor(0x33, 0x33, 0x33));
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, accent);

        Paragraph pMeta = new Paragraph();
        pMeta.setLeading(22);
        pMeta.add(new Chunk("VERIFICATION ID:  ", labelFont));
        pMeta.add(new Chunk(verificationId, metaFont));
        pMeta.add(Chunk.NEWLINE);
        pMeta.add(new Chunk("ISSUE DATE:  ", labelFont));
        pMeta.add(new Chunk(issueDate, metaFont));
        pMeta.add(Chunk.NEWLINE);
        pMeta.add(new Chunk("INSTRUCTOR:  ", labelFont));
        pMeta.add(new Chunk(instructor, metaFont));
        pMeta.add(Chunk.NEWLINE);
        pMeta.add(new Chunk("STATUS:  ", labelFont));
        pMeta.add(new Chunk(status, metaFont));
        doc.add(pMeta);

        Paragraph pFooter = new Paragraph("\n\nValidated by the Fortress Academy verification registry \u2014 "
            + verificationId, midFont);
        pFooter.setAlignment(Element.ALIGN_CENTER);
        doc.add(pFooter);

        doc.close();
    }
}
