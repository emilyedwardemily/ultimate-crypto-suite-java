package steganography;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import crypto.AESUtil; 

/**
 * UC-Suite PRO: StegEngine V14.0 [PRODUCTION]
 * Feature: Double-Layer Security (AES + LSB Steganography)
 * Fixes: Memory-efficient Pixel Handling & Type Safety
 */
public class StegTool {
    private static final int MAGIC_NUMBER = 0x53544547; // "STEG" katika Hex

    /**
     * ENCODE: Inaficha ujumbe uliotiwa siri (AES) ndani ya picha.
     */
    public static void encode(File coverImage, String message, String key, String outPath) throws Exception {
        // 1. GHOST FEATURE: Encrypt message kwanza kwa AES-256
        String encryptedPayload = AESUtil.encrypt(message, key);
        byte[] msgBytes = encryptedPayload.getBytes(StandardCharsets.UTF_8);

        // 2. IMAGE PREPARATION: Kuzuia ClassCastException na kuhakikisha muundo wa RGB
        BufferedImage original = ImageIO.read(coverImage);
        BufferedImage cleanImg = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g = cleanImg.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Andaa payload (Header: Magic Number + Length | Body: Encrypted Message)
        ByteBuffer buffer = ByteBuffer.allocate(8 + msgBytes.length);
        buffer.putInt(MAGIC_NUMBER);
        buffer.putInt(msgBytes.length);
        buffer.put(msgBytes);
        byte[] payload = buffer.array();

        int width = cleanImg.getWidth();
        int height = cleanImg.getHeight();
        
        // Pata array ya pixels zote
        int[] pixels = cleanImg.getRGB(0, 0, width, height, null, 0, width);

        // 3. CAPACITY CHECK: Hakikisha picha ina nafasi ya kutosha (1 bit kwa kila pixel)
        if (payload.length * 8 > pixels.length) {
            throw new Exception("Intelligence Payload too large for this image dimensions!");
        }

        // 4. LSB INJECTION: Ingiza payload kwenye Least Significant Bit
        for (int i = 0; i < payload.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                int bitValue = (payload[i] >> (7 - bit)) & 1;
                int pixelIndex = i * 8 + bit;
                // Badilisha bit ya mwisho pekee (LSB)
                pixels[pixelIndex] = (pixels[pixelIndex] & 0xFFFFFFFE) | bitValue;
            }
        }

        // 5. SAVE: Rudisha pixels na hifadhi kama PNG (PNG haina 'compression' inayoharibu bits)
        cleanImg.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(cleanImg, "png", new File(outPath));
        System.out.println("🖼️ [STEG] Payload embedded successfully: " + outPath);
    }

    /**
     * DECODE: Inatafuta na kufungua ujumbe wa siri kutoka kwenye pixels za picha.
     */
    public static String decode(File stegoImage, String key) throws Exception {
        BufferedImage img = ImageIO.read(stegoImage);
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);

        // 1. READ HEADER: Pata Magic Number (4 bytes) na Length (4 bytes)
        byte[] header = new byte[8];
        for (int i = 0; i < 64; i++) {
            int bit = pixels[i] & 1;
            header[i / 8] = (byte) ((header[i / 8] << 1) | bit);
        }

        ByteBuffer hb = ByteBuffer.wrap(header);
        int magic = hb.getInt();
        if (magic != MAGIC_NUMBER) {
            throw new Exception("SECURITY ALERT: This image does not contain UC-Suite intelligence data!");
        }

        int len = hb.getInt();
        if (len <= 0 || (len * 8 + 64) > pixels.length) {
            throw new Exception("CORRUPTION: Encrypted payload length is invalid.");
        }

        // 2. READ ENCRYPTED BODY
        byte[] payload = new byte[len];
        for (int i = 0; i < len * 8; i++) {
            int bit = pixels[i + 64] & 1;
            payload[i / 8] = (byte) ((payload[i / 8] << 1) | bit);
        }

        // 3. DECRYPT: Rudisha ujumbe asilia
        String encryptedData = new String(payload, StandardCharsets.UTF_8);
        return AESUtil.decrypt(encryptedData, key);
    }
}