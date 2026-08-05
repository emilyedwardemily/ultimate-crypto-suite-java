package app;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ULTIMATE CRYPTO SUITE - RUNTIME ANTI-TAMPER / ANTI-DEBUG GUARD
 *
 * Performs two checks at startup:
 *  1. ANTI-DEBUG: aborts if the JVM was started with a debugger/attaching agent
 *     (-agentlib:jdwp, -Xdebug, -Xrunjdwp or debug flags in JAVA_TOOL_OPTIONS).
 *  2. ANTI-TAMPER: in a packaged (jar-based) build, computes SHA-256 over every
 *     entry of its own archive (excluding the self-describing tamper resource,
 *     the manifest and signature files) and compares it against the expected
 *     hash embedded in the archive. Any modification of the shipped binary
 *     therefore invalidates the hash and aborts startup.
 *
 * The expected hash is injected by the build pipeline (see scripts/hash-and-sign.sh)
 * into the META-INF/self.tamper resource AFTER obfuscation and packaging, so it is
 * not circular: the resource is excluded from the hashed content.
 */
public final class TamperGuard {

    private static final String TAMPER_RESOURCE = "META-INF/self.tamper";

    private TamperGuard() {
    }

    /** Runs all integrity checks. Returns true when the runtime is trustworthy. */
    public static boolean verify() {
        boolean debugOk = !debuggerDetected();
        boolean tamperOk = tamperCheck();
        return debugOk && tamperOk;
    }

    /**
     * Detects a JVM launched for debugging / under a profiler or debug agent.
     */
    private static boolean debuggerDetected() {
        try {
            RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
            List<String> args = mx.getInputArguments();
            for (String a : args) {
                String lower = a.toLowerCase();
                if (lower.contains("jdwp") || lower.contains("-xdebug")
                        || lower.contains("-xrunjdwp") || lower.contains("debug")) {
                    System.err.println("[SECURITY] debug flag detected: " + a);
                    return true;
                }
            }

            String tool = System.getenv("JAVA_TOOL_OPTIONS");
            if (tool != null && (tool.toLowerCase().contains("jdwp")
                    || tool.toLowerCase().contains("-agentlib"))) {
                System.err.println("[SECURITY] JAVA_TOOL_OPTIONS debug agent: " + tool);
                return true;
            }
            String jdkOpt = System.getenv("JDK_JAVA_OPTIONS");
            if (jdkOpt != null && (jdkOpt.toLowerCase().contains("jdwp")
                    || jdkOpt.toLowerCase().contains("-agentlib"))) {
                System.err.println("[SECURITY] JDK_JAVA_OPTIONS debug agent: " + jdkOpt);
                return true;
            }
            return false;
        } catch (Throwable t) {
            // If we cannot even inspect the runtime, fail closed.
            System.err.println("[SECURITY] runtime inspection failed: " + t);
            return true;
        }
    }

    /**
     * Computes the SHA-256 over all archive entries (canonical order), excluding
     * the tamper resource itself, the manifest and signature files, then compares
     * it with the expected hash inside the tamper resource.
     */
    private static boolean tamperCheck() {
        Path jarPath = ownArchivePath();
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            // Running from a class directory (e.g. mvn javafx:run) or an IDE.
            // Nothing to verify until we are launched from a packaged archive.
            return true;
        }

        String expected;
        try (InputStream in = TamperGuard.class.getClassLoader().getResourceAsStream(TAMPER_RESOURCE)) {
            if (in == null) {
                // Packaged build must always carry the tamper resource.
                System.err.println("[SECURITY] Integrity manifest missing - refusing to start.");
                return false;
            }
            expected = new String(readAll(in), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            System.err.println("[SECURITY] Could not read integrity manifest: " + e.getMessage());
            return false;
        }
        if (expected == null || expected.length() != 64) {
            System.err.println("[SECURITY] Malformed integrity manifest.");
            return false;
        }

        String actual;
        try {
            actual = sha256OfArchive(jarPath);
        } catch (Exception e) {
            System.err.println("[SECURITY] Could not hash archive: " + e.getMessage());
            return false;
        }

        if (!actual.equalsIgnoreCase(expected)) {
            System.err.println("[SECURITY] Binary integrity check FAILED - application has been modified.");
            return false;
        }
        return true;
    }

    /** Returns the path of the running archive, or null when launched from classes. */
    private static Path ownArchivePath() {
        try {
            var loc = TamperGuard.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc == null) {
                return null;
            }
            return Path.of(loc.toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String sha256OfArchive(Path jarPath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream().forEach(e -> names.add(e.getName()));
            Collections.sort(names);
            for (String name : names) {
                if (isExcludedEntry(name)) {
                    continue;
                }
                JarEntry entry = jar.getJarEntry(name);
                byte[] content = readAll(jar.getInputStream(entry));
                digest.update(content);
            }
        }
        return toHex(digest.digest());
    }

    private static boolean isExcludedEntry(String name) {
        if (name.equals(TAMPER_RESOURCE)) {
            return true;
        }
        String upper = name.toUpperCase();
        return upper.equals("META-INF/MANIFEST.MF")
                || upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".RSA")
                    || upper.endsWith(".DSA") || upper.endsWith(".EC"));
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
