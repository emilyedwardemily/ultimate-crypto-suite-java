package ui;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;

import org.json.JSONArray;
// --- JSON & COLLECTIONS IMPORTS ---
import org.json.JSONObject;

import academy.AcademyService;
import academy.AcademyService.GlobalPosition;
import academy.AcademyService.Standing;
import academy.AcademyUi;
import academy.Challenge;
import app.ApiClient;
import app.ApiException;
import app.DatabaseManager;
import app.LicenseManager;
import crypto.XORUtil;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import steganography.StegTool;

public class Dashboard extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(Dashboard.class);
    private static final String BG_DARK = "-fx-background-color: #050505;";
    private static final String TITLE_STYLE = "-fx-text-fill: #39FF14; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Courier New';";
    private static final String BTN_GREEN_BOLD = "-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String JSON_RESULT = "result";
    private static final String JSON_ACTION = "action";
    private static final String JSON_MODULE = "module";
    private static final String BTN_GREEN = "-fx-background-color: #238636; -fx-text-fill: white;";
    private static final String BTN_BLUE = "-fx-background-color: #58a6ff; -fx-text-fill: white;";
    private static final String BTN_PURPLE = "-fx-background-color: #8957e5; -fx-text-fill: white;";
    private static final String BTN_PURPLE_BOLD = "-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String OUTPUT_STYLE = "-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-border-width: 0.3;";
    private static final String LABEL_COLOR = "#8b949e";

    private TextArea inputArea;
    private TextArea outputArea;
    private PasswordField keyField;
    private TextField mfaField; 
    private Label signatureLabel;
    private Label mfaStatusLabel;
    private ProgressBar strengthMeter;
    private VBox terminalLogs;
    private String operatorID;
    private File selectedImageFile; 
    private TableView<JSONObject> auditTable;

    private static int totalXP = 0;
    private static int completedChallenges = 0;
    private static final int CHALLENGE_COUNT = 284;
    private static final java.util.Map<String, Integer> leaderboard = new java.util.HashMap<>();
    private static final java.util.Set<String> achievedBadges = new java.util.LinkedHashSet<>();
    private static String diffFilter = "ALL";

    // --- FORTRESS ACADEMY v2 (progress service + navigation tracking) ---
    private final AcademyService academy;
    private boolean academyActive = false;
    private int practiceTick = 0;
    private AnimationTimer academyTimer;
    private AnimationTimer missionClock;
    private String leaderTab = "global";
    private boolean suppressBadgeAnimations = true;
    private final java.util.ArrayDeque<String> badgeAnimQueue = new java.util.ArrayDeque<>();
    private boolean badgeAnimActive = false;

    private static final String[][] RANKS = {
        {"0",     "Script Kiddie"},
        {"200",   "Cipher Punk"},
        {"500",   "Code Breaker"},
        {"900",   "Crypto Analyst"},
        {"1400",  "Cipher Specialist"},
        {"2000",  "Cryptographer General"},
        {"2700",  "Military-Grade Specialist"}
    };

    private static final java.util.LinkedHashMap<String, String[]> ALL_BADGES = new java.util.LinkedHashMap<>();
    static {
        ALL_BADGES.put("first_blood",     new String[]{"\uD83E\uDE78", "First Blood"});
        ALL_BADGES.put("caesar_slayer",   new String[]{"\u2694\uFE0F", "Caesar Slayer"});
        ALL_BADGES.put("xor_master",      new String[]{"\uD83D\uDD37", "XOR Master"});
        ALL_BADGES.put("cipher_app",      new String[]{"\uD83C\uDF00", "Cipher Apprentice"});
        ALL_BADGES.put("cipher_expert",   new String[]{"\uD83D\uDD2E", "Cipher Expert"});
        ALL_BADGES.put("cipher_master",   new String[]{"\uD83D\uDC51", "Cipher Master"});
        ALL_BADGES.put("level_five",      new String[]{"\u2B50", "Level 5"});
        ALL_BADGES.put("xp_century",      new String[]{"\uD83D\uDCAF", "Century (1000 XP)"});
        ALL_BADGES.put("completionist",   new String[]{"\uD83C\uDFC1", "Completionist"});
        ALL_BADGES.put("daily_grinder",   new String[]{"\uD83C\uDFC5", "Daily Grinder"});
        ALL_BADGES.put("daily_streak5",   new String[]{"\uD83D\uDCC5", "5-Day Mission Streak"});
        ALL_BADGES.put("daily_master",    new String[]{"\uD83D\uDD11", "Daily Mission Master"});
        ALL_BADGES.put("weekly_raider",   new String[]{"\uD83D\uDCC6", "Weekly Raider"});
        ALL_BADGES.put("weekend_warrior", new String[]{"\uD83C\uDFF0\uFE0F", "Weekend Warrior"});
        ALL_BADGES.put("caesar_master",   new String[]{"\uD83C\uDFC6", "Caesar Master"});
        ALL_BADGES.put("aes_beginner",    new String[]{"\uD83D\uDD12", "AES Beginner"});
        ALL_BADGES.put("aes_expert",      new String[]{"\uD83D\uDD10", "AES Expert"});
        ALL_BADGES.put("rsa_wizard",      new String[]{"\uD83E\uDDE9", "RSA Wizard"});
        ALL_BADGES.put("xor_killer",      new String[]{"\uD83D\uDCA5", "XOR Killer"});
        ALL_BADGES.put("hash_master",     new String[]{"\uD83D\uDD22", "Hash Master"});
        ALL_BADGES.put("stego_hunter",    new String[]{"\uD83D\uDD0E", "Stego Hunter"});
        ALL_BADGES.put("crypto_legend",   new String[]{"\uD83D\uDC51", "Crypto Legend"});
        ALL_BADGES.put("elite_analyst",   new String[]{"\uD83D\uDC5C", "Elite Analyst"});
        ALL_BADGES.put("soc_defender",    new String[]{"\uD83D\uDEE1\uFE0F", "SOC Defender"});
        ALL_BADGES.put("cyber_guardian",  new String[]{"\uD83D\uDCAA", "Cyber Guardian"});
        ALL_BADGES.put("hundred_percent", new String[]{"\uD83C\uDFAF", "100% Completion"});
        ALL_BADGES.put("perfect_score",   new String[]{"\uD83C\uDF3F", "Perfect Score"});
        ALL_BADGES.put("speed_runner",    new String[]{"\uD83D\uDC80", "Speed Runner"});
        ALL_BADGES.put("no_hint_champion", new String[]{"\uD83E\uDDE7", "No Hint Champion"});
    }
    
    private static final String API_SECRET_KEY = "Emily_Crypto_Secure_2026_KIU";
    private static final String PYTHON_URL = "https://ultimate-crypto-python.onrender.com";
    private static final String NODE_URL = "https://ultimate-crypto-node-gateway.onrender.com";

    public Dashboard() {
        String hwid = LicenseManager.getHardwareID();
        this.operatorID = "UC-PRO-" + Integer.toHexString(hwid.hashCode()).toUpperCase();

        // Fortress Academy v2: load persisted progress and restore legacy statics
        this.academy = new AcademyService(this.operatorID);
        academy.load();
        for (java.util.Map.Entry<String, Integer> e : academy.getSolved().entrySet()) {
            leaderboard.put(operatorID + "_" + e.getKey(), e.getValue());
        }
        totalXP = academy.getTotalXp();
        completedChallenges = academy.getSolvedCount();
        computeBadges();
        startAcademyTimer();

        setStyle("-fx-background-color: #050505; -fx-border-color: #39FF14; -fx-border-width: 0.5;"); 
        setLeft(createSidebar());
        showAESModule(); 
        
        addLog("DEEP DEFENSE: V20.4 OBSIDIAN KERNEL LOADED.");
        addLog("AUTH STATUS: " + operatorID + " ATTACHED.");
        addLog("[ACADEMY] Fortress profile restored: " + totalXP + " XP, " + completedChallenges + " challenges.");
        // Kurekodi boot kwenye Atlas
        sendAuditLog("SYSTEM_BOOT", "CORE_KERNEL");
        suppressBadgeAnimations = false;
    }

    /**
     * Tracks practice time while the operator stays inside the Academy,
     * persisting a 30-second batch every half minute to avoid disk chatter.
     */
    private void startAcademyTimer() {
        academyTimer = new AnimationTimer() {
            private long lastNanos = -1;
            @Override
            public void handle(long now) {
                if (lastNanos < 0) { lastNanos = now; return; }
                long elapsedMs = (now - lastNanos) / 1_000_000;
                lastNanos = now;
                if (!academyActive) { practiceTick = 0; return; }
                practiceTick += (int) elapsedMs;
                while (practiceTick >= 30_000) {
                    practiceTick -= 30_000;
                    academy.addPracticeSeconds(30);
                }
            }
        };
        academyTimer.start();
    }

    /** Flushes any remaining partial practice time when leaving the Academy. */
    private void flushPracticeTime() {
        if (practiceTick >= 1000) {
            academy.addPracticeSeconds(practiceTick / 1000);
            practiceTick = 0;
        }
    }

    // --- STEGANOGRAPHY CORE LOGIC (REFIXED) ---

    private void handleHideData() {
        if (selectedImageFile == null) {
            addLog("[DENIED] No carrier image selected.");
            return;
        }
        if (keyField.getText().isEmpty()) {
            addLog("[DENIED] Security Key required for GHOST encryption.");
            return;
        }

        String rawMessage = inputArea.getText();
        String secretKey = keyField.getText();
        String outputPath = "stego_output.png";

        VBox main = (VBox) getCenter();
        Button btnAbort = new Button("🛑 TERMINATE PROCESS");
        btnAbort.setStyle("-fx-background-color: #f85149; -fx-text-fill: white; -fx-font-weight: bold;");
        
        main.getChildren().add(main.getChildren().size() - 1, btnAbort);

        Task<Void> stegTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("INIT: Establishing secure link...");
                updateProgress(1, 4); Thread.sleep(300);
                updateMessage("GHOST: Running AES-256 Payload Encryption...");
                updateProgress(2, 4); Thread.sleep(300);
                updateMessage("LSB: Injecting intelligence into pixels...");
                StegTool.encode(selectedImageFile, rawMessage, secretKey, outputPath);
                updateProgress(4, 4);
                updateMessage("DONE: Forensic Stealth Mode active.");
                return null;
            }
        };

        strengthMeter.progressProperty().bind(stegTask.progressProperty());
        stegTask.messageProperty().addListener((obs, old, msg) -> addLog(msg));

        stegTask.setOnSucceeded(event -> {
            main.getChildren().remove(btnAbort);
            strengthMeter.progressProperty().unbind();
            selectedImageFile = new File(outputPath);
            addLog("[SUCCESS] Intelligence hidden in: " + outputPath);
            addLog("[SYSTEM] Carrier updated to stego_output.png");
            sendAuditLog("STEG_HIDE", "STEGANOGRAPHY");
            revealFileInExplorer(selectedImageFile);
        });

        stegTask.setOnFailed(event -> {
            main.getChildren().remove(btnAbort);
            strengthMeter.progressProperty().unbind();
            addLog("[CRITICAL] Process failed.");
        });

        Thread stegThread = new Thread(stegTask);
        stegThread.setDaemon(true);
        stegThread.start();
    }

   private void setupLayout(String titleStr, String inputHint) {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Label title = new Label(titleStr);
        title.setStyle(TITLE_STYLE);

        // --- MFA GATE WITH VERIFY & EMAIL BUTTONS ---
        HBox mfaBar = new HBox(10);
        mfaBar.setAlignment(Pos.CENTER_LEFT);
        
        mfaField = new TextField();
        mfaField.setPromptText("OTP CODE");
        mfaField.setPrefWidth(90);
        mfaField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d;");

        Button btnVerify = new Button("VERIFY");
        btnVerify.setStyle(BTN_GREEN_BOLD);
        btnVerify.setOnAction(e -> handleOTPVerification()); 

        Button btnEmail = new Button("SEND EMAIL");
        btnEmail.setStyle(BTN_BLUE);
        btnEmail.setOnAction(e -> handleSendEmailOTP());

        mfaStatusLabel = new Label("LOCKED");
        mfaStatusLabel.setTextFill(Color.web("#f85149"));
        mfaStatusLabel.setStyle("-fx-font-weight: bold;");

        mfaBar.getChildren().addAll(new Label("MFA GATE:"), mfaField, btnVerify, btnEmail, mfaStatusLabel);
        // --------------------------------------------

        inputArea = new TextArea();
        inputArea.setPromptText(inputHint);
        inputArea.setPrefHeight(130);
        inputArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3; -fx-border-color: #30363d;");

        keyField = new PasswordField();
        keyField.setPromptText("Security Key / Passphrase...");
        keyField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d;");
        
        strengthMeter = new ProgressBar(0);
        strengthMeter.setPrefWidth(Double.MAX_VALUE);
        keyField.textProperty().addListener((obs, old, val) -> updateStrength(val));

        signatureLabel = new Label("INTEGRITY SEAL: PENDING GENESIS");
        signatureLabel.setMaxWidth(Double.MAX_VALUE);
        signatureLabel.setStyle("-fx-background-color: #161b22; -fx-text-fill: #8b949e; -fx-padding: 10; -fx-border-color: #30363d;");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(130);
        outputArea.setStyle(OUTPUT_STYLE);

        terminalLogs = new VBox(3);
        terminalLogs.setPadding(new Insets(10));
        terminalLogs.setStyle("-fx-background-color: #000000;"); 
        ScrollPane logScroll = new ScrollPane(terminalLogs);
        logScroll.setPrefHeight(110);
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: #000000; -fx-background-color: #000000; -fx-border-color: #39FF14; -fx-border-width: 0.3;");

        HBox actionBox = new HBox(20);
        Button btnSync = new Button("🛡️ QUANTUM SYNC");
        btnSync.setStyle(BTN_GREEN);
        btnSync.setOnAction(e -> handleSecureSync(titleStr));

        Button btnSign = new Button("🖋️ RSA SIGN");
        btnSign.setStyle(BTN_PURPLE);
        btnSign.setOnAction(e -> handleDigitalSignature());
        actionBox.getChildren().addAll(btnSign, btnSync);

        main.getChildren().addAll(title, mfaBar, new Label("PLAINTEXT / CIPHERTEXT:"), inputArea, 
                                  new Label("SECURITY KEY:"), keyField, strengthMeter, 
                                  signatureLabel, new Label("RESULT MATRICES:"), outputArea, 
                                  actionBox, new Label("REAL-TIME FORENSICS:"), logScroll);
        
        main.getChildren().forEach(n -> { if (n instanceof Label label && n != title) label.setTextFill(Color.web(LABEL_COLOR)); });
        setCenter(main);
    }

    // --- MODULE VIEWS ---

    private void showAESModule() {
        setupLayout("AES-256 CASCADING ENGINE", "Enter intelligence for hybrid encryption...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button enc = new Button("ENCRYPT");
        Button dec = new Button("DECRYPT");
        enc.setStyle(BTN_BLUE);
        dec.setStyle(BTN_BLUE);
        enc.setOnAction(e -> executeAES(true));
        dec.setOnAction(e -> executeAES(false));
        hb.getChildren().addAll(enc, dec);
        main.getChildren().add(9, hb);
    }

    private void showPGPModule() {
        setupLayout("PGP (PRETTY GOOD PRIVACY)", "Enter message for OpenPGP standard...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button enc = new Button("PGP ENCRYPT");
        Button send = new Button("🚀 DISPATCH SECURE");
        enc.setStyle(BTN_GREEN);
        send.setStyle("-fx-background-color: #d73a49; -fx-text-fill: white; -fx-font-weight: bold;");
        enc.setOnAction(e -> executePythonService("/pgp-encrypt", "[PGP] Encrypting..."));
        send.setOnAction(e -> handleSecureDispatch("PGP Dispatch"));
        hb.getChildren().addAll(enc, send);
        main.getChildren().add(9, hb);
    }

    private void showSMIMEModule() {
        setupLayout("S/MIME EMAIL SECURITY", "Prepare secure MIME data...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button btnCert = new Button("GENERATE X.509 CERT");
        Button send = new Button("🚀 DISPATCH SECURE");
        btnCert.setStyle(BTN_BLUE);
        send.setStyle("-fx-background-color: #d73a49; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCert.setOnAction(e -> executePythonService("/smime-gen", "[S/MIME] Generating Certificate..."));
        send.setOnAction(e -> handleSecureDispatch("S/MIME Dispatch"));
        hb.getChildren().addAll(btnCert, send);
        main.getChildren().add(9, hb);
    }

    private void showRSAModule() {
        setupLayout("RSA-4096 ASYMMETRIC GATE", "Generate Public/Private keys...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button gen = new Button("GENERATE KEYPAIR");
        Button enc = new Button("RSA ENCRYPT");
        gen.setStyle(BTN_PURPLE);
        enc.setStyle(BTN_BLUE);
        gen.setOnAction(e -> executePythonService("/rsa-keygen", "[RSA] Initiating..."));
        enc.setOnAction(e -> executePythonService("/rsa-encrypt", "[RSA] Applying Cipher..."));
        hb.getChildren().addAll(gen, enc);
        main.getChildren().add(9, hb);
    }

    private void showXORModule() {
        setupLayout("XOR BITWISE ENGINE", "Fast bitwise transformation...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button enc = new Button("XOR ENCRYPT");
        Button dec = new Button("XOR DECRYPT");
        enc.setStyle(BTN_BLUE);
        dec.setStyle(BTN_PURPLE);
        enc.setOnAction(e -> {
            outputArea.setText(XORUtil.encrypt(inputArea.getText(), keyField.getText()));
            sendAuditLog("XOR_ENCRYPT", "XOR");
        });
        dec.setOnAction(e -> {
            outputArea.setText(XORUtil.decrypt(inputArea.getText(), keyField.getText()));
            sendAuditLog("XOR_DECRYPT", "XOR");
        });
        hb.getChildren().addAll(enc, dec);
        main.getChildren().add(9, hb);
    }

    private void showPasswordGenerator() {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Label title = new Label("🔑 PASSWORD GENERATOR");
        title.setStyle(TITLE_STYLE);

        Label lenLabel = new Label("LENGTH: 16");
        lenLabel.setStyle("-fx-text-fill: #39FF14;");
        Slider lenSlider = new Slider(8, 64, 16);
        lenSlider.setShowTickLabels(true);
        lenSlider.setShowTickMarks(true);
        lenSlider.setMajorTickUnit(8);
        lenSlider.valueProperty().addListener((obs, old, val) -> lenLabel.setText("LENGTH: " + val.intValue()));

        CheckBox chkUpper = new CheckBox("UPPERCASE (A-Z)");
        CheckBox chkLower = new CheckBox("lowercase (a-z)");
        CheckBox chkDigits = new CheckBox("Digits (0-9)");
        CheckBox chkSymbols = new CheckBox("Symbols (!@#$)");
        chkUpper.setSelected(true);
        chkLower.setSelected(true);
        chkDigits.setSelected(true);
        chkSymbols.setSelected(true);
        for (CheckBox cb : new CheckBox[]{chkUpper, chkLower, chkDigits, chkSymbols}) {
            cb.setStyle("-fx-text-fill: #e6edf3; -fx-background-color: #0d1117; -fx-border-color: #30363d;");
        }

        VBox optionsBox = new VBox(8, chkUpper, chkLower, chkDigits, chkSymbols);
        optionsBox.setPadding(new Insets(10));
        optionsBox.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-radius: 4;");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(80);
        outputArea.setStyle(OUTPUT_STYLE);

        Button btnGenerate = new Button("⚡ GENERATE");
        btnGenerate.setStyle(BTN_GREEN_BOLD);
        Button btnCopy = new Button("📋 COPY");
        btnCopy.setStyle(BTN_BLUE);

        btnGenerate.setOnAction(e -> {
            String pw = generatePassword(
                (int) lenSlider.getValue(),
                chkUpper.isSelected(),
                chkLower.isSelected(),
                chkDigits.isSelected(),
                chkSymbols.isSelected()
            );
            outputArea.setText(pw);
            addLog("[PASSGEN] Generated " + pw.length() + "-char password.");
            sendAuditLog("PASSWORD_GEN", "PASSGEN");
        });

        btnCopy.setOnAction(e -> {
            String pw = outputArea.getText();
            if (!pw.isEmpty()) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(pw);
                clipboard.setContent(content);
                addLog("[PASSGEN] Password copied to clipboard.");
            }
        });

        HBox controls = new HBox(10, btnGenerate, btnCopy);

        main.getChildren().addAll(title, lenLabel, lenSlider, new Label("CHARACTER SETS:"), optionsBox,
                                  new Label("GENERATED PASSWORD:"), outputArea, controls);
        main.getChildren().forEach(n -> {
            if (n instanceof Label label && n != title) label.setTextFill(Color.web(LABEL_COLOR));
        });
        setCenter(main);
    }

    private static final java.security.SecureRandom RNG = new java.security.SecureRandom();

    private String generatePassword(int length, boolean upper, boolean lower, boolean digits, boolean symbols) {
        StringBuilder pool = new StringBuilder();
        if (upper) pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (lower) pool.append("abcdefghijklmnopqrstuvwxyz");
        if (digits) pool.append("0123456789");
        if (symbols) pool.append("!@#$%^&*()_+-=[]{}|;:',.<>?/`~");
        if (pool.isEmpty()) pool.append("abcdefghijklmnopqrstuvwxyz");

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(pool.charAt(RNG.nextInt(pool.length())));
        }
        return result.toString();
    }

    private void showStegModule() {
        setupLayout("IMAGE STEGANOGRAPHY", "Hide intelligence inside carrier pixels...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button btnSel = new Button("📸 SELECT IMAGE");
        Button btnHide = new Button("🔒 HIDE DATA");
        Button btnExt = new Button("🔓 EXTRACT DATA");
        Button btnSave = new Button("💾 SAVE AS");

        btnSave.setStyle(BTN_GREEN);
        btnSave.setOnAction(e -> handleSaveImage());

        btnSel.setOnAction(e -> handleSelectImage());

        btnHide.setOnAction(e -> handleHideData());

        btnExt.setOnAction(e -> handleExtractData());

        hb.getChildren().addAll(btnSel, btnHide, btnExt, btnSave);
        main.getChildren().add(9, hb);
    }

    private void handleSaveImage() {
        if (selectedImageFile == null) {
            addLog("[DENIED] No image to save.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Secure Intelligence");
        fc.setInitialFileName("stego_result.png");
        File file = fc.showSaveDialog(null);
        if (file == null) return;
        try {
            java.nio.file.Files.copy(selectedImageFile.toPath(), file.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            addLog("[SAVED] Image stored at: " + file.getAbsolutePath());
        } catch (Exception ex) {
            addLog("[ERROR] Save failed.");
        }
    }

    private void handleSelectImage() {
        selectedImageFile = new FileChooser().showOpenDialog(null);
        if (selectedImageFile != null) {
            addLog("[FILE] Loaded: " + selectedImageFile.getName());
        }
    }

    private void handleExtractData() {
        if (selectedImageFile == null) {
            addLog("[INFO] Select carrier image first.");
            selectedImageFile = new FileChooser().showOpenDialog(null);
        }
        if (selectedImageFile == null) return;
        new Thread(() -> {
            try {
                addLog("[SCAN] Analyzing: " + selectedImageFile.getName());
                String recovered = StegTool.decode(selectedImageFile, keyField.getText());
                Platform.runLater(() -> {
                    outputArea.setText(recovered);
                    addLog("[SUCCESS] Intelligence recovered.");
                    sendAuditLog("STEG_EXTRACT", "STEGANOGRAPHY");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[ERROR] " + ex.getMessage()));
            }
        }).start();
    }

    private void showShamirModule() {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Label title = new Label("🔀 SHAMIR SECRET SPLITTER");
        title.setStyle(TITLE_STYLE);

        Label modeLabel = new Label("MODE:");
        modeLabel.setTextFill(Color.web(LABEL_COLOR));
        ComboBox<String> modeBox = new ComboBox<>();
        modeBox.getItems().addAll("SPLIT SECRET", "RECONSTRUCT");
        modeBox.setValue("SPLIT SECRET");
        modeBox.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");

        TextArea secretInput = new TextArea();
        secretInput.setPromptText("Enter secret to split...");
        secretInput.setPrefHeight(80);
        secretInput.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3; -fx-border-color: #30363d;");

        HBox paramRow = new HBox(10);
        TextField nField = new TextField("5");
        nField.setPromptText("Total Shares (n)");
        nField.setPrefWidth(120);
        nField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d;");
        TextField kField = new TextField("3");
        kField.setPromptText("Threshold (k)");
        kField.setPrefWidth(120);
        kField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d;");
        paramRow.getChildren().addAll(nField, kField);
        paramRow.setVisible(true);

        TextArea sharesInput = new TextArea();
        sharesInput.setPromptText("Paste shares JSON array here...");
        sharesInput.setPrefHeight(120);
        sharesInput.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3; -fx-border-color: #30363d;");
        sharesInput.setVisible(false);

        modeBox.setOnAction(e -> {
            boolean splitMode = modeBox.getValue().equals("SPLIT SECRET");
            paramRow.setVisible(splitMode);
            nField.setVisible(splitMode);
            kField.setVisible(splitMode);
            sharesInput.setVisible(!splitMode);
            secretInput.setPromptText(splitMode ? "Enter secret to split..." : "Enter share count (ignored)");
        });

        TextArea outputAreaShamir = new TextArea();
        outputAreaShamir.setEditable(false);
        outputAreaShamir.setPrefHeight(150);
        outputAreaShamir.setStyle(OUTPUT_STYLE);

        HBox actionRow = new HBox(10);
        Button btnExecute = new Button("⚡ EXECUTE");
        btnExecute.setStyle(BTN_GREEN_BOLD);

        Button btnCopyShares = new Button("📋 COPY SHARES");
        btnCopyShares.setStyle(BTN_BLUE);

        btnExecute.setOnAction(e -> {
            if (modeBox.getValue().equals("SPLIT SECRET")) {
                handleShamirSplit(secretInput, nField, kField, outputAreaShamir);
            } else {
                handleShamirReconstruct(sharesInput, outputAreaShamir);
            }
        });

        btnCopyShares.setOnAction(e -> {
            String text = outputAreaShamir.getText();
            if (!text.isEmpty()) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
                addLog("[SHAMIR] Shares copied to clipboard.");
            }
        });

        actionRow.getChildren().addAll(btnExecute, btnCopyShares);

        main.getChildren().addAll(title, modeLabel, modeBox, new Label("SECRET / SHARES:"),
                secretInput, paramRow, sharesInput, actionRow,
                new Label("OUTPUT:"), outputAreaShamir);
        main.getChildren().forEach(n -> {
            if (n instanceof Label label && n != title) label.setTextFill(Color.web(LABEL_COLOR));
        });
        setCenter(main);
    }

    private void handleShamirSplit(TextArea secretInput, TextField nField, TextField kField, TextArea outputArea) {
        String secret = secretInput.getText().trim();
        if (secret.isEmpty()) {
            addLog("[DENIED] Secret cannot be empty.");
            return;
        }

        int n, k;
        try {
            n = Integer.parseInt(nField.getText().trim());
            k = Integer.parseInt(kField.getText().trim());
        } catch (NumberFormatException ex) {
            addLog("[DENIED] n and k must be valid numbers.");
            return;
        }

        if (k < 2) {
            addLog("[DENIED] Threshold k must be at least 2.");
            return;
        }
        if (n < k) {
            addLog("[DENIED] n must be >= k.");
            return;
        }

        int finalN = n;
        int finalK = k;
        new Thread(() -> {
            try {
                addLog("[SHAMIR] Splitting secret into " + finalN + " shares (k=" + finalK + ")...");
                JSONObject result = ApiClient.splitSecret(secret, finalN, finalK);
                JSONArray shares = result.getJSONArray("shares");
                StringBuilder sb = new StringBuilder();
                sb.append("Threshold: ").append(finalK).append("\n");
                sb.append("Total Shares: ").append(finalN).append("\n\n");
                for (int i = 0; i < shares.length(); i++) {
                    JSONObject share = shares.getJSONObject(i);
                    sb.append("Share ").append(share.getInt("index")).append(": ")
                      .append(share.getString("value")).append("\n");
                }
                Platform.runLater(() -> {
                    outputArea.setText(sb.toString());
                    addLog("[SUCCESS] Secret split into " + finalN + " shares.");
                    sendAuditLog("SHAMIR_SPLIT", "SECRET_SHARING");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[ERROR] Shamir split failed: " + ex.getMessage()));
            }
        }).start();
    }

    private void handleShamirReconstruct(TextArea sharesInput, TextArea outputArea) {
        String sharesJson = sharesInput.getText().trim();
        if (sharesJson.isEmpty()) {
            addLog("[DENIED] Paste shares JSON first.");
            return;
        }

        new Thread(() -> {
            try {
                JSONArray shares = new JSONArray(sharesJson);
                if (shares.length() < 2) {
                    Platform.runLater(() -> addLog("[DENIED] At least 2 shares required."));
                    return;
                }
                addLog("[SHAMIR] Reconstructing secret from " + shares.length() + " shares...");
                String secret = ApiClient.reconstructSecret(shares);
                Platform.runLater(() -> {
                    outputArea.setText("RECOVERED SECRET:\n" + secret);
                    addLog("[SUCCESS] Secret reconstructed from " + shares.length() + " shares.");
                    sendAuditLog("SHAMIR_RECONSTRUCT", "SECRET_SHARING");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[ERROR] Reconstruction failed: " + ex.getMessage()));
            }
        }).start();
    }

   private void showCaesarModule() {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Label title = new Label("CAESAR & LEGACY ENGINES");
        title.setStyle(TITLE_STYLE);

        // 1. Ongeza VIGENERE kwenye ComboBox
        ComboBox<String> cipherType = new ComboBox<>();
        cipherType.getItems().addAll("CAESAR SHIFT", "ROT13", "ATBASH (MIRROR)", "VIGENERE");
        cipherType.setValue("CAESAR SHIFT");
        cipherType.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");

        TextArea caesarIn = new TextArea(); 
        caesarIn.setPromptText("Enter plaintext or ciphertext...");
        caesarIn.setPrefHeight(100);
        caesarIn.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3; -fx-border-color: #30363d;");

        // 2. TextField ya Secret Key (kwa ajili ya Vigenère)
        TextField keyInput = new TextField();
        keyInput.setPromptText("ENTER VIGENERE KEY (e.g., KIU_STARS)...");
        keyInput.setManaged(false); // Haichukui nafasi kwanza
        keyInput.setVisible(false); // Ifiche kwanza
        keyInput.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-border-width: 0.5;");

        Label shiftValLabel = new Label("CURRENT SHIFT: 3");
        shiftValLabel.setStyle("-fx-text-fill: #39FF14;");
        Slider sl = new Slider(1, 25, 3);
        sl.valueProperty().addListener((obs, old, newVal) -> shiftValLabel.setText("CURRENT SHIFT: " + newVal.intValue()));

        // 3. Logic ya kuonyesha/kuficha vitu kulingana na algorithm
        cipherType.setOnAction(e -> {
            boolean isVigenere = cipherType.getValue().equals("VIGENERE");
            keyInput.setVisible(isVigenere);
            keyInput.setManaged(isVigenere); // Inatokea na kuchukua nafasi ikihitajika
            sl.setVisible(!isVigenere);      // Ficha slider kama ni Vigenere
            shiftValLabel.setVisible(!isVigenere);
            
            if(cipherType.getValue().equals("ROT13")) sl.setValue(13);
        });

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(100);
        outputArea.setStyle(OUTPUT_STYLE);

        HBox controls = new HBox(10);
        Button btnEnc = new Button("🔐 ENCRYPT");
        Button btnDec = new Button("🔓 DECRYPT");
        btnEnc.setStyle(BTN_GREEN_BOLD);
        btnDec.setStyle(BTN_PURPLE_BOLD);

        // 4. Update Actions kutuma pia Secret Key
        btnEnc.setOnAction(e -> handleLegacyCrypto(caesarIn.getText(), (int)sl.getValue(), cipherType.getValue(), true, keyInput.getText()));
        btnDec.setOnAction(e -> handleLegacyCrypto(caesarIn.getText(), (int)sl.getValue(), cipherType.getValue(), false, keyInput.getText()));

        controls.getChildren().addAll(btnEnc, btnDec);
        main.getChildren().addAll(title, new Label("SELECT ALGORITHM:"), cipherType, 
                                  new Label("INPUT:"), caesarIn, 
                                  keyInput, // Sehemu ya neno la siri
                                  shiftValLabel, sl, 
                                  controls, new Label("RESULT MATRICES:"), outputArea);
        
        main.getChildren().forEach(n -> { if (n instanceof Label label && n != title) label.setTextFill(Color.web(LABEL_COLOR)); });
        setCenter(main);
    }

    // 5. Updated handleLegacyCrypto inayopokea 'vigenereKey'
    private void handleLegacyCrypto(String text, int shift, String type, boolean encrypt, String vigenereKey) {
        new Thread(() -> {
            try {
                JSONObject p = new JSONObject();
                p.put("data", text);
                p.put("type", type.toLowerCase().replace(" ", "_"));
                p.put("shift", encrypt ? shift : -shift);
                p.put("key", vigenereKey); // Tuma siri kwenda Python
                
                String res = callSecurePython("/legacy-cipher", p);
                String result = new JSONObject(res).getString(JSON_RESULT);
                
                Platform.runLater(() -> {
                    outputArea.setText(result);
                    addLog("[LEGACY] " + type + " " + (encrypt ? "Encryption" : "Decryption") + " Done.");
                });
            } catch (Exception ex) { Platform.runLater(() -> addLog("[ERROR] Legacy Engine Fail.")); }
        }).start();
    }

    private void showIntegrityModule() {
        setupLayout("FORENSIC AUDIT TOOL", "Wipe or Hash Intelligence...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button btnHash = new Button("GENERATE FILE HASH");
        Button btnWipe = new Button("🛡️ SECURE WIPE");
        
        btnHash.setStyle(BTN_BLUE);
        btnWipe.setStyle("-fx-background-color: #f85149; -fx-text-fill: white;");
        
        btnHash.setOnAction(e -> {
            File f = new FileChooser().showOpenDialog(null);
            if (f != null) {
                try {
                    byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hash) sb.append(String.format("%02x", b));
                    outputArea.setText("FILE: " + f.getName() + "\nHASH: " + sb.toString());
                    addLog("[SCAN] Forensic scan complete.");
                    sendAuditLog("HASH_GEN", "FORENSICS");
                } catch (Exception ex) { addLog("[ERROR] Scan failed."); }
            }
        });

        btnWipe.setOnAction(e -> {
            File f = new FileChooser().showOpenDialog(null);
            if(f != null) {
                try {
                    JSONObject p = new JSONObject(); p.put("file_path", f.getAbsolutePath());
                    callSecurePython("/secure-wipe", p);
                    addLog("[VAPORIZED] " + f.getName() + " removed securely.");
                    sendAuditLog("SECURE_WIPE", "FORENSICS");
                } catch (Exception ex) { addLog("[ERROR] Wipe engine failed."); }
            }
        });

        hb.getChildren().addAll(btnHash, btnWipe);
        main.getChildren().add(9, hb);
    }





    // === CTF CHALLENGE DATA ===
    private static class ChallengeData {
        String id, title, descr, hint, flag, diff, family;
        int stars, xp;
        ChallengeData(String id, String title, int stars, int xp, String diff, String family, String descr, String hint, String flag) {
            this.id = id; this.title = title; this.stars = stars; this.xp = xp;
            this.diff = diff; this.family = family; this.descr = descr; this.hint = hint; this.flag = flag;
        }
    }

    private ChallengeData[] getChallenges() {
        return new ChallengeData[] {
            new ChallengeData("easy_1", "Caesar Shift 3", 1, 100, "EASY", "caesar",
                "Caesar +3. Decrypt: FLSKHU", "Shift each letter back by 3 positions.", "UC{CIPHER}"),
            new ChallengeData("easy_2", "Caesar Shift 7", 1, 100, "EASY", "caesar",
                "Caesar +7. Decrypt: ZLJYLA", "Shift each letter back by 7 positions.", "UC{SECRET}"),
            new ChallengeData("easy_3", "Caesar Shift 5", 1, 100, "EASY", "caesar",
                "Caesar +5. Decrypt: MFHPJW", "Shift each letter back by 5 positions.", "UC{HACKER}"),
            new ChallengeData("easy_4", "Caesar Shift 10", 1, 100, "EASY", "caesar",
                "Caesar +10. Decrypt: WYECO", "Shift each letter back by 10 positions.", "UC{MOUSE}"),
            new ChallengeData("easy_5", "Caesar Shift 13", 1, 100, "EASY", "caesar",
                "Caesar +13. Decrypt: PBQR", "Shift each letter back by 13 positions.", "UC{CODE}"),
            new ChallengeData("easy_6", "Caesar Shift 2", 1, 100, "EASY", "caesar",
                "Caesar +2. Decrypt: DAVG", "Shift each letter back by 2 positions.", "UC{BYTE}"),
            new ChallengeData("easy_7", "Caesar Shift 8", 1, 100, "EASY", "caesar",
                "Caesar +8. Decrypt: JQBA", "Shift each letter back by 8 positions.", "UC{BITS}"),
            new ChallengeData("easy_8", "Caesar Shift 11", 1, 100, "EASY", "caesar",
                "Caesar +11. Decrypt: VPJ", "Shift each letter back by 11 positions.", "UC{KEY}"),
            new ChallengeData("easy_9", "Caesar Shift 4", 1, 100, "EASY", "caesar",
                "Caesar +4. Decrypt: PSGO", "Shift each letter back by 4 positions.", "UC{LOCK}"),
            new ChallengeData("easy_10", "Caesar Shift 6", 1, 100, "EASY", "caesar",
                "Caesar +6. Decrypt: NGYN", "Shift each letter back by 6 positions.", "UC{HASH}"),
            new ChallengeData("easy_11", "Caesar Shift 9", 1, 100, "EASY", "caesar",
                "Caesar +9. Decrypt: WXMN", "Shift each letter back by 9 positions.", "UC{NODE}"),
            new ChallengeData("easy_12", "Caesar Shift 12", 1, 100, "EASY", "caesar",
                "Caesar +12. Decrypt: PMFM", "Shift each letter back by 12 positions.", "UC{DATA}"),
            new ChallengeData("easy_13", "Caesar Shift 1", 1, 100, "EASY", "caesar",
                "Caesar +1. Decrypt: DMPVE", "Shift each letter back by 1 positions.", "UC{CLOUD}"),
            new ChallengeData("easy_14", "Caesar Shift 14", 1, 100, "EASY", "caesar",
                "Caesar +14. Decrypt: ORAWB", "Shift each letter back by 14 positions.", "UC{ADMIN}"),
            new ChallengeData("easy_15", "Caesar Shift 17", 1, 100, "EASY", "caesar",
                "Caesar +17. Decrypt: XLVJK", "Shift each letter back by 17 positions.", "UC{GUEST}"),
            new ChallengeData("easy_16", "Caesar Shift 19", 1, 100, "EASY", "caesar",
                "Caesar +19. Decrypt: KHHM", "Shift each letter back by 19 positions.", "UC{ROOT}"),
            new ChallengeData("easy_17", "Caesar Shift 21", 1, 100, "EASY", "caesar",
                "Caesar +21. Decrypt: OJFZI", "Shift each letter back by 21 positions.", "UC{TOKEN}"),
            new ChallengeData("easy_18", "Caesar Shift 23", 1, 100, "EASY", "caesar",
                "Caesar +23. Decrypt: PXIQ", "Shift each letter back by 23 positions.", "UC{SALT}"),
            new ChallengeData("easy_19", "Caesar Shift 25", 1, 100, "EASY", "caesar",
                "Caesar +25. Decrypt: UZTKS", "Shift each letter back by 25 positions.", "UC{VAULT}"),
            new ChallengeData("easy_20", "Caesar Shift 3", 1, 100, "EASY", "caesar",
                "Caesar +3. Decrypt: IODJ", "Shift each letter back by 3 positions.", "UC{FLAG}"),
            new ChallengeData("easy_21", "Caesar Shift 7", 1, 100, "EASY", "caesar",
                "Caesar +7. Decrypt: OBUA", "Shift each letter back by 7 positions.", "UC{HUNT}"),
            new ChallengeData("easy_22", "Caesar Shift 5", 1, 100, "EASY", "caesar",
                "Caesar +5. Decrypt: JQNYJ", "Shift each letter back by 5 positions.", "UC{ELITE}"),
            new ChallengeData("easy_23", "Caesar Shift 10", 1, 100, "EASY", "caesar",
                "Caesar +10. Decrypt: ZBSWO", "Shift each letter back by 10 positions.", "UC{PRIME}"),
            new ChallengeData("easy_24", "Caesar Shift 13", 1, 100, "EASY", "caesar",
                "Caesar +13. Decrypt: ABIN", "Shift each letter back by 13 positions.", "UC{NOVA}"),
            new ChallengeData("easy_25", "Caesar Shift 2", 1, 100, "EASY", "caesar",
                "Caesar +2. Decrypt: QPAZ", "Shift each letter back by 2 positions.", "UC{ONYX}"),
            new ChallengeData("easy_26", "Caesar Shift 8", 1, 100, "EASY", "caesar",
                "Caesar +8. Decrypt: OPWAB", "Shift each letter back by 8 positions.", "UC{GHOST}"),
            new ChallengeData("easy_27", "Caesar Shift 11", 1, 100, "EASY", "caesar",
                "Caesar +11. Decrypt: NJMPC", "Shift each letter back by 11 positions.", "UC{CYBER}"),
            new ChallengeData("easy_28", "Caesar Shift 4", 1, 100, "EASY", "caesar",
                "Caesar +4. Decrypt: JVSWX", "Shift each letter back by 4 positions.", "UC{FROST}"),
            new ChallengeData("easy_29", "Caesar Shift 6", 1, 100, "EASY", "caesar",
                "Caesar +6. Decrypt: HRGFK", "Shift each letter back by 6 positions.", "UC{BLAZE}"),
            new ChallengeData("easy_30", "Caesar Shift 9", 1, 100, "EASY", "caesar",
                "Caesar +9. Decrypt: BQJMXF", "Shift each letter back by 9 positions.", "UC{SHADOW}"),
            new ChallengeData("easy_31", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: PVCURE", "ROT13 shifts exactly 13.", "UC{CIPHER}"),
            new ChallengeData("easy_32", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: FRPERG", "ROT13 shifts exactly 13.", "UC{SECRET}"),
            new ChallengeData("easy_33", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: UNPXRE", "ROT13 shifts exactly 13.", "UC{HACKER}"),
            new ChallengeData("easy_34", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: ZBHFR", "ROT13 shifts exactly 13.", "UC{MOUSE}"),
            new ChallengeData("easy_35", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: PBQR", "ROT13 shifts exactly 13.", "UC{CODE}"),
            new ChallengeData("easy_36", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: OLGR", "ROT13 shifts exactly 13.", "UC{BYTE}"),
            new ChallengeData("easy_37", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: OVGF", "ROT13 shifts exactly 13.", "UC{BITS}"),
            new ChallengeData("easy_38", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: XRL", "ROT13 shifts exactly 13.", "UC{KEY}"),
            new ChallengeData("easy_39", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: YBPX", "ROT13 shifts exactly 13.", "UC{LOCK}"),
            new ChallengeData("easy_40", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: UNFU", "ROT13 shifts exactly 13.", "UC{HASH}"),
            new ChallengeData("easy_41", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: ABQR", "ROT13 shifts exactly 13.", "UC{NODE}"),
            new ChallengeData("easy_42", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: QNGN", "ROT13 shifts exactly 13.", "UC{DATA}"),
            new ChallengeData("easy_43", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: PYBHQ", "ROT13 shifts exactly 13.", "UC{CLOUD}"),
            new ChallengeData("easy_44", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: NQZVA", "ROT13 shifts exactly 13.", "UC{ADMIN}"),
            new ChallengeData("easy_45", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: THRFG", "ROT13 shifts exactly 13.", "UC{GUEST}"),
            new ChallengeData("easy_46", "ROT13 Flip", 1, 80, "EASY", "rot13",
                "ROT13: EBBG", "ROT13 shifts exactly 13.", "UC{ROOT}"),
            new ChallengeData("easy_47", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: REHPIC", "Read the string backwards.", "UC{CIPHER}"),
            new ChallengeData("easy_48", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: TERCES", "Read the string backwards.", "UC{SECRET}"),
            new ChallengeData("easy_49", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: REKCAH", "Read the string backwards.", "UC{HACKER}"),
            new ChallengeData("easy_50", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: ESUOM", "Read the string backwards.", "UC{MOUSE}"),
            new ChallengeData("easy_51", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: EDOC", "Read the string backwards.", "UC{CODE}"),
            new ChallengeData("easy_52", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: ETYB", "Read the string backwards.", "UC{BYTE}"),
            new ChallengeData("easy_53", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: STIB", "Read the string backwards.", "UC{BITS}"),
            new ChallengeData("easy_54", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: YEK", "Read the string backwards.", "UC{KEY}"),
            new ChallengeData("easy_55", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: KCOL", "Read the string backwards.", "UC{LOCK}"),
            new ChallengeData("easy_56", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: HSAH", "Read the string backwards.", "UC{HASH}"),
            new ChallengeData("easy_57", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: EDON", "Read the string backwards.", "UC{NODE}"),
            new ChallengeData("easy_58", "Backwards Brain", 1, 60, "EASY", "reverse",
                "Reverse: ATAD", "Read the string backwards.", "UC{DATA}"),
            new ChallengeData("easy_59", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: XRKSVI", "A<->Z, B<->Y, C<->X.", "UC{CIPHER}"),
            new ChallengeData("easy_60", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: HVXIVG", "A<->Z, B<->Y, C<->X.", "UC{SECRET}"),
            new ChallengeData("easy_61", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: SZXPVI", "A<->Z, B<->Y, C<->X.", "UC{HACKER}"),
            new ChallengeData("easy_62", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: NLFHV", "A<->Z, B<->Y, C<->X.", "UC{MOUSE}"),
            new ChallengeData("easy_63", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: XLWV", "A<->Z, B<->Y, C<->X.", "UC{CODE}"),
            new ChallengeData("easy_64", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: YBGV", "A<->Z, B<->Y, C<->X.", "UC{BYTE}"),
            new ChallengeData("easy_65", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: YRGH", "A<->Z, B<->Y, C<->X.", "UC{BITS}"),
            new ChallengeData("easy_66", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: PVB", "A<->Z, B<->Y, C<->X.", "UC{KEY}"),
            new ChallengeData("easy_67", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: OLXP", "A<->Z, B<->Y, C<->X.", "UC{LOCK}"),
            new ChallengeData("easy_68", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: SZHS", "A<->Z, B<->Y, C<->X.", "UC{HASH}"),
            new ChallengeData("easy_69", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: MLWV", "A<->Z, B<->Y, C<->X.", "UC{NODE}"),
            new ChallengeData("easy_70", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: WZGZ", "A<->Z, B<->Y, C<->X.", "UC{DATA}"),
            new ChallengeData("easy_71", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: XOLFW", "A<->Z, B<->Y, C<->X.", "UC{CLOUD}"),
            new ChallengeData("easy_72", "Atbash Mirror", 1, 90, "EASY", "atbash",
                "Atbash: ZWNRM", "A<->Z, B<->Y, C<->X.", "UC{ADMIN}"),
            new ChallengeData("easy_73", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4349504845527D", "Every 2 hex chars = 1 byte.", "UC{CIPHER}"),
            new ChallengeData("easy_74", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B5345435245547D", "Every 2 hex chars = 1 byte.", "UC{SECRET}"),
            new ChallengeData("easy_75", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4841434B45527D", "Every 2 hex chars = 1 byte.", "UC{HACKER}"),
            new ChallengeData("easy_76", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4D4F5553457D", "Every 2 hex chars = 1 byte.", "UC{MOUSE}"),
            new ChallengeData("easy_77", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B434F44457D", "Every 2 hex chars = 1 byte.", "UC{CODE}"),
            new ChallengeData("easy_78", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B425954457D", "Every 2 hex chars = 1 byte.", "UC{BYTE}"),
            new ChallengeData("easy_79", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B424954537D", "Every 2 hex chars = 1 byte.", "UC{BITS}"),
            new ChallengeData("easy_80", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4B45597D", "Every 2 hex chars = 1 byte.", "UC{KEY}"),
            new ChallengeData("easy_81", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4C4F434B7D", "Every 2 hex chars = 1 byte.", "UC{LOCK}"),
            new ChallengeData("easy_82", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B484153487D", "Every 2 hex chars = 1 byte.", "UC{HASH}"),
            new ChallengeData("easy_83", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B4E4F44457D", "Every 2 hex chars = 1 byte.", "UC{NODE}"),
            new ChallengeData("easy_84", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B444154417D", "Every 2 hex chars = 1 byte.", "UC{DATA}"),
            new ChallengeData("easy_85", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B434C4F55447D", "Every 2 hex chars = 1 byte.", "UC{CLOUD}"),
            new ChallengeData("easy_86", "Hex Runner", 1, 90, "EASY", "hex",
                "Hex -> ASCII: 55437B41444D494E7D", "Every 2 hex chars = 1 byte.", "UC{ADMIN}"),
            new ChallengeData("easy_87", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [67 73 80 72 69 82]", "Each number 0-127 = one ASCII char.", "UC{CIPHER}"),
            new ChallengeData("easy_88", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [83 69 67 82 69 84]", "Each number 0-127 = one ASCII char.", "UC{SECRET}"),
            new ChallengeData("easy_89", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [72 65 67 75 69 82]", "Each number 0-127 = one ASCII char.", "UC{HACKER}"),
            new ChallengeData("easy_90", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [77 79 85 83 69]", "Each number 0-127 = one ASCII char.", "UC{MOUSE}"),
            new ChallengeData("easy_91", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [67 79 68 69]", "Each number 0-127 = one ASCII char.", "UC{CODE}"),
            new ChallengeData("easy_92", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [66 89 84 69]", "Each number 0-127 = one ASCII char.", "UC{BYTE}"),
            new ChallengeData("easy_93", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [66 73 84 83]", "Each number 0-127 = one ASCII char.", "UC{BITS}"),
            new ChallengeData("easy_94", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [75 69 89]", "Each number 0-127 = one ASCII char.", "UC{KEY}"),
            new ChallengeData("easy_95", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [76 79 67 75]", "Each number 0-127 = one ASCII char.", "UC{LOCK}"),
            new ChallengeData("easy_96", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [72 65 83 72]", "Each number 0-127 = one ASCII char.", "UC{HASH}"),
            new ChallengeData("easy_97", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [78 79 68 69]", "Each number 0-127 = one ASCII char.", "UC{NODE}"),
            new ChallengeData("easy_98", "ASCII Lab", 1, 80, "EASY", "ascii",
                "ASCII codes: [68 65 84 65]", "Each number 0-127 = one ASCII char.", "UC{DATA}"),
            new ChallengeData("easy_99", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 010000110100100101010000010010000100010101010010", "Every 8 bits = one byte.", "UC{CIPHER}"),
            new ChallengeData("easy_100", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 010100110100010101000011010100100100010101010100", "Every 8 bits = one byte.", "UC{SECRET}"),
            new ChallengeData("easy_101", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 010010000100000101000011010010110100010101010010", "Every 8 bits = one byte.", "UC{HACKER}"),
            new ChallengeData("easy_102", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 0100110101001111010101010101001101000101", "Every 8 bits = one byte.", "UC{MOUSE}"),
            new ChallengeData("easy_103", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01000011010011110100010001000101", "Every 8 bits = one byte.", "UC{CODE}"),
            new ChallengeData("easy_104", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01000010010110010101010001000101", "Every 8 bits = one byte.", "UC{BYTE}"),
            new ChallengeData("easy_105", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01000010010010010101010001010011", "Every 8 bits = one byte.", "UC{BITS}"),
            new ChallengeData("easy_106", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 010010110100010101011001", "Every 8 bits = one byte.", "UC{KEY}"),
            new ChallengeData("easy_107", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01001100010011110100001101001011", "Every 8 bits = one byte.", "UC{LOCK}"),
            new ChallengeData("easy_108", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01001000010000010101001101001000", "Every 8 bits = one byte.", "UC{HASH}"),
            new ChallengeData("easy_109", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01001110010011110100010001000101", "Every 8 bits = one byte.", "UC{NODE}"),
            new ChallengeData("easy_110", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 01000100010000010101010001000001", "Every 8 bits = one byte.", "UC{DATA}"),
            new ChallengeData("easy_111", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 0100001101001100010011110101010101000100", "Every 8 bits = one byte.", "UC{CLOUD}"),
            new ChallengeData("easy_112", "Binary Decoder", 1, 110, "EASY", "binary",
                "Binary -> ASCII: 0100000101000100010011010100100101001110", "Every 8 bits = one byte.", "UC{ADMIN}"),
            new ChallengeData("easy_113", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: Q0lQSEVS", "Base64 packs 3 bytes into 4 chars.", "UC{CIPHER}"),
            new ChallengeData("easy_114", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: U0VDUkVU", "Base64 packs 3 bytes into 4 chars.", "UC{SECRET}"),
            new ChallengeData("easy_115", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: SEFDS0VS", "Base64 packs 3 bytes into 4 chars.", "UC{HACKER}"),
            new ChallengeData("easy_116", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: TU9VU0U=", "Base64 packs 3 bytes into 4 chars.", "UC{MOUSE}"),
            new ChallengeData("easy_117", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: Q09ERQ==", "Base64 packs 3 bytes into 4 chars.", "UC{CODE}"),
            new ChallengeData("easy_118", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: QllURQ==", "Base64 packs 3 bytes into 4 chars.", "UC{BYTE}"),
            new ChallengeData("easy_119", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: QklUUw==", "Base64 packs 3 bytes into 4 chars.", "UC{BITS}"),
            new ChallengeData("easy_120", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: S0VZ", "Base64 packs 3 bytes into 4 chars.", "UC{KEY}"),
            new ChallengeData("easy_121", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: TE9DSw==", "Base64 packs 3 bytes into 4 chars.", "UC{LOCK}"),
            new ChallengeData("easy_122", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: SEFTSA==", "Base64 packs 3 bytes into 4 chars.", "UC{HASH}"),
            new ChallengeData("easy_123", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: Tk9ERQ==", "Base64 packs 3 bytes into 4 chars.", "UC{NODE}"),
            new ChallengeData("easy_124", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: REFUQQ==", "Base64 packs 3 bytes into 4 chars.", "UC{DATA}"),
            new ChallengeData("easy_125", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: Q0xPVUQ=", "Base64 packs 3 bytes into 4 chars.", "UC{CLOUD}"),
            new ChallengeData("easy_126", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: QURNSU4=", "Base64 packs 3 bytes into 4 chars.", "UC{ADMIN}"),
            new ChallengeData("easy_127", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: R1VFU1Q=", "Base64 packs 3 bytes into 4 chars.", "UC{GUEST}"),
            new ChallengeData("easy_128", "Base64 Breaker", 1, 120, "EASY", "base64",
                "Base64 decode: Uk9PVA==", "Base64 packs 3 bytes into 4 chars.", "UC{ROOT}"),
            new ChallengeData("easy_129", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: INEVASCFKI======", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{CIPHER}"),
            new ChallengeData("easy_130", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: KNCUGUSFKQ======", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{SECRET}"),
            new ChallengeData("easy_131", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: JBAUGS2FKI======", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{HACKER}"),
            new ChallengeData("easy_132", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: JVHVKU2F", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{MOUSE}"),
            new ChallengeData("easy_133", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: INHUIRI=", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{CODE}"),
            new ChallengeData("easy_134", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: IJMVIRI=", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{BYTE}"),
            new ChallengeData("easy_135", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: IJEVIUY=", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{BITS}"),
            new ChallengeData("easy_136", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: JNCVS===", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{KEY}"),
            new ChallengeData("easy_137", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: JRHUGSY=", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{LOCK}"),
            new ChallengeData("easy_138", "Base32 Coder", 1, 120, "EASY", "base32",
                "Base32 decode: JBAVGSA=", "Base32 uses A-Z and 2-7. Uppercase output.", "UC{HASH}"),
            new ChallengeData("easy_139", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 103 111 120 110 105 122", "Each 3-digit octal number = one byte.", "UC{CIPHER}"),
            new ChallengeData("easy_140", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 123 105 103 122 105 124", "Each 3-digit octal number = one byte.", "UC{SECRET}"),
            new ChallengeData("easy_141", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 110 101 103 113 105 122", "Each 3-digit octal number = one byte.", "UC{HACKER}"),
            new ChallengeData("easy_142", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 115 117 125 123 105", "Each 3-digit octal number = one byte.", "UC{MOUSE}"),
            new ChallengeData("easy_143", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 103 117 104 105", "Each 3-digit octal number = one byte.", "UC{CODE}"),
            new ChallengeData("easy_144", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 102 131 124 105", "Each 3-digit octal number = one byte.", "UC{BYTE}"),
            new ChallengeData("easy_145", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 102 111 124 123", "Each 3-digit octal number = one byte.", "UC{BITS}"),
            new ChallengeData("easy_146", "Octal Lab", 1, 100, "EASY", "octal",
                "Octal -> ASCII: 113 105 131", "Each 3-digit octal number = one byte.", "UC{KEY}"),
            new ChallengeData("med_147", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: EKDRMTP", "Key=CTF. Each letter shifted by key index.", "UC{CRYPTON}"),
            new ChallengeData("med_148", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: KPEYVGDLK", "Key=KEY. Each letter shifted by key index.", "UC{ALGORITHM}"),
            new ChallengeData("med_149", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: OAURPNI", "Key=HACK. Each letter shifted by key index.", "UC{HASHING}"),
            new ChallengeData("med_150", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: YZFZYTEOZK", "Key=XOR. Each letter shifted by key index.", "UC{BLOCKCHAIN}"),
            new ChallengeData("med_151", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: QBTJCBHZGR", "Key=MORSE. Each letter shifted by key index.", "UC{ENCRYPTION}"),
            new ChallengeData("med_152", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: YMRVPKBXSE", "Key=VIPER. Each letter shifted by key index.", "UC{DECRYPTION}"),
            new ChallengeData("med_153", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: SWUIJOOP", "Key=NODE. Each letter shifted by key index.", "UC{FIREWALL}"),
            new ChallengeData("med_154", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: AFQDZQQV", "Key=LOCK. Each letter shifted by key index.", "UC{PROTOCOL}"),
            new ChallengeData("med_155", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: HAXWOOWH", "Key=SAFE. Each letter shifted by key index.", "UC{PASSWORD}"),
            new ChallengeData("med_156", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: YVKXXZBB", "Key=GRID. Each letter shifted by key index.", "UC{SECURITY}"),
            new ChallengeData("med_157", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: MXWDXWQL", "Key=CTF. Each letter shifted by key index.", "UC{KERBEROS}"),
            new ChallengeData("med_158", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: PSPORQSGQ", "Key=KEY. Each letter shifted by key index.", "UC{FORENSICS}"),
            new ChallengeData("med_159", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: IOVXLT", "Key=HACK. Each letter shifted by key index.", "UC{BOTNET}"),
            new ChallengeData("med_160", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: MVZPVZKU", "Key=XOR. Each letter shifted by key index.", "UC{PHISHING}"),
            new ChallengeData("med_161", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: YOCOEDS", "Key=MORSE. Each letter shifted by key index.", "UC{MALWARE}"),
            new ChallengeData("med_162", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: MICWFHEPVV", "Key=VIPER. Each letter shifted by key index.", "UC{RANSOMWARE}"),
            new ChallengeData("med_163", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: FHHKNBR", "Key=NODE. Each letter shifted by key index.", "UC{STEGANO}"),
            new ChallengeData("med_164", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: VSAZLWT", "Key=LOCK. Each letter shifted by key index.", "UC{KEYPAIR}"),
            new ChallengeData("med_165", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: UIULWRYIPT", "Key=SAFE. Each letter shifted by key index.", "UC{CIPHERTEXT}"),
            new ChallengeData("med_166", "Vigenere Gate", 2, 200, "MEDIUM", "vigenere",
                "Vigenere: VCILTKMAZ", "Key=GRID. Each letter shifted by key index.", "UC{PLAINTEXT}"),
            new ChallengeData("med_167", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key K): CBkSGx8EBQ==", "XOR key='K'. Use XOR tool -> Decrypt.", "UC{CRYPTON}"),
            new ChallengeData("med_168", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key M): DAEKAh8EGQUA", "XOR key='M'. Use XOR tool -> Decrypt.", "UC{ALGORITHM}"),
            new ChallengeData("med_169", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key X): EBkLEBEWHw==", "XOR key='X'. Use XOR tool -> Decrypt.", "UC{HASHING}"),
            new ChallengeData("med_170", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key Z): GBYVGREZEhsTFA==", "XOR key='Z'. Use XOR tool -> Decrypt.", "UC{BLOCKCHAIN}"),
            new ChallengeData("med_171", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key Q): FB8SAwgBBRgeHw==", "XOR key='Q'. Use XOR tool -> Decrypt.", "UC{ENCRYPTION}"),
            new ChallengeData("med_172", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key P): FBUTAgkABBkfHg==", "XOR key='P'. Use XOR tool -> Decrypt.", "UC{DECRYPTION}"),
            new ChallengeData("med_173", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key T): Eh0GEQMVGBg=", "XOR key='T'. Use XOR tool -> Decrypt.", "UC{FIREWALL}"),
            new ChallengeData("med_174", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key F): FhQJEgkFCQo=", "XOR key='F'. Use XOR tool -> Decrypt.", "UC{PROTOCOL}"),
            new ChallengeData("med_175", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key H): GAkbGx8HGgw=", "XOR key='H'. Use XOR tool -> Decrypt.", "UC{PASSWORD}"),
            new ChallengeData("med_176", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key J): GQ8JHxgDHhM=", "XOR key='J'. Use XOR tool -> Decrypt.", "UC{SECURITY}"),
            new ChallengeData("med_177", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key K): AA4ZCQ4ZBBg=", "XOR key='K'. Use XOR tool -> Decrypt.", "UC{KERBEROS}"),
            new ChallengeData("med_178", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key M): CwIfCAMeBA4e", "XOR key='M'. Use XOR tool -> Decrypt.", "UC{FORENSICS}"),
            new ChallengeData("med_179", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key X): GhcMFh0M", "XOR key='X'. Use XOR tool -> Decrypt.", "UC{BOTNET}"),
            new ChallengeData("med_180", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key Z): ChITCRITFB0=", "XOR key='Z'. Use XOR tool -> Decrypt.", "UC{PHISHING}"),
            new ChallengeData("med_181", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key Q): HBAdBhADFA==", "XOR key='Q'. Use XOR tool -> Decrypt.", "UC{MALWARE}"),
            new ChallengeData("med_182", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key P): AhEeAx8dBxECFQ==", "XOR key='P'. Use XOR tool -> Decrypt.", "UC{RANSOMWARE}"),
            new ChallengeData("med_183", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key T): BwARExUaGw==", "XOR key='T'. Use XOR tool -> Decrypt.", "UC{STEGANO}"),
            new ChallengeData("med_184", "XOR Breaker", 2, 180, "MEDIUM", "xor",
                "XOR (key F): DQMfFgcPFA==", "XOR key='F'. Use XOR tool -> Decrypt.", "UC{KEYPAIR}"),
            new ChallengeData("med_185", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -.-. .-. -.-- .--. -", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{CRYPT}"),
            new ChallengeData("med_186", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: .... ..- -. -", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{HUNT}"),
            new ChallengeData("med_187", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: ...- .- ..- .-.. -", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{VAULT}"),
            new ChallengeData("med_188", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -.- . -.-- .... --- .-.. .", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{KEYHOLE}"),
            new ChallengeData("med_189", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: ... . -.-. .-. . -", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{SECRET}"),
            new ChallengeData("med_190", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -.-. .. .--. .... . .-.", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{CIPHER}"),
            new ChallengeData("med_191", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: .-.. --- -.-. -.- ... -- .. - ....", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{LOCKSMITH}"),
            new ChallengeData("med_192", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: - .-. . .- ... ..- .-. .", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{TREASURE}"),
            new ChallengeData("med_193", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -.. .- .-. -.- -. . -", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{DARKNET}"),
            new ChallengeData("med_194", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: .--. .- ... ... .-- --- .-. -..", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{PASSWORD}"),
            new ChallengeData("med_195", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: .... .- -.-. -.- . .-.", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{HACKER}"),
            new ChallengeData("med_196", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -. --- -.. .", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{NODE}"),
            new ChallengeData("med_197", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -... .-.. --- -.-. -.- -.-. .... .- .. -.", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{BLOCKCHAIN}"),
            new ChallengeData("med_198", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: ..-. .. .-. . .-- .- .-.. .-..", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{FIREWALL}"),
            new ChallengeData("med_199", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: -.- . .-. -... . .-. --- ...", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{KERBEROS}"),
            new ChallengeData("med_200", "Morse Mania", 2, 170, "MEDIUM", "morse",
                "Morse: ... - . --. .- -. ---", "Dots and dashes: . = dot, - = dash. Spaces separate letters.", "UC{STEGANO}"),
            new ChallengeData("med_201", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: JWMCVCPC", "Affine: c=(a*p+b) mod 26, a=5, b=8.", "UC{VIGENERE}"),
            new ChallengeData("med_202", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: KDSFYXZVX", "Affine: c=(a*p+b) mod 26, a=5, b=11.", "UC{FORENSICS}"),
            new ChallengeData("med_203", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: AKVUVNVG", "Affine: c=(a*p+b) mod 26, a=5, b=3.", "UC{PROTOCOL}"),
            new ChallengeData("med_204", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: PLHDIJEL", "Affine: c=(a*p+b) mod 26, a=5, b=17.", "UC{KEYSTONE}"),
            new ChallengeData("med_205", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: AJIVCL", "Affine: c=(a*p+b) mod 26, a=5, b=21.", "UC{BINARY}"),
            new ChallengeData("med_206", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: QHTQVUL", "Affine: c=(a*p+b) mod 26, a=5, b=7.", "UC{HASHING}"),
            new ChallengeData("med_207", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: HAXUDKEBFA", "Affine: c=(a*p+b) mod 26, a=5, b=13.", "UC{ENCRYPTION}"),
            new ChallengeData("med_208", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: TXGSDQADUA", "Affine: c=(a*p+b) mod 26, a=5, b=9.", "UC{CIPHERTEXT}"),
            new ChallengeData("med_209", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: ZWFMGH", "Affine: c=(a*p+b) mod 26, a=5, b=15.", "UC{CRYPTO}"),
            new ChallengeData("med_210", "Affine Formula", 3, 260, "MEDIUM", "affine",
                "Affine -> decrypt: ATGFLB", "Affine: c=(a*p+b) mod 26, a=5, b=19.", "UC{RANSOM}"),
            new ChallengeData("med_211", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (2 rails): WRFAEIERM", "Rail fence with 2 rails. Read in zig-zag.", "UC{WIREFRAME}"),
            new ChallengeData("med_212", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (3 rails): EYRNRPOCT", "Rail fence with 3 rails. Read in zig-zag.", "UC{ENCRYPTOR}"),
            new ChallengeData("med_213", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (4 rails): RAAODIRL", "Rail fence with 4 rails. Read in zig-zag.", "UC{RAILROAD}"),
            new ChallengeData("med_214", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (5 rails): ZIGZGA", "Rail fence with 5 rails. Read in zig-zag.", "UC{ZIGZAG}"),
            new ChallengeData("med_215", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (2 rails): FNEIEECLN", "Rail fence with 2 rails. Read in zig-zag.", "UC{FENCELINE}"),
            new ChallengeData("med_216", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (3 rails): PNTLITXAE", "Rail fence with 3 rails. Read in zig-zag.", "UC{PLAINTEXT}"),
            new ChallengeData("med_217", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (4 rails): MIOALNRO", "Rail fence with 4 rails. Read in zig-zag.", "UC{MONORAIL}"),
            new ChallengeData("med_218", "Rail Fence", 3, 240, "MEDIUM", "railfence",
                "Rail fence (5 rails): TTRRAONPS", "Rail fence with 5 rails. Read in zig-zag.", "UC{TRANSPORT}"),
            new ChallengeData("med_219", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: 53CR37", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{SECRET}"),
            new ChallengeData("med_220", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: H4CK3R", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{HACKER}"),
            new ChallengeData("med_221", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: CY83R", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{CYBER}"),
            new ChallengeData("med_222", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: 9H057", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{GHOST}"),
            new ChallengeData("med_223", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: V1C70RY", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{VICTORY}"),
            new ChallengeData("med_224", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: 5H4D0W", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{SHADOW}"),
            new ChallengeData("med_225", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: PH4N70M", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{PHANTOM}"),
            new ChallengeData("med_226", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: N19H7", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{NIGHT}"),
            new ChallengeData("med_227", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: 3L173", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{ELITE}"),
            new ChallengeData("med_228", "1337 Speak", 2, 160, "MEDIUM", "leet",
                "1337: CRYP70", "leet: 4=A, 3=E, 1=I, 0=O, 5=S, 7=T.", "UC{CRYPTO}"),
            new ChallengeData("med_229", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: abbab abaaa aabba aabbb baabb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{NIGHT}"),
            new ChallengeData("med_230", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: aaaab ababb aaaaa aaaba ababa", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{BLACK}"),
            new ChallengeData("med_231", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: abbaa abbba babaa baaba aabaa", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{MOUSE}"),
            new ChallengeData("med_232", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: aabbb abbba babaa abbab aaabb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{HOUND}"),
            new ChallengeData("med_233", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: baaba baabb aabaa aabaa ababb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{STEEL}"),
            new ChallengeData("med_234", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: babba abbba baaab ababb aaabb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{WORLD}"),
            new ChallengeData("med_235", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: baaab abbba bbaaa aaaaa ababb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{ROYAL}"),
            new ChallengeData("med_236", "Bacon's Bite", 3, 230, "MEDIUM", "bacon",
                "Baconian: aabba babaa aaaaa baaab aaabb", "Baconian: each letter = 5 a/b symbols (a=0,b=1).", "UC{GUARD}"),
            new ChallengeData("hard_237", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: WQU_LGFX", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{ZERO_DAY}"),
            new ChallengeData("hard_238", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: VISEGC_EF_CBK", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{MAN_IN_MIDDLE}"),
            new ChallengeData("hard_239", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: APQ_CCZQ_TOM", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{ONE_TIME_PAD}"),
            new ChallengeData("hard_240", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: UCYPU_UESKXBP", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{RAINBOW_TABLE}"),
            new ChallengeData("hard_241", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: CBCOYXT_BSJQ", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{SIDE_CHANNEL}"),
            new ChallengeData("hard_242", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: BRSMV_VQJSZ", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{BRUTE_FORCE}"),
            new ChallengeData("hard_243", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: NDLULNTSD", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{FREQUENCY}"),
            new ChallengeData("hard_244", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: TFWQPEDLBPF", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{HOMOMORPHIC}"),
            new ChallengeData("hard_245", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: TSSJRKVJQ", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{SIGNATURE}"),
            new ChallengeData("hard_246", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: NFI_SZIQVN", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{PUBLIC_KEY}"),
            new ChallengeData("hard_247", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: VTL_CJRSXSN", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{PRIVATE_KEY}"),
            new ChallengeData("hard_248", "Triple Agent", 5, 450, "HARD", "tripleagent",
                "Triple-encrypted: TLYXJACBF", "Steps: Vigenere(Key=LOCKED) -> Reverse -> ROT13.", "UC{HANDSHAKE}"),
            new ChallengeData("hard_249", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: --.. . .-. --- ..--.- -.. .- -.--", "Long Morse string. Words separated by /.", "UC{ZERO_DAY}"),
            new ChallengeData("hard_250", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: -- .- -. ..--.- .. -. ..--.- -- .. -.. -.. .-.. .", "Long Morse string. Words separated by /.", "UC{MAN_IN_MIDDLE}"),
            new ChallengeData("hard_251", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: --- -. . ..--.- - .. -- . ..--.- .--. .- -..", "Long Morse string. Words separated by /.", "UC{ONE_TIME_PAD}"),
            new ChallengeData("hard_252", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: .-. .- .. -. -... --- .-- ..--.- - .- -... .-.. .", "Long Morse string. Words separated by /.", "UC{RAINBOW_TABLE}"),
            new ChallengeData("hard_253", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: ... .. -.. . ..--.- -.-. .... .- -. -. . .-..", "Long Morse string. Words separated by /.", "UC{SIDE_CHANNEL}"),
            new ChallengeData("hard_254", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: -... .-. ..- - . ..--.- ..-. --- .-. -.-. .", "Long Morse string. Words separated by /.", "UC{BRUTE_FORCE}"),
            new ChallengeData("hard_255", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: ..-. .-. . --.- ..- . -. -.-. -.--", "Long Morse string. Words separated by /.", "UC{FREQUENCY}"),
            new ChallengeData("hard_256", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: .... --- -- --- -- --- .-. .--. .... .. -.-.", "Long Morse string. Words separated by /.", "UC{HOMOMORPHIC}"),
            new ChallengeData("hard_257", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: ... .. --. -. .- - ..- .-. .", "Long Morse string. Words separated by /.", "UC{SIGNATURE}"),
            new ChallengeData("hard_258", "Morse Master", 4, 380, "HARD", "morsehard",
                "Morse: .--. ..- -... .-.. .. -.-. ..--.- -.- . -.--", "Long Morse string. Words separated by /.", "UC{PUBLIC_KEY}"),
            new ChallengeData("hard_259", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key K9): EXwZdhR9CmAUfBNpB3YCbQ==", "XOR key='K9'. Multi-char XOR.", "UC{ZERO_DAY_EXPLOIT}"),
            new ChallengeData("hard_260", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key M7): Hn4JchJ0BXYDeQh7EnYZYwx0Bg==", "XOR key='M7'. Multi-char XOR.", "UC{SIDE_CHANNEL_ATTACK}"),
            new ChallengeData("hard_261", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key X2): F3wdbQx7FXcHYhl2", "XOR key='X2'. Multi-char XOR.", "UC{ONE_TIME_PAD}"),
            new ChallengeData("hard_262", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key ZQ): CBATHxgeDQ4OEBgdHw==", "XOR key='ZQ'. Multi-char XOR.", "UC{RAINBOW_TABLE}"),
            new ChallengeData("hard_263", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key P5): EmcFYRVqFnoCdhVqHHQS", "XOR key='P5'. Multi-char XOR.", "UC{BRUTE_FORCE_LAB}"),
            new ChallengeData("hard_264", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key T8): BG0WdB17C3MRYQtxGn4GeQ==", "XOR key='T8'. Multi-char XOR.", "UC{PUBLIC_KEY_INFRA}"),
            new ChallengeData("hard_265", "XOR Siege", 4, 350, "HARD", "xorhard",
                "XOR (key F3): DnwLfAt8FGMOegVsBWcA", "XOR key='F3'. Multi-char XOR.", "UC{HOMOMORPHIC_CTF}"),
            new ChallengeData("hard_266", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 17): HLREKLD_TFDGLKVI", "Unknown shift. Try all 25 shifts.", "UC{QUANTUM_COMPUTER}"),
            new ChallengeData("hard_267", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 23): AFCCFB_EBIIJXK", "Unknown shift. Try all 25 shifts.", "UC{DIFFIE_HELLMAN}"),
            new ChallengeData("hard_268", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 19): FXKDEX_MKXX", "Unknown shift. Try all 25 shifts.", "UC{MERKLE_TREE}"),
            new ChallengeData("hard_269", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 21): MNV_AVXOJMDIB", "Unknown shift. Try all 25 shifts.", "UC{RSA_FACTORING}"),
            new ChallengeData("hard_270", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 15): TAAXEIXR_RJGKT", "Unknown shift. Try all 25 shifts.", "UC{ELLIPTIC_CURVE}"),
            new ChallengeData("hard_271", "Caesar Fortress", 4, 330, "HARD", "caesarhard",
                "Caesar (shift 25): AKNBJBGZHM_ENQDMRHBR", "Unknown shift. Try all 25 shifts.", "UC{BLOCKCHAIN_FORENSICS}"),
            new ChallengeData("hard_272", "Vigenere Fortress", 5, 420, "HARD", "vigenerehard",
                "Vigenere: NQMQN_EEH_TAVZR", "Key=VIGENEREFORTRESS. Long key, long text.", "UC{SIGMA_AND_OMEGA}"),
            new ChallengeData("hard_273", "Vigenere Fortress", 5, 420, "HARD", "vigenerehard",
                "Vigenere: SOPNWK_VKPSEL", "Key=HANDSHAKE. Long key, long text.", "UC{LOCKED_VALLEY}"),
            new ChallengeData("hard_274", "Vigenere Fortress", 5, 420, "HARD", "vigenerehard",
                "Vigenere: OVPZESSVRZSC_101", "Key=MERKLE. Long key, long text.", "UC{CRYPTOGRAPHY_101}"),
            new ChallengeData("hard_275", "Vigenere Fortress", 5, 420, "HARD", "vigenerehard",
                "Vigenere: IAQMYA_QNKNUNI", "Key=ENIGMA. Long key, long text.", "UC{ENIGMA_MACHINE}"),
            new ChallengeData("hard_276", "Vigenere Fortress", 5, 420, "HARD", "vigenerehard",
                "Vigenere: QOVCSA_XGOSY", "Key=POLYBIUS. Long key, long text.", "UC{BAKERS_DOZEN}"),
            new ChallengeData("hard_277", "Rail Fortress", 4, 340, "HARD", "railhard",
                "Rail fence (4 rails): D_FOER_EULALNEBIC", "Rail fence with 4 rails.", "UC{DOUBLE_RAIL_FENCE}"),
            new ChallengeData("hard_278", "Rail Fortress", 4, 340, "HARD", "railhard",
                "Rail fence (5 rails): TIRZGI_ZPEALG", "Rail fence with 5 rails.", "UC{TRIPLE_ZIGZAG}"),
            new ChallengeData("hard_279", "Rail Fortress", 4, 340, "HARD", "railhard",
                "Rail fence (6 rails): S_ELLVIAEABNR_", "Rail fence with 6 rails.", "UC{SEVEN_RAIL_LAB}"),
            new ChallengeData("hard_280", "Rail Fortress", 4, 340, "HARD", "railhard",
                "Rail fence (4 rails): C_IRRSPEALHI", "Rail fence with 4 rails.", "UC{CIPHER_RAILS}"),
            new ChallengeData("hard_281", "Affine Fortress", 4, 360, "HARD", "affinehard",
                "Affine: UINKXGJ_JINDGO", "Affine: a=7, b=19.", "UC{PROVING_GROUND}"),
            new ChallengeData("hard_282", "Affine Fortress", 4, 360, "HARD", "affinehard",
                "Affine: XAAHKP_XNNXJOY", "Affine: a=11, b=23.", "UC{AFFINE_ASSAULT}"),
            new ChallengeData("hard_283", "Affine Fortress", 4, 360, "HARD", "affinehard",
                "Affine: OVSNFA_FDUTPG", "Affine: a=15, b=5.", "UC{LINEAR_AMBUSH}"),
            new ChallengeData("hard_284", "Affine Fortress", 4, 360, "HARD", "affinehard",
                "Affine: FJUY_EMPDNW", "Affine: a=17, b=9.", "UC{MATH_PRISON}"),
        };
    }

    private static String getLeaderboardText() {
        if (leaderboard.isEmpty()) return "No leaderboard data yet.";
        StringBuilder sb = new StringBuilder("\uD83C\uDFC6 TOP CADETS:  ");
        leaderboard.entrySet().stream()
            .sorted(java.util.Map.Entry.<String,Integer>comparingByValue().reversed())
            .limit(3)
            .forEach(e -> sb.append(e.getKey()).append("(").append(e.getValue()).append("xp) "));
        return sb.toString();
    }

    private static String getRankForXP(int xp) {
        String rank = RANKS[0][1];
        for (String[] r : RANKS) {
            if (xp >= Integer.parseInt(r[0])) rank = r[1];
        }
        return rank;
    }

    private static String getNextRank(int xp) {
        for (int i = 0; i < RANKS.length - 1; i++) {
            int threshold = Integer.parseInt(RANKS[i + 1][0]);
            if (xp < threshold) return RANKS[i + 1][1];
        }
        return "MAX RANK";
    }

    private static double getRankProgress(int xp) {
        for (int i = RANKS.length - 1; i >= 0; i--) {
            int curr = Integer.parseInt(RANKS[i][0]);
            if (xp >= curr) {
                if (i == RANKS.length - 1) return 1.0;
                int next = Integer.parseInt(RANKS[i + 1][0]);
                return (double) (xp - curr) / (next - curr);
            }
        }
        return xp / 200.0;
    }

    private static int getRankXPNeeded(int xp) {
        for (int i = RANKS.length - 1; i >= 0; i--) {
            int curr = Integer.parseInt(RANKS[i][0]);
            if (xp >= curr) {
                if (i == RANKS.length - 1) return 0;
                return Integer.parseInt(RANKS[i + 1][0]) - xp;
            }
        }
        return 200 - xp;
    }

    private void computeBadges() {
        java.util.Set<String> before = new java.util.HashSet<>(achievedBadges);
        achievedBadges.clear();
        if (completedChallenges >= 1)  achievedBadges.add("first_blood");
        if (hasDoneFamily("caesar"))   achievedBadges.add("caesar_slayer");
        if (hasDoneFamily("xor"))      achievedBadges.add("xor_master");
        if (completedChallenges >= 3)  achievedBadges.add("cipher_app");
        if (completedChallenges >= 7)  achievedBadges.add("cipher_expert");
        if (completedChallenges >= CHALLENGE_COUNT) achievedBadges.add("cipher_master");
        if (totalXP / 200 + 1 >= 5)    achievedBadges.add("level_five");
        if (totalXP >= 1000)           achievedBadges.add("xp_century");
        if (completedChallenges >= CHALLENGE_COUNT) achievedBadges.add("completionist");
        if (academy.getDailyDone() >= 1)  achievedBadges.add("daily_grinder");
        if (academy.getDailyDone() >= 5)  achievedBadges.add("daily_streak5");
        if (academy.getDailyDone() >= 10) achievedBadges.add("daily_master");
        if (academy.getWeeklyDone() >= 1) achievedBadges.add("weekly_raider");
        if (academy.getWeekendDone() >= 1) achievedBadges.add("weekend_warrior");

        if (hasDoneAllFamily("caesar", "caesarhard")) achievedBadges.add("caesar_master");
        if (academy.getModuleUses("AES") >= 1)   achievedBadges.add("aes_beginner");
        if (academy.getModuleUses("AES") >= 15)  achievedBadges.add("aes_expert");
        if (academy.getModuleUses("RSA") >= 10)  achievedBadges.add("rsa_wizard");
        if (hasDoneAllFamily("xor", "xorhard"))  achievedBadges.add("xor_killer");
        if (academy.getModuleUses("HASH") >= 10) achievedBadges.add("hash_master");
        if (academy.getModuleUses("STEGO") >= 5) achievedBadges.add("stego_hunter");
        if (totalXP >= 5000)                    achievedBadges.add("crypto_legend");
        if (countDoneDifficulty("MEDIUM") + countDoneDifficulty("HARD") >= 15)
                                                achievedBadges.add("elite_analyst");
        if (academy.getTotalModuleUses() >= 25) achievedBadges.add("soc_defender");
        if (academy.getMissionCompletions() >= 5) achievedBadges.add("cyber_guardian");
        if (completedChallenges >= CHALLENGE_COUNT) achievedBadges.add("hundred_percent");
        if (academy.getCorrect() >= 10 && academy.getAttempts() == academy.getCorrect())
                                                achievedBadges.add("perfect_score");
        if (academy.getFastSolves() >= 1)       achievedBadges.add("speed_runner");
        if (academy.getHintUses() == 0 && completedChallenges >= 10)
                                                achievedBadges.add("no_hint_champion");

        if (!suppressBadgeAnimations) {
            for (String b : achievedBadges) {
                if (!before.contains(b)) playBadgeUnlockAnimation(b);
            }
        }
    }

    private boolean hasDoneAllFamily(String... families) {
        for (ChallengeData ch : getChallenges()) {
            for (String f : families) {
                if (ch.family.equals(f) && !isChallengeDone(ch.id)) return false;
            }
        }
        return true;
    }

    private int countDoneDifficulty(String diff) {
        int n = 0;
        for (ChallengeData ch : getChallenges()) {
            if (ch.diff.equals(diff) && isChallengeDone(ch.id)) n++;
        }
        return n;
    }

    /** Queues a badge unlock so simultaneous unlocks play one after another. */
    private void playBadgeUnlockAnimation(String badgeId) {
        if (suppressBadgeAnimations) return;
        badgeAnimQueue.add(badgeId);
        pumpBadgeAnimations();
    }

    private void pumpBadgeAnimations() {
        if (badgeAnimActive || badgeAnimQueue.isEmpty()) return;
        String badgeId = badgeAnimQueue.poll();
        String[] meta = ALL_BADGES.get(badgeId);
        if (meta == null) { pumpBadgeAnimations(); return; }
        badgeAnimActive = true;

        String icon = meta[0];
        String name = meta[1];

        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(34, 46, 34, 46));
        box.setStyle("-fx-background-color: rgba(13,17,23,0.96); -fx-background-radius: 18; "
            + "-fx-border-color: #FFD700; -fx-border-radius: 18; -fx-border-width: 2;");
        Label sparkle = new Label("\u2728 \uD83C\uDF1F \u2728");
        sparkle.setStyle("-fx-font-size: 18px;");
        Label iconLab = new Label(icon);
        iconLab.setStyle("-fx-font-size: 66px;");
        Label head = new Label("BADGE UNLOCKED");
        head.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 15px; -fx-font-weight: bold;"
            + " -fx-letter-spacing: 2;");
        Label nameLab = new Label(name);
        nameLab.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 22px; -fx-font-weight: bold;");
        box.getChildren().addAll(sparkle, iconLab, head, nameLab);
        AcademyUi.glow(box, javafx.scene.paint.Color.web(AcademyUi.GOLD, 0.55));

        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        popup.setAlwaysOnTop(true);
        Scene popScene = new Scene(box, javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(popScene);
        javafx.geometry.Rectangle2D vb = javafx.stage.Screen.getPrimary().getVisualBounds();
        popup.setX(vb.getMinX() + (vb.getWidth() - 340) / 2);
        popup.setY(vb.getMinY() + (vb.getHeight() - 260) / 2);
        popup.show();

        box.setScaleX(0.3);
        box.setScaleY(0.3);
        box.setOpacity(0);
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
            javafx.util.Duration.millis(420), box);
        st.setFromX(0.3); st.setFromY(0.3); st.setToX(1.06); st.setToY(1.06);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(420), box);
        ft.setFromValue(0); ft.setToValue(1);
        javafx.animation.SequentialTransition in = new javafx.animation.SequentialTransition();
        javafx.animation.ParallelTransition pop = new javafx.animation.ParallelTransition(st, ft);
        javafx.animation.ScaleTransition settle = new javafx.animation.ScaleTransition(
            javafx.util.Duration.millis(160), box);
        settle.setFromX(1.06); settle.setFromY(1.06); settle.setToX(1); settle.setToY(1);
        in.getChildren().addAll(pop, settle);
        in.setOnFinished(e2 -> {
            javafx.animation.FadeTransition out = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(550), box);
            out.setFromValue(1); out.setToValue(0);
            out.setDelay(javafx.util.Duration.millis(2100));
            out.setOnFinished(e3 -> {
                popup.close();
                badgeAnimActive = false;
                pumpBadgeAnimations();
            });
            out.play();
        });
        in.play();
    }

    private boolean isChallengeDone(String id) {
        return leaderboard.containsKey(operatorID + "_" + id);
    }

    private boolean hasDoneFamily(String family) {
        for (ChallengeData ch : getChallenges()) {
            if (ch.family.equals(family) && isChallengeDone(ch.id)) return true;
        }
        return false;
    }

    private static java.util.List<java.util.Map.Entry<String, Integer>> getGlobalRankings() {
        java.util.Map<String, Integer> xpPerOp = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Integer> e : leaderboard.entrySet()) {
            String op = e.getKey().replaceFirst("_[^_]+$", "");
            xpPerOp.merge(op, e.getValue(), Integer::sum);
        }
        java.util.List<java.util.Map.Entry<String, Integer>> list = new java.util.ArrayList<>(xpPerOp.entrySet());
        list.sort(java.util.Map.Entry.<String, Integer>comparingByValue().reversed());
        return list;
    }

    // === PROFILE DASHBOARD ===

    private void showProfile() {
        academyActive = true;
        VBox main = new VBox(15);
        main.setPadding(new Insets(25));
        main.setStyle(BG_DARK);

        Button backBtn = new Button("\u2B05 BACK TO ACADEMY");
        backBtn.setStyle("-fx-background-color: #30363d; -fx-text-fill: white; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> showLearningModule());

        // Header
        HBox header = new HBox(20);
        Label avatar = new Label("\uD83C\uDFF4\u200D\u2620\uFE0F");
        avatar.setStyle("-fx-font-size: 48px;");
        VBox idBox = new VBox(4);
        Label opLabel = new Label(operatorID);
        opLabel.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 18px; -fx-font-weight: bold;");
        int lvl = totalXP / 200 + 1;
        String rank = getRankForXP(totalXP);
        Label rl = new Label("Level " + lvl + " \u2014 " + rank);
        rl.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 14px;");
        idBox.getChildren().addAll(opLabel, rl);
        header.getChildren().addAll(avatar, idBox);

        // Rank Progress Card
        VBox rankCard = new VBox(8);
        rankCard.setStyle("-fx-background-color: #161b22; -fx-padding: 18; -fx-border-color: #30363d; -fx-border-radius: 8;");
        Label rTitle = new Label("\uD83C\uDFC6 RANK PROGRESS");
        rTitle.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-weight: bold;");

        String nextRank = getNextRank(totalXP);
        double progress = getRankProgress(totalXP);
        int needed = getRankXPNeeded(totalXP);

        ProgressBar rBar = new ProgressBar(Math.min(progress, 1.0));
        rBar.setPrefWidth(Double.MAX_VALUE);
        rBar.setStyle("-fx-accent: #FFD700;");

        Label rStat = new Label(String.format("%s  |  +%d XP to %s  |  %.0f%%",
            rank, needed, nextRank, progress * 100));
        rStat.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");

        rankCard.getChildren().addAll(rTitle, rBar, rStat);

        // Stats Card
        VBox statsCard = new VBox(8);
        statsCard.setStyle("-fx-background-color: #161b22; -fx-padding: 18; -fx-border-color: #30363d; -fx-border-radius: 8;");
        Label sTitle = new Label("\uD83D\uDCCA STATISTICS");
        sTitle.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-weight: bold;");

        int rankPos = 1;
        java.util.List<java.util.Map.Entry<String, Integer>> rankings = getGlobalRankings();
        for (java.util.Map.Entry<String, Integer> e : rankings) {
            if (e.getKey().equals(operatorID)) break;
            rankPos++;
        }

        String[] sLines = {
            "\uD83C\uDFC6 Challenges: " + completedChallenges + "/" + CHALLENGE_COUNT,
            "\u26A1 Total XP: " + totalXP,
            "\uD83D\uDD30 Level: " + lvl,
            "\uD83C\uDFC6 Leaderboard: #" + rankPos + "/" + rankings.size()
        };
        for (String sl : sLines) {
            Label l = new Label(sl);
            l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
            statsCard.getChildren().add(l);
        }

        // Badges Card
        VBox badgeCard = new VBox(8);
        badgeCard.setStyle("-fx-background-color: #161b22; -fx-padding: 18; -fx-border-color: #30363d; -fx-border-radius: 8;");
        Label bTitle = new Label("\uD83C\uDFC6 BADGES (" + achievedBadges.size() + "/" + ALL_BADGES.size() + ")");
        bTitle.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane badgeGrid = new GridPane();
        badgeGrid.setHgap(10);
        badgeGrid.setVgap(10);
        int col = 0, row = 0;
        for (java.util.Map.Entry<String, String[]> b : ALL_BADGES.entrySet()) {
            boolean earned = achievedBadges.contains(b.getKey());
            VBox cell = new VBox(2);
            cell.setStyle("-fx-background-color: " + (earned ? "#1a3a2a" : "#0d1117") + "; "
                + "-fx-padding: 10; -fx-border-color: " + (earned ? "#39FF14" : "#30363d") + "; "
                + "-fx-border-radius: 6; -fx-alignment: center;");
            cell.setPrefWidth(120);
            Label iconLbl = new Label(earned ? b.getValue()[0] : "\u2753");
            iconLbl.setStyle("-fx-font-size: 20px;");
            Label nameLbl = new Label(earned ? b.getValue()[1] : "???");
            nameLbl.setStyle("-fx-text-fill: " + (earned ? "#c9d1d9" : "#484f58") + "; -fx-font-size: 10px; -fx-alignment: center;");
            cell.getChildren().addAll(iconLbl, nameLbl);
            badgeGrid.add(cell, col, row);
            col++;
            if (col >= 3) { col = 0; row++; }
        }
        badgeCard.getChildren().addAll(bTitle, badgeGrid);

        // Global Leaderboard Card
        VBox lbCard = new VBox(8);
        lbCard.setStyle("-fx-background-color: #161b22; -fx-padding: 18; -fx-border-color: #30363d; -fx-border-radius: 8;");
        Label lbTitle = new Label("\uD83C\uDFC6 GLOBAL CRYPTOGRAPH LEADERBOARD");
        lbTitle.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-weight: bold;");

        int pos = 1;
        for (java.util.Map.Entry<String, Integer> e : rankings) {
            boolean isMe = e.getKey().equals(operatorID);
            String stars = "\u2B50".repeat(Math.min((e.getValue() / 300) + 1, 5));
            String line = String.format("#%d  %s%-25s %4d XP  %s",
                pos, isMe ? "\u25B6 " : "  ", e.getKey(), e.getValue(), stars);
            Label ll = new Label(line);
            ll.setStyle("-fx-text-fill: " + (isMe ? "#39FF14" : "#c9d1d9") + "; "
                + "-fx-font-size: 12px; -fx-font-family: 'Courier New';");
            if (isMe) ll.setStyle(ll.getStyle() + " -fx-font-weight: bold;");
            lbCard.getChildren().add(ll);
            pos++;
            if (pos > 10) break;
        }

        if (rankings.isEmpty()) {
            lbCard.getChildren().add(new Label("  No operators yet — complete a challenge to appear!"));
        }

        // Military-Grade progress
        int milXP = Math.min(totalXP, 2700);
        double milPct = milXP / 2700.0;
        ProgressBar milBar = new ProgressBar(milPct);
        milBar.setPrefWidth(Double.MAX_VALUE);
        milBar.setStyle("-fx-accent: #f78166;");
        Label milLabel = new Label(String.format(
            "\uD83D\uDEE1 Military-Grade Specialist: %.0f%%  (%d/2700 XP)", milPct * 100, totalXP));
        milLabel.setStyle("-fx-text-fill: #f78166; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Layout
        VBox topRow = new VBox(12, header, rankCard, statsCard);
        VBox bottomRow = new VBox(12, badgeCard, lbCard, milBar, milLabel, backBtn);

        ScrollPane scroll = new ScrollPane(new VBox(12, topRow, bottomRow));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(700);

        main.getChildren().add(scroll);
        setCenter(main);
    }

    // ============================================================
    // FORTRESS ACADEMY v2 — PROFESSIONAL OPERATOR DASHBOARD
    // ============================================================

    private static final String[][] ALL_CERTS = {
        {"RECRUIT", "First Blood", "Solve 5 challenges", "5"},
        {"ANALYST", "Cipher Breaker", "Solve 25 challenges", "25"},
        {"EXPERT", "Cryptographer", "Solve 50 challenges", "50"},
        {"ELITE", "Cryptologist", "Solve 100 challenges", "100"},
        {"GRANDMASTER", "Fortress Legend", "Solve all " + CHALLENGE_COUNT + " challenges", String.valueOf(CHALLENGE_COUNT)}
    };

    private static final String[] CERT_FAMILIES = {
        "caesar", "rot13", "atbash", "hex", "binary", "base64",
        "vigenere", "xor", "morse", "affine", "railfence", "tripleagent"
    };

    private static String countryFlag(String country) {
        return switch (country) {
            case "Tanzania" -> "\uD83C\uDDF9\uD83C\uDDFF";
            case "Kenya" -> "\uD83C\uDDF0\uD83C\uDDEA";
            case "Uganda" -> "\uD83C\uDDFA\uD83C\uDDEC";
            case "Nigeria" -> "\uD83C\uDDF3\uD83C\uDDEC";
            case "South Africa" -> "\uD83C\uDDFF\uD83C\uDDE6";
            case "Egypt" -> "\uD83C\uDDEA\uD83C\uDDEC";
            case "Ghana" -> "\uD83C\uDDEC\uD83C\uDDED";
            case "Morocco" -> "\uD83C\uDDF2\uD83C\uDDE6";
            case "Rwanda" -> "\uD83C\uDDF7\uD83C\uDDFC";
            case "Ethiopia" -> "\uD83C\uDDEA\uD83C\uDDF9";
            case "USA" -> "\uD83C\uDDFA\uD83C\uDDF8";
            case "UK" -> "\uD83C\uDDEC\uD83C\uDDE7";
            case "Germany" -> "\uD83C\uDDE9\uD83C\uDDEA";
            case "India" -> "\uD83C\uDDEE\uD83C\uDDF3";
            case "Brazil" -> "\uD83C\uDDE7\uD83C\uDDF7";
            case "China" -> "\uD83C\uDDE8\uD83C\uDDF3";
            case "Japan" -> "\uD83C\uDDEF\uD83C\uDDF5";
            case "France" -> "\uD83C\uDDEB\uD83C\uDDF7";
            case "Canada" -> "\uD83C\uDDE8\uD83C\uDDE6";
            case "Australia" -> "\uD83C\uDDE6\uD83C\uDDFA";
            default -> "\uD83C\uDF10";
        };
    }

    private static String diffColor(String diff) {
        return switch (diff) {
            case "EASY" -> AcademyUi.GREEN;
            case "MEDIUM" -> AcademyUi.GOLD;
            default -> AcademyUi.RED;
        };
    }

    private void resetAcademyProgress() {
        leaderboard.clear();
        totalXP = 0;
        completedChallenges = 0;
        achievedBadges.clear();
        academy.resetAll();
        showAcademyDashboard();
        addLog("[ACADEMY] Fortress profile wiped and reset.");
    }

    private void showAcademyDashboard() {
        academyActive = true;
        stopMissionClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = AcademyUi.neon("\uD83C\uDF93 UC-FORTRESS ACADEMY", AcademyUi.GREEN, 24);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.GREEN, 0.35));
        Label sub = AcademyUi.caption("Learn \u2022 Practice \u2022 Compete \u2014 cryptographic training ground, operator.", 12);
        titleRow.getChildren().addAll(title, AcademyUi.spacer(), sub);

        HBox navRow = new HBox(10);
        Button challengesBtn = AcademyUi.button("\uD83C\uDFEF CHALLENGES", "#1f6feb", "#ffffff");
        challengesBtn.setOnAction(e -> showLearningModule());
        Button profileBtn = AcademyUi.button("\uD83D\uDC64 PROFILE", "#8957e5", "#ffffff");
        profileBtn.setOnAction(e -> showProfile());
        Button lbBtn = AcademyUi.button("\uD83C\uDFC6 LEADERBOARD", "#30363d", AcademyUi.LIGHT);
        lbBtn.setOnAction(e -> showGlobalLeaderboard());
        Button missionsBtn = AcademyUi.button("\uD83C\uDFC5 MISSIONS", "#1f6feb", "#ffffff");
        missionsBtn.setOnAction(e -> showDailyChallenges());
        Button certBtn = AcademyUi.button("\uD83D\uDCDC CERTIFICATES", "#30363d", AcademyUi.LIGHT);
        certBtn.setOnAction(e -> showCertificates());
        Button resetBtn = AcademyUi.button("\uD83D\uDD04 RESET", "#da3633", "#ffffff");
        resetBtn.setOnAction(e -> resetAcademyProgress());
        navRow.getChildren().addAll(challengesBtn, profileBtn, lbBtn, missionsBtn, certBtn, AcademyUi.spacer(), resetBtn);

        VBox headWrap = new VBox(10, titleRow, navRow);
        AcademyUi.animateIn(headWrap);

        main.getChildren().addAll(headWrap, buildHeroCard(), buildStatGrid());

        HBox split = new HBox(16);
        javafx.scene.Node act = buildActivityCard();
        javafx.scene.Node mentor = buildAiMentorCard();
        javafx.scene.layout.HBox.setHgrow(act, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(mentor, javafx.scene.layout.Priority.ALWAYS);
        split.getChildren().addAll(act, mentor);
        main.getChildren().add(split);

        main.getChildren().addAll(buildPracticeLab(), buildCertPreview());

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildHeroCard() {
        HBox hero = new HBox(20);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(20));
        hero.setStyle("-fx-background-color: #161b22; -fx-background-radius: 14; -fx-border-color: #39FF14; -fx-border-radius: 14; -fx-border-width: 1.2;");
        AcademyUi.glow(hero, javafx.scene.paint.Color.web(AcademyUi.GREEN, 0.15));

        Label avatar = new Label("\uD83D\uDD75\uFE0F");
        avatar.setStyle("-fx-font-size: 52px;");

        VBox idCol = new VBox(4);
        idCol.setAlignment(Pos.CENTER_LEFT);
        Label name = AcademyUi.neon(operatorID, AcademyUi.GREEN, 18);
        int lvl = totalXP / 200 + 1;
        GlobalPosition gp = academy.getGlobalPosition();
        Label rankLine = AcademyUi.text("Level " + lvl + "  \u2022  " + getRankForXP(totalXP)
            + "  \u2022  " + countryFlag(gp.country) + " " + gp.country, 13);
        Label streakLine = AcademyUi.caption("\uD83D\uDD25 " + academy.getStreak() + "-day streak  |  \uD83C\uDFC6 Global #"
            + gp.globalRank + " / " + gp.globalCount, 12);
        idCol.getChildren().addAll(name, rankLine, streakLine);

        Region sp = AcademyUi.spacer();

        VBox xpCol = new VBox(6);
        xpCol.setPrefWidth(340);
        Label xpLab = AcademyUi.neon(totalXP + " XP", AcademyUi.GOLD, 20);
        Label nextRankLab = AcademyUi.text("Current: " + getRankForXP(totalXP) + "  \u2192  next: " + getNextRank(totalXP), 12);
        ProgressBar xpBar = new ProgressBar();
        xpBar.setPrefWidth(320);
        AcademyUi.animateProgress(xpBar, getRankProgress(totalXP), AcademyUi.GOLD);
        int needed = getRankXPNeeded(totalXP);
        Label neededLab = AcademyUi.caption((needed == 0 ? "MAXIMUM RANK REACHED" : "+" + needed + " XP to " + getNextRank(totalXP)), 11);
        xpCol.getChildren().addAll(xpLab, nextRankLab, xpBar, neededLab);

        hero.getChildren().addAll(avatar, idCol, sp, xpCol);
        AcademyUi.animateIn(hero);
        return hero;
    }

    private javafx.scene.Node buildStatGrid() {
        VBox box = new VBox(12);
        box.getChildren().add(AcademyUi.section("\uD83D\uDCCA OPERATOR TELEMETRY", AcademyUi.BLUE));

        GlobalPosition gp = academy.getGlobalPosition();
        double sr = academy.getSuccessRate();
        String[][] tiles = {
            {"\uD83C\uDFC6", String.valueOf(completedChallenges), "CHALLENGES SOLVED", AcademyUi.GREEN},
            {"\uD83C\uDFAF", String.valueOf(academy.getRemaining(CHALLENGE_COUNT)), "REMAINING", AcademyUi.RED},
            {"\uD83C\uDFAF", String.format("%.0f%%", sr), "SUCCESS RATE", AcademyUi.BLUE},
            {"\u23F1\uFE0F", String.format("%.1f h", academy.getPracticeHours()), "HOURS PRACTICED", AcademyUi.PURPLE},
            {"\uD83D\uDD25", String.valueOf(academy.getStreak()), "CURRENT STREAK", AcademyUi.ORANGE},
            {"\uD83C\uDF1F", String.valueOf(academy.getBestStreak()), "BEST STREAK", AcademyUi.GOLD},
            {"\uD83D\uDCC5", String.valueOf(academy.getDailyXp()), "DAILY XP", AcademyUi.GREEN},
            {"\uD83D\uDDD3\uFE0F", String.valueOf(academy.getWeeklyXp()), "WEEKLY XP", AcademyUi.BLUE},
            {"\uD83D\uDCC6", String.valueOf(academy.getMonthlyXp()), "MONTHLY XP", AcademyUi.PURPLE},
            {"\uD83E\uDE99", String.valueOf(academy.getCoins()), "COINS", AcademyUi.GOLD},
            {"\uD83C\uDF96\uFE0F", String.valueOf(academy.getCertPoints()), "CERT POINTS", AcademyUi.BLUE},
            {"\u26A1", String.valueOf(totalXP), "TOTAL XP", AcademyUi.GOLD},
            {"\uD83C\uDF0D", "#" + gp.globalRank + "/" + gp.globalCount, "GLOBAL RANK", AcademyUi.PURPLE},
            {"\uD83D\uDDFA\uFE0F", "#" + gp.countryRank + "/" + gp.countryCount, gp.country.toUpperCase() + " RANK", AcademyUi.BLUE}
        };
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        int col = 0, row = 0;
        for (String[] t : tiles) {
            VBox tile = AcademyUi.statTile(t[0], t[1], t[2], t[3]);
            tile.setPrefWidth(160);
            grid.add(tile, col, row);
            col++;
            if (col >= 4) { col = 0; row++; }
        }
        box.getChildren().add(grid);
        return box;
    }

    private javafx.scene.Node buildActivityCard() {
        VBox card = AcademyUi.cardAccent(AcademyUi.BLUE);
        card.getChildren().add(AcademyUi.section("\uD83D\uDD59 RECENT ACTIVITY", AcademyUi.BLUE));
        java.util.List<String> acts = academy.getActivity();
        if (acts.isEmpty()) {
            card.getChildren().add(AcademyUi.caption(
                "No activity yet. Crack your first cipher in CHALLENGES to light up this feed.", 12));
        } else {
            int shown = 0;
            for (String a : acts) {
                if (shown++ >= 8) break;
                Label l = new Label("\u25B8 " + a);
                l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
                card.getChildren().add(l);
            }
        }
        return card;
    }

    private javafx.scene.Node buildAiMentorCard() {
        VBox card = AcademyUi.cardAccent(AcademyUi.PURPLE);
        card.getChildren().addAll(
            AcademyUi.section("\uD83E\uDD16 AI MENTOR", AcademyUi.PURPLE),
            AcademyUi.text(academy.mentorTip(), 12),
            AcademyUi.caption("Tips refresh as your progress grows. Use \uD83E\uDD16 AI HINT on any challenge card for step-by-step help.", 11));
        return card;
    }

    private void generateAndRefresh(String difficulty) {
        Challenge ch = academy.generateChallenge(difficulty);
        addLog("[ACADEMY] Generated " + difficulty + " cipher \u2014 " + ch.title + " (+" + ch.xp + " XP).");
        showAcademyDashboard();
    }

    private javafx.scene.Node buildPracticeLab() {
        VBox box = new VBox(12);
        box.getChildren().add(AcademyUi.section("\u26A1 PRACTICE LAB \u2014 GENERATED CHALLENGES", AcademyUi.GREEN));

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label how = AcademyUi.caption("The AI vault summons fresh, verifiable ciphers on demand. Solve them for real XP.", 11);
        Region sp = AcademyUi.spacer();
        Button easyBtn = AcademyUi.button("\uD83D\uDFE2 EASY", "#238636", "#ffffff");
        Button medBtn = AcademyUi.button("\uD83D\uDFE1 MEDIUM", "#9e6a03", "#ffffff");
        Button hardBtn = AcademyUi.button("\uD83D\uDD34 HARD", "#b62324", "#ffffff");
        easyBtn.setOnAction(e -> generateAndRefresh("EASY"));
        medBtn.setOnAction(e -> generateAndRefresh("MEDIUM"));
        hardBtn.setOnAction(e -> generateAndRefresh("HARD"));
        controls.getChildren().addAll(how, sp, easyBtn, medBtn, hardBtn);
        box.getChildren().add(controls);

        java.util.List<Challenge> gens = academy.getGenerated();
        if (gens.isEmpty()) {
            box.getChildren().add(AcademyUi.caption(
                "No generated challenges yet. Hit EASY / MEDIUM / HARD above to summon one.", 12));
            return box;
        }
        int from = Math.max(0, gens.size() - 6);
        for (int i = from; i < gens.size(); i++) {
            box.getChildren().add(buildGeneratedCard(gens.get(i)));
        }
        if (gens.size() > 6) {
            box.getChildren().add(AcademyUi.caption(
                "+" + (gens.size() - 6) + " more in your vault (persisted across sessions).", 11));
        }
        return box;
    }

    private javafx.scene.Node buildGeneratedCard(Challenge ch) {
        VBox card = AcademyUi.card();
        boolean done = leaderboard.containsKey(operatorID + "_" + ch.id);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label starsLab = new Label("\u2B50".repeat(ch.stars));
        starsLab.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13px;");
        Label titleLab = AcademyUi.neon(ch.title, AcademyUi.GREEN, 14);
        Label diffLab = AcademyUi.pill(ch.diff, diffColor(ch.diff));
        Label xpLab = AcademyUi.pill("+" + ch.xp + " XP", AcademyUi.BLUE);
        Label famLab = AcademyUi.caption(AcademyService.familyName(ch.family), 11);
        Region sp = AcademyUi.spacer();
        Label statusLab = done
            ? AcademyUi.pill("\u2705 DONE", "#3fb950")
            : AcademyUi.pill("\uD83D\uDD12 UNSOLVED", AcademyUi.RED);
        top.getChildren().addAll(starsLab, titleLab, diffLab, xpLab, famLab, sp, statusLab);

        Label descr = AcademyUi.text(ch.descr, 12);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        TextField answer = new TextField();
        answer.setPromptText("Enter flag or answer...");
        answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-pref-width: 320; -fx-background-radius: 6;");
        answer.setDisable(done);

        Button submit = AcademyUi.button(done ? "\u2705 DONE" : "\u26A1 SUBMIT", done ? "#238636" : "#1f6feb", "#ffffff");
        submit.setDisable(done);
        long t0 = System.currentTimeMillis();
        if (!done) {
            String cid = ch.id; int cxp = ch.xp; String cflag = ch.flag;
            submit.setOnAction(e -> {
                if (answer.getText().trim().equalsIgnoreCase(cflag)) {
                    leaderboard.put(operatorID + "_" + cid, cxp);
                    totalXP += cxp;
                    completedChallenges++;
                    academy.onSolve(cid, cxp);
                    academy.recordSolveTime(System.currentTimeMillis() - t0);
                    computeBadges();
                    sendAuditLog("GEN_" + cid.toUpperCase(), "ACADEMY");
                    addLog("[ACADEMY] +" + cxp + "XP \u2014 generated cipher cracked!");
                    showAcademyDashboard();
                } else {
                    academy.recordAttempt(false);
                    answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                    addLog("[ACADEMY] Wrong answer for generated cipher. Try again.");
                }
            });
        }

        Button aiBtn = AcademyUi.button("\uD83E\uDD16 AI HINT", "#8957e5", "#ffffff");
        Label hintLab = AcademyUi.caption("", 11);
        int[] hintLevel = {0};
        aiBtn.setOnAction(e -> {
            hintLevel[0]++;
            if (hintLevel[0] > 3) hintLevel[0] = 1;
            academy.recordHintUse();
            String hint = academy.aiHint(ch, hintLevel[0]);
            hintLab.setText("\uD83E\uDD16 " + hint);
            hintLab.setStyle("-fx-text-fill: " + (hintLevel[0] == 3 ? AcademyUi.GOLD : "#a79fe6")
                + "; -fx-font-size: 11px; -fx-font-style: italic;");
        });

        Button simBtn = AcademyUi.button("\uD83E\uDDEA SIMULATE", "#30363d", AcademyUi.LIGHT);
        String simFam = ch.family;
        simBtn.setOnAction(e -> showAlgorithmPlayground(simFam));

        Button delBtn = AcademyUi.button("\uD83D\uDDD1\uFE0F", "#da3633", "#ffffff");
        delBtn.setOnAction(e -> { academy.removeGenerated(ch.id); showAcademyDashboard(); });

        row.getChildren().addAll(answer, submit, aiBtn, simBtn, delBtn);
        card.getChildren().addAll(top, descr, row, hintLab);
        return card;
    }

    private javafx.scene.Node buildCertPreview() {
        VBox card = AcademyUi.cardAccent(AcademyUi.GOLD);
        int earned = earnedCertificateCount();
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label status = AcademyUi.text(earned + " of " + (ALL_CERTS.length + CERT_FAMILIES.length)
            + " certificates earned.", 12);
        Button openBtn = AcademyUi.button("\uD83D\uDCDC OPEN CERTIFICATE VAULT", "#8957e5", "#ffffff");
        openBtn.setOnAction(e -> showCertificates());
        row.getChildren().addAll(status, AcademyUi.spacer(), openBtn);
        card.getChildren().addAll(AcademyUi.section("\uD83C\uDFC5 CERTIFICATES", AcademyUi.GOLD), row);
        return card;
    }

    private int earnedCertificateCount() {
        int n = 0;
        for (String[] c : ALL_CERTS) {
            if (completedChallenges >= Integer.parseInt(c[3])) n++;
        }
        for (String f : CERT_FAMILIES) {
            if (hasDoneFamily(f)) n++;
        }
        return n;
    }

    private void showCertificates() {
        academyActive = true;
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFC5 FORTRESS CERTIFICATE VAULT", AcademyUi.GOLD, 22);
        Label sub = AcademyUi.caption(
            "Earn certificates by mastering tiers and cipher families. Export any earned certificate as a signed PDF.", 12);

        main.getChildren().addAll(backBtn, title, sub);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        int col = 0, row = 0;
        for (String[] c : ALL_CERTS) {
            boolean earned = completedChallenges >= Integer.parseInt(c[3]);
            grid.add(buildCertCard(c[0], c[1], c[2], earned), col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }
        for (String f : CERT_FAMILIES) {
            boolean earned = hasDoneFamily(f);
            grid.add(buildCertCard(
                AcademyService.familyName(f).toUpperCase(),
                AcademyService.familyName(f),
                "Master the " + AcademyService.familyName(f), earned), col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }
        main.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildCertCard(String name, String certTitle, String criteria, boolean earned) {
        VBox card = earned ? AcademyUi.cardAccent(AcademyUi.GOLD) : AcademyUi.card();
        card.setPrefWidth(380);
        Label seal = new Label(earned ? "\uD83C\uDFC6" : "\uD83D\uDD12");
        seal.setStyle("-fx-font-size: 34px;");
        Label n = AcademyUi.neon(name, earned ? AcademyUi.GOLD : AcademyUi.DIM, 15);
        Label t = AcademyUi.text(certTitle, 12);
        Label c = AcademyUi.caption(criteria, 11);
        card.getChildren().addAll(seal, n, t, c);
        if (earned) {
            Button export = AcademyUi.button("\uD83D\uDCC4 EXPORT PDF", "#8957e5", "#ffffff");
            export.setOnAction(e -> exportCertificatePdf(name, certTitle));
            card.getChildren().add(export);
        } else {
            card.getChildren().add(AcademyUi.caption("LOCKED \u2014 keep training.", 10));
        }
        return card;
    }

    private void exportCertificatePdf(String certName, String certTitle) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("UC_Fortress_" + certName.replace(" ", "_") + "_Certificate.pdf");
        File file = fc.showSaveDialog(null);
        if (file == null) return;
        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(
                com.itextpdf.text.PageSize.A4.rotate(), 50, 50, 50, 50);
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(file));
            doc.open();

            com.itextpdf.text.Font titleFont = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA_BOLD, 42, com.itextpdf.text.BaseColor.GREEN);
            com.itextpdf.text.Font bodyFont = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA, 16, com.itextpdf.text.BaseColor.DARK_GRAY);
            com.itextpdf.text.Font nameFont = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA_BOLD, 26, com.itextpdf.text.BaseColor.BLACK);
            com.itextpdf.text.Font smallFont = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA, 12, com.itextpdf.text.BaseColor.DARK_GRAY);

            com.itextpdf.text.Paragraph p1 = new com.itextpdf.text.Paragraph("UC-FORTRESS ACADEMY", titleFont);
            p1.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p1);

            com.itextpdf.text.Paragraph p2 = new com.itextpdf.text.Paragraph("\nCERTIFICATE OF ACHIEVEMENT", bodyFont);
            p2.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p2);

            com.itextpdf.text.Paragraph p3 = new com.itextpdf.text.Paragraph("\n\nThis certifies that", bodyFont);
            p3.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p3);

            com.itextpdf.text.Paragraph p4 = new com.itextpdf.text.Paragraph(operatorID, nameFont);
            p4.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p4);

            com.itextpdf.text.Paragraph p5 = new com.itextpdf.text.Paragraph(
                "\nhas earned the " + certTitle + " certificate\nfor outstanding cryptographic achievement "
                + "in the Fortress Academy training program.", bodyFont);
            p5.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p5);

            com.itextpdf.text.Paragraph p6 = new com.itextpdf.text.Paragraph(
                "\n\nIssued: " + java.time.LocalDate.now().toString(), smallFont);
            p6.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(p6);

            doc.close();
            addLog("[ACADEMY] Certificate exported: " + file.getAbsolutePath());
        } catch (Exception ex) {
            addLog("[ERROR] Certificate export failed: " + ex.getMessage());
        }
    }

    private void showGlobalLeaderboard() {
        academyActive = true;
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFC6 CRYPTOGRAPH NETWORK \u2014 LEADERBOARDS", AcademyUi.PURPLE, 22);
        Label sub = AcademyUi.caption(
            "Deterministic offline standings \u2014 simulated tiers, stable per operator. Keep solving to climb!", 12);

        HBox tabRow = new HBox(8);
        String[][] tabs = {
            {"global", "\uD83C\uDF0D GLOBAL"},
            {"weekly", "\uD83D\uDCC5 WEEKLY"},
            {"monthly", "\uD83D\uDDD3 MONTHLY"},
            {"friends", "\uD83D\uDC65 FRIENDS"},
            {"universities", "\uD83C\uDF93 UNIVERSITIES"},
            {"companies", "\uD83C\uDFE2 COMPANIES"},
            {"cryptographers", "\uD83E\uDDE0 CRYPTOGRAPHERS"}
        };
        for (String[] t : tabs) {
            boolean active = leaderTab.equals(t[0]);
            Button tb = AcademyUi.button(t[1], active ? AcademyUi.PURPLE : "#30363d",
                active ? "#0d1117" : AcademyUi.LIGHT);
            tb.setOnAction(e -> {
                leaderTab = t[0];
                showGlobalLeaderboard();
            });
            tabRow.getChildren().add(tb);
        }

        main.getChildren().addAll(backBtn, title, sub, tabRow);

        VBox board = new VBox(16);
        switch (leaderTab) {
            case "weekly" -> {
                HBox cards = new HBox(16);
                cards.getChildren().add(summaryCard("\uD83D\uDCC5 THIS WEEK", academy.getWeeklyXp() + " XP",
                    "earned this week \u2014 solve daily to climb", AcademyUi.GREEN));
                board.getChildren().add(cards);
                board.getChildren().add(buildLeaderboardBoard(academy.getWeeklyStandings(20), AcademyUi.GREEN,
                    "\uD83D\uDCC5 WEEKLY LEADERBOARD", "This week's earned XP, deterministic per operator."));
            }
            case "monthly" -> {
                HBox cards = new HBox(16);
                cards.getChildren().add(summaryCard("\uD83D\uDDD3 THIS MONTH", academy.getMonthlyXp() + " XP",
                    "earned this month \u2014 pace yourself", AcademyUi.BLUE));
                board.getChildren().add(cards);
                board.getChildren().add(buildLeaderboardBoard(academy.getMonthlyStandings(20), AcademyUi.BLUE,
                    "\uD83D\uDDD3 MONTHLY LEADERBOARD", "This month's earned XP across the network."));
            }
            case "friends" -> {
                HBox cards = new HBox(16);
                cards.getChildren().add(summaryCard("\uD83D\uDC65 YOUR CIRCLE", "10 friends",
                    "head-to-head with your squad", AcademyUi.GOLD));
                board.getChildren().add(cards);
                board.getChildren().add(buildLeaderboardBoard(academy.getFriendsStandings(20), AcademyUi.GOLD,
                    "\uD83D\uDC65 FRIENDS LEADERBOARD", "Your squad \u2014 settle who really decodes."));
            }
            case "universities" -> board.getChildren().add(buildLeaderboardBoard(academy.getTopUniversities(20),
                AcademyUi.PURPLE, "\uD83C\uDF93 TOP UNIVERSITIES", "Aggregate team XP of the top academies."));
            case "companies" -> board.getChildren().add(buildLeaderboardBoard(academy.getTopCompanies(20),
                AcademyUi.ORANGE, "\uD83C\uDFE2 TOP COMPANIES", "Aggregate team XP of the top security firms."));
            case "cryptographers" -> board.getChildren().add(buildLeaderboardBoard(academy.getTopCryptographers(20),
                AcademyUi.GOLD, "\uD83E\uDDE0 LEGENDARY CRYPTOGRAPHERS", "Lifetime reputation of history's greatest minds."));
            default -> {
                GlobalPosition gp = academy.getGlobalPosition();
                HBox cards = new HBox(16);
                cards.getChildren().add(summaryCard("\uD83C\uDF0D GLOBAL", "#" + gp.globalRank,
                    "of " + gp.globalCount + " operators", AcademyUi.PURPLE));
                cards.getChildren().add(summaryCard(countryFlag(gp.country) + " " + gp.country.toUpperCase(),
                    "#" + gp.countryRank, "of " + gp.countryCount + " in " + gp.country, AcademyUi.BLUE));
                board.getChildren().add(cards);
                board.getChildren().add(buildLeaderboardBoard(academy.getGlobalStandings(20), AcademyUi.LIGHT,
                    "\uD83C\uDF0D GLOBAL LEADERBOARD", "All-time XP across the simulated network."));
            }
        }
        main.getChildren().add(board);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private VBox summaryCard(String title, String value, String sub, String accent) {
        VBox box = AcademyUi.cardAccent(accent);
        box.setPrefWidth(300);
        box.getChildren().addAll(
            AcademyUi.section(title, accent),
            AcademyUi.neon(value, accent, 26),
            AcademyUi.caption(sub, 11));
        return box;
    }

    private VBox buildLeaderboardBoard(java.util.List<Standing> rows, String accent, String boardTitle, String boardSub) {
        VBox card = AcademyUi.card();
        card.getChildren().add(AcademyUi.section(boardTitle, accent));
        card.getChildren().add(AcademyUi.caption(boardSub, 11));

        Label header = new Label(String.format("%-5s %-3s %-24s %-18s %-5s %-9s %s",
            "#", "AV", "OPERATOR", "COUNTRY", "LVL", "XP", "BADGES"));
        header.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: "
            + AcademyUi.DIM + "; -fx-font-weight: bold;");
        card.getChildren().add(header);

        for (Standing s : rows) {
            int badges = s.me ? achievedBadges.size() : s.badges;
            String glyph = "\uD83D\uDEE1\uFE0F".repeat(Math.min(badges, 5))
                + (badges > 5 ? " \u00D7" + badges : "");
            Label line = new Label(String.format("%-5d %-3s %-24s %-18s %-5s %-9d %s",
                s.rank, s.avatar, truncate(s.name, 24),
                countryFlag(s.country) + " " + truncate(s.country, 15),
                "Lv" + s.level, s.xp, glyph));
            line.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-text-fill: "
                + (s.me ? AcademyUi.GREEN : AcademyUi.LIGHT) + (s.me ? "; -fx-font-weight: bold;" : ";"));
            card.getChildren().add(line);
        }
        return card;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "\u2026";
    }

    private void showDailyChallenges() {
        academyActive = true;
        stopMissionClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFC5 DAILY CHALLENGES \u2014 MISSIONS", AcademyUi.GOLD, 22);
        Label sub = AcademyUi.caption(
            "Auto-generated missions, refreshed daily, weekly and on weekends. Solve them for XP, coins, badges and certificate points.", 12);

        HBox wallet = new HBox(16);
        wallet.getChildren().addAll(
            summaryCard("\uD83E\uDE99 WALLET", academy.getCoins() + " coins",
                "earned from every solve + mission rewards", AcademyUi.GOLD),
            summaryCard("\uD83C\uDF96\uFE0F CERT POINTS", academy.getCertPoints() + " pts",
                "mission currency that feeds your certificates", AcademyUi.BLUE));

        VBox missions = new VBox(16);
        missions.getChildren().add(AcademyUi.section("\uD83C\uDFC5 ACTIVE MISSIONS", AcademyUi.GOLD));
        java.util.List<Label> tickers = new java.util.ArrayList<>();
        AcademyService.Mission daily = academy.getMission("DAILY");
        missions.getChildren().add(buildMissionCard(daily, tickers, AcademyUi.GREEN));
        AcademyService.Mission weekly = academy.getMission("WEEKLY");
        missions.getChildren().add(buildMissionCard(weekly, tickers, AcademyUi.BLUE));
        if (academy.isWeekendToday()) {
            missions.getChildren().add(buildMissionCard(academy.getMission("WEEKEND"), tickers, AcademyUi.PURPLE));
        } else {
            missions.getChildren().add(buildWeekendLockedCard());
        }

        main.getChildren().addAll(backBtn, title, sub, wallet, missions);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);

        missionClock = new AnimationTimer() {
            private long last = -1;
            @Override
            public void handle(long now) {
                if (last < 0 || now - last > 1_000_000_000L) {
                    last = now;
                    for (Label l : tickers) {
                        l.setText(formatCountdown(academy.getMissionSecondsLeft((String) l.getUserData())));
                    }
                }
            }
        };
        missionClock.start();
    }

    private void stopMissionClock() {
        if (missionClock != null) {
            missionClock.stop();
            missionClock = null;
        }
    }

    private static String formatCountdown(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private javafx.scene.Node buildMissionCard(AcademyService.Mission m,
        java.util.List<Label> tickers, String accent) {
        VBox card = AcademyUi.card();
        boolean done = m.done || leaderboard.containsKey(operatorID + "_" + m.key);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        String icon = m.type.equals("WEEKEND") ? "\uD83C\uDFF0\uFE0F "
            : m.type.equals("WEEKLY") ? "\uD83D\uDCC6 " : "\uD83C\uDFC5 ";
        Label typeLab = AcademyUi.neon(icon + m.type + " MISSION", accent, 15);
        Label countdown = AcademyUi.pill(formatCountdown(academy.getMissionSecondsLeft(m.type)), accent);
        countdown.setUserData(m.type);
        tickers.add(countdown);
        Region sp = AcademyUi.spacer();
        Label statusLab = done
            ? AcademyUi.pill("\u2705 COMPLETED", "#3fb950")
            : AcademyUi.pill("\uD83D\uDD12 ACTIVE", accent);
        top.getChildren().addAll(typeLab, sp, countdown, statusLab);

        Label period = AcademyUi.caption("Window " + m.period + "  \u2022  auto-generated for " + operatorID, 11);
        Label challengeLab = AcademyUi.text("\uD83C\uDD98 " + m.challenge.descr, 13);
        Label famLab = AcademyUi.caption(AcademyService.familyName(m.challenge.family)
            + "  \u2022  " + m.challenge.stars + " stars  \u2022  difficulty " + m.challenge.diff, 11);

        HBox rewards = new HBox(10);
        rewards.getChildren().addAll(
            AcademyUi.pill("+" + (m.challenge.xp + m.bonusXp) + " XP", AcademyUi.GREEN),
            AcademyUi.pill("\uD83E\uDE99 +" + m.coins + " coins", AcademyUi.GOLD),
            AcademyUi.pill("\uD83C\uDF96\uFE0F +" + m.certPoints + " cert pts", AcademyUi.BLUE),
            AcademyUi.pill("\uD83C\uDFC5 badge unlock", AcademyUi.PURPLE));

        card.getChildren().addAll(top, period, challengeLab, famLab, rewards);

        if (!done) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            TextField answer = new TextField();
            answer.setPromptText("Enter flag or answer...");
            answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-pref-width: 320; -fx-background-radius: 6;");
            Button submit = AcademyUi.button("\u26A1 SUBMIT", "#1f6feb", "#ffffff");
            Button aiBtn = AcademyUi.button("\uD83E\uDD16 AI HINT", "#8957e5", "#ffffff");
            Label hintLab = AcademyUi.caption("", 11);
            int[] hintLevel = {0};
            aiBtn.setOnAction(e -> {
                hintLevel[0]++;
                if (hintLevel[0] > 3) hintLevel[0] = 1;
                academy.recordHintUse();
                hintLab.setText(academy.aiHint(m.challenge, hintLevel[0]));
            });

            long t0 = System.currentTimeMillis();
            String key = m.key;
            String flag = m.challenge.flag;
            int cxp = m.challenge.xp;
            int cbonus = m.bonusXp;
            int ccoins = m.coins;
            String mtype = m.type;
            submit.setOnAction(e -> {
                if (answer.getText().trim().equalsIgnoreCase(flag)) {
                    leaderboard.put(operatorID + "_" + key, cxp);
                    totalXP += cxp + cbonus;
                    completedChallenges++;
                    academy.solveMission(m);
                    academy.recordSolveTime(System.currentTimeMillis() - t0);
                    computeBadges();
                    sendAuditLog("MISSION_" + mtype, "ACADEMY");
                    addLog("[ACADEMY] " + mtype + " mission complete! +" + (cxp + cbonus)
                        + " XP, +" + ccoins + " coins.");
                    showDailyChallenges();
                } else {
                    academy.recordAttempt(false);
                    answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                    addLog("[ACADEMY] Wrong answer for " + mtype + " mission. Try again.");
                }
            });

            row.getChildren().addAll(answer, submit, aiBtn, hintLab);
            card.getChildren().add(row);
        }
        return card;
    }

    private javafx.scene.Node buildWeekendLockedCard() {
        VBox card = AcademyUi.card();
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label typeLab = AcademyUi.neon("\uD83C\uDFF0\uFE0F WEEKEND MISSION", AcademyUi.PURPLE, 15);
        Region sp = AcademyUi.spacer();
        Label lock = AcademyUi.pill("\uD83D\uDD12 LOCKED", AcademyUi.RED);
        top.getChildren().addAll(typeLab, sp, lock);
        Label info = AcademyUi.text(
            "Unlocks Saturday 00:00 \u2014 HARD tier with triple rewards. Weekend warriors only.", 12);
        card.getChildren().addAll(top, info);
        return card;
    }

    private void showLearningModule() {
        academyActive = true;
        VBox main = new VBox(15);
        main.setPadding(new Insets(25));
        main.setStyle(BG_DARK);

        HBox titleRow = new HBox(15);
        Label title = new Label("\uD83C\uDF93 UC-FORTRESS ACADEMY \u2014 CTF MODE");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 22px; -fx-font-weight: bold;");
        Button profileBtn = new Button("\uD83D\uDC64 MY PROFILE");
        profileBtn.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;");
        profileBtn.setOnAction(e -> showProfile());
        titleRow.getChildren().addAll(title, profileBtn);

        int xpForRank = totalXP;
        int level = totalXP / 200 + 1;
        double pct = (double) completedChallenges / CHALLENGE_COUNT;
        ProgressBar xpBar = new ProgressBar(pct);
        xpBar.setPrefWidth(Double.MAX_VALUE);
        xpBar.setStyle("-fx-accent: #39FF14;");

        Label stats = new Label(String.format(
            "\uD83D\uDD30 Level %d  |  \u26A1 %d XP  |  \uD83C\uDFC6 %d/%d  |  Rank: %s",
            level, totalXP, completedChallenges, CHALLENGE_COUNT, getRankForXP(xpForRank)));
        stats.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        Label leaderLab = new Label(getLeaderboardText());
        leaderLab.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;");

        HBox filterRow = new HBox(8);
        Label filterLabel = new Label("\uD83D\uDD0D FILTER:");
        filterLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-weight: bold;");
        filterRow.getChildren().add(filterLabel);
        String[][] filters = {
            {"ALL", "\uD83C\uDF10 ALL", "#30363d", "#c9d1d9"},
            {"EASY", "\uD83D\uDFE2 EASY", "#1a3a2a", "#39FF14"},
            {"MEDIUM", "\uD83D\uDFE1 MEDIUM", "#3a2a1a", "#FFD700"},
            {"HARD", "\uD83D\uDD34 HARD", "#3a1a1a", "#f85149"}
        };
        for (String[] f : filters) {
            Button fb = new Button(f[1]);
            boolean active = diffFilter.equals(f[0]);
            fb.setStyle("-fx-background-color: " + (active ? f[3] : f[2]) + "; "
                + "-fx-text-fill: " + (active ? "#0d1117" : f[3]) + "; -fx-font-weight: bold; -fx-font-size: 12px;");
            fb.setOnAction(e -> {
                diffFilter = f[0];
                showLearningModule();
            });
            filterRow.getChildren().add(fb);
        }

        VBox challengesBox = new VBox(12);
        challengesBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 8;");

        int shown = 0;
        for (ChallengeData ch : getChallenges()) {
            if (!diffFilter.equals("ALL") && !ch.diff.equals(diffFilter)) continue;
            shown++;
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #161b22; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 6;");

            HBox header = new HBox(10);
            Label starsLab = new Label("\u2B50".repeat(ch.stars));
            starsLab.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px;");
            Label titleLab = new Label(ch.title);
            titleLab.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 15px; -fx-font-weight: bold;");
            Label xpLab = new Label("+" + ch.xp + " XP");
            xpLab.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label diffLab = new Label(ch.diff);
            String diffColor = switch (ch.diff) {
                case "EASY" -> "#39FF14";
                case "MEDIUM" -> "#FFD700";
                default -> "#f85149";
            };
            diffLab.setStyle("-fx-text-fill: " + diffColor + "; -fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-background-color: #0d1117; -fx-padding: 2 8 2 8; -fx-border-color: " + diffColor + "; -fx-border-radius: 4;");
            Label statusLab = new Label();
            statusLab.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

            boolean done = leaderboard.containsKey(operatorID + "_" + ch.id);
            if (done) {
                statusLab.setText("\u2705 DONE");
                statusLab.setStyle(statusLab.getStyle() + "; -fx-text-fill: #3fb950;");
            } else {
                statusLab.setText("\uD83D\uDD12 UNSOLVED");
                statusLab.setStyle(statusLab.getStyle() + "; -fx-text-fill: #f85149;");
            }

            header.getChildren().addAll(starsLab, titleLab, xpLab, diffLab, statusLab);

            Label descr = new Label(ch.descr);
            descr.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px; -fx-wrap-text: true;");
            descr.setMaxWidth(650);

            Label hint = new Label(ch.hint);
            hint.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-style: italic;");

            HBox answerRow = new HBox(8);
            TextField answerField = new TextField();
            answerField.setPromptText("Enter flag or answer...");
            answerField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-pref-width: 350;");
            answerField.setDisable(done);

            Button submitBtn = new Button(done ? "\u2705 DONE" : "\u26A1 SUBMIT");
            submitBtn.setStyle(done
                ? "-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;"
                : "-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-weight: bold;");
            submitBtn.setDisable(done);

            if (!done) {
                String fid = ch.id;
                int fxp = ch.xp;
                String fflag = ch.flag;
                long t0 = System.currentTimeMillis();
                submitBtn.setOnAction(e -> {
                    String guess = answerField.getText().trim();
                    if (guess.equalsIgnoreCase(fflag)) {
                        leaderboard.put(operatorID + "_" + fid, fxp);
                        totalXP += fxp;
                        completedChallenges++;
                        academy.onSolve(fid, fxp);
                        academy.recordSolveTime(System.currentTimeMillis() - t0);
                        computeBadges();
                        sendAuditLog("CTF_" + fid.toUpperCase(), "ACADEMY");
                        addLog("[CTF] +" + fxp + "XP \u2014 " + ch.title + " cracked!");
                        showLearningModule();
                    } else {
                        academy.recordAttempt(false);
                        answerField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                        addLog("[CTF] Wrong answer for " + ch.title + ". Try again.");
                    }
                });
            }

            answerRow.getChildren().addAll(answerField, submitBtn);

            Button simBtn = new Button("\uD83E\uDDEA SIMULATE");
            simBtn.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            String sid = ch.family;
            simBtn.setOnAction(ev -> showAlgorithmPlayground(sid));

            card.getChildren().addAll(header, descr, answerRow, hint, simBtn);
            challengesBox.getChildren().add(card);
        }

        // Reset progress button
        Button resetBtn = new Button("\uD83D\uDD04 RESET PROGRESS");
        resetBtn.setStyle("-fx-background-color: #da3633; -fx-text-fill: white; -fx-font-weight: bold;");
        resetBtn.setOnAction(e -> {
            leaderboard.clear();
            totalXP = 0;
            completedChallenges = 0;
            achievedBadges.clear();
            academy.resetAll();
            showLearningModule();
            addLog("[CTF] Academy progress reset.");
        });

        ScrollPane scroll = new ScrollPane(challengesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefHeight(550);

        Label shownLab = new Label("Showing " + shown + " of " + CHALLENGE_COUNT + " challenges \u2014 keep training, operator!");
        shownLab.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-style: italic;");

        main.getChildren().addAll(titleRow, stats, xpBar, filterRow, leaderLab, scroll, shownLab, resetBtn);
        setCenter(main);
    }

    // === ALGORITHM PLAYGROUND — INTERACTIVE CODE LAB ===

    private static String algoPseudo(String algoId) {
        return switch (algoId) {
            case "caesar" -> """
                # CAESAR CIPHER — Encryption
                def caesar_encrypt(text, shift):
                    result = ""
                    for char in text:
                        if char.isalpha():
                            idx = ord(char) - ord('A')
                            new_idx = (idx + shift) % 26
                            result += chr(new_idx + ord('A'))
                        else:
                            result += char
                    return result""";
            case "xor" -> """
                # XOR BITWISE — Single-byte XOR
                def xor_encrypt(data, key_byte):
                    result = []
                    for byte in data.encode():
                        xored = byte ^ key_byte
                        result.append(chr(xored))
                    return ''.join(result)
                # Also return hex: [hex(b) for b in xored]""";
            case "atbash" -> """
                # ATBASH CIPHER — Reverse Alphabet
                def atbash(text):
                    result = ""
                    for char in text:
                        if char.isalpha():
                            idx = ord(char) - ord('A')
                            mirrored = 25 - idx  # A<->Z, B<->Y ...
                            result += chr(mirrored + ord('A'))
                        else:
                            result += char
                    return result""";
            case "vigenere" -> """
                # VIGENERE CIPHER — Keyed Shift
                def vigenere_encrypt(text, key):
                    result = ""
                    key = key.upper()
                    for i, char in enumerate(text):
                        if char.isalpha():
                            p = ord(char) - ord('A')
                            k = ord(key[i % len(key)]) - ord('A')
                            c = (p + k) % 26
                            result += chr(c + ord('A'))
                        else:
                            result += char
                    return result""";
            case "binary" -> """
                # TEXT TO BINARY
                def to_binary(text):
                    for char in text:
                        val = ord(char)
                        bits = format(val, '08b')
                        print(f"{char} -> {bits}")""";
            case "base64" -> """
                # BASE64 ENCODING
                def base64_encode(data):
                    bytes = data.encode()
                    b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    b64 += "abcdefghijklmnopqrstuvwxyz0123456789+/"
                    # 3 bytes -> 4 six-bit values -> 4 b64 chars
                    for triple in chunks(bytes, 3):
                        # combine 24 bits, split into 4x6 bits
                        ...""";
            case "rot13" -> """
                # ROT13 = CAESAR WITH SHIFT 13
                def rot13(text):
                    return caesar(text, 13)
                # Encrypt twice = original (13+13=26)""";
            case "reverse" -> """
                # STRING REVERSAL
                def reverse(text):
                    return text[::-1]""";
            case "hex" -> """
                # TEXT TO HEX
                def to_hex(text):
                    return ''.join(format(ord(c), '02X') for c in text)""";
            case "ascii" -> """
                # TEXT TO ASCII CODES
                def to_ascii(text):
                    return [ord(c) for c in text]""";
            case "octal" -> """
                # TEXT TO OCTAL
                def to_octal(text):
                    return ' '.join(format(ord(c), 'o') for c in text)""";
            case "base32" -> """
                # BASE32 ENCODING (A-Z, 2-7)
                def base32_encode(data):
                    # 5 bits per char, uses alphabet
                    # A=0..Z=25, 2=26..7=31
                    ...""";
            case "morse" -> """
                # MORSE CODE
                MORSE = { 'A': '.-', 'B': '-...', ... }
                def to_morse(text):
                    return ' '.join(MORSE.get(c, '/') for c in text)""";
            case "affine" -> """
                # AFFINE CIPHER: c = (a*p + b) mod 26
                def affine_encrypt(text, a, b):
                    out = ""
                    for char in text:
                        if char.isalpha():
                            c = (a * (ord(char)-65) + b) % 26
                            out += chr(c + 65)
                    return out
                # Requires gcd(a,26) == 1 for decryption""";
            case "railfence" -> """
                # RAIL FENCE (ZIG-ZAG)
                def rail_fence(text, rails):
                    fence = [[] for _ in range(rails)]
                    r, d = 0, 1
                    for c in text:
                        fence[r].append(c)
                        r += d
                        if r == rails-1: d = -1
                        elif r == 0: d = 1
                    return ''.join(''.join(row) for row in fence)""";
            case "leet" -> """
                # LEET SPEAK
                LEET = {'A':'4','E':'3','I':'1','O':'0','S':'5','T':'7'}
                def to_leet(text):
                    return ''.join(LEET.get(c, c) for c in text.upper())""";
            case "bacon" -> """
                # BACONIAN CIPHER (5 a/b per letter)
                BACON = {'A':'aaaaa','B':'aaaab', ... }
                def bacon_encode(text):
                    return ' '.join(BACON.get(c) for c in text if c.isalpha())""";
            case "tripleagent" -> """
                # TRIPLE STACK: Vigenere -> Reverse -> ROT13
                def triple(text, key):
                    return rot13(reverse(vigenere(text, key)))""";
            default -> "# Select an algorithm to see its pseudocode.";
        };
    }

    private static String algoParamLabel(String algoId) {
        return switch (algoId) {
            case "caesar" -> "Shift (0-25)";
            case "xor" -> "Key char";
            case "vigenere" -> "Keyword";
            case "affine" -> "a,b (e.g. 5,8)";
            case "railfence" -> "Rails (2-9)";
            default -> "Parameter";
        };
    }

    private void showAlgorithmPlayground(String algoId) {
        academyActive = true;
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Button backBtn = new Button("\u2B05 BACK TO ACADEMY");
        backBtn.setStyle("-fx-background-color: #30363d; -fx-text-fill: white; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> showLearningModule());

        String algoTitle = switch (algoId) {
            case "caesar" -> "Caesar Cipher";
            case "xor" -> "XOR Bitwise";
            case "atbash" -> "Atbash Cipher";
            case "vigenere" -> "Vigenere Cipher";
            case "binary" -> "Binary Encoding";
            case "base64" -> "Base64 Encoding";
            case "rot13" -> "ROT13 Cipher";
            case "reverse" -> "String Reversal";
            case "hex" -> "Hex Encoding";
            case "ascii" -> "ASCII Encoding";
            case "octal" -> "Octal Encoding";
            case "base32" -> "Base32 Encoding";
            case "morse" -> "Morse Code";
            case "affine" -> "Affine Cipher";
            case "railfence" -> "Rail Fence Cipher";
            case "leet" -> "Leet Speak";
            case "bacon" -> "Baconian Cipher";
            case "tripleagent" -> "Triple-Stack Cipher";
            default -> "Algorithm";
        };

        Label title = new Label("\uD83E\uDDEA " + algoTitle + " \u2014 Step-by-Step Simulation");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea codeArea = new TextArea(algoPseudo(algoId));
        codeArea.setEditable(false);
        codeArea.setPrefRowCount(7);
        codeArea.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-border-color: #30363d;");

        HBox inputRow = new HBox(10);
        TextField inputField = new TextField("HELLO");
        inputField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-pref-width: 200;");
        inputField.setPromptText("Input text...");

        TextField paramField = new TextField("3");
        paramField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-pref-width: 100;");
        paramField.setPromptText(algoParamLabel(algoId));
        if (switch (algoId) {
                case "atbash", "binary", "base64", "rot13", "reverse", "hex", "ascii", "octal", "base32", "leet", "bacon" -> true;
                default -> false;
            }) {
            paramField.setVisible(false);
            paramField.setManaged(false);
        }

        Button runBtn = new Button("\u25B6 RUN SIMULATION");
        runBtn.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox stepsBox = new VBox(6);
        stepsBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label outputLabel = new Label("Output will appear here after running simulation...");
        outputLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        outputLabel.setWrapText(true);

        runBtn.setOnAction(e -> {
            String in = inputField.getText();
            String param = paramField.getText();
            stepsBox.getChildren().clear();
            outputLabel.setText("");
            if (!in.isEmpty()) simulateAlgo(algoId, in, param, stepsBox, outputLabel);
        });

        inputRow.getChildren().addAll(inputField, paramField, runBtn);

        VBox content = new VBox(12, backBtn, title, codeArea, inputRow, stepsBox, outputLabel);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(650);

        main.getChildren().add(scroll);
        setCenter(main);
    }

    private void simulateAlgo(String algoId, String input, String param, VBox stepsBox, Label outputLabel) {
        input = input.toUpperCase();
        switch (algoId) {
            case "caesar" -> simulateCaesar(input, param, stepsBox, outputLabel);
            case "xor" -> simulateXor(input, param, stepsBox, outputLabel);
            case "atbash" -> simulateAtbash(input, stepsBox, outputLabel);
            case "vigenere" -> simulateVigenere(input, param, stepsBox, outputLabel);
            case "binary" -> simulateBinary(input, stepsBox, outputLabel);
            case "base64" -> simulateBase64(input, stepsBox, outputLabel);
            case "rot13" -> simulateRot13(input, stepsBox, outputLabel);
            case "reverse" -> simulateReverse(input, stepsBox, outputLabel);
            case "hex" -> simulateHex(input, stepsBox, outputLabel);
            case "ascii" -> simulateAscii(input, stepsBox, outputLabel);
            case "octal" -> simulateOctal(input, stepsBox, outputLabel);
            case "base32" -> simulateBase32(input, stepsBox, outputLabel);
            case "morse" -> simulateMorse(input, stepsBox, outputLabel);
            case "affine" -> simulateAffine(input, param, stepsBox, outputLabel);
            case "railfence" -> simulateRailFence(input, param, stepsBox, outputLabel);
            case "leet" -> simulateLeet(input, stepsBox, outputLabel);
            case "bacon" -> simulateBacon(input, stepsBox, outputLabel);
            case "tripleagent" -> simulateTriple(input, param, stepsBox, outputLabel);
        }
    }

    private void addStep(VBox box, String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-family: 'Courier New'; -fx-wrap-text: true;");
        box.getChildren().add(lbl);
    }

    private void addSep(VBox box) {
        Label sep = new Label("\u2500".repeat(50));
        sep.setStyle("-fx-text-fill: #30363d; -fx-font-size: 10px;");
        box.getChildren().add(sep);
    }

    // ----- SIMULATIONS -----

    private void simulateCaesar(String input, String shiftStr, VBox box, Label out) {
        int shift;
        try { shift = Integer.parseInt(shiftStr.trim()) % 26; } catch (Exception e) { shift = 3; }
        addStep(box, "\u25B6 CAESAR SHIFT = " + shift, "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                int idx = c - 'A';
                int newIdx = (idx + shift) % 26;
                char enc = (char) (newIdx + 'A');
                addStep(box, String.format("  Step %d: '%c' (idx=%2d) \u2192 (idx+%d)%%26=%2d \u2192 '%c'",
                    i + 1, c, idx, shift, newIdx, enc), "#c9d1d9");
                result.append(enc);
            } else {
                addStep(box, String.format("  Step %d: '%c' (non-alpha, keep)", i + 1, c), "#8b949e");
                result.append(c);
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateXor(String input, String keyStr, VBox box, Label out) {
        if (keyStr.isEmpty()) keyStr = "K";
        byte keyByte = (byte) keyStr.toUpperCase().charAt(0);
        addStep(box, "\u25B6 XOR KEY = '" + (char) keyByte + "' (0x" + Integer.toHexString(keyByte).toUpperCase() + ")", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        StringBuilder hexOut = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            byte inByte = (byte) input.charAt(i);
            byte xored = (byte) (inByte ^ keyByte);
            addStep(box, String.format("  Byte %d: 0x%02X (%s) \u2295 0x%02X = 0x%02X (%s)",
                i + 1, inByte, binaryPad(inByte), keyByte & 0xFF, xored, binaryPad(xored)), "#c9d1d9");
            result.append((char) xored);
            hexOut.append(String.format("%02X", xored));
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT (text): " + result.toString() + "  |  (hex): 0x" + hexOut.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static String binaryPad(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    private void simulateAtbash(String input, VBox box, Label out) {
        addStep(box, "\u25B6 ATBASH MAPPING: A\u2194Z  B\u2194Y  C\u2194X ...", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                int idx = c - 'A';
                int mirrored = 25 - idx;
                char enc = (char) (mirrored + 'A');
                addStep(box, String.format("  Step %d: '%c' (pos %2d) \u2192 25-%2d=%2d \u2192 '%c'",
                    i + 1, c, idx, idx, mirrored, enc), "#c9d1d9");
                result.append(enc);
            } else {
                addStep(box, String.format("  Step %d: '%c' (non-alpha, keep)", i + 1, c), "#8b949e");
                result.append(c);
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateVigenere(String input, String key, VBox box, Label out) {
        if (key.isEmpty()) key = "KEY";
        key = key.toUpperCase();
        addStep(box, "\u25B6 VIGENERE KEY = \"" + key + "\" (repeating)", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                int p = c - 'A';
                int k = key.charAt(i % key.length()) - 'A';
                int cVal = (p + k) % 26;
                char enc = (char) (cVal + 'A');
                addStep(box, String.format("  Step %d: '%c'(p=%2d) + '%c'(k=%2d) = %2d %% 26 = %2d \u2192 '%c'",
                    i + 1, c, p, key.charAt(i % key.length()), k, p + k, cVal, enc), "#c9d1d9");
                result.append(enc);
            } else {
                addStep(box, String.format("  Step %d: '%c' (non-alpha, keep)", i + 1, c), "#8b949e");
                result.append(c);
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateBinary(String input, VBox box, Label out) {
        addStep(box, "\u25B6 TEXT TO BINARY CONVERSION", "#58a6ff");
        addSep(box);
        StringBuilder fullBinary = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int val = (int) c;
            String bin = String.format("%8s", Integer.toBinaryString(val)).replace(' ', '0');
            fullBinary.append(bin).append(" ");
            addStep(box, String.format("  '%c' \u2192 ASCII %3d \u2192 %s",
                c, val, bin), "#c9d1d9");
            addStep(box, String.format("           Bit weights: %s",
                formatBitWeights(bin)), "#8b949e");
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 FULL BINARY: " + fullBinary.toString().trim());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static String formatBitWeights(String bits) {
        int[] weights = {128, 64, 32, 16, 8, 4, 2, 1};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                if (!sb.isEmpty()) sb.append(" + ");
                sb.append(weights[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : "0";
    }

    private void simulateBase64(String input, VBox box, Label out) {
        addStep(box, "\u25B6 BASE64 ENCODING — 3 bytes \u2192 4 Base64 chars", "#58a6ff");
        addSep(box);
        byte[] bytes = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String b64table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder fullB64 = new StringBuilder();

        addStep(box, "  Input bytes: " + bytes.length + " (hex: " + toHex(bytes) + ")", "#c9d1d9");
        addSep(box);

        int padding = (3 - bytes.length % 3) % 3;
        for (int i = 0; i < bytes.length; i += 3) {
            int triple = (bytes[i] & 0xFF) << 16;
            if (i + 1 < bytes.length) triple |= (bytes[i + 1] & 0xFF) << 8;
            if (i + 2 < bytes.length) triple |= (bytes[i + 2] & 0xFF);

            addStep(box, String.format("  Block %d: %02X %02X %02X \u2192 24-bit: %06X",
                i / 3 + 1,
                bytes[i] & 0xFF,
                i + 1 < bytes.length ? bytes[i + 1] & 0xFF : 0,
                i + 2 < bytes.length ? bytes[i + 2] & 0xFF : 0,
                triple), "#c9d1d9");

            for (int j = 0; j < 4; j++) {
                int sixBit = (triple >> (18 - j * 6)) & 0x3F;
                char b64Char = b64table.charAt(sixBit);
                boolean isPad = (j == 2 && padding >= 2) || (j == 3 && padding >= 1);
                addStep(box, String.format("    6-bit %d: %06d (%2d) \u2192 '%c'%s",
                    j + 1, sixBit, sixBit, isPad ? '=' : b64Char,
                    isPad ? " (padding)" : ""), isPad ? "#8b949e" : "#7ee787");
                fullB64.append(isPad ? '=' : b64Char);
            }
            if (i + 3 < bytes.length) addStep(box, "", "#c9d1d9"); // spacing
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 BASE64: " + fullB64.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateRot13(String input, VBox box, Label out) {
        addStep(box, "\u25B6 ROT13 — CAESAR SHIFT 13", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                char enc = (char) (((c - 'A' + 13) % 26) + 'A');
                addStep(box, String.format("  Step %d: '%c' \u2192 (idx+13)%%26 \u2192 '%c'", i + 1, c, enc), "#c9d1d9");
                result.append(enc);
            } else {
                addStep(box, String.format("  Step %d: '%c' (non-alpha, keep)", i + 1, c), "#8b949e");
                result.append(c);
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateReverse(String input, VBox box, Label out) {
        addStep(box, "\u25B6 STRING REVERSAL", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = input.length() - 1; i >= 0; i--) {
            addStep(box, String.format("  Step %d: take char[%d] = '%c'", input.length() - i, i, input.charAt(i)), "#c9d1d9");
            result.append(input.charAt(i));
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateHex(String input, VBox box, Label out) {
        addStep(box, "\u25B6 TEXT \u2192 HEX", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String h = String.format("%02X", (int) c);
            addStep(box, String.format("  Byte %d: '%c' (0x%02X) \u2192 %s", i + 1, c, (int) c, h), "#c9d1d9");
            result.append(h);
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateAscii(String input, VBox box, Label out) {
        addStep(box, "\u25B6 TEXT \u2192 ASCII CODES", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int code = (int) c;
            addStep(box, String.format("  Char %d: '%c' \u2192 %d", i + 1, c, code), "#c9d1d9");
            result.append(code).append(" ");
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: [" + result.toString().trim().replace(" ", ", ") + "]");
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateOctal(String input, VBox box, Label out) {
        addStep(box, "\u25B6 TEXT \u2192 OCTAL", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String o = Integer.toOctalString((int) c);
            addStep(box, String.format("  Char %d: '%c' (%d) \u2192 octal %s", i + 1, c, (int) c, o), "#c9d1d9");
            result.append(o).append(" ");
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString().trim());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateBase32(String input, VBox box, Label out) {
        addStep(box, "\u25B6 BASE32 ENCODING (A-Z + 2-7)", "#58a6ff");
        addSep(box);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        byte[] bytes = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        int buffer = 0, bits = 0;
        for (int i = 0; i < bytes.length; i++) {
            buffer = (buffer << 8) | (bytes[i] & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                int idx = (buffer >> bits) & 0x1F;
                result.append(alphabet.charAt(idx));
            }
        }
        if (bits > 0) result.append(alphabet.charAt((buffer << (5 - bits)) & 0x1F));
        while (result.length() % 8 != 0) result.append('=');
        addStep(box, "  Input bytes: " + bytes.length + " \u2192 grouped into 5-bit chunks", "#c9d1d9");
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateMorse(String input, VBox box, Label out) {
        addStep(box, "\u25B6 TEXT \u2192 MORSE CODE", "#58a6ff");
        addSep(box);
        java.util.Map<Character, String> morse = new java.util.HashMap<>();
        String[] mc = {".-","-...","-.-.","-..",".","..-.","--.","....","..",".---","-.-",".-..",
            "--","-.","---",".--.","--.-",".-.","...","-","..-","...-",".--","-..-","-.--","--.."};
        for (int i = 0; i < 26; i++) morse.put((char) ('A' + i), mc[i]);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (morse.containsKey(c)) {
                result.append(morse.get(c));
                if (i < input.length() - 1) result.append(" ");
                addStep(box, String.format("  Char %d: '%c' \u2192 %s", i + 1, c, morse.get(c)), "#c9d1d9");
            } else {
                result.append('/');
                addStep(box, String.format("  Char %d: '%c' \u2192 / (word separator)", i + 1, c), "#8b949e");
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateAffine(String input, String param, VBox box, Label out) {
        int a = 5, b = 8;
        try {
            String[] p = param.trim().split(",");
            a = Integer.parseInt(p[0].trim()) % 26;
            b = Integer.parseInt(p[1].trim()) % 26;
        } catch (Exception e) { a = 5; b = 8; }
        addStep(box, "\u25B6 AFFINE CIPHER — c = (a*p + b) mod 26  (a=" + a + ", b=" + b + ")", "#58a6ff");
        addSep(box);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                int idx = c - 'A';
                int enc = (a * idx + b) % 26;
                char e = (char) (enc + 'A');
                addStep(box, String.format("  Step %d: '%c'(%2d) \u2192 (%d*%2d+%d)%%26=%2d \u2192 '%c'",
                    i + 1, c, idx, a, idx, b, enc, e), "#c9d1d9");
                result.append(e);
            } else {
                addStep(box, String.format("  Step %d: '%c' (non-alpha, keep)", i + 1, c), "#8b949e");
                result.append(c);
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateRailFence(String input, String railsStr, VBox box, Label out) {
        int rails;
        try { rails = Math.max(2, Math.min(9, Integer.parseInt(railsStr.trim()))); } catch (Exception e) { rails = 3; }
        addStep(box, "\u25B6 RAIL FENCE — " + rails + " rails, zig-zag read", "#58a6ff");
        addSep(box);
        int n = input.length();
        char[][] fence = new char[rails][n];
        for (char[] row : fence) java.util.Arrays.fill(row, ' ');
        int r = 0, d = 1;
        for (int i = 0; i < n; i++) {
            fence[r][i] = input.charAt(i);
            r += d;
            if (r == rails - 1) d = -1;
            else if (r == 0) d = 1;
        }
        for (int rr = 0; rr < rails; rr++) {
            addStep(box, "  Rail " + (rr + 1) + ": " + new String(fence[rr]), "#7ee787");
        }
        StringBuilder result = new StringBuilder();
        for (char[] row : fence) for (char c : row) if (c != ' ') result.append(c);
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateLeet(String input, VBox box, Label out) {
        addStep(box, "\u25B6 LEET SPEAK", "#58a6ff");
        addSep(box);
        java.util.Map<Character, Character> leet = new java.util.HashMap<>();
        leet.put('A', '4'); leet.put('E', '3'); leet.put('I', '1'); leet.put('O', '0');
        leet.put('S', '5'); leet.put('T', '7'); leet.put('G', '9'); leet.put('B', '8');
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (leet.containsKey(c)) {
                result.append(leet.get(c));
                addStep(box, String.format("  Step %d: '%c' \u2192 '%c'", i + 1, c, leet.get(c)), "#c9d1d9");
            } else {
                result.append(c);
                addStep(box, String.format("  Step %d: '%c' (unchanged)", i + 1, c), "#8b949e");
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateBacon(String input, VBox box, Label out) {
        addStep(box, "\u25B6 BACONIAN CIPHER — 5 a/b per letter", "#58a6ff");
        addSep(box);
        String[] bacon = {"aaaaa","aaaab","aaaba","aaabb","aabaa","aabab","aabba","aabbb","abaaa","abaab",
            "ababa","ababb","abbaa","abbab","abbba","abbbb","baaaa","baaab","baaba","baabb","babaa","babab","babba","babbb","bbaaa","bbaab"};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                String code = bacon[c - 'A'];
                result.append(code).append(' ');
                addStep(box, String.format("  Char %d: '%c' \u2192 %s", i + 1, c, code), "#c9d1d9");
            } else {
                addStep(box, String.format("  Char %d: '%c' (skipped)", i + 1, c), "#8b949e");
            }
        }
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString().trim());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateTriple(String input, String keyStr, VBox box, Label out) {
        if (keyStr.isEmpty()) keyStr = "LOCKED";
        addStep(box, "\u25B6 TRIPLE STACK — Vigenere \u2192 Reverse \u2192 ROT13 (key=" + keyStr + ")", "#58a6ff");
        addSep(box);
        addStep(box, "  Layer 1 (Vigenere):", "#58a6ff");
        StringBuilder vig = new StringBuilder();
        int i = 0;
        for (char c : input.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                char k = keyStr.toUpperCase().charAt(i % keyStr.length());
                char e = (char) (((c - 'A' + (k - 'A')) % 26) + 'A');
                vig.append(e); i++;
            } else vig.append(c);
        }
        addStep(box, "    " + vig.toString(), "#c9d1d9");
        String rev = vig.reverse().toString();
        addStep(box, "  Layer 2 (Reverse): " + rev, "#58a6ff");
        StringBuilder result = new StringBuilder();
        for (char c : rev.toCharArray()) {
            if (c >= 'A' && c <= 'Z') result.append((char) (((c - 'A' + 13) % 26) + 'A'));
            else result.append(c);
        }
        addStep(box, "  Layer 3 (ROT13): " + result.toString(), "#58a6ff");
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + result.toString());
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }

    // --- FORENSIC HISTORY & ATLAS INTEGRATION ---

    private void showAuditHistory() {
        VBox main = new VBox(15);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Label title = new Label("SYSTEM FORENSIC HISTORY");
        title.setStyle(TITLE_STYLE);

        auditTable = new TableView<>();
        auditTable.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d;");
        
        TableColumn<JSONObject, String> colTime = new TableColumn<>("TIMESTAMP");
        colTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString("timestamp", "N/A")));
        colTime.setPrefWidth(180);

        TableColumn<JSONObject, String> colAction = new TableColumn<>("ACTION");
        colAction.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString(JSON_ACTION, "UNKNOWN")));
        colAction.setPrefWidth(200);

        TableColumn<JSONObject, String> colModule = new TableColumn<>("MODULE");
        colModule.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString(JSON_MODULE, "GENERAL")));
        colModule.setPrefWidth(150);

        auditTable.getColumns().add(colTime);
        auditTable.getColumns().add(colAction);
        auditTable.getColumns().add(colModule);
        auditTable.setPrefHeight(450);

        HBox controls = new HBox(15);
        Button btnRefresh = new Button("🔄 REFRESH FROM ATLAS");
        btnRefresh.setStyle(BTN_GREEN_BOLD);
        btnRefresh.setOnAction(e -> refreshAuditLogs());

        Button btnExport = new Button("📄 GENERATE REPORT");
        btnExport.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;");
        btnExport.setOnAction(e -> generateForensicReport());

        controls.getChildren().addAll(btnRefresh, btnExport);
        main.getChildren().addAll(title, auditTable, controls);
        setCenter(main);
        
        refreshAuditLogs();
    }

    private void generateForensicReport() {
        if (auditTable.getItems().isEmpty()) { addLog("[DENIED] No logs to export."); return; }
        
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Forensic_Report_" + operatorID + ".txt");
        File file = fc.showSaveDialog(null);

        if (file != null) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("**************************************************\n");
                sb.append("* UC-FORTRESS FORENSIC REPORT                    *\n");
                sb.append("**************************************************\n");
                sb.append("OPERATOR ID : ").append(operatorID).append("\n");
                sb.append("REPORT DATE : ").append(java.time.Instant.now().toString()).append("\n");
                sb.append("--------------------------------------------------\n\n");

                for (JSONObject entry : auditTable.getItems()) {
                    sb.append(String.format("[%s] -> %-18s | MOD: %s%n", 
                        entry.optString("timestamp"), entry.optString(JSON_ACTION), entry.optString(JSON_MODULE)));
                }
                
                sb.append("\n[END OF REPORT - INTEGRITY SEALED]");
                java.nio.file.Files.writeString(file.toPath(), sb.toString());
                addLog("[EXPORTED] Report saved successfully.");
                revealFileInExplorer(file);
            } catch (Exception e) { addLog("[ERROR] Export failed."); }
        }
    }

    private void refreshAuditLogs() {
        new Thread(() -> {
            try {
                addLog("[FETCH] Connecting to MongoDB Atlas...");
                String response = callSecurePythonGet("/get-audit-logs");
                JSONObject json = new JSONObject(response);
                if (json.has("logs")) {
                    Platform.runLater(() -> {
                        String logsJson = json.get("logs").toString();
                        JSONArray logsArray = new JSONArray(logsJson);
                        auditTable.getItems().clear();
                        for (int i = 0; i < logsArray.length(); i++) {
                            auditTable.getItems().add(logsArray.getJSONObject(i));
                        }
                        addLog("[SUCCESS] Forensic history synced.");
                    });
                }
            } catch (Exception e) { Platform.runLater(() -> addLog("[ERROR] Atlas Sync Fail.")); }
        }).start();
    }

    private String callSecurePythonGet(String ep) throws ApiException {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(PYTHON_URL + ep))
                    .header("X-API-KEY", API_SECRET_KEY).GET().build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new ApiException("Invalid API Key", 401);
            }
            return response.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("API request failed: " + ep, e);
        }
    }

    private String callNodeSecure(String ep, JSONObject p) throws ApiException {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(7)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(NODE_URL + ep))
                    .header("Content-Type", "application/json").header("X-API-KEY", API_SECRET_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(p.toString())).build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new ApiException("Invalid API Key", 401);
            }
            return response.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Node request failed: " + ep, e);
        }
    }

    private void handleSecureDispatch(String title) {
        TextInputDialog dialog = new TextInputDialog("recipient@example.com");
        dialog.setTitle(title);
        dialog.setHeaderText("Enter Recipient's Email Address:");
        dialog.showAndWait().ifPresent(email -> {
            try {
                JSONObject p = new JSONObject();
                p.put("to", email);
                p.put("content", outputArea.getText());
                callSecurePython("/send-secure-email", p);
                addLog("[DISPATCH] Packet sent to: " + email);
            } catch (Exception ex) { addLog("[ERROR] Dispatch failed."); }
        });
    }

    private void executeAES(boolean encrypt) {
        String inputData = inputArea.getText().trim();
        String securityKey = keyField.getText().trim();

        if (inputData.isEmpty() || securityKey.isEmpty()) {
            addLog("[DENIED] Key or Plaintext missing.");
            return;
        }

        new Thread(() -> executeAesTask(encrypt, inputData, securityKey)).start();
    }

    private void executeAesTask(boolean encrypt, String inputData, String securityKey) {
        try {
            Platform.runLater(() -> addLog(encrypt ? "[JAVA] Initiating XOR + AES Layering..." : "[JAVA] Reversing Hybrid Layers..."));

            String processed = encrypt ? XORUtil.encrypt(inputData, securityKey) : inputData;

            JSONObject p = new JSONObject();
            p.put("data", processed);
            p.put("key", securityKey);
            p.put("ts", java.time.Instant.now().getEpochSecond());

            String res = callSecurePython(encrypt ? "/encrypt" : "/decrypt", p);

            if (res != null && !res.isEmpty()) {
                JSONObject resJson = new JSONObject(res);
                String pythonResult = resJson.optString(JSON_RESULT, "");

                if (!pythonResult.isEmpty()) {
                    String finalOutput = encrypt ? pythonResult : XORUtil.decrypt(pythonResult, securityKey);
                    Platform.runLater(() -> {
                        outputArea.setText(finalOutput);
                        addLog("[SUCCESS] " + (encrypt ? "Encryption" : "Decryption") + " Finalized.");
                    });
                }
            }

            sendAuditLog(encrypt ? "AES_ENCRYPT" : "AES_DECRYPT", "AES_HYBRID");

        } catch (Exception e) {
            Platform.runLater(() -> {
                addLog("[CRITICAL] Server Delay: Render is still warming up.");
                outputArea.setText("🌐 SERVER WAKING UP: Please wait 10 seconds and try again.");
            });
        }
    }

    private void executePythonService(String endpoint, String logMsg) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> addLog(logMsg));

                JSONObject p = new JSONObject();
                p.put("data", inputArea.getText().trim());
                p.put("key", keyField.getText().trim());

                String res = callSecurePython(endpoint, p);
                Platform.runLater(() -> handleServiceResponse(res, endpoint));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    addLog("[ERROR] Bridge failed: " + ex.getMessage());
                    outputArea.setText("🌐 SERVER OFFLINE: Render is still waking up...");
                });
            }
        }).start();
    }

    private void handleServiceResponse(String res, String endpoint) {
        try {
            JSONObject responseJson = new JSONObject(res);
            String finalOutput = "";

            if (responseJson.has(JSON_RESULT)) {
                finalOutput = responseJson.getString(JSON_RESULT);
            } else if (responseJson.has("message")) {
                finalOutput = responseJson.getString("message");
            } else {
                finalOutput = res;
            }

            outputArea.setText(finalOutput);
            addLog("[SUCCESS] Gateway handshaking finalized.");
            sendAuditLog("PYTHON_SERVICE", endpoint);
        } catch (Exception e) {
            outputArea.setText(res);
            addLog("[INFO] RAW Output displayed.");
        }
    }

    private String callSecurePython(String ep, JSONObject p) throws ApiException {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(7)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(PYTHON_URL + ep))
                    .header("Content-Type", "application/json").header("X-API-KEY", API_SECRET_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(p.toString())).build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new ApiException("Invalid API Key", 401);
            }
            return response.body();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("API request failed: " + ep, e);
        }
    }

    private void handleSecureSync(String t) {
        new Thread(() -> {
            try {
                JSONObject p = new JSONObject(); 
                p.put("otp", mfaField.getText());
                p.put("data", outputArea.getText()); 
                
                String response = callSecurePython("/verify-otp", p);
                if (response.contains("success")) {
                    DatabaseManager.syncData(operatorID, t, outputArea.getText());
                    Platform.runLater(() -> { 
                        mfaStatusLabel.setText("VERIFIED & SYNCED"); 
                        mfaStatusLabel.setTextFill(javafx.scene.paint.Color.LIME); 
                        addLog("[CLOUD] Sync successful to MongoDB Atlas."); 
                    });
                    sendAuditLog("CLOUD_SYNC", t);
                } else {
                    Platform.runLater(() -> addLog("[DENIED] OTP Verification Failed."));
                }
            } catch (Exception e) { Platform.runLater(() -> addLog("[CRITICAL] Sync Fail. Check Backend.")); }
        }).start();
    }

    private void handleDigitalSignature() {
        new Thread(() -> {
            try {
                JSONObject p = new JSONObject(); p.put("data", outputArea.getText());
                String sig = new JSONObject(callSecurePython("/sign", p)).getString("signature");
                String preview = sig.length() > 30 ? sig.substring(0, 30).toUpperCase() + "..." : sig.toUpperCase();
                Platform.runLater(() -> {
                    signatureLabel.setText("RSA SEAL: " + preview);
                    signatureLabel.setStyle("-fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-padding: 10;");
                    addLog("[SIGNED] Integrity seal attached.");
                });
                sendAuditLog("RSA_SIGN", "INTEGRITY");
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Platform.runLater(() -> addLog("[ERROR] Signing Fail: " + msg));
            }
        }).start();
    }

    private void updateStrength(String k) {
        double s = k.length() / 16.0;
        strengthMeter.setProgress(Math.min(s, 1.0));
        strengthMeter.setStyle("-fx-accent: " + (s < 0.5 ? "red" : "#39FF14") + ";");
    }

    private void addLog(String m) {
        Platform.runLater(() -> {
            Label l = new Label("> " + m);
            l.setStyle("-fx-text-fill: #39FF14; -fx-font-family: 'Courier New'; -fx-font-size: 11px;");
            terminalLogs.getChildren().add(l);
        });
    }

    private void sendAuditLog(String action, String module) {
        new Thread(() -> {
            try {
                JSONObject auditEntry = new JSONObject();
                auditEntry.put("operator_id", this.operatorID); 
                auditEntry.put(JSON_ACTION, action);
                auditEntry.put(JSON_MODULE, module);
                callSecurePython("/audit-log", auditEntry);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown";
                log.warn("Audit link failed: {}", msg);
            }
        }).start();
    }

    private void revealFileInExplorer(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception ex) {
            log.warn("File reveal failed: {}", ex.getMessage());
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #010409; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.png"));
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(img);
            logoView.setFitWidth(180);
            logoView.setPreserveRatio(true);
            sidebar.getChildren().add(logoView);
        } catch (Exception e) {
            Label logoFallback = new Label("UC FORTRESS");
            logoFallback.setStyle("-fx-text-fill: #39FF14; -fx-font-weight: bold; -fx-font-size: 24px;");
            sidebar.getChildren().add(logoFallback);
        }

        sidebar.getChildren().add(new Separator());
        
        sidebar.getChildren().addAll(
            createMenuBtn("🛡️ AES HYBRID", e -> showAESModule()),
            createMenuBtn("📧 PGP ENCRYPT", e -> showPGPModule()),
            createMenuBtn("🔐 S/MIME SECURE", e -> showSMIMEModule()),
            createMenuBtn("🔑 RSA GATEWAY", e -> showRSAModule()),
            createMenuBtn("⚡ XOR BITWISE", e -> showXORModule()),
            createMenuBtn("🔑 PASSWORD GEN", e -> showPasswordGenerator()),
            createMenuBtn("🖼️ STEGANOGRAPHY", e -> showStegModule()),
            createMenuBtn("🔀 SHAMIR SPLIT", e -> showShamirModule()),
            createMenuBtn("🏛️ CAESAR LEGACY", e -> showCaesarModule()),
            createMenuBtn("🔍 FORENSIC AUDIT", e -> showIntegrityModule()),
            createMenuBtn("📊 VIEW HISTORY", e -> showAuditHistory()),
            new Separator(),
            createMenuBtn("🎓 ACADEMY", e -> showAcademyDashboard())
        );

        sidebar.getChildren().add(new Separator());

if (LoginScreen.USER_ROLE.equalsIgnoreCase("ADMIN")) {
        
        // Ongeza mstari wa kutenganisha (Separator)
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363d;");
        
        Button adminPanelBtn = new Button("🛠 ADMIN CONSOLE");
        adminPanelBtn.setMaxWidth(Double.MAX_VALUE);
        adminPanelBtn.setPrefHeight(40);
        // Rangi nyekundu ili ionekane ni sehemu ya hatari/nguvu (Power)
        adminPanelBtn.setStyle("-fx-background-color: #f85149; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        // Hapa ndipo unapofungua hiyo dashboard ya kuona users
        adminPanelBtn.setOnAction(e -> { academyActive = false; showAdminUserManagement(); }); 
        
        sidebar.getChildren().addAll(sep, adminPanelBtn);
    }

    return sidebar;
}

    // --- ADMIN MANAGEMENT METHOD ---
    
    /**
     * Hii ndio method inayokosekana ambayo inafungua muonekano wa Admin
     */
    private void showAdminUserManagement() {
        VBox adminView = new VBox(20);
        adminView.setPadding(new Insets(30));
        adminView.setStyle(BG_DARK);

        Label title = new Label("🛠️ SYSTEM ADMINISTRATION: USER DATABASE");
        title.setStyle("-fx-text-fill: #f85149; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        // Sehemu ya kuonyesha list ya watumiaji
        TextArea userDisplay = new TextArea();
        userDisplay.setEditable(false);
        userDisplay.setPrefHeight(450);
        userDisplay.setText("""
                --- [ UC-FORTRESS ADMIN CONSOLE ] ---
                FETCHING DATA FROM MONGODB ATLAS...
                
                """);
        userDisplay.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #39FF14;");

        Button btnRefresh = new Button("🔄 REFRESH USER LIST");
        btnRefresh.setStyle(BTN_GREEN_BOLD);
        
        // Hapa baadaye tutaweka kodi ya kuvuta data halisi
        btnRefresh.setOnAction(e -> userDisplay.appendText("[SCAN] User fetch initiated...\n[LOG] Access granted to: " + LoginScreen.USERNAME + "\n"));

        adminView.getChildren().addAll(title, btnRefresh, userDisplay);
        
        // Hii itabadilisha eneo la katikati la software yako kuwa muonekano wa Admin
        setCenter(adminView);
        
        // Log kwenye terminal yako ya pembeni
        addLog("[CRITICAL] Admin Dashboard Accessed.");
    }
    
// 1. Method ya kutuma OTP kwenye Email ya mteja
    private void handleSendEmailOTP() {
        addLog("[WAIT] Dispatching secure token to email...");
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", LoginScreen.USERNAME.isEmpty() ? operatorID : LoginScreen.USERNAME);

                callNodeSecure("/api/auth/send-otp", payload);
                
                Platform.runLater(() -> addLog("[SUCCESS] OTP sent! Check your inbox/spam folder."));
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[ERROR] Email Dispatcher failed: " + ex.getMessage()));
            }
        }).start();
    }

    // 2. Method ya kuhakiki kama OTP iliyoingizwa ni sahihi
    private void handleOTPVerification() {
        String enteredCode = mfaField.getText().trim();
        
        if (enteredCode.isEmpty()) {
            addLog("[DENIED] Security field is empty.");
            return;
        }

        addLog("[SCAN] Verifying identity token...");
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", LoginScreen.USERNAME.isEmpty() ? operatorID : LoginScreen.USERNAME);
                payload.put("otp", enteredCode);

                String responseStr = callNodeSecure("/api/auth/verify-otp", payload);
                JSONObject resJson = new JSONObject(responseStr);

                Platform.runLater(() -> {
                    if (resJson.optBoolean("success", false)) {
                        mfaStatusLabel.setText("GATE UNLOCKED");
                        mfaStatusLabel.setTextFill(Color.web("#39FF14"));
                        mfaField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #39FF14;");
                        addLog("[AUTH] ACCESS GRANTED. AES Engine Engaged.");
                        sendAuditLog("MFA_SUCCESS", "AUTH_GATE");
                    } else {
                        addLog("[DENIED] Verification failed. Token mismatch.");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[ERROR] Auth server timeout: " + ex.getMessage()));
            }
        }).start();
    }
    
    private Button createMenuBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-alignment: center-left; -fx-cursor: hand;");
        b.setOnAction(e -> {
            boolean isAcademy = text.contains("ACADEMY");
            if (academyActive && !isAcademy) flushPracticeTime();
            academyActive = isAcademy;
            if (!isAcademy) recordModuleUseFromMenu(text);
            event.handle(e);
        });
        return b;
    }

    private void recordModuleUseFromMenu(String text) {
        String mod = null;
        if (text.contains("AES")) mod = "AES";
        else if (text.contains("RSA")) mod = "RSA";
        else if (text.contains("XOR")) mod = "XOR";
        else if (text.contains("STEGANOGRAPHY")) mod = "STEGO";
        else if (text.contains("FORENSIC AUDIT")) mod = "HASH";
        if (mod != null) {
            academy.recordModuleUse(mod);
            computeBadges();
        }
    }
}