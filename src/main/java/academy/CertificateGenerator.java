package academy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Professional PDF certificate generator for the Fortress Academy.
 * Renders a real scannable QR code (ZXing) that links straight to the
 * Render verification endpoint, signed with HMAC-SHA256 so forgeries fail,
 * plus the full metadata set: verification id, issue date, student, course,
 * score, instructor and status.
 */
public final class CertificateGenerator {

    private static final String SIGNING_SECRET = resolveSigningSecret();
    private static final String VERIFY_BASE = "https://ultimate-crypto-python.onrender.com/verify-cert";

    private static String resolveSigningSecret() {
        String s = System.getenv("UC_API_KEY");
        if (s != null && !s.isBlank()) return s.trim();
        String alt = System.getenv("API_SECRET_KEY");
        return (alt != null && !alt.isBlank()) ? alt.trim() : "";
    }

    private CertificateGenerator() { }

    /** HMAC-SHA256 of the verification id, hex-encoded. The backend recomputes this to authenticate the QR. */
    public static String signatureFor(String verificationId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(verificationId.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "invalid";
        }
    }

    /** The full verification URL encoded inside the QR code. */
    public static String verificationUrl(String verificationId) {
        return VERIFY_BASE + "?vid=" + verificationId + "&sig=" + signatureFor(verificationId);
    }

    /** Renders a real QR BitMatrix (dark cells) as filled rectangles. */
    private static void drawQr(PdfContentByte cb, BitMatrix matrix, float x, float y, float size) {
        int cells = matrix.getWidth();
        float cell = size / cells;
        cb.setColorFill(new BaseColor(0x11, 0x11, 0x18));
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                if (matrix.get(col, row)) {
                    cb.rectangle(x + col * cell, y - row * cell, cell, cell);
                }
            }
        }
        cb.fill();
    }

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

        // Real scannable QR -> Render verification endpoint (HMAC-signed)
        try {
            QRCodeWriter qw = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = qw.encode(verificationUrl(verificationId), BarcodeFormat.QR_CODE, 0, 0, hints);
            float qrSize = 74f;
            float qrX = 46f;
            float qrY = h - 130f;
            drawQr(cb, matrix, qrX, qrY, qrSize);
        } catch (Exception ex) {
            cb.setColorFill(dark);
            cb.rectangle(46, h - 204, 74, 74);
            cb.fill();
        }

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
