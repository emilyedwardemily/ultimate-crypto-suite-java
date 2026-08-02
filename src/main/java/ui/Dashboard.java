package ui;

import java.awt.Desktop;
import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

import org.json.JSONArray;
// --- JSON & COLLECTIONS IMPORTS ---
import org.json.JSONObject;

import academy.AcademyService;
import academy.AcademyService.GlobalPosition;
import academy.AcademyService.Standing;
import academy.AcademyUi;
import academy.CertificateGenerator;
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
    private static final int CHALLENGE_COUNT = 310;
    private static final java.util.Map<String, Integer> leaderboard = new java.util.HashMap<>();
    private static final java.util.Set<String> achievedBadges = new java.util.LinkedHashSet<>();
    private static String diffFilter = "ALL";

    // --- FORTRESS ACADEMY v2 (progress service + navigation tracking) ---
    private final AcademyService academy;
    private boolean academyActive = false;
    private int practiceTick = 0;
    private AnimationTimer academyTimer;
    private AnimationTimer missionClock;
    private AnimationTimer challengeClock;
    private String leaderTab = "global";
    private boolean suppressBadgeAnimations = true;
    private final java.util.ArrayDeque<String> badgeAnimQueue = new java.util.ArrayDeque<>();
    private boolean badgeAnimActive = false;

    // --- MULTIPLAYER (item 17) ---
    private int pvpRounds = 3;
    private int pvpRound;
    private int playerScore;
    private int oppScore;
    private Challenge pvpChallenge;
    private String pvpOpponent = "HexHacker";
    private String pvpRoom = "QUICK MATCH";
    private AnimationTimer pvpTimer;
    private int pvpSecondsLeft;
    private Label pvpClockLab;
    private Label pvpScoreLab;
    private TextField pvpAnswer;
    private VBox pvpStepsBox;
    private boolean pvpAnswered;
    private boolean pvpActive;

    // --- TOURNAMENTS (item 18) ---
    private String cupType;
    private int cupRounds;
    private int cupRound;
    private int cupScore;
    private Challenge cupChallenge;
    private AnimationTimer cupTimer;
    private int cupSecondsLeft;
    private boolean cupActive;

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
        ALL_BADGES.put("cup_winner",      new String[]{"\uD83C\uDFC6", "Cup Winner"});
        ALL_BADGES.put("season_champion", new String[]{"\uD83D\uDC51", "Season Champion"});
        ALL_BADGES.put("pvp_veteran",     new String[]{"\uD83C\uDF96\uFE0F", "PvP Veteran"});
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
            new ChallengeData("expert_1", "Polybius Lattice", 5, 480, "EXPERT", "expert",
                "Polybius coordinates: 35 31 44 33 34 44 | 12 43 42 45 44 15", "5x5 grid, rows/cols 1-5. I/J share a cell.", "UC{POLYBIUS}"),
            new ChallengeData("expert_2", "Playfair Digraph", 5, 480, "EXPERT", "expert",
                "Playfair pair KC EQ reveals the plaintext digraph CO.", "Build a 5x5 key square and decrypt pair by pair.", "UC{PLAYFAIR}"),
            new ChallengeData("expert_3", "Bifid Shuffle", 5, 480, "EXPERT", "expert",
                "Bifid coords: 31 24 13 55 22 14 41 35 12 44 51 23", "Write row/col coords, read down the columns, then map back.", "UC{BIFID}"),
            new ChallengeData("expert_4", "Columnar Transposition", 5, 480, "EXPERT", "expert",
                "Key=ELITE. Cipher: LIOEEDTKSROVA", "Write the message in columns under the key, read columns in key order.", "UC{TRANSPOSE}"),
            new ChallengeData("expert_5", "Running Key", 5, 480, "EXPERT", "expert",
                "Running key text: FORTRESSDEFENSE. Cipher: RODERQN", "Key stream comes from the key text, not a repeating word.", "UC{RUNNING}"),
            new ChallengeData("expert_6", "Porta Cipher", 5, 480, "EXPERT", "expert",
                "Porta key=CAPTAIN. Cipher: EMMKTRW", "Key maps plaintext into mirrored reciprocal alphabets.", "UC{PORTA}"),
            new ChallengeData("expert_7", "Nihilist Square", 5, 480, "EXPERT", "expert",
                "Nihilist sums: 141 158 186 144 173 152", "Add plaintext coordinates to key coordinates.", "UC{NIHILIST}"),
            new ChallengeData("expert_8", "Baconian Grid", 5, 480, "EXPERT", "expert",
                "Bacon sequence: abbba bbaab ababb aabbb baaab babba", "5 a/b symbols per letter (a=0, b=1) in binary order.", "UC{BACONIAN}"),
            new ChallengeData("expert_9", "Rail Six Deep", 5, 480, "EXPERT", "expert",
                "Rail fence (6 rails): CIFEPERALHIKS", "Zig-zag down and up across six rails.", "UC{RAILSIX}"),
            new ChallengeData("expert_10", "Myszkowski Shift", 5, 480, "EXPERT", "expert",
                "Myszkowski key=SECURE. Cipher: EAORLSPTK", "Columnar variant where duplicate key letters share a column.", "UC{MYSZKOWSKI}"),
            new ChallengeData("expert_11", "Autokey Vault", 5, 480, "EXPERT", "expert",
                "Autokey key=KEY, cipher: RHENOTUX", "After the keyword, the plaintext itself extends the key.", "UC{AUTOKEY}"),
            new ChallengeData("expert_12", "Four-Square", 5, 480, "EXPERT", "expert",
                "Four-square digraph pair: HL MR RG OP", "Four 5x5 squares encrypt digraphs with two keys.", "UC{FOURSQUARE}"),
            new ChallengeData("nightmare_1", "Triple DES Vault", 6, 650, "NIGHTMARE", "nightmare",
                "Vault: base64 of XOR(rot13(reverse(msg)), KEY=DARK)", "Peel the stack: base64, then XOR, then ROT13, then reverse.", "UC{TRIPLEDES}"),
            new ChallengeData("nightmare_2", "Vernam Stream", 6, 650, "NIGHTMARE", "nightmare",
                "Vernam pad: msg XOR pad, pad=GHOSTLY. Cipher: VTMPDYR", "One-time pad with a printed pad. XOR each byte with the pad.", "UC{VERNAM}"),
            new ChallengeData("nightmare_3", "Double Transposition", 6, 650, "NIGHTMARE", "nightmare",
                "Twice columnar: key1=GRID, key2=SNAKE. Cipher: OTEPRSCSA", "Transpose, then transpose again with a second key.", "UC{DOUBLE}"),
            new ChallengeData("nightmare_4", "ChaCha Core", 6, 650, "NIGHTMARE", "nightmare",
                "Core round: the quarter-round diffuses the state matrix", "Follow the ARX rounds: add, rotate, XOR.", "UC{CHACHA}"),
            new ChallengeData("nightmare_5", "RSA Trapdoor", 6, 650, "NIGHTMARE", "nightmare",
                "RSA n=77, e=7. Cipher: 46 25 49", "Factor n into p and q, derive the private key, decrypt each block.", "UC{RSATRAP}"),
            new ChallengeData("nightmare_6", "Feistel Forge", 6, 650, "NIGHTMARE", "nightmare",
                "Feistel round: split L/R, XOR with f(R,key), swap", "Eight rounds with alternating subkeys.", "UC{FEISTEL}"),
            new ChallengeData("nightmare_7", "AES S-Box Labyrinth", 6, 650, "NIGHTMARE", "nightmare",
                "S-box byte 0x53 maps through the GF(2^8) inverse", "Inverse the multiplicative group, then apply the affine transform.", "UC{SBOX}"),
            new ChallengeData("nightmare_8", "Salted Rotor Stack", 6, 650, "NIGHTMARE", "nightmare",
                "Enigma-style rotors I-IV with salt TANZ", "Rotors step and reflect; set ring positions from the salt.", "UC{ROTOR}"),
            new ChallengeData("impossible_1", "One-Time Pad Matrix", 7, 900, "IMPOSSIBLE", "impossible",
                "OTP matrix pad: 89 47 12 63 31 | msg bytes XOR pad bytes", "Perfect secrecy needs a truly random pad used once.", "UC{OTPMATRIX}"),
            new ChallengeData("impossible_2", "Quantum Key Distribution", 7, 900, "IMPOSSIBLE", "impossible",
                "BB84: 11010 10110 01100 | basis: Z X Z X X Z X Z X X", "Compare measurement bases publicly, keep matching bits.", "UC{QUANTUM}"),
            new ChallengeData("impossible_3", "Chaos Entropy Core", 7, 900, "IMPOSSIBLE", "impossible",
                "Logistic map x(n+1)=r*x(n)*(1-x(n)), r=3.9, seed=0.4000", "Iterate the chaotic map to derive the key stream.", "UC{CHAOS}"),
            new ChallengeData("impossible_4", "Infinite Key Labyrinth", 7, 900, "IMPOSSIBLE", "impossible",
                "Key stream generated by a non-repeating cellular automaton", "Rule 30 on a ring, each generation extends the pad.", "UC{LABYRINTH}"),
            new ChallengeData("impossible_5", "Homomorphic Vault", 7, 900, "IMPOSSIBLE", "impossible",
                "Encrypted values: [enc(7), enc(3), enc(5)] sum to enc(15)", "Compute on ciphertext without ever decrypting.", "UC{HOMOMORPHIC}"),
            new ChallengeData("impossible_6", "AI Adversarial Enigma", 7, 900, "IMPOSSIBLE", "impossible",
                "The model keeps rewriting its own rotors mid-encryption", "Track the evolving key schedule — no single static key exists.", "UC{ADVERSARIAL}"),
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
        if (academy.getTournamentWins() >= 1)   achievedBadges.add("cup_winner");
        if (academy.getTournamentWins() >= 3)   achievedBadges.add("season_champion");
        if (academy.getPvpWins() >= 3)          achievedBadges.add("pvp_veteran");

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
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ACADEMY", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83D\uDC64 OPERATOR PROFILE", AcademyUi.GREEN, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.GREEN, 0.3));
        main.getChildren().addAll(backBtn, title);

        // ----- Identity header -----
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18));
        header.setStyle("-fx-background-color: #161b22; -fx-background-radius: 14; -fx-border-color: #39FF14; -fx-border-radius: 14;");

        Label avatar = new Label(academy.getAvatar());
        avatar.setStyle("-fx-font-size: 56px;");
        Button changeAv = AcademyUi.button("\uD83D\uDD04", "#30363d", AcademyUi.LIGHT);
        changeAv.setOnAction(e -> {
            String[] av = AcademyService.avatars();
            int idx = java.util.Arrays.asList(av).indexOf(academy.getAvatar());
            academy.setAvatar(av[(idx + 1) % av.length]);
            showProfile();
        });
        VBox avBox = new VBox(6, avatar, changeAv);
        avBox.setAlignment(Pos.CENTER);

        VBox idBox = new VBox(6);
        idBox.setAlignment(Pos.CENTER_LEFT);
        Label op = AcademyUi.neon(operatorID, AcademyUi.GREEN, 20);
        int lvl = totalXP / 200 + 1;
        int careerIdx = academy.earnedCareerRank();
        String[] career = AcademyService.CAREER_RANKS[careerIdx];
        Label rankLine = AcademyUi.text("Level " + lvl + "  \u2022  " + getRankForXP(totalXP) + "  \u2022  "
            + career[2] + " " + career[1], 13);
        Label locLine = AcademyUi.caption(countryFlag(academy.getCountry()) + " " + academy.getCountry()
            + (academy.getUniversity().isEmpty() ? "" : "  \u2022  \uD83C\uDF93 " + academy.getUniversity()), 12);
        idBox.getChildren().addAll(op, rankLine, locLine);

        Region sp1 = AcademyUi.spacer();
        HBox quickStats = new HBox(14);
        quickStats.getChildren().add(AcademyUi.statTile("\u26A1", totalXP + "", "TOTAL XP", AcademyUi.GOLD));
        quickStats.getChildren().add(AcademyUi.statTile("\uD83E\uDE99", academy.getCoins() + "", "COINS", AcademyUi.GREEN));
        quickStats.getChildren().add(AcademyUi.statTile("\uD83C\uDFC6", completedChallenges + "", "SOLVED", AcademyUi.BLUE));
        quickStats.getChildren().add(AcademyUi.statTile("\uD83C\uDF96\uFE0F", academy.getCertPoints() + "", "CERT PTS", AcademyUi.PURPLE));
        header.getChildren().addAll(avBox, idBox, sp1, quickStats);
        main.getChildren().add(header);

        // ----- Editable bio / country / university -----
        VBox editCard = AcademyUi.cardAccent(AcademyUi.BLUE);
        editCard.getChildren().add(AcademyUi.section("\u270F\uFE0F PROFILE DETAILS", AcademyUi.BLUE));
        HBox bioRow = new HBox(8);
        bioRow.setAlignment(Pos.CENTER_LEFT);
        TextField bioField = new TextField(academy.getBio());
        bioField.setPrefWidth(460);
        bioField.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");
        Button bioSave = AcademyUi.button("SAVE BIO", "#238636", "#ffffff");
        bioSave.setOnAction(e -> {
            academy.setBio(bioField.getText().trim().isEmpty() ? "Cryptographic operator in training." : bioField.getText().trim());
            addLog("[PROFILE] Bio updated.");
        });
        bioRow.getChildren().addAll(new Label("Bio: "), bioField, bioSave);

        ComboBox<String> countryBox = new ComboBox<>();
        countryBox.getItems().addAll(java.util.Arrays.asList(AcademyService.countries()));
        countryBox.setValue(academy.getCountry());
        countryBox.setOnAction(e -> {
            if (countryBox.getValue() != null) { academy.setCountry(countryBox.getValue()); addLog("[PROFILE] Country set to " + countryBox.getValue() + "."); }
        });
        ComboBox<String> uniBox = new ComboBox<>();
        uniBox.getItems().addAll(java.util.Arrays.asList(AcademyService.universities()));
        uniBox.getItems().add(0, "None");
        uniBox.setValue(academy.getUniversity().isEmpty() ? "None" : academy.getUniversity());
        uniBox.setOnAction(e -> {
            if (uniBox.getValue() != null) {
                academy.setUniversity(uniBox.getValue().equals("None") ? "" : uniBox.getValue());
                addLog("[PROFILE] University set to " + uniBox.getValue() + ".");
            }
        });
        HBox pickRow = new HBox(14, new Label("\uD83C\uDF0D Country: "), countryBox, new Label("\uD83C\uDF93 University: "), uniBox);
        pickRow.setAlignment(Pos.CENTER_LEFT);
        editCard.getChildren().addAll(bioRow, pickRow);
        main.getChildren().add(editCard);

        // ----- Career progress mini-card -----
        VBox careerCard = AcademyUi.cardAccent(AcademyUi.GOLD);
        careerCard.getChildren().add(AcademyUi.section("\uD83C\uDFC6 CAREER", AcademyUi.GOLD));
        int nextIdx = careerIdx + 1;
        if (nextIdx < AcademyService.CAREER_RANKS.length) {
            int curXp = Integer.parseInt(career[0]);
            int nextXp = Integer.parseInt(AcademyService.CAREER_RANKS[nextIdx][0]);
            ProgressBar cBar = new ProgressBar(Math.min(1.0, (double) (totalXP - curXp) / Math.max(1, nextXp - curXp)));
            cBar.setPrefWidth(Double.MAX_VALUE);
            cBar.setStyle("-fx-accent: #FFD700;");
            careerCard.getChildren().addAll(cBar,
                AcademyUi.caption("Next rank: " + AcademyService.CAREER_RANKS[nextIdx][1]
                    + "  \u2022  +" + Math.max(0, nextXp - totalXP) + " XP", 12));
        } else {
            careerCard.getChildren().add(AcademyUi.text("MAX RANK \u2014 Cyber Legend.", 12));
        }
        Button careerBtn = AcademyUi.button("\uD83C\uDFC6 OPEN CAREER MODE", "#1f6feb", "#ffffff");
        careerBtn.setOnAction(e -> showCareerMode());
        careerCard.getChildren().add(careerBtn);
        main.getChildren().add(careerCard);

        // ----- Statistics -----
        VBox statsCard = AcademyUi.cardAccent(AcademyUi.PURPLE);
        statsCard.getChildren().add(AcademyUi.section("\uD83D\uDCCA STATISTICS", AcademyUi.PURPLE));
        int rankPos = 1;
        java.util.List<java.util.Map.Entry<String, Integer>> rankings = getGlobalRankings();
        for (java.util.Map.Entry<String, Integer> e : rankings) {
            if (e.getKey().equals(operatorID)) break;
            rankPos++;
        }
        String[][] sLines = {
            {"\uD83C\uDFC6", String.valueOf(completedChallenges) + "/" + CHALLENGE_COUNT, "CHALLENGES SOLVED"},
            {"\uD83C\uDFAF", String.format("%.0f%%", academy.getSuccessRate()), "SUCCESS RATE"},
            {"\uD83D\uDD25", String.valueOf(academy.getStreak()), "CURRENT STREAK"},
            {"\uD83C\uDFC6", "#" + rankPos + "/" + rankings.size(), "LEADERBOARD"},
            {"\u23F1\uFE0F", String.format("%.1f h", academy.getPracticeHours()), "PRACTICE TIME"},
            {"\uD83C\uDFC6", String.valueOf(academy.getPvpWins()), "PVP WINS"},
            {"\uD83C\uDFC1", String.valueOf(academy.getTournamentWins()), "CUPS WON"}
        };
        GridPane statGrid = new GridPane();
        statGrid.setHgap(12);
        statGrid.setVgap(12);
        int sc = 0, sr = 0;
        for (String[] s : sLines) {
            VBox tile = AcademyUi.statTile(s[0], s[1], s[2], AcademyUi.PURPLE);
            tile.setPrefWidth(150);
            statGrid.add(tile, sc, sr);
            sc++;
            if (sc >= 4) { sc = 0; sr++; }
        }
        statsCard.getChildren().add(statGrid);
        main.getChildren().add(statsCard);

        // ----- XP history + activity chart -----
        HBox chartsRow = new HBox(16);
        VBox xpCard = AcademyUi.cardAccent(AcademyUi.GOLD);
        xpCard.getChildren().add(AcademyUi.section("\uD83D\uDCC8 XP HISTORY (7 DAYS)", AcademyUi.GOLD));
        javafx.scene.canvas.Canvas xpChart = new javafx.scene.canvas.Canvas(360, 130);
        drawXpChart(xpChart);
        xpCard.getChildren().add(xpChart);
        chartsRow.getChildren().add(xpCard);

        VBox actCard = AcademyUi.cardAccent(AcademyUi.BLUE);
        actCard.getChildren().add(AcademyUi.section("\uD83D\uDD59 RECENT ACTIVITY", AcademyUi.BLUE));
        java.util.List<String> acts = academy.getActivity();
        if (acts.isEmpty()) {
            actCard.getChildren().add(AcademyUi.caption("No activity yet \u2014 crack a cipher to light up the feed.", 12));
        } else {
            int shown = 0;
            for (String a : acts) {
                if (shown++ >= 8) break;
                Label l = new Label("\u25B8 " + a);
                l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
                actCard.getChildren().add(l);
            }
        }
        chartsRow.getChildren().add(actCard);
        main.getChildren().add(chartsRow);

        // ----- Skills -----
        VBox skills = AcademyUi.cardAccent(AcademyUi.GREEN);
        skills.getChildren().add(AcademyUi.section("\uD83D\uDCA0 SKILLS", AcademyUi.GREEN));
        String[] fams = {"caesar", "aes", "rsa", "xor", "hash", "stego", "vigenere", "playfair", "hill", "transposition"};
        GridPane skillGrid = new GridPane();
        skillGrid.setHgap(14);
        skillGrid.setVgap(8);
        for (int i = 0; i < fams.length; i++) {
            int prof = academy.proficiency(fams[i]);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label fn = new Label(fams[i].toUpperCase());
            fn.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-pref-width: 110;");
            ProgressBar pb = new ProgressBar(prof / 100.0);
            pb.setPrefWidth(120);
            pb.setStyle("-fx-accent: " + (prof >= 70 ? "#39FF14" : prof >= 40 ? "#FFD700" : "#f85149") + ";");
            Label pv = new Label(prof + "%");
            pv.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px;");
            row.getChildren().addAll(fn, pb, pv);
            skillGrid.add(row, i % 2, i / 2);
        }
        skills.getChildren().add(skillGrid);
        main.getChildren().add(skills);

        // ----- Badges -----
        VBox badgeCard = AcademyUi.cardAccent(AcademyUi.PURPLE);
        badgeCard.getChildren().add(AcademyUi.section("\uD83C\uDFC6 BADGES (" + achievedBadges.size() + "/" + ALL_BADGES.size() + ")", AcademyUi.PURPLE));
        GridPane badgeGrid = new GridPane();
        badgeGrid.setHgap(10);
        badgeGrid.setVgap(10);
        int col = 0, brow = 0;
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
            badgeGrid.add(cell, col, brow);
            col++;
            if (col >= 3) { col = 0; brow++; }
        }
        badgeCard.getChildren().add(badgeGrid);
        main.getChildren().add(badgeCard);

        // ----- Rank history -----
        VBox rankHistCard = AcademyUi.cardAccent(AcademyUi.GOLD);
        rankHistCard.getChildren().add(AcademyUi.section("\uD83D\uDCC5 RANK HISTORY", AcademyUi.GOLD));
        java.util.List<String> rh = academy.getRankHistory();
        if (rh.isEmpty()) {
            rankHistCard.getChildren().add(AcademyUi.caption("No promotions yet \u2014 climb the ladder in CAREER MODE.", 12));
        } else {
            for (String h : rh) {
                String[] parts = h.split("\\|");
                Label l = new Label("\u25B8 " + (parts.length == 2 ? parts[0] + "  (" + parts[1] + ")" : h));
                l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
                rankHistCard.getChildren().add(l);
            }
        }
        main.getChildren().add(rankHistCard);

        // ----- Recent challenges -----
        VBox recent = AcademyUi.cardAccent(AcademyUi.BLUE);
        recent.getChildren().add(AcademyUi.section("\uD83D\uDCDC RECENT CHALLENGES", AcademyUi.BLUE));
        java.util.List<String> solved = new java.util.ArrayList<>(academy.getSolved().keySet());
        if (solved.isEmpty()) {
            recent.getChildren().add(AcademyUi.caption("No challenges solved yet.", 12));
        } else {
            int shown = 0;
            for (int i = solved.size() - 1; i >= 0 && shown < 8; i--, shown++) {
                Label l = new Label("\uD83D\uDDF2\uFE0F " + solved.get(i));
                l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
                recent.getChildren().add(l);
            }
        }
        main.getChildren().add(recent);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    /** Draws a 7-day XP bar chart into the given canvas. */
    private void drawXpChart(javafx.scene.canvas.Canvas cv) {
        javafx.scene.canvas.GraphicsContext g = cv.getGraphicsContext2D();
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, cv.getWidth(), cv.getHeight());
        int[] vals = new int[7];
        java.time.LocalDate today = java.time.LocalDate.now();
        double max = 1;
        for (int i = 0; i < 7; i++) {
            String day = today.minusDays(6 - i).toString();
            int v = academy.getDailyXp(day);
            vals[i] = v;
            max = Math.max(max, v);
        }
        double slot = cv.getWidth() / 7.0;
        double bw = slot * 0.6;
        for (int i = 0; i < 7; i++) {
            double h = (vals[i] / max) * 100;
            g.setFill(Color.web("#FFD700"));
            g.fillRoundRect(i * slot + (slot - bw) / 2, 118 - h, bw, h, 4, 4);
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 9));
            g.fillText(today.minusDays(6 - i).getDayOfMonth() + "", i * slot + (slot - bw) / 2, 128);
        }
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
            case "EASY"      -> AcademyUi.GREEN;
            case "MEDIUM"    -> AcademyUi.GOLD;
            case "HARD"      -> AcademyUi.RED;
            case "EXPERT"    -> "#a371f7";
            case "NIGHTMARE" -> "#00d4ff";
            case "IMPOSSIBLE" -> "#ff4fd8";
            default          -> AcademyUi.LIGHT;
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
        stopChallengeClock();
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

    private void generateAndRefresh(String difficulty, String family) {
        Challenge ch = academy.generateChallenge(difficulty, family);
        addLog("[ACADEMY] Generated " + difficulty + " " + AcademyService.familyName(ch.family)
            + " challenge \u2014 " + ch.title + " (+" + ch.xp + " XP).");
        showAcademyDashboard();
    }

    private javafx.scene.Node buildPracticeLab() {
        VBox box = new VBox(12);
        box.getChildren().add(AcademyUi.section("\u26A1 PRACTICE LAB \u2014 GENERATED CHALLENGES", AcademyUi.GREEN));

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label how = AcademyUi.caption("The AI vault summons fresh, verifiable ciphers on demand. Solve them for real XP.", 11);
        Region sp = AcademyUi.spacer();
        javafx.scene.control.ComboBox<String> famBox = new javafx.scene.control.ComboBox<>();
        famBox.getItems().addAll(AcademyService.GENERATOR_FAMILIES);
        famBox.setValue("random");
        famBox.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-font-size: 11px;");
        Button easyBtn = AcademyUi.button("\uD83D\uDFE2 EASY", "#238636", "#ffffff");
        Button medBtn = AcademyUi.button("\uD83D\uDFE1 MEDIUM", "#9e6a03", "#ffffff");
        Button hardBtn = AcademyUi.button("\uD83D\uDD34 HARD", "#b62324", "#ffffff");
        Button expBtn = AcademyUi.button("\uD83D\uDFE3 EXPERT", "#6e40c9", "#ffffff");
        Button ngtBtn = AcademyUi.button("\uD83D\uDD35 NIGHTMARE", "#0b7285", "#ffffff");
        Button impBtn = AcademyUi.button("\uD83D\uDC93 IMPOSSIBLE", "#a61e4d", "#ffffff");
        easyBtn.setOnAction(e -> generateAndRefresh("EASY", famBox.getValue()));
        medBtn.setOnAction(e -> generateAndRefresh("MEDIUM", famBox.getValue()));
        hardBtn.setOnAction(e -> generateAndRefresh("HARD", famBox.getValue()));
        expBtn.setOnAction(e -> generateAndRefresh("EXPERT", famBox.getValue()));
        ngtBtn.setOnAction(e -> generateAndRefresh("NIGHTMARE", famBox.getValue()));
        impBtn.setOnAction(e -> generateAndRefresh("IMPOSSIBLE", famBox.getValue()));
        controls.getChildren().addAll(how, sp, famBox, easyBtn, medBtn, hardBtn, expBtn, ngtBtn, impBtn);
        box.getChildren().add(controls);

        java.util.List<Challenge> gens = academy.getGenerated();
        if (gens.isEmpty()) {
            box.getChildren().add(AcademyUi.caption(
                "No generated challenges yet. Hit EASY / MEDIUM / HARD / EXPERT / NIGHTMARE / IMPOSSIBLE above to summon one.", 12));
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
                    academy.recordSolveTime(cid, System.currentTimeMillis() - t0);
                    computeBadges();
                    sendAuditLog("GEN_" + cid.toUpperCase(), "ACADEMY");
                    addLog("[ACADEMY] +" + cxp + "XP \u2014 generated cipher cracked!");
                    showAcademyDashboard();
                } else {
                    academy.recordAttempt(false);
                    academy.recordWrongFamily(ch.family);
                    answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                    addLog("[ACADEMY] Wrong answer for generated cipher. Try again.");
                }
            });
        }

        Button aiBtn = AcademyUi.button("\uD83E\uDD16 AI MENTOR", "#8957e5", "#ffffff");
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

        Label hintOut = AcademyUi.caption("", 11);
        HBox hintBar = buildHintBar(ch.flag, ch.family, ch.hint, answer, hintOut);

        long gfast = academy.getFastestMs(ch.id);
        long gavg = academy.getAvgMs(ch.id);
        Label gTime = AcademyUi.caption(String.format(
            "\u23F1 FASTEST %s   |   \uD83D\uDCCA AVG %s",
            gfast > 0 ? formatElapsed(gfast) : "\u2014",
            gavg > 0 ? formatElapsed(gavg) : "\u2014"), 11);

        Button simBtn = AcademyUi.button("\uD83E\uDDEA SIMULATE", "#30363d", AcademyUi.LIGHT);
        String simFam = ch.family;
        simBtn.setOnAction(e -> showAlgorithmPlayground(simFam));

        Button delBtn = AcademyUi.button("\uD83D\uDDD1\uFE0F", "#da3633", "#ffffff");
        delBtn.setOnAction(e -> { academy.removeGenerated(ch.id); showAcademyDashboard(); });

        row.getChildren().addAll(answer, submit, aiBtn, simBtn, delBtn);
        card.getChildren().addAll(top, descr, gTime, row, hintBar, hintOut, hintLab);
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

        Label courseLab = AcademyUi.neon("\uD83C\uDFE6 COURSE CERTIFICATES \u2014 one per track, granted at 80%+", AcademyUi.GOLD, 15);
        main.getChildren().add(courseLab);
        VBox courseBox = new VBox(10);
        courseBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 14; -fx-border-color: #30363d; -fx-border-radius: 8;");
        for (AcademyService.Category c : academy.getCategories()) {
            String status = academy.certificateStatus(c);
            HBox courseRow = new HBox(12);
            courseRow.setAlignment(Pos.CENTER_LEFT);
            Label cName = AcademyUi.text(c.name, 13);
            Label cScore = AcademyUi.caption(String.format("Score %.0f%%", c.completionPercent), 11);
            Label cStatus = AcademyUi.pill(status, status.equals("GRANTED") ? "#3fb950" : "#30363d");
            Button cBtn = AcademyUi.button(
                status.equals("GRANTED") ? "\uD83D\uDCC4 VIEW / DOWNLOAD" : "\uD83D\uDD12 LOCKED",
                status.equals("GRANTED") ? "#8957e5" : "#30363d", "#ffffff");
            cBtn.setDisable(!status.equals("GRANTED"));
            cBtn.setOnAction(e -> showCertificateDialog(c));
            Region sp = AcademyUi.spacer();
            courseRow.getChildren().addAll(cName, cScore, sp, cStatus, cBtn);
            courseBox.getChildren().add(courseRow);
        }
        main.getChildren().add(courseBox);

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
        exportCourseCertificatePdf(null, certName, certTitle);
    }

    /** Downloads a professional PDF certificate (verification id, QR, metadata). */
    private void exportCourseCertificatePdf(AcademyService.Category cat, String certName, String courseTitle) {
        int score = cat != null ? (int) Math.round(cat.completionPercent) : Math.min(100, completedChallenges);
        String course = courseTitle != null ? courseTitle : (cat != null ? cat.name : certName);
        String courseId = cat != null ? cat.id : ("course_" + course.replace(" ", "_").toLowerCase());
        String vid = academy.verificationId(courseId);
        String instructor = academy.instructorFor(courseId);
        String issueDate = java.time.LocalDate.now().toString();
        String status = cat != null ? academy.certificateStatus(cat) : "GRANTED";

        FileChooser fc = new FileChooser();
        fc.setInitialFileName("UC_Fortress_" + course.replace(" ", "_") + "_Certificate.pdf");
        File file = fc.showSaveDialog(null);
        if (file == null) return;
        try {
            CertificateGenerator.generate(course, operatorID, vid, score, instructor,
                issueDate, status, file);
            addLog("[ACADEMY] Certificate exported: " + file.getAbsolutePath());
        } catch (Exception ex) {
            addLog("[ERROR] Certificate export failed: " + ex.getMessage());
        }
    }

    // === CATEGORY PAGE (ITEM 8 + 9) — modern cards with live progress ===

    private void showCategoryPage() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83D\uDCC2 COURSE CATEGORIES", AcademyUi.GREEN, 22);
        Label sub = AcademyUi.caption(
            "Every track with live completion, difficulty, XP and estimated time. Pass 80% to unlock the certificate.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        int col = 0, row = 0;
        for (AcademyService.Category c : academy.getCategories()) {
            grid.add(buildCategoryCard(c), col, row);
            col++;
            if (col >= 3) { col = 0; row++; }
        }
        main.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildCategoryCard(AcademyService.Category c) {
        VBox card = c.completionPercent >= 80 ? AcademyUi.cardAccent(AcademyUi.GOLD) : AcademyUi.card();
        card.setPrefWidth(360);
        card.setPrefHeight(250);

        String icon = switch (c.id) {
            case "aes" -> "\uD83D\uDD12"; case "rsa" -> "\uD83D\uDD10"; case "ecc" -> "\uD83D\uDD0D";
            case "hashing" -> "\uD83D\uDD22"; case "encoding" -> "\uD83D\uDD1F"; case "steganography" -> "\uD83D\uDD0E";
            case "digital-signatures" -> "\u270D\uFE0F"; case "pgp" -> "\uD83D\uDCE7"; case "smime" -> "\uD83D\uDCE9";
            case "network-security" -> "\uD83D\uDD25"; case "web-security" -> "\uD83D\uDEA9"; case "reverse-engineering" -> "\uD83D\uDD2C";
            case "binary-exploitation" -> "\uD83D\uDCBE"; case "forensics" -> "\uD83D\uDDD1\uFE0F"; case "osint" -> "\uD83D\uDD0D";
            case "password-cracking" -> "\uD83D\uDD11"; case "malware-analysis" -> "\uD83E\uDEB5";
            default -> "\uD83D\uDD10";
        };

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Label iconLab = new Label(icon);
        iconLab.setStyle("-fx-font-size: 26px;");
        Label nameLab = AcademyUi.neon(c.name, c.completionPercent >= 80 ? AcademyUi.GOLD : AcademyUi.GREEN, 15);
        Region sp = AcademyUi.spacer();
        Label diffPill = AcademyUi.pill(c.difficulty.toUpperCase(), diffColor(c.difficulty));
        head.getChildren().addAll(iconLab, nameLab, sp, diffPill);

        Label descr = AcademyUi.caption(c.descr, 11);
        descr.setWrapText(true);

        ProgressBar bar = new ProgressBar(c.completionPercent / 100.0);
        bar.setPrefWidth(300);
        bar.setStyle("-fx-accent: " + (c.completionPercent >= 80 ? AcademyUi.GOLD : AcademyUi.GREEN) + ";");
        Label pct = AcademyUi.caption(String.format("\uD83C\uDFAF COMPLETION: %.1f%%", c.completionPercent), 12);
        pct.setStyle("-fx-text-fill: " + (c.completionPercent >= 80 ? AcademyUi.GOLD : "#39FF14")
            + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label lessons = AcademyUi.caption(String.format(
            "\uD83D\uDD13 Unlocked %d   \u2713 Completed %d   \u23F3 Remaining %d",
            c.unlockedLessons, c.completedLessons, c.remainingLessons), 11);

        HBox meta = new HBox(8);
        meta.getChildren().addAll(
            AcademyUi.pill("\u26A1 " + c.xp + " XP", AcademyUi.BLUE),
            AcademyUi.pill("\u23F1 " + c.estMinutes + " min", AcademyUi.PURPLE));

        Button certBtn = AcademyUi.button(
            c.completionPercent >= 80 ? "\uD83C\uDFC5 CERTIFICATE" : "\uD83D\uDD12 LOCKED", 
            c.completionPercent >= 80 ? "#8957e5" : "#30363d", "#ffffff");
        certBtn.setDisable(c.completionPercent < 80);
        certBtn.setOnAction(e -> showCertificateDialog(c));

        card.getChildren().addAll(head, descr, bar, pct, lessons, meta, certBtn);
        return card;
    }

    private void showCertificateDialog(AcademyService.Category c) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Certificate \u2014 " + c.name);

        String status = academy.certificateStatus(c);
        int score = (int) Math.round(c.completionPercent);
        String vid = academy.verificationId(c.id);
        String instructor = academy.instructorFor(c.id);
        String issueDate = java.time.LocalDate.now().toString();

        VBox box = new VBox(12);
        box.setPadding(new Insets(22));
        box.setStyle("-fx-background-color: #0d1117; -fx-border-color: #FFD700; -fx-border-radius: 10;");
        box.setPrefWidth(480);
        box.setAlignment(Pos.CENTER);

        box.getChildren().addAll(
            AcademyUi.neon("\uD83C\uDFC6 CERTIFICATE OF ACHIEVEMENT", AcademyUi.GOLD, 18),
            AcademyUi.caption("UC-FORTRESS ACADEMY \u2014 " + c.name, 12),
            AcademyUi.text("Student: " + operatorID, 13),
            AcademyUi.text("Course: " + c.name, 13),
            AcademyUi.text("Score: " + score + "%", 13),
            AcademyUi.text("Instructor: " + instructor, 13),
            AcademyUi.text("Issue Date: " + issueDate, 13),
            AcademyUi.text("Verification ID: " + vid, 12),
            AcademyUi.pill("STATUS: " + status, status.equals("GRANTED") ? "#3fb950" : "#f85149"),
            AcademyUi.caption("Every certificate is verified via its unique ID and embedded QR matrix.", 11));

        Button dlBtn = AcademyUi.button("\uD83D\uDCC4 DOWNLOAD PDF", "#8957e5", "#ffffff");
        dlBtn.setDisable(!status.equals("GRANTED"));
        dlBtn.setOnAction(e -> { exportCourseCertificatePdf(c, c.name, c.name); dialog.close(); });
        Button closeBtn = AcademyUi.button("\u2715 CLOSE", "#30363d", AcademyUi.LIGHT);
        closeBtn.setOnAction(e -> dialog.close());

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER);
        btns.getChildren().addAll(dlBtn, closeBtn);
        box.getChildren().add(btns);

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
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

    private void stopChallengeClock() {
        if (challengeClock != null) {
            challengeClock.stop();
            challengeClock = null;
        }
    }

    private static String formatElapsed(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    /**
     * HINT SYSTEM (item 7): every hint costs XP. L1/L2/L3 reveal progressive
     * hints, REVEAL fills the answer in, EXPLAIN walks through the solution.
     */
    private HBox buildHintBar(String flag, String family, String hintText,
                              TextField answerField, Label out) {
        String[] labels = {
            "\uD83D\uDCA1 L1 (50 XP)", "\uD83D\uDCA1 L2 (100 XP)", "\uD83D\uDCA1 L3 (150 XP)",
            "\uD83D\uDC41 REVEAL (400 XP)", "\uD83D\uDCD6 EXPLAIN (200 XP)"
        };
        int[] costs = {50, 100, 150, 400, 200};
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            Button b = new Button(labels[i]);
            b.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-size: 10px;"
                + " -fx-font-weight: bold; -fx-cursor: hand;");
            b.setOnAction(e -> {
                int cost = costs[idx];
                int spent = academy.spendXp(cost);
                if (spent < cost) {
                    out.setText("\u26A0\uFE0F NOT ENOUGH XP \u2014 need " + cost + ", you have " + academy.getTotalXp() + ".");
                    out.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px; -fx-font-style: italic;");
                    totalXP = academy.getTotalXp();
                    return;
                }
                totalXP = academy.getTotalXp();
                academy.recordHintUse();
                addLog("[ACADEMY] Hint used \u2014 " + cost + " XP spent.");
                String text;
                String color = "#a79fe6";
                switch (idx) {
                    case 0 -> text = "\uD83D\uDCA1 " + genericFamilyTip(family);
                    case 1 -> text = "\uD83D\uDCA1 " + hintText;
                    case 2 -> text = "\uD83D\uDCA1 Flags live inside UC{...}. Peel the "
                        + family + " layer one step at a time \u2014 then reverse it.";
                    case 3 -> {
                        text = "\uD83D\uDC41 ANSWER REVEALED: " + flag;
                        color = "#ff4fd8";
                        answerField.setText(flag);
                    }
                    default -> text = "\uD83D\uDCD6 Explain: " + hintText
                        + " Apply the inverse operation, then wrap the result in UC{...}. Answer: " + flag + ".";
                }
                out.setText(text);
                out.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-style: italic;");
                computeBadges();
            });
            box.getChildren().add(b);
        }
        return box;
    }

    private static String genericFamilyTip(String family) {
        return switch (family) {
            case "caesar", "caesarhard" -> "Shift each letter by a fixed offset around the alphabet.";
            case "rot13" -> "ROT13 is a Caesar shift of exactly 13.";
            case "reverse" -> "Reverse the string order.";
            case "atbash" -> "Mirror the alphabet: A<->Z, B<->Y, C<->X.";
            case "hex" -> "Every two hex characters decode to one byte.";
            case "ascii" -> "Map each number to its ASCII character.";
            case "binary" -> "Every eight bits decode to one byte.";
            case "octal" -> "Each three-digit octal value is one byte.";
            case "base64" -> "Base64 encodes three bytes into four characters.";
            case "base32" -> "Base32 uses A-Z and 2-7.";
            case "leet" -> "leet replaces letters with digits: 4=A, 3=E, 0=O.";
            case "morse", "morsehard" -> "Dots and dashes; spaces separate letters.";
            case "bacon" -> "Baconian maps each letter to five a/b symbols.";
            case "affine", "affinehard" -> "c = (a*p + b) mod 26; invert to decrypt.";
            case "railfence", "railhard" -> "Write in a zig-zag across the rails, then read rows.";
            case "vigenere", "vigenerehard" -> "Shift each letter by the key's letter index.";
            case "xor", "xorhard" -> "XOR each byte with the key; XOR twice restores the text.";
            case "tripleagent" -> "Three layers stacked: ROT13, reverse, then a keyed cipher.";
            case "expert" -> "Multi-step cipher: combine substitution, transposition and grids.";
            case "nightmare" -> "Stacked algorithms with keys and salts \u2014 peel every layer.";
            case "impossible" -> "Chaotic or one-time key streams; recover the seed or pad first.";
            default -> "Analyze the cipher text, identify the transform, then invert it.";
        };
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
            Label hintLab = AcademyUi.caption("", 11);
            HBox hintBar = buildHintBar(m.challenge.flag, m.challenge.family,
                m.challenge.hint, answer, hintLab);

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
                    academy.recordSolveTime(m.challenge.id, System.currentTimeMillis() - t0);
                    computeBadges();
                    sendAuditLog("MISSION_" + mtype, "ACADEMY");
                    addLog("[ACADEMY] " + mtype + " mission complete! +" + (cxp + cbonus)
                        + " XP, +" + ccoins + " coins.");
                    showDailyChallenges();
                } else {
                    academy.recordAttempt(false);
                    academy.recordWrongFamily(m.challenge.family);
                    answer.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                    addLog("[ACADEMY] Wrong answer for " + mtype + " mission. Try again.");
                }
            });

            row.getChildren().addAll(answer, submit, hintBar, hintLab);
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
        stopChallengeClock();
        List<Label> clockTicks = new java.util.ArrayList<>();
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

        long pb = academy.getPersonalBestMs();
        long wr = academy.getWorldRecordMs();
        Label clockStats = new Label(String.format(
            "\u23F1 CHALLENGE TIMER \u2014 PERSONAL BEST: %s  |  \uD83C\uDF0D WORLD RECORD: %s",
            pb > 0 ? formatElapsed(pb) : "\u2014", formatElapsed(wr)));
        clockStats.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox filterRow = new HBox(8);
        Label filterLabel = new Label("\uD83D\uDD0D FILTER:");
        filterLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-weight: bold;");
        filterRow.getChildren().add(filterLabel);
        String[][] filters = {
            {"ALL", "\uD83C\uDF10 ALL", "#30363d", "#c9d1d9"},
            {"EASY", "\uD83D\uDFE2 EASY", "#1a3a2a", "#39FF14"},
            {"MEDIUM", "\uD83D\uDFE1 MEDIUM", "#3a2a1a", "#FFD700"},
            {"HARD", "\uD83D\uDD34 HARD", "#3a1a1a", "#f85149"},
            {"EXPERT", "\uD83D\uDFE3 EXPERT", "#2a1a3a", "#a371f7"},
            {"NIGHTMARE", "\uD83D\uDD35 NIGHTMARE", "#0a2a3a", "#00d4ff"},
            {"IMPOSSIBLE", "\uD83D\uDC93 IMPOSSIBLE", "#3a0a2a", "#ff4fd8"}
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
            String dColor = diffColor(ch.diff);
            diffLab.setStyle("-fx-text-fill: " + dColor + "; -fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-background-color: #0d1117; -fx-padding: 2 8 2 8; -fx-border-color: " + dColor + "; -fx-border-radius: 4;");
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
                        academy.recordSolveTime(fid, System.currentTimeMillis() - t0);
                        computeBadges();
                        sendAuditLog("CTF_" + fid.toUpperCase(), "ACADEMY");
                        addLog("[CTF] +" + fxp + "XP \u2014 " + ch.title + " cracked!");
                        showLearningModule();
                    } else {
                        academy.recordAttempt(false);
                        academy.recordWrongFamily(ch.family);
                        answerField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                        addLog("[CTF] Wrong answer for " + ch.title + ". Try again.");
                    }
                });
            }

            answerRow.getChildren().addAll(answerField, submitBtn);

            Label timerLab = new Label("\u23F1 00:00:00");
            timerLab.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 12px; -fx-font-weight: bold;");
            long tStart = System.currentTimeMillis();
            timerLab.setUserData(tStart);
            clockTicks.add(timerLab);
            long fast = academy.getFastestMs(ch.id);
            long avg = academy.getAvgMs(ch.id);
            Label tStats = AcademyUi.caption(String.format(
                "\u23F1 FASTEST %s   |   \uD83D\uDCCA AVG %s",
                fast > 0 ? formatElapsed(fast) : "\u2014",
                avg > 0 ? formatElapsed(avg) : "\u2014"), 11);
            HBox timerRow = new HBox(14);
            timerRow.setAlignment(Pos.CENTER_LEFT);
            timerRow.getChildren().addAll(timerLab, tStats);

            Label hintOut = AcademyUi.caption("", 11);
            HBox hintRow = buildHintBar(ch.flag, ch.family, ch.hint, answerField, hintOut);

            Button simBtn = new Button("\uD83E\uDDEA SIMULATE");
            simBtn.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            String sid = ch.family;
            simBtn.setOnAction(ev -> showAlgorithmPlayground(sid));

            card.getChildren().addAll(header, descr, timerRow, answerRow, hintRow, hintOut, hint, simBtn);
            challengesBox.getChildren().add(card);
        }

        challengeClock = new AnimationTimer() {
            private long last = -1;
            @Override
            public void handle(long now) {
                if (last < 0 || now - last > 1_000_000_000L) {
                    last = now;
                    long cur = System.currentTimeMillis();
                    for (Label l : clockTicks) {
                        long start = (Long) l.getUserData();
                        l.setText("\u23F1 " + formatElapsed(cur - start));
                    }
                }
            }
        };
        challengeClock.start();

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

        main.getChildren().addAll(titleRow, stats, xpBar, clockStats, filterRow, leaderLab, scroll, shownLab, resetBtn);
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
            case "aesenc" -> """
                # AES-128-CBC ENCRYPT
                key = base64decode(key_b64)   # 16 bytes
                iv  = base64decode(iv_b64)    # 16 bytes
                cipher = AES.new(key, CBC, iv)
                padded = pkcs5_pad(plaintext) # pad to block
                ct = cipher.encrypt(padded)
                return base64encode(ct)""";
            case "aesdec" -> """
                # AES-128-CBC DECRYPT
                key = base64decode(key_b64)
                iv  = base64decode(iv_b64)
                cipher = AES.new(key, CBC, iv)
                pt = cipher.decrypt(base64decode(ciphertext))
                return unpad(pt, 'pkcs5')""";
            case "rsaenc" -> """
                # RSA KEYGEN + ENCRYPT
                p, q = 61, 53                 # primes
                n = p * q                     # 3233
                phi = (p-1) * (q-1)           # 3120
                e = 17                        # public exp
                d = pow(e, -1, phi)           # 2753
                m = plaintext_as_number()
                c = pow(m, e, n)              # ciphertext""";
            case "rsadec" -> """
                # RSA DECRYPT
                m = pow(c, d, n)   # private op
                text = number_to_text(m)
                return text""";
            case "playfair" -> """
                # PLAYFAIR DIGRAPH ENCRYPT
                sq = build_key_square(key)    # 5x5, I/J merged
                for a, b in digraphs(plain):  # no repeats
                    if same_row(a,b): shift right
                    elif same_col(a,b): shift down
                    else: rectangle corners""";
            case "hill" -> """
                # HILL CIPHER (2x2, mod 26)
                K = matrix(key[0..3])          # key matrix
                for x, y in pairs(plain):
                    c1 = (K00*x + K01*y) % 26
                    c2 = (K10*x + K11*y) % 26
                    emit chr(c1+'A'), chr(c2+'A')""";
            case "transposition" -> """
                # COLUMNAR TRANSPOSITION
                rows = ceil(len(text) / cols)
                grid = [ ['X']*cols for _ in rows ]
                fill grid with text left-to-right
                return read columns top-to-bottom""";
            case "hash" -> """
                # SHA-256 DIGEST
                digest = sha256(plaintext)     # 32 bytes
                return hex(digest)             # 64 hex chars
                # one flipped bit -> ~50% of bits change""";
            case "pwdstrength" -> """
                # PASSWORD STRENGTH METER
                score = 0
                for check in [length, upper, lower,
                              digit, symbol, no_repeat,
                              not_common, length>12]:
                    score += (check passes)
                # 0-2 weak, 3-5 fair, 6-8 strong""";
            case "signature" -> """
                # RSA DIGITAL SIGNATURE
                # SIGN (private key):
                digest = sha256(message)
                sig = pow(digest_as_int, d, n)
                # VERIFY (public key):
                check = pow(sig, e, n)
                accept if check == digest_as_int""";
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
            case "aesenc", "aesdec" -> "16-byte key";
            case "rsaenc", "rsadec" -> "e or d";
            case "playfair" -> "Key phrase";
            case "hill" -> "4 letters (a,b,c,d)";
            case "transposition" -> "Columns (2-9)";
            case "signature" -> "16-byte key";
            default -> "Parameter";
        };
    }

    // === VISUAL LEARNING (ITEM 12) — animated crypto demos ===

    @FunctionalInterface
    private interface AnimatedDrawer { void draw(javafx.scene.canvas.Canvas c, double p); }

    private void showVisualLearning() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFAC VISUAL LEARNING", AcademyUi.GREEN, 22);
        Label sub = AcademyUi.caption(
            "Watch encryption, bit shifting, RSA keygen, AES rounds, hashing, steganography and network packets come to life. Hit PLAY on any demo.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        AnimatedDrawer[] demos = {
            animEncrypt(), animDecrypt(), animBitShift(), animRsaKeygen(),
            animAesRounds(), animHash(), animStego(), animPackets()
        };
        String[] names = {
            "Encryption", "Decryption", "Bit Shifting (XOR)", "RSA Key Generation",
            "AES Rounds", "Hashing / Avalanche", "Steganography Hiding", "Network Packets"
        };
        String[] descs = {
            "Every letter of HELLO shifts +3 as the Caesar wheel turns.",
            "Ciphertext KHOOR slides back -3 into plaintext HELLO.",
            "Bit-by-bit: 'A' \u2295 0x03 = 'B' \u2014 each bit flips under XOR.",
            "Two primes 61 and 53 merge into n=3233, then e and d appear.",
            "A 4x4 state block cycles through all 10 rounds of AES-128.",
            "One extra character avalanches into ~half the digest changing.",
            "Message bits hide in the LSB of pixel green channels.",
            "Encrypted packets stream across the wire from client to server."
        };
        int col = 0, row = 0;
        for (int i = 0; i < demos.length; i++) {
            grid.add(buildAnimCard(names[i], descs[i], demos[i]), col, row);
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

    private javafx.scene.Node buildAnimCard(String name, String descr, AnimatedDrawer drawer) {
        VBox card = AcademyUi.card();
        card.setPrefWidth(520);
        Label nameLab = AcademyUi.neon("\uD83C\uDFAC " + name, AcademyUi.GREEN, 14);
        Label descLab = AcademyUi.caption(descr, 11);
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(480, 200);
        drawer.draw(canvas, 0);
        HBox btns = new HBox(10);
        Button play = AcademyUi.button("\u25B6 PLAY", "#238636", "#ffffff");
        Button reset = AcademyUi.button("\u21BA RESET", "#30363d", AcademyUi.LIGHT);
        btns.getChildren().addAll(play, reset);
        card.getChildren().addAll(nameLab, descLab, canvas, btns);

        AnimationTimer timer = new AnimationTimer() {
            private long start = -1;
            @Override public void handle(long now) {
                if (start < 0) start = now;
                double p = (now - start) / 4_000_000_000.0;
                if (p >= 1) { drawer.draw(canvas, 1); start = -1; stop(); return; }
                drawer.draw(canvas, p);
            }
        };
        play.setOnAction(e -> { timer.stop(); timer.start(); });
        reset.setOnAction(e -> { timer.stop(); drawer.draw(canvas, 0); });
        return card;
    }

    private static void clearCanvas(javafx.scene.canvas.Canvas c) {
        javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.web("#050505"));
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }

    private static void drawAnimTitle(javafx.scene.canvas.GraphicsContext g, String s) {
        g.setFill(Color.web("#a79fe6"));
        g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 14));
        g.fillText(s, 12, 22);
    }

    private AnimatedDrawer animEncrypt() {
        String plain = "HELLO";
        int shift = 3;
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "CAESAR +" + shift + " \u2014 ENCRYPTION");
            g.setFont(javafx.scene.text.Font.font("Monospace", 26));
            int done = (int) Math.floor(p * plain.length());
            for (int i = 0; i < plain.length(); i++) {
                double x = 40 + i * 82;
                char ch = plain.charAt(i);
                if (i < done) {
                    char e = (char) ('A' + (ch - 'A' + shift) % 26);
                    g.setFill(Color.web("#39FF14"));
                    g.fillText(String.valueOf(e), x, 110);
                    g.setFont(javafx.scene.text.Font.font("Monospace", 12));
                    g.setFill(Color.web("#3fb950"));
                    g.fillText(ch + "\u2192" + e, x - 8, 150);
                    g.setFont(javafx.scene.text.Font.font("Monospace", 26));
                } else {
                    g.setFill(Color.web("#8b949e"));
                    g.fillText(String.valueOf(ch), x, 110);
                }
            }
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.setFill(Color.web("#8b949e"));
            g.fillText("Shift each letter forward " + shift + " positions (wrapping Z\u2192A).", 20, 182);
        };
    }

    private AnimatedDrawer animDecrypt() {
        String cipher = "KHOOR";
        int shift = 3;
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "CAESAR -" + shift + " \u2014 DECRYPTION");
            g.setFont(javafx.scene.text.Font.font("Monospace", 26));
            int done = (int) Math.floor(p * cipher.length());
            for (int i = 0; i < cipher.length(); i++) {
                double x = 40 + i * 82;
                char ch = cipher.charAt(i);
                if (i < done) {
                    char d = (char) ('A' + ((ch - 'A' - shift + 26) % 26));
                    g.setFill(Color.web("#00d4ff"));
                    g.fillText(String.valueOf(d), x, 110);
                    g.setFont(javafx.scene.text.Font.font("Monospace", 12));
                    g.setFill(Color.web("#58a6ff"));
                    g.fillText(ch + "\u2192" + d, x - 8, 150);
                    g.setFont(javafx.scene.text.Font.font("Monospace", 26));
                } else {
                    g.setFill(Color.web("#8b949e"));
                    g.fillText(String.valueOf(ch), x, 110);
                }
            }
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.setFill(Color.web("#8b949e"));
            g.fillText("Shift each letter backward " + shift + " positions to recover the text.", 20, 182);
        };
    }

    private AnimatedDrawer animBitShift() {
        int[] a = {0, 1, 0, 0, 0, 0, 0, 1};
        int[] k = {0, 0, 0, 0, 0, 0, 1, 1};
        int[] r = new int[8];
        for (int i = 0; i < 8; i++) r[i] = a[i] ^ k[i];
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "'A' \u2295 0x03 = 'B' \u2014 BIT BY BIT");
            int done = (int) Math.floor(p * 8);
            String[] labels = {"PLAIN  01000001", "KEY    00000011", "XORED  01000010"};
            int[][] rows = {a, k, r};
            for (int row = 0; row < 3; row++) {
                double y = 46 + row * 46;
                g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 13));
                g.setFill(Color.web("#8b949e"));
                g.fillText(labels[row], 14, y + 16);
                for (int bit = 0; bit < 8; bit++) {
                    double x = 180 + bit * 34;
                    g.setFill(bit < done ? Color.web(row == 2 ? "#39FF14" : "#FFD700") : Color.web("#21262d"));
                    g.fillRoundRect(x, y, 26, 26, 4, 4);
                    g.setStroke(Color.web("#30363d"));
                    g.strokeRoundRect(x, y, 26, 26, 4, 4);
                    g.setFill(Color.web("#e6edf3"));
                    g.fillText(String.valueOf(rows[row][bit]), x + 8, y + 18);
                }
            }
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.setFill(Color.web("#8b949e"));
            g.fillText("XOR is reversible \u2014 applying the key again restores the plaintext.", 20, 192);
        };
    }

    private AnimatedDrawer animRsaKeygen() {
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "RSA KEY GENERATION");
            // primes fly in
            double xp = 90 + Math.min(120, p * 340);
            double xq = 390 - Math.min(120, p * 340);
            g.setFill(Color.web("#FFD700"));
            g.fillOval(xp - 30, 70, 60, 60);
            g.fillOval(xq - 30, 70, 60, 60);
            g.setFill(Color.web("#0d1117"));
            g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 14));
            g.fillText("p=61", xp - 15, 105);
            g.fillText("q=53", xq - 15, 105);
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.setFill(Color.web("#8b949e"));
            g.fillText("Choose two primes p and q", 14, 44);
            if (p > 0.3) {
                g.setFill(Color.web("#39FF14"));
                g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 15));
                g.fillText("n = p*q = 3233", 240 - Math.min(60, p * 200), 88);
                g.setFill(Color.web("#8b949e"));
                g.setFont(javafx.scene.text.Font.font("Monospace", 12));
                g.fillText("n is the public modulus", 250, 108);
            }
            if (p > 0.55) {
                g.setFill(Color.web("#58a6ff"));
                g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 13));
                g.fillText("PUBLIC  (n=3233, e=17)", 240, 140);
            }
            if (p > 0.8) {
                g.setFill(Color.web("#f85149"));
                g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 13));
                g.fillText("PRIVATE d = e\u207b\u00b9 mod \u03C6 = 2753", 240, 168);
            }
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.fillText("Public key encrypts; only the private key decrypts.", 20, 192);
        };
    }

    private AnimatedDrawer animAesRounds() {
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            int round = (int) Math.floor(p * 11);
            round = Math.min(10, round);
            drawAnimTitle(g, "AES-128 \u2014 ROUND " + round + "/10");
            for (int cell = 0; cell < 16; cell++) {
                int r = cell / 4, col = cell % 4;
                double x = 150 + col * 44, y = 42 + r * 36;
                int v = (round * 31 + cell * 7 + r * 3) % 256;
                g.setFill(Color.rgb(10 + (v % 90), 80 + (r * 20) % 120, 60 + (col * 30) % 90));
                g.fillRoundRect(x, y, 36, 28, 5, 5);
                g.setStroke(Color.web("#39FF14"));
                g.strokeRoundRect(x, y, 36, 28, 5, 5);
                g.setFill(Color.web("#e6edf3"));
                g.setFont(javafx.scene.text.Font.font("Monospace", 10));
                g.fillText(String.format("%02X", v), x + 6, y + 18);
            }
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.fillText("SubBytes \u2192 ShiftRows \u2192 MixColumns \u2192 AddRoundKey", 20, 190);
            if (round >= 10) {
                g.setFill(Color.web("#39FF14"));
                g.setFont(javafx.scene.text.Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 13));
                g.fillText("CIPHERTEXT READY", 150, 200);
            }
        };
    }

    private AnimatedDrawer animHash() {
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "SHA-256 \u2014 AVALANCHE EFFECT");
            String h1 = AcademyService.sha256Hex("HELLO");
            String h2 = AcademyService.sha256Hex("HELLO!");
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            int shown1 = (int) Math.floor(Math.max(0, (p - 0.05) / 0.4) * 64);
            shown1 = Math.min(64, shown1);
            g.setFill(Color.web("#8b949e"));
            g.fillText("sha256(\"HELLO\")", 20, 50);
            g.setFill(Color.web("#39FF14"));
            g.fillText(h1.substring(0, shown1), 150, 50);
            if (p > 0.5) {
                int shown2 = (int) Math.floor(Math.max(0, (p - 0.55) / 0.35) * 64);
                shown2 = Math.min(64, shown2);
                g.setFill(Color.web("#8b949e"));
                g.fillText("sha256(\"HELLO!\")", 20, 92);
                for (int i = 0; i < shown2; i++) {
                    g.setFill(h1.charAt(i) == h2.charAt(i) ? Color.web("#00d4ff") : Color.web("#f85149"));
                    g.fillText(String.valueOf(h2.charAt(i)), 150 + i * 8, 92);
                }
            }
            g.setFill(Color.web("#8b949e"));
            g.fillText("One extra '!' changes " + countDiffs(h1, h2) + "/64 hex chars \u2014 hashes are one-way.", 20, 132);
            if (p > 0.9) {
                g.setFill(Color.web("#f85149"));
                g.fillText("\u26A0\uFE0F You cannot \"decrypt\" a hash \u2014 brute-force the preimage instead.", 20, 160);
            }
        };
    }

    private static int countDiffs(String a, String b) {
        int n = 0;
        for (int i = 0; i < a.length(); i++) if (a.charAt(i) != b.charAt(i)) n++;
        return n;
    }

    private AnimatedDrawer animStego() {
        String msg = "HI";
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "STEGANOGRAPHY \u2014 LSB EMBEDDING");
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            int cellSize = 22;
            int total = 8 * 8;
            int embedded = (int) Math.floor(p * total);
            StringBuilder bits = new StringBuilder();
            for (int i = 0; i < total; i++) {
                int r = i / 8, col = i % 8;
                double x = 20 + col * (cellSize + 4), y = 44 + r * (cellSize + 4);
                int base = 100 + (r * 17 + col * 9) % 120;
                int bit = 0;
                int byteIdx = i / 8, bitIdx = 7 - (i % 8);
                if (byteIdx < data.length) bit = (data[byteIdx] >> bitIdx) & 1;
                int green = (base & 0xFE) | (i < embedded ? bit : 0);
                g.setFill(Color.rgb(20, green, 40));
                g.fillRoundRect(x, y, cellSize, cellSize, 3, 3);
                if (i < embedded) {
                    g.setStroke(Color.web("#FFD700"));
                    g.strokeRoundRect(x, y, cellSize, cellSize, 3, 3);
                    bits.append(bit);
                }
            }
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.fillText("Bits embedded so far: " + bits.toString(), 20, 232);
            g.fillText("The image looks identical \u2014 only the least-significant bits carry data.", 20, 250);
        };
    }

    private AnimatedDrawer animPackets() {
        return (c, p) -> {
            clearCanvas(c);
            javafx.scene.canvas.GraphicsContext g = c.getGraphicsContext2D();
            drawAnimTitle(g, "NETWORK PACKETS \u2014 ENCRYPTED TRAFFIC");
            g.setFill(Color.web("#58a6ff"));
            g.fillRoundRect(20, 100, 70, 34, 6, 6);
            g.fillRoundRect(392, 100, 70, 34, 6, 6);
            g.setFill(Color.web("#e6edf3"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 11));
            g.fillText("CLIENT", 28, 121);
            g.fillText("SERVER", 400, 121);
            g.setStroke(Color.web("#30363d"));
            g.setLineDashes(6, 6);
            g.strokeLine(90, 117, 392, 117);
            g.setLineDashes(0);
            double launched = p * 9;
            for (int i = 0; i < (int) launched; i++) {
                double x = 90 + ((launched - i) * 33);
                if (x < 90) x = 90;
                if (x > 390) x = 390;
                g.setFill(Color.web("#39FF14"));
                g.fillOval(x - 7, 110, 14, 14);
                g.setFill(Color.web("#0d1117"));
                g.setFont(javafx.scene.text.Font.font("Monospace", 8));
                g.fillText("p" + (i % 10), x - 6, 120);
            }
            if (p > 0.5) {
                g.setFont(javafx.scene.text.Font.font("Monospace", 11));
                g.setFill(Color.web("#00d4ff"));
                g.fillText("AES-encrypted payload \u2022 TLS 1.3 \u2022 HMAC verified", 20, 170);
            }
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 12));
            g.fillText("Packets stream continuously; each carries a ciphertext fragment.", 20, 192);
        };
    }

    // === AI MENTOR CONSOLE (ITEM 13) ===

    private void showAiMentor() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83E\uDD16 AI MENTOR", AcademyUi.PURPLE, 22);
        Label sub = AcademyUi.caption(
            "Your personal crypto coach \u2014 explains mistakes, teaches concepts, quizzes you, and tracks your weaknesses in real time.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        TextArea chat = new TextArea();
        chat.setEditable(false);
        chat.setWrapText(true);
        chat.setPrefRowCount(12);
        chat.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #a79fe6;"
            + " -fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-border-color: #30363d;");
        chat.setText(mentorGreeting());

        javafx.scene.control.ComboBox<String> topicBox = new javafx.scene.control.ComboBox<>();
        topicBox.getItems().addAll("AES", "RSA", "XOR", "Hashing", "Steganography", "Caesar", "Vigenere",
            "Playfair", "Hill", "Transposition", "Digital Signatures", "Base64");
        topicBox.setValue("AES");
        topicBox.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-font-size: 11px;");

        Button teachBtn = AcademyUi.button("\uD83D\uDCDA TEACH A CONCEPT", "#8957e5", "#ffffff");
        Button algoBtn = AcademyUi.button("\uD83E\uDDEA EXPLAIN AN ALGORITHM", "#8957e5", "#ffffff");
        Button quizBtn = AcademyUi.button("\uD83C\uDFAF GENERATE A QUIZ", "#1f6feb", "#ffffff");
        Button mistakeBtn = AcademyUi.button("\uD83D\uDCA1 EXPLAIN MY MISTAKES", "#f0883e", "#ffffff");
        Button hintBtn = AcademyUi.button("\uD83E\uDD16 GIVE ME A HINT", "#a371f7", "#ffffff");
        Button recBtn = AcademyUi.button("\uD83E\uDDED RECOMMEND NEXT LESSON", "#3fb950", "#ffffff");
        Button weakBtn = AcademyUi.button("\uD83C\uDFAF TRACK MY WEAKNESSES", "#00d4ff", "#111111");
        Button encBtn = AcademyUi.button("\uD83D\uDCAA ENCOURAGE ME", "#FFD700", "#111111");

        VBox quizBox = new VBox(8);
        quizBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 14; -fx-border-color: #30363d; -fx-border-radius: 8;");
        quizBox.setVisible(false);
        quizBox.setManaged(false);

        teachBtn.setOnAction(e -> {
            mentorSay(chat, "Teaching \"" + topicBox.getValue() + "\"", academy.teachConcept(topicBox.getValue()));
        });
        algoBtn.setOnAction(e -> {
            String algo = topicBox.getValue().toLowerCase().replace(" ", "");
            String pseudo = switch (algo) {
                case "aes" -> algoPseudo("aesenc");
                case "rsa" -> algoPseudo("rsaenc");
                case "xor" -> algoPseudo("xor");
                case "hashing" -> algoPseudo("hash");
                case "steganography" -> "LSB embedding:\n  for each pixel:\n    bit = next_bit(message)\n    green = (green & 0xFE) | bit";
                case "caesar" -> algoPseudo("caesar");
                case "vigenere" -> algoPseudo("vigenere");
                case "playfair" -> algoPseudo("playfair");
                case "hill" -> algoPseudo("hill");
                case "transposition" -> algoPseudo("transposition");
                case "digitalsignatures" -> algoPseudo("signature");
                case "base64" -> algoPseudo("base64");
                default -> algoPseudo("xor");
            };
            mentorSay(chat, "Algorithm \"" + topicBox.getValue() + "\"", pseudo);
        });
        quizBtn.setOnAction(e -> renderMentorQuiz(chat, quizBox));
        mistakeBtn.setOnAction(e -> {
            var weak = academy.getWeakFamilies();
            if (weak.isEmpty()) {
                mentorSay(chat, "Mistake report", "No wrong answers recorded yet. Attempt any challenge and miss on purpose? No \u2014 just keep solving; "
                    + "every wrong answer teaches me where to focus. (Weakness tracking activates on your first incorrect solve.)");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String[] w : weak) {
                    sb.append("\u2022 ").append(w[0]).append(" (").append(w[1]).append(" misses)\n");
                    sb.append("  ").append(academy.explainMistake(w[2])).append("\n");
                }
                mentorSay(chat, "Mistake report", sb.toString());
            }
        });
        hintBtn.setOnAction(e -> {
            mentorSay(chat, "Hint", "Use \uD83E\uDDEA AI HINT on any challenge card, or open the CRYPTO LAB and run the algorithm on a sample word to watch it step by step. "
                + "For a targeted nudge on your current focus: " + academy.teachConcept(topicBox.getValue()).split("\n")[0]);
        });
        recBtn.setOnAction(e -> mentorSay(chat, "Recommendation", academy.recommendNextLesson()));
        weakBtn.setOnAction(e -> {
            var weak = academy.getWeakFamilies();
            if (weak.isEmpty()) {
                mentorSay(chat, "Weakness profile", "Clean profile \u2014 no recurring mistakes detected yet. The mentor will track families you miss and steer you toward them.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String[] w : weak) sb.append("\u2022 ").append(w[0]).append(" \u2014 ").append(w[1]).append(" wrong\n");
                sb.append("Tip: open the CRYPTO LAB and replay the weakest family on a sample before your next attempt.");
                mentorSay(chat, "Weakness profile", sb.toString());
            }
        });
        encBtn.setOnAction(e -> mentorSay(chat, "Mentor", academy.encourage()));

        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.getChildren().addAll(topicBox, teachBtn, algoBtn, quizBtn, hintBtn);
        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(mistakeBtn, recBtn, weakBtn, encBtn);

        main.getChildren().addAll(chat, row1, row2, quizBox);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private static void mentorSay(TextArea chat, String role, String body) {
        chat.appendText("\n\n\u25B8 " + role.toUpperCase() + "\n" + body);
        chat.setScrollTop(Double.MAX_VALUE);
    }

    private static String mentorGreeting() {
        return "\uD83E\uDD16 AI MENTOR ONLINE. I have read your solve history and your weak spots. "
            + "Ask me to TEACH A CONCEPT, EXPLAIN AN ALGORITHM, GENERATE A QUIZ, EXPLAIN MY MISTAKES, "
            + "TRACK MY WEAKNESSES, RECOMMEND NEXT LESSON or just ENCOURAGE ME.";
    }

    private void renderMentorQuiz(TextArea chat, VBox quizBox) {
        academy.generateQuiz();
        quizBox.getChildren().clear();
        quizBox.setVisible(true);
        quizBox.setManaged(true);
        int[] qi = {0};
        int[] score = {0};
        renderQuizQuestion(chat, quizBox, qi, score);
    }

    private void renderQuizQuestion(TextArea chat, VBox quizBox, int[] qi, int[] score) {
        quizBox.getChildren().clear();
        if (qi[0] >= academy.quizCount()) {
            Label done = AcademyUi.text("\uD83C\uDFC6 QUIZ COMPLETE \u2014 SCORE " + score[0] + "/" + academy.quizCount(), 13);
            done.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 13px; -fx-font-weight: bold;");
            quizBox.getChildren().add(done);
            mentorSay(chat, "Quiz", "Final score " + score[0] + "/" + academy.quizCount()
                + ". Review EXPLAIN ANSWERS on each question above, or generate a fresh quiz tomorrow.");
            return;
        }
        int i = qi[0];
        quizBox.getChildren().add(AcademyUi.neon("Q" + (i + 1) + "/" + academy.quizCount() + ": " + academy.quizQuestion(i), AcademyUi.GOLD, 13));
        String[] opts = academy.quizOptions(i);
        int correct = academy.quizAnswerIndex(i);
        Label verdict = AcademyUi.caption("", 12);
        for (int o = 0; o < 4; o++) {
            final int pick = o;
            Button b = AcademyUi.button((o == correct ? "\u2713 " : "") + opts[o],
                o == correct ? "#238636" : "#21262d", "#ffffff");
            b.setOnAction(ev -> {
                boolean ok = pick == correct;
                if (ok) score[0]++;
                String v = (ok ? "\u2705 CORRECT \u2014 " : "\u274C NOT QUITE \u2014 ") + academy.quizExplain(i);
                verdict.setText(v);
                verdict.setStyle("-fx-text-fill: " + (ok ? "#3fb950" : "#f85149") + "; -fx-font-size: 12px; -fx-wrap-text: true;");
            });
            quizBox.getChildren().add(b);
        }
        Button next = AcademyUi.button("\u25B6 NEXT", "#1f6feb", "#ffffff");
        next.setOnAction(ev -> { qi[0]++; renderQuizQuestion(chat, quizBox, qi, score); });
        quizBox.getChildren().addAll(verdict, next);
    }

    // === INTERACTIVE CRYPTO LAB (ITEM 11) — hub of live simulators ===

    private void showCryptoLab() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83E\uDDEA INTERACTIVE CRYPTO LAB", AcademyUi.GREEN, 22);
        Label sub = AcademyUi.caption(
            "Live laboratories \u2014 type any input, hit RUN, and watch every cipher step-by-step in real time.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        String[][] labs = {
            {"aesenc", "\uD83D\uDD12", "AES Encrypt", "AES-128-CBC with key + IV, PKCS5 padding."},
            {"aesdec", "\uD83D\uDD13", "AES Decrypt", "Reverse CBC: key, IV and ciphertext in."},
            {"rsaenc", "\uD83D\uDD10", "RSA Encrypt", "Generate p,q,n,e,d then encrypt a number."},
            {"rsadec", "\uD83D\uDD11", "RSA Decrypt", "m = c^d mod n with the private exponent."},
            {"caesar", "\uD83D\uDD20", "Caesar Simulator", "Shift letters around the alphabet."},
            {"vigenere", "\uD83D\uDD1F", "Vigenere Simulator", "Repeating-key substitution."},
            {"playfair", "\uD83C\uDFB4", "Playfair Simulator", "Digraph cipher on a 5x5 key square."},
            {"hill", "\uD83D\uDD16", "Hill Cipher", "Linear algebra on 2x2 key matrices."},
            {"railfence", "\uD83D\uDE8B", "Rail Fence", "Zig-zag transposition across rails."},
            {"transposition", "\uD83D\uDD32", "Transposition", "Columnar shuffle that preserves letters."},
            {"xor", "\uD83C\uDFAF", "XOR Bitwise", "Byte-level XOR with a key."},
            {"hash", "\uD83D\uDD22", "Hash Generator", "SHA-256 digest + avalanche demo."},
            {"pwdstrength", "\uD83D\uDD11", "Password Strength", "Score a password against 8 checks."},
            {"signature", "\u270D\uFE0F", "Digital Signature", "Sign with RSA, verify the digest."},
            {"affine", "\uD83C\uDF9D\uFE0F", "Affine Cipher", "c = (a*p + b) mod 26."},
            {"morse", "\uD83D\uDCE1", "Morse Code", "Dots and dashes for every letter."},
            {"bacon", "\uD83E\uDD53", "Baconian Cipher", "Five a/b symbols per letter."},
            {"tripleagent", "\uD83C\uDFE6", "Triple-Stack", "ROT13 + Reverse + Vigenere."},
            {"atbash", "\uD83E\uDE9E", "Atbash", "Mirror the alphabet A\u2194Z."},
            {"rot13", "\uD83D\uDD04", "ROT13", "Caesar with a fixed shift of 13."},
            {"reverse", "\u2194\uFE0F", "Reverse", "Flip the string order."},
            {"leet", "\uD83D\uDCDF", "Leet Speak", "4=A, 3=E, 0=O substitution."},
            {"hex", "\uD83D\uDDA4", "Hex Encoding", "Bytes as two hex digits."},
            {"ascii", "\uD83D\uDD22", "ASCII Encoding", "Letters as 0-127 codes."},
            {"octal", "\uD83D\uDDF2\uFE0F", "Octal Encoding", "Bytes as three octal digits."},
            {"binary", "\uD83D\uDC68\u200D\uD83D\uDCBB", "Binary Encoding", "Bytes as eight bits."},
            {"base64", "\uD83D\uDDC2", "Base64 Encoding", "Three bytes into four chars."},
            {"base32", "\uD83D\uDDC3", "Base32 Encoding", "A-Z and 2-7, five bits per char."}
        };

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        int col = 0, row = 0;
        for (String[] l : labs) {
            grid.add(buildLabCard(l[0], l[1], l[2], l[3]), col, row);
            col++;
            if (col >= 4) { col = 0; row++; }
        }
        main.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildLabCard(String id, String icon, String name, String descr) {
        VBox card = AcademyUi.card();
        card.setPrefWidth(250);
        card.setPrefHeight(120);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1a1f29; -fx-border-color: #39FF14; -fx-border-radius: 8; -fx-padding: 14;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-radius: 8; -fx-padding: 14;"));

        Label nameLab = AcademyUi.neon(icon + " " + name, AcademyUi.GREEN, 14);
        Label descLab = AcademyUi.caption(descr, 11);
        descLab.setWrapText(true);
        Button run = AcademyUi.button("\u25B6 OPEN LAB", "#1f6feb", "#ffffff");
        run.setOnAction(e -> showAlgorithmPlayground(id));
        HBox bt = new HBox();
        bt.setAlignment(Pos.CENTER_RIGHT);
        bt.getChildren().add(run);
        card.getChildren().addAll(nameLab, descLab, bt);
        return card;
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
            case "aesenc" -> "AES Encrypt";
            case "aesdec" -> "AES Decrypt";
            case "rsaenc" -> "RSA Encrypt";
            case "rsadec" -> "RSA Decrypt";
            case "playfair" -> "Playfair Cipher";
            case "hill" -> "Hill Cipher";
            case "transposition" -> "Columnar Transposition";
            case "hash" -> "SHA-256 Hash Generator";
            case "pwdstrength" -> "Password Strength Meter";
            case "signature" -> "RSA Digital Signature";
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
                case "atbash", "binary", "base64", "rot13", "reverse", "hex", "ascii", "octal", "base32", "leet", "bacon", "hash", "pwdstrength" -> true;
                default -> false;
            }) {
            paramField.setVisible(false);
            paramField.setManaged(false);
        }
        if (algoId.startsWith("aes") || algoId.startsWith("rsa") || algoId.equals("signature")) {
            paramField.setText("16-char-key-here!!");
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
            case "aesenc" -> simulateAes(input, param, stepsBox, outputLabel, true);
            case "aesdec" -> simulateAes(input, param, stepsBox, outputLabel, false);
            case "rsaenc" -> simulateRsa(input, param, stepsBox, outputLabel, true);
            case "rsadec" -> simulateRsa(input, param, stepsBox, outputLabel, false);
            case "playfair" -> simulatePlayfair(input, param, stepsBox, outputLabel);
            case "hill" -> simulateHill(input, param, stepsBox, outputLabel);
            case "transposition" -> simulateTransposition(input, param, stepsBox, outputLabel);
            case "hash" -> simulateHash(input, stepsBox, outputLabel);
            case "pwdstrength" -> simulatePwdStrength(input, stepsBox, outputLabel);
            case "signature" -> simulateSignature(input, param, stepsBox, outputLabel);
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

    // ---- INTERACTIVE CRYPTO LAB simulators (ITEM 11) ----

    private void simulateAes(String input, String param, VBox box, Label out, boolean encrypt) {
        String keyB64 = java.util.Base64.getEncoder().encodeToString(pad16(param).getBytes(StandardCharsets.UTF_8));
        String ivB64 = java.util.Base64.getEncoder().encodeToString("1234567890123456".getBytes(StandardCharsets.UTF_8));
        addStep(box, "# AES-128-CBC " + (encrypt ? "ENCRYPT" : "DECRYPT"), "#58a6ff");
        addStep(box, "  Key (b64): " + keyB64, "#c9d1d9");
        addStep(box, "  IV  (b64): " + ivB64, "#c9d1d9");
        if (encrypt) {
            addStep(box, "  1. Split plaintext into 16-byte blocks.", "#8b949e");
            addStep(box, "  2. XOR each block with the previous cipher block (CBC).", "#8b949e");
            addStep(box, "  3. Run 10 rounds: SubBytes -> ShiftRows -> MixColumns -> AddRoundKey.", "#8b949e");
            String ct = AcademyService.aesEncryptB64(input, keyB64, ivB64);
            addSep(box);
            out.setText("\uD83D\uDD12 CIPHERTEXT (b64): " + ct);
        } else {
            String ct = input;
            String plain = AcademyService.aesDecryptB64(ct, keyB64, ivB64);
            addStep(box, "  1. Base64-decode the ciphertext.", "#8b949e");
            addStep(box, "  2. Apply the inverse AES rounds with the key schedule.", "#8b949e");
            addStep(box, "  3. XOR the first block with the IV, then unpad PKCS5.", "#8b949e");
            addSep(box);
            out.setText("\uD83D\uDD13 PLAINTEXT: " + plain);
        }
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateRsa(String input, String param, VBox box, Label out, boolean encrypt) {
        long p = 61, q = 53, n = p * q, phi = (p - 1) * (q - 1);
        long e = 17, d = AcademyService.modInverse(e, phi);
        addStep(box, "# RSA " + (encrypt ? "ENCRYPT" : "DECRYPT"), "#58a6ff");
        addStep(box, "  p=" + p + "  q=" + q + "  n=" + n + "  \u03C6=" + phi, "#c9d1d9");
        addStep(box, "  Public (e)=" + e + "   Private (d)=" + d, "#c9d1d9");
        if (encrypt) {
            long m = AcademyService.wordToNumber(input);
            long c = BigInteger.valueOf(m).modPow(BigInteger.valueOf(e), BigInteger.valueOf(n)).longValue();
            addStep(box, "  m (plaintext number) = " + m, "#8b949e");
            addStep(box, "  c = m^e mod n = " + c, "#8b949e");
            addStep(box, "  Anyone can encrypt with (n,e); only d can reverse it.", "#8b949e");
            addSep(box);
            out.setText("\uD83D\uDD10 CIPHERTEXT c = " + c);
        } else {
            String cStr = input.trim().replaceAll("[^0-9]", "");
            if (cStr.isEmpty()) {
                out.setText("\u26A0\uFE0F Enter a numeric ciphertext c first.");
                out.setStyle("-fx-text-fill: #f85149; -fx-font-size: 13px;");
                return;
            }
            long c = Long.parseLong(cStr);
            long m = BigInteger.valueOf(c).modPow(BigInteger.valueOf(d), BigInteger.valueOf(n)).longValue();
            addStep(box, "  m = c^d mod n = " + m, "#8b949e");
            String word = AcademyService.numberToWord(m);
            addStep(box, "  Digits -> letters (A=10...): " + word, "#8b949e");
            addSep(box);
            out.setText("\uD83D\uDD13 PLAINTEXT: " + word);
        }
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulatePlayfair(String input, String key, VBox box, Label out) {
        addStep(box, "# PLAYFAIR \u2014 5x5 key square", "#58a6ff");
        String k = (key.isEmpty() ? "MONARCHY" : key).toUpperCase().replace("J", "I");
        addStep(box, "  Key square (from '" + k + "'):", "#c9d1d9");
        addStep(box, "  " + String.join(" ", playfairRows(k)), "#c9d1d9");
        String ct = AcademyService.playfairEncrypt(input, k);
        addStep(box, "  Split into digraphs; same-row -> shift right, same-column -> shift down, else rectangle.", "#8b949e");
        addSep(box);
        out.setText("\uD83D\uDD13 ENCRYPTED: " + ct);
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static java.util.List<String> playfairRows(String key) {
        String k = (key + "ABCDEFGHIKLMNOPQRSTUVWXYZ").replace("J", "I");
        java.util.LinkedHashSet<Character> seen = new java.util.LinkedHashSet<>();
        for (char c : k.toCharArray()) if (c >= 'A' && c <= 'Z') seen.add(c);
        java.util.List<String> rows = new java.util.ArrayList<>();
        int i = 0;
        for (char c : seen) {
            if (i % 5 == 0) rows.add("");
            rows.set(rows.size() - 1, rows.get(rows.size() - 1) + c);
            i++;
        }
        return rows;
    }

    private void simulateHill(String input, String key, VBox box, Label out) {
        String k = (key.length() >= 4 ? key : "GYBN").toUpperCase();
        addStep(box, "# HILL CIPHER (2x2, mod 26)", "#58a6ff");
        addStep(box, "  Key matrix from '" + k + "':", "#c9d1d9");
        int a = k.charAt(0) - 'A', b = k.charAt(1) - 'A', c = k.charAt(2) - 'A', dd = k.charAt(3) - 'A';
        addStep(box, String.format("  [ %2d %2d ]\n  [ %2d %2d ]", a, b, c, dd), "#c9d1d9");
        long det = (long) a * dd - (long) b * c;
        addStep(box, "  det = " + det + " (must be coprime with 26 to decrypt).", "#8b949e");
        String ct = AcademyService.hillEncrypt(input, k);
        addStep(box, "  For each plaintext pair (x,y): c1=(a*x+b*y)%26, c2=(c*x+d*y)%26.", "#8b949e");
        addSep(box);
        out.setText("\uD83D\uDD13 ENCRYPTED: " + ct);
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateTransposition(String input, String param, VBox box, Label out) {
        int cols;
        try { cols = Math.max(2, Math.min(9, Integer.parseInt(param.trim()))); }
        catch (Exception e) { cols = 4; }
        addStep(box, "# COLUMNAR TRANSPOSITION (" + cols + " columns)", "#58a6ff");
        String ct = AcademyService.transpositionEncrypt(input, cols);
        String pt = AcademyService.transpositionDecrypt(ct, cols);
        addStep(box, "  Write text into " + cols + " columns, pad with X, read rows.", "#8b949e");
        addStep(box, "  ENCRYPTED: " + ct, "#c9d1d9");
        addStep(box, "  DECRYPTED round-trip: " + pt, "#58a6ff");
        addStep(box, "  Frequencies unchanged \u2014 pure permutation.", "#8b949e");
        addSep(box);
        out.setText("\uD83D\uDEE1 OUTPUT: " + ct);
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulateHash(String input, VBox box, Label out) {
        addStep(box, "# SHA-256 HASH GENERATOR", "#58a6ff");
        String h1 = AcademyService.sha256Hex(input);
        addStep(box, "  sha256('" + input + "') = " + h1, "#c9d1d9");
        String tweaked = input.isEmpty() ? "A" : input;
        String h2 = AcademyService.sha256Hex(tweaked + "!");
        int diffs = 0;
        for (int i = 0; i < 64; i++) if (h1.charAt(i) != h2.charAt(i)) diffs++;
        addStep(box, "  sha256('" + tweaked + "!') = " + h2, "#f85149");
        addStep(box, "  Avalanche: " + diffs + "/64 hex chars changed from ONE extra char \u2014 hashes are one-way.", "#8b949e");
        addSep(box);
        out.setText("\uD83D\uDD22 DIGEST: " + h1);
        out.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private void simulatePwdStrength(String input, VBox box, Label out) {
        addStep(box, "# PASSWORD STRENGTH METER", "#58a6ff");
        String p = input;
        int score = 0;
        String[] checks = new String[8];
        checks[0] = "Length >= 8 chars";
        checks[1] = "Has UPPERCASE";
        checks[2] = "Has lowercase";
        checks[3] = "Has a digit";
        checks[4] = "Has a symbol";
        checks[5] = "No repeating runs (aaa, 111)";
        checks[6] = "Not a common password";
        checks[7] = "Length >= 12 chars";
        boolean[] pass = {
            p.length() >= 8,
            p.matches(".*[A-Z].*"),
            p.matches(".*[a-z].*"),
            p.matches(".*\\d.*"),
            p.matches(".*[^A-Za-z0-9].*"),
            !p.matches(".*(.)\\1\\1.*"),
            !COMMON_PASSWORDS.contains(p.toLowerCase()),
            p.length() >= 12
        };
        for (int i = 0; i < 8; i++) {
            if (pass[i]) score++;
            addStep(box, "  " + (pass[i] ? "\u2705" : "\u274C") + " " + checks[i], pass[i] ? "#3fb950" : "#f85149");
        }
        String verdict = score <= 2 ? "WEAK \u2014 crack in seconds" : score <= 5 ? "FAIR \u2014 dictionary-solvable" : "STRONG \u2014 resists brute force";
        String color = score <= 2 ? "#f85149" : score <= 5 ? "#FFD700" : "#39FF14";
        addSep(box);
        out.setText("\uD83D\uDD11 SCORE " + score + "/8 \u2014 " + verdict.toUpperCase());
        out.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static final java.util.Set<String> COMMON_PASSWORDS = java.util.Set.of(
        "password", "123456", "12345678", "qwerty", "abc123", "letmein", "admin", "welcome",
        "monkey", "dragon", "111111", "password1", "iloveyou", "sunshine", "princess", "football");

    private void simulateSignature(String input, String param, VBox box, Label out) {
        long p = 61, q = 53, n = p * q, phi = (p - 1) * (q - 1);
        long e = 17, d = AcademyService.modInverse(e, phi);
        addStep(box, "# RSA DIGITAL SIGNATURE", "#58a6ff");
        addStep(box, "  Signer private key d=" + d + ", public (n=" + n + ", e=" + e + ")", "#c9d1d9");
        long digest = new BigInteger(AcademyService.sha256Hex(input).substring(0, 16), 16)
            .mod(BigInteger.valueOf(n)).longValue();
        long sig = BigInteger.valueOf(digest).modPow(BigInteger.valueOf(d), BigInteger.valueOf(n)).longValue();
        long verify = BigInteger.valueOf(sig).modPow(BigInteger.valueOf(e), BigInteger.valueOf(n)).longValue();
        addStep(box, "  digest = sha256(msg) mod n = " + digest, "#8b949e");
        addStep(box, "  SIGN: sig = digest^d mod n = " + sig, "#58a6ff");
        addStep(box, "  VERIFY: digest' = sig^e mod n = " + verify, "#58a6ff");
        boolean ok = digest == verify;
        addStep(box, "  " + (ok ? "\u2705 Signature valid \u2014 only d could produce it." : "\u274C Signature invalid!"), ok ? "#3fb950" : "#f85149");
        addSep(box);
        out.setText((ok ? "\u2705" : "\u274C") + " VERIFY: " + (ok ? "SIGNATURE VALID" : "INVALID"));
        out.setStyle("-fx-text-fill: " + (ok ? "#3fb950" : "#f85149") + "; -fx-font-size: 14px; -fx-font-family: 'Courier New'; -fx-font-weight: bold;");
    }

    private static String pad16(String s) {
        String t = s == null ? "" : s;
        if (t.length() >= 16) return t.substring(0, 16);
        StringBuilder sb = new StringBuilder(t);
        while (sb.length() < 16) sb.append('!');
        return sb.toString();
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

    // ============================================================
    // ATTACK SIMULATOR (ITEM 15)
    // ============================================================

    private void showAttackSimulator() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ACADEMY", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83D\uDC79 ATTACK SIMULATOR", AcademyUi.RED, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.RED, 0.3));
        Label sub = AcademyUi.caption(
            "Interactive red-team laboratories \u2014 watch real cryptographic attacks unfold step by step.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        String[][] attacks = {
            {"brute", "\uD83D\uDD10", "Brute Force", "Try every key until the plaintext makes sense."},
            {"dictionary", "\uD83D\uDCDA", "Dictionary Attack", "Crack a SHA-256 hash using a common word list."},
            {"freq", "\uD83D\uDCC8", "Frequency Analysis", "Letter-frequency stats against monoalphabetic ciphers."},
            {"chosen", "\uD83C\uDFAF", "Chosen Plaintext", "Craft inputs that reveal ECB block patterns."},
            {"known", "\uD83D\uDCC4", "Known Plaintext", "Recover the key from plaintext/ciphertext pairs."},
            {"replay", "\uD83D\uDD04", "Replay Attack", "Capture a request and replay it undetected."},
            {"mitm", "\uD83D\uDD28", "MITM", "Intercept Alice and Bob's key exchange."},
            {"rainbow", "\uD83C\uDF08", "Rainbow Tables", "Precomputed chains crack passwords in milliseconds."},
            {"pwd", "\uD83D\uDD11", "Password Cracking", "Estimate crack time at real GPU hash rates."},
            {"collision", "\uD83D\uDCA5", "Collision Demo", "Birthday attack on truncated hashes."}
        };
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        int col = 0, row = 0;
        for (String[] a : attacks) {
            grid.add(buildAttackCard(a[0], a[1], a[2], a[3]), col, row);
            col++;
            if (col >= 4) { col = 0; row++; }
        }
        main.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildAttackCard(String id, String icon, String name, String descr) {
        VBox card = AcademyUi.cardAccent(AcademyUi.RED);
        card.setPrefWidth(250);
        Label nameLab = AcademyUi.neon(icon + " " + name, AcademyUi.RED, 14);
        Label descLab = AcademyUi.caption(descr, 11);
        descLab.setWrapText(true);
        Button run = AcademyUi.button("\uD83D\uDE80 LAUNCH ATTACK", "#f85149", "#ffffff");
        run.setOnAction(e -> showAttackLab(id, name));
        card.getChildren().addAll(nameLab, descLab, run);
        return card;
    }

    private void showAttackLab(String attackId, String name) {
        academyActive = true;
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ATTACK SIMULATOR", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAttackSimulator());

        Label title = new Label("\uD83D\uDC79 " + name + " \u2014 Interactive Attack Lab");
        title.setStyle("-fx-text-fill: #f85149; -fx-font-size: 18px; -fx-font-weight: bold;");

        TextArea codeArea = new TextArea(attackPseudo(attackId));
        codeArea.setEditable(false);
        codeArea.setPrefRowCount(6);
        codeArea.setStyle(OUTPUT_STYLE);

        TextField inputField = new TextField(attackDefaultInput(attackId));
        inputField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #30363d; -fx-pref-width: 340;");

        Button runBtn = new Button("\uD83D\uDE80 RUN ATTACK");
        runBtn.setStyle("-fx-background-color: #f85149; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox stepsBox = new VBox(6);
        stepsBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label outputLabel = new Label("Attack output will appear here...");
        outputLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        outputLabel.setWrapText(true);

        runBtn.setOnAction(e -> {
            stepsBox.getChildren().clear();
            outputLabel.setText("");
            simulateAttack(attackId, inputField.getText(), stepsBox, outputLabel);
        });

        HBox inputRow = new HBox(10, inputField, runBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, backBtn, title, codeArea, inputRow, stepsBox, outputLabel);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(650);
        main.getChildren().add(scroll);
        setCenter(main);
    }

    private static String attackPseudo(String id) {
        return switch (id) {
            case "brute" -> "for key in 0..25:\n    candidate = decrypt(cipher, key)\n    score = englishness(candidate)\n    return best(candidate)";
            case "dictionary" -> "for word in wordlist:\n    if sha256(word) == target_hash: return word";
            case "freq" -> "count[letter] for ciphertext\norder = sort(count, desc)\nmap order[i] -> ETAOINHSRDLU[i]";
            case "chosen" -> "blocks = split(plaintext, 16)\nfor b: ecb_out = E(b)\ncompare: equal blocks leak equality";
            case "known" -> "key[i] = plain[i] XOR cipher[i] (repeating key)";
            case "replay" -> "capture(request)\nserver.accepts(request)  # no nonce\nserver.accepts(request)  # replay works!";
            case "mitm" -> "A ->Eve-> B : g^a\nB ->Eve-> A : g^b\nEve shares key with both (two separate sessions)";
            case "rainbow" -> "chain = [p0, R(h(p0)), R(h(R(h(p0))))...]\nlookup: reduce target hash -> walk chain -> hit";
            case "pwd" -> "for charset in [...]\n  time = keyspace / gpu_rate\n  if time < 1yr: cracked";
            case "collision" -> "seen = {}\nfor i in 0..:\n  h = truncate(hash(i), b)\n  if h in seen: collision found";
            default -> "run attack";
        };
    }

    private static String attackDefaultInput(String id) {
        return switch (id) {
            case "brute" -> "KHOOR ZRUOG";
            case "dictionary" -> "P@ssw0rd!";
            case "freq" -> "ATTACKATDAWN";
            case "chosen" -> "HELLOHELLOHELLOHELLOHELLO";
            case "known" -> "ATTACK";
            case "replay" -> "GRANT ACCESS admin=root";
            case "mitm" -> "g=5 p=23";
            case "rainbow" -> "VaultKeeper";
            case "pwd" -> "P@ssw0rd!";
            case "collision" -> "24";
            default -> "";
        };
    }

    private void simulateAttack(String attackId, String input, VBox box, Label out) {
        switch (attackId) {
            case "brute" -> simulateBruteForce(input, box, out);
            case "dictionary" -> simulateDictionary(input, box, out);
            case "freq" -> simulateFreqAnalysis(input, box, out);
            case "chosen" -> simulateChosenPlaintext(input, box, out);
            case "known" -> simulateKnownPlaintext(input, box, out);
            case "replay" -> simulateReplay(input, box, out);
            case "mitm" -> simulateMitm(input, box, out);
            case "rainbow" -> simulateRainbow(input, box, out);
            case "pwd" -> simulatePwdCrack(input, box, out);
            case "collision" -> simulateCollision(input, box, out);
        }
    }

    private static final String[] COMMON_WORDS = {
        "P@ssw0rd!", "password", "qwerty123", "letmein", "admin", "root",
        "monkey", "dragon", "football", "iloveyou", "sunshine", "princess",
        "hello", "welcome", "shadow", "12345678", "ninja", "mustang",
        "VaultKeeper", "KaliQueen", "HashQueen", "secret", "trustno1", "master"
    };

    private void simulateBruteForce(String input, VBox box, Label out) {
        String cipher = input.toUpperCase().replaceAll("[^A-Z ]", "");
        addStep(box, "\u25B6 BRUTE FORCE over 25 shift keys", "#f85149");
        addSep(box);
        if (cipher.isBlank()) { addStep(box, "No ciphertext.", "#8b949e"); return; }
        String best = "";
        int bestScore = -1, bestKey = -1;
        for (int k = 1; k < 26; k++) {
            StringBuilder sb = new StringBuilder();
            for (char c : cipher.toCharArray()) {
                if (c == ' ') { sb.append(' '); continue; }
                sb.append((char) ('A' + (c - 'A' - k + 26) % 26));
            }
            String cand = sb.toString();
            int score = englishness(cand);
            String mark = k % 5 == 0 ? " *" : "";
            addStep(box, String.format("key %2d: %s %s", k, cand, mark), k % 5 == 0 ? "#c9d1d9" : "#484f58");
            if (score > bestScore) { bestScore = score; best = cand; bestKey = k; }
        }
        addSep(box);
        addStep(box, "\u2705 BEST CANDIDATE (shift " + bestKey + "): " + best, "#39FF14");
        out.setText("Brute force examined 25 keys in ~0ms. Key " + bestKey + " produced the most English-looking text.");
    }

    private static int englishness(String s) {
        int score = 0;
        String up = s.toUpperCase();
        if (up.contains("THE")) score += 8;
        if (up.contains("AND")) score += 5;
        if (up.contains("ATTACK")) score += 20;
        if (up.contains("HELLO")) score += 15;
        int vowels = 0;
        for (char c : up.toCharArray()) {
            if ("AEIOU".indexOf(c) >= 0) vowels++;
            if ("ETAOINSHRDLCUMWFGYPBVKJXQZ".indexOf(c) >= 0) score++;
        }
        score += vowels;
        return score;
    }

    private void simulateDictionary(String input, VBox box, Label out) {
        String target = input.isEmpty() ? "P@ssw0rd!" : input;
        String hash = AcademyService.sha256Hex(target);
        addStep(box, "\u25B6 DICTIONARY ATTACK on SHA-256 hash", "#f85149");
        addStep(box, "target = " + target, "#c9d1d9");
        addStep(box, "digest = " + hash.substring(0, 24) + "\u2026", "#8b949e");
        addSep(box);
        long start = System.nanoTime();
        String hit = null;
        for (int i = 0; i < COMMON_WORDS.length; i++) {
            String w = COMMON_WORDS[i];
            String h = AcademyService.sha256Hex(w);
            boolean shown = i % 6 == 0;
            if (shown) addStep(box, String.format("[%02d] %-14s -> %s%s", i, w, h.substring(0, 16) + "\u2026",
                h.equals(hash) ? "   \u2713 MATCH" : ""), h.equals(hash) ? "#39FF14" : "#484f58");
            if (h.equals(hash)) { hit = w; break; }
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        addSep(box);
        if (hit != null) {
            addStep(box, "\uD83D\uDCA5 CRACKED: password = \"" + hit + "\"", "#39FF14");
            out.setText("Dictionary attack cracked \"" + hit + "\" in " + ms + " ms (" + (COMMON_WORDS.length) + " word entries hashed).");
        } else {
            addStep(box, "\u274C No hit in the bundled word list.", "#f85149");
            out.setText("No dictionary hit. The password is not in the top 24 common passwords \u2014 a larger wordlist would help.");
        }
    }

    private void simulateFreqAnalysis(String input, VBox box, Label out) {
        String plain = input.toUpperCase().replaceAll("[^A-Z]", "");
        if (plain.isBlank()) plain = "ATTACKATDAWN";
        java.util.Random r = new java.util.Random(plain.hashCode());
        String[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
        StringBuilder cipher = new StringBuilder();
        for (char c : plain.toCharArray()) {
            if (c == ' ') { cipher.append(' '); continue; }
            cipher.append(alphabet[(c - 'A' + r.nextInt(26)) % 26]);
        }
        int[] counts = new int[26];
        for (char c : plain.toCharArray()) counts[c - 'A']++;
        java.util.List<int[]> pairs = new java.util.ArrayList<>();
        for (int i = 0; i < 26; i++) pairs.add(new int[]{i, counts[i]});
        pairs.sort((a, b) -> Integer.compare(b[1], a[1]));
        addStep(box, "\u25B6 FREQUENCY ANALYSIS on ciphertext", "#f85149");
        addStep(box, "ciphertext = " + cipher, "#c9d1d9");
        addSep(box);
        addStep(box, "Encrypted frequency histogram (top 8):", "#58a6ff");
        for (int i = 0; i < 8; i++) {
            int[] p = pairs.get(i);
            char letter = (char) ('A' + p[0]);
            int bar = Math.round(p[1] * 20f / Math.max(1, pairs.get(0)[1]));
            addStep(box, String.format("  %c : %s (%d)", letter, "\u2588".repeat(Math.max(1, bar)), p[1]), "#39FF14");
        }
        addSep(box);
        addStep(box, "English expects: E T A O I N S H \u2014 map the most frequent cipher letters to these.", "#f78166");
        out.setText("The most frequent ciphertext letters are \"" + (char)('A' + pairs.get(0)[0])
            + "\" and \"" + (char)('A' + pairs.get(1)[0]) + "\" \u2014 likely E and T in a simple substitution.");
    }

    private void simulateChosenPlaintext(String input, VBox box, Label out) {
        String plain = input.toUpperCase().replaceAll("[^A-Z]", "");
        if (plain.isEmpty()) plain = "HELLOHELLOHELLOHELLOHELLO";
        addStep(box, "\u25B6 CHOSEN PLAINTEXT \u2014 ECB vs CBC", "#f85149");
        addSep(box);
        addStep(box, "Plaintext (chosen by attacker): " + plain, "#c9d1d9");
        StringBuilder ecb = new StringBuilder();
        StringBuilder cbc = new StringBuilder();
        java.util.Random r = new java.util.Random(42);
        byte prev = (byte) r.nextInt(256);
        int block = 0;
        for (int i = 0; i < plain.length(); i++) {
            byte p = (byte) plain.charAt(i);
            byte e = (byte) ((p + 3) % 26 + 'A');
            if (block < 2 && i / 16 > block) { addStep(box, "\u2500 block boundary \u2500", "#8b949e"); block++; }
            ecb.append((char) (e));
            byte c = (byte) (e ^ prev);
            cbc.append((char) ('A' + (c & 0x1F) % 26));
            prev = e;
        }
        addStep(box, "ECB ciphertext:  " + ecb, "#39FF14");
        addStep(box, "CBC ciphertext:  " + cbc, "#58a6ff");
        addSep(box);
        boolean sameEcb = ecb.length() >= 32 && ecb.substring(0, 16).equals(ecb.substring(16, 32));
        addStep(box, sameEcb
            ? "\uD83D\uDCA1 ECB leaks: blocks 1 and 2 are IDENTICAL \u2192 attacker knows the plaintext blocks repeat."
            : "\uD83D\uDCA1 ECB: identical plaintext blocks produce identical ciphertext blocks.",
            "#FFD700");
        addStep(box, "CBC chains each block through the previous ciphertext \u2014 identical blocks differ.", "#f78166");
        out.setText("Chosen plaintext exploits structural leaks: in ECB mode the attacker sees repeat patterns ("
            + (sameEcb ? "observed" : "verifiable with 32+ identical chars") + ").");
    }

    private void simulateKnownPlaintext(String input, VBox box, Label out) {
        String known = input.toUpperCase().replaceAll("[^A-Z]", "");
        if (known.length() < 2) known = "ATTACK";
        String key = "SECRETKEY";
        addStep(box, "\u25B6 KNOWN PLAINTEXT \u2014 recover repeating-XOR key", "#f85149");
        addSep(box);
        StringBuilder keyPad = new StringBuilder();
        for (int i = 0; i < known.length(); i++) keyPad.append(key.charAt(i % key.length()));
        StringBuilder cipher = new StringBuilder();
        for (int i = 0; i < known.length(); i++)
            cipher.append((char) (known.charAt(i) ^ keyPad.charAt(i)));
        addStep(box, "Plaintext : " + known, "#c9d1d9");
        addStep(box, "Ciphertext: " + cipher, "#c9d1d9");
        addSep(box);
        StringBuilder recovered = new StringBuilder();
        for (int i = 0; i < known.length(); i++) {
            char k = (char) (known.charAt(i) ^ cipher.charAt(i));
            recovered.append(k);
            if (i % 3 == 2) addStep(box, String.format("  key byte[%d] = P XOR C = %c", i, k), "#39FF14");
        }
        addSep(box);
        addStep(box, "\u2705 Recovered key fragment: \"" + recovered + "\u2026\"", "#FFD700");
        addStep(box, "With enough known plaintext the full repeating key is trivially recovered.", "#f78166");
        out.setText("Known plaintext gave the key bytes: " + recovered);
    }

    private void simulateReplay(String input, VBox box, Label out) {
        String req = input.isEmpty() ? "GRANT ACCESS admin=root" : input;
        addStep(box, "\u25B6 REPLAY ATTACK \u2014 capture & replay a session request", "#f85149");
        addSep(box);
        addStep(box, "[SNIFF] Eavesdropped wire capture:", "#58a6ff");
        addStep(box, "  > " + req + "   [SEQ 0x7A1F]", "#c9d1d9");
        addStep(box, "[SERVER] \u2705 ACCEPTED \u2014 session token valid, no nonce check.", "#39FF14");
        addSep(box);
        addStep(box, "[REPLAY] Attacker re-sends the exact same bytes...", "#f85149");
        addStep(box, "  > " + req + "   [SEQ 0x7A1F]  (replayed)", "#c9d1d9");
        addStep(box, "[SERVER] \u2705 ACCEPTED AGAIN \u2014 access granted a second time!", "#39FF14");
        addSep(box);
        addStep(box, "\uD83D\uDCA1 Mitigation: one-time nonces, timestamps, sequence numbers + replay detection.", "#FFD700");
        out.setText("Replay succeeded: the server accepted the identical request twice because it has no anti-replay protection.");
    }

    private void simulateMitm(String input, VBox box, Label out) {
        addStep(box, "\u25B6 MAN-IN-THE-MIDDLE \u2014 key exchange interception", "#f85149");
        addSep(box);
        addStep(box, "Setup: Alice and Bob try Diffie-Hellman over an insecure channel.", "#c9d1d9");
        addStep(box, "Eve sits between them and relays everything.", "#c9d1d9");
        addSep(box);
        addStep(box, "Alice ->(Eve)-> Bob   :  A = g^a (mod p)", "#39FF14");
        addStep(box, "Eve replaces it with  :  A' = g^e (mod p)", "#f85149");
        addStep(box, "Bob ->(Eve)-> Alice   :  B = g^b (mod p)", "#39FF14");
        addStep(box, "Eve replaces it with  :  B' = g^e (mod p)", "#f85149");
        addSep(box);
        addStep(box, "Alice computes k1 = B'^a = g^(ea)  \u2014 shared with Eve", "#f78166");
        addStep(box, "Bob   computes k2 = A'^b = g^(eb)  \u2014 shared with Eve", "#f78166");
        addStep(box, "Eve now decrypts, reads and re-encrypts every message in both sessions.", "#f85149");
        addSep(box);
        addStep(box, "\uD83D\uDCA1 Mitigation: authenticated key exchange (certificates / signatures).", "#FFD700");
        out.setText("MITM complete: Alice trusts k1, Bob trusts k2, Eve controls both keys.");
    }

    private void simulateRainbow(String input, VBox box, Label out) {
        String target = input.isEmpty() ? "VaultKeeper" : input;
        addStep(box, "\u25B6 RAINBOW TABLE \u2014 precomputed hash chains", "#f85149");
        addStep(box, "target password to crack: " + target, "#c9d1d9");
        addSep(box);
        java.util.Map<String, String> chainEnds = new java.util.HashMap<>();
        java.util.Random r = new java.util.Random(7);
        int rows = 8;
        for (int i = 0; i < rows; i++) {
            String start = COMMON_WORDS[r.nextInt(COMMON_WORDS.length)];
            String cur = start;
            StringBuilder chain = new StringBuilder();
            chain.append(start);
            for (int s = 0; s < 5; s++) {
                String h = AcademyService.sha256Hex(cur);
                int reduced = (Math.abs(h.hashCode()) + s * 31) % COMMON_WORDS.length;
                cur = COMMON_WORDS[reduced];
                chain.append(" \u2192 ").append(cur);
            }
            chainEnds.put(cur, start);
            addStep(box, String.format("chain %d: %s", i, chain), "#484f58");
        }
        addSep(box);
        String thash = AcademyService.sha256Hex(target);
        String hit = null;
        for (int s = 0; s < 5 && hit == null; s++) {
            String reduced = COMMON_WORDS[(Math.abs(thash.hashCode()) + s * 31) % COMMON_WORDS.length];
            addStep(box, "reduce(" + thash.substring(0, 12) + "\u2026) -> " + reduced, "#58a6ff");
            if (chainEnds.containsKey(reduced)) {
                String start = chainEnds.get(reduced);
                String walk = start;
                for (int i = 0; i < 5; i++) {
                    String h = AcademyService.sha256Hex(walk);
                    if (h.equals(thash)) { hit = walk; break; }
                    int red = (Math.abs(h.hashCode()) + i * 31) % COMMON_WORDS.length;
                    walk = COMMON_WORDS[red];
                }
                if (hit != null) break;
            }
        }
        addSep(box);
        if (hit != null) {
            addStep(box, "\uD83D\uDCA5 RAINBOW HIT: password = \"" + hit + "\"", "#39FF14");
            out.setText("Rainbow table lookup recovered \"" + hit + "\" \u2014 seconds, not years. Precomputation trades disk for time.");
        } else {
            addStep(box, "\u274C No chain hit for this hash.", "#f85149");
            out.setText("No rainbow hit \u2014 salted hashes make rainbow tables useless, which is exactly the point.");
        }
    }

    private void simulatePwdCrack(String input, VBox box, Label out) {
        String pwd = input.isEmpty() ? "P@ssw0rd!" : input;
        int len = pwd.length();
        int classes = 0;
        if (pwd.matches(".*[a-z].*")) classes++;
        if (pwd.matches(".*[A-Z].*")) classes++;
        if (pwd.matches(".*\\d.*")) classes++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) classes++;
        int strength = Math.min(4, classes);
        long keyspace = 1L;
        for (int i = 0; i < len; i++) {
            int pool = switch (classes) { case 0, 1 -> 26; case 2 -> 52; case 3 -> 62; default -> 95; };
            keyspace = Math.min(Long.MAX_VALUE, keyspace * pool);
        }
        long gpuRate = 10_000_000_000L;
        double seconds = keyspace / (double) gpuRate;
        addStep(box, "\u25B6 PASSWORD CRACKING \u2014 keyspace & GPU time", "#f85149");
        addStep(box, "password   : " + pwd, "#c9d1d9");
        addStep(box, "length     : " + len + " chars", "#c9d1d9");
        addStep(box, "char classes: " + classes + " (lower/upper/digit/symbol)", "#c9d1d9");
        addStep(box, "strength   : " + "\u2605".repeat(Math.max(1, strength)) + "\u2606".repeat(4 - Math.max(0, Math.min(4, strength))), "#FFD700");
        addSep(box);
        addStep(box, String.format("keyspace   : %.3g combinations", (double) keyspace), "#58a6ff");
        addStep(box, String.format("GPU rate   : %.1f billion guesses/s", gpuRate / 1e9), "#58a6ff");
        addStep(box, "est. time  : " + formatTime(seconds), strength >= 3 ? "#39FF14" : "#f85149");
        addSep(box);
        addStep(box, strength >= 4 ? "\uD83D\uDEE1\uFE0F VERDICT: cryptographically strong \u2014 multi-year crack time."
            : "\uD83D\uDCA5 VERDICT: crackable in " + formatTime(seconds) + ". Lengthen it and add another character class.",
            strength >= 4 ? "#39FF14" : "#f85149");
        out.setText("Estimated crack time at 10^10 guesses/s: " + formatTime(seconds));
    }

    private static String formatTime(double seconds) {
        if (seconds < 1) return "< 1 second";
        if (seconds < 60) return (int) seconds + " seconds";
        if (seconds < 3600) return String.format("%.1f minutes", seconds / 60);
        if (seconds < 86400) return String.format("%.1f hours", seconds / 3600);
        if (seconds < 31557600) return String.format("%.1f days", seconds / 86400);
        return String.format("%.1f years", seconds / 31557600);
    }

    private void simulateCollision(String input, VBox box, Label out) {
        int bits;
        try { bits = Integer.parseInt(input.trim()); } catch (Exception e) { bits = 24; }
        bits = Math.max(8, Math.min(40, bits));
        int mask = bits >= 32 ? -1 : (1 << bits) - 1;
        java.util.Map<Integer, String> seen = new java.util.HashMap<>();
        addStep(box, "\u25B6 BIRTHDAY COLLISION \u2014 truncate hash to " + bits + " bits", "#f85149");
        addStep(box, "birthday bound ~ 2^(" + bits + "/2) \u2248 " + (int) Math.pow(2, bits / 2.0) + " candidates", "#c9d1d9");
        addSep(box);
        int i = 0;
        Integer foundKey = null;
        String a = null, b = null;
        while (foundKey == null && i < 2_000_000) {
            String s = "msg-" + i;
            String h = AcademyService.sha256Hex(s);
            int k = (h.substring(0, 8).hashCode()) & mask;
            if (seen.containsKey(k)) {
                foundKey = k;
                a = seen.get(k);
                b = s;
                break;
            }
            seen.put(k, s);
            if (i % 200_000 == 0) addStep(box, String.format("  %d strings stored, %d slots filled...", i, seen.size()), "#484f58");
            i++;
        }
        addSep(box);
        if (foundKey != null) {
            addStep(box, "\uD83D\uDCA5 COLLISION FOUND after " + i + " attempts!", "#39FF14");
            addStep(box, "  " + a + "  -> trunc(" + bits + " bits) = " + Integer.toHexString(foundKey), "#c9d1d9");
            addStep(box, "  " + b + "  -> trunc(" + bits + " bits) = " + Integer.toHexString(foundKey), "#c9d1d9");
            addStep(box, "Two different inputs, identical truncated digest \u2014 that is a hash collision.", "#FFD700");
            out.setText("Collision in " + bits + "-bit truncated hash found after " + i + " candidates (bound ~" + (int) Math.pow(2, bits / 2.0) + ").");
        } else {
            addStep(box, "No collision within 2,000,000 candidates.", "#f85149");
            out.setText("No collision found in range \u2014 reduce the bit width to see the birthday effect sooner.");
        }
    }

    // ============================================================
    // CAREER MODE (ITEM 16)
    // ============================================================

    private void showCareerMode() {
        academyActive = true;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ACADEMY", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFC6 CAREER MODE", AcademyUi.GOLD, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.GOLD, 0.3));
        Label sub = AcademyUi.caption(
            "Climb from Script Kiddie to Cyber Legend across 16 ranks. Claim promotions to bank bonus XP and coins.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        int earned = academy.earnedCareerRank();
        int claimed = academy.getCareerRank();
        String[] cur = AcademyService.CAREER_RANKS[earned];

        VBox curCard = AcademyUi.cardAccent(AcademyUi.GOLD);
        curCard.getChildren().add(AcademyUi.section("\uD83D\uDCCD CURRENT RANK", AcademyUi.GOLD));
        Label big = AcademyUi.neon(cur[2] + " " + cur[1], AcademyUi.GOLD, 24);
        curCard.getChildren().add(big);
        curCard.getChildren().add(AcademyUi.text(claimed >= earned
            ? "Claimed. Keep solving to push toward the next rank."
            : "You qualify for this rank \u2014 claim your promotion to bank the bonus!", 13));
        int nextIdx = earned + 1;
        if (nextIdx < AcademyService.CAREER_RANKS.length) {
            int curXp = Integer.parseInt(cur[0]);
            int nextXp = Integer.parseInt(AcademyService.CAREER_RANKS[nextIdx][0]);
            ProgressBar pbar = new ProgressBar(Math.min(1.0, (double) (totalXP - curXp) / Math.max(1, nextXp - curXp)));
            pbar.setPrefWidth(Double.MAX_VALUE);
            pbar.setStyle("-fx-accent: #FFD700;");
            curCard.getChildren().addAll(pbar,
                AcademyUi.caption("Next: " + AcademyService.CAREER_RANKS[nextIdx][1] + "  \u2022  +"
                    + Math.max(0, nextXp - totalXP) + " XP", 12));
        } else {
            curCard.getChildren().add(AcademyUi.text("MAXIMUM CAREER RANK REACHED \u2014 legendary status.", 13));
        }
        Button claimBtn = AcademyUi.button(
            claimed < earned ? "\uD83C\uDFC6 CLAIM PROMOTION  (+" + Integer.parseInt(cur[0]) / 20 + " XP, +" + (10 + earned * 5) + " coins)"
                             : "\u2705 PROMOTION CLAIMED",
            claimed < earned ? "#1f6feb" : "#30363d", "#ffffff");
        claimBtn.setOnAction(e -> {
            int promoted = academy.promoteIfEligible();
            if (promoted >= 0) {
                totalXP = academy.getTotalXp();
                computeBadges();
                addLog("[CAREER] Promoted to " + AcademyService.CAREER_RANKS[promoted][1] + ".");
                showCareerMode();
            }
        });
        curCard.getChildren().add(claimBtn);
        main.getChildren().add(curCard);

        VBox ladder = AcademyUi.cardAccent(AcademyUi.BLUE);
        ladder.getChildren().add(AcademyUi.section("\uD83E\uDE84 RANK LADDER", AcademyUi.BLUE));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int col = 0, rowIdx = 0;
        for (int i = AcademyService.CAREER_RANKS.length - 1; i >= 0; i--) {
            String[] r = AcademyService.CAREER_RANKS[i];
            boolean isClaimed = i <= claimed;
            boolean isEarned = i <= earned;
            VBox cell = new VBox(4);
            cell.setPrefWidth(200);
            cell.setPrefHeight(88);
            cell.setStyle("-fx-background-color: " + (isClaimed ? "#1a3a2a" : isEarned ? "#12263a" : "#0d1117") + ";"
                + "-fx-padding: 10; -fx-border-color: " + (isClaimed ? "#FFD700" : isEarned ? "#39FF14" : "#30363d") + ";"
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-alignment: center;");
            Label icon = new Label(isClaimed || isEarned ? r[2] : "\uD83D\uDD12");
            icon.setStyle("-fx-font-size: 22px;");
            Label nm = new Label(r[1]);
            nm.setStyle("-fx-text-fill: " + (isClaimed ? "#FFD700" : isEarned ? "#39FF14" : "#484f58")
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center;");
            Label xp = new Label(r[0] + " XP");
            xp.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px; -fx-alignment: center;");
            cell.getChildren().addAll(icon, nm, xp);
            grid.add(cell, col, rowIdx);
            col++;
            if (col >= 4) { col = 0; rowIdx++; }
        }
        ladder.getChildren().add(grid);
        main.getChildren().add(ladder);

        VBox hist = AcademyUi.cardAccent(AcademyUi.PURPLE);
        hist.getChildren().add(AcademyUi.section("\uD83D\uDCC5 RANK HISTORY", AcademyUi.PURPLE));
        java.util.List<String> rankHist = academy.getRankHistory();
        if (rankHist.isEmpty()) {
            hist.getChildren().add(AcademyUi.caption("No promotions claimed yet. Claim your first rank to start the timeline.", 12));
        } else {
            for (String h : rankHist) {
                String[] parts = h.split("\\|");
                Label l = new Label("\u25B8 " + (parts.length == 2 ? parts[0] + "  (" + parts[1] + ")" : h));
                l.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
                hist.getChildren().add(l);
            }
        }
        main.getChildren().add(hist);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    // ============================================================
    // MULTIPLAYER (ITEM 17)
    // ============================================================

    private static String randomOpponent() {
        String[] foes = {"KaliQueen", "HexHacker", "ByteBoss", "RSA_Rob", "MorseMan", "NeonNadia", "VaultKeeper", "CipherCrew"};
        return foes[new java.util.Random().nextInt(foes.length)];
    }

    private void showMultiplayer() {
        academyActive = true;
        stopPvpTimer();
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ACADEMY", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());

        Label title = AcademyUi.neon("\uD83C\uDFAE MULTIPLAYER ARENA", AcademyUi.BLUE, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.BLUE, 0.3));
        Label sub = AcademyUi.caption(
            "Player vs Player \u2022 Challenge Rooms \u2022 Private Rooms \u2014 live timer, live score, winner's circle.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        HBox stats = new HBox(12);
        stats.getChildren().add(AcademyUi.statTile("\uD83C\uDFC6", String.valueOf(academy.getPvpWins()), "PVP WINS", AcademyUi.GREEN));
        stats.getChildren().add(AcademyUi.statTile("\uD83E\uDE99", String.valueOf(academy.getCoins()), "COINS", AcademyUi.GOLD));
        stats.getChildren().add(AcademyUi.statTile("\uD83C\uDFC1", String.valueOf(academy.getTournamentWins()), "CUP WINS", AcademyUi.PURPLE));
        main.getChildren().add(stats);

        VBox quick = AcademyUi.cardAccent(AcademyUi.GREEN);
        quick.getChildren().add(AcademyUi.section("\u26A1 QUICK MATCH", AcademyUi.GREEN));
        quick.getChildren().add(AcademyUi.text("First to win 3 rounds against a random rival. Live 60s timer per round.", 12));
        Button quickBtn = AcademyUi.button("\u2694\uFE0F START PVP", "#238636", "#ffffff");
        quickBtn.setOnAction(e -> startPvpMatch("QUICK MATCH", randomOpponent()));
        quick.getChildren().add(quickBtn);
        main.getChildren().add(quick);

        VBox rooms = AcademyUi.cardAccent(AcademyUi.PURPLE);
        rooms.getChildren().add(AcademyUi.section("\uD83D\uDEAA CHALLENGE ROOMS", AcademyUi.PURPLE));
        String[][] roomDefs = {
            {"Training Room", "KaliQueen"},
            {"Neon Arena", "HexHacker"},
            {"Vault Club", "VaultKeeper"},
            {"RSA Rumble", "RSA_Rob"},
            {"Cipher Club", "MorseMan"},
            {"Byte Boss Lair", "ByteBoss"}
        };
        for (String[] r : roomDefs) {
            HBox row = new HBox(10);
            Label nm = new Label("\uD83D\uDE80 " + r[0] + "  \u2014  vs " + r[1]);
            nm.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
            Button join = AcademyUi.button("JOIN", "#8957e5", "#ffffff");
            join.setOnAction(e -> startPvpMatch(r[0], r[1]));
            row.getChildren().addAll(nm, AcademyUi.spacer(), join);
            rooms.getChildren().add(row);
        }
        main.getChildren().add(rooms);

        VBox priv = AcademyUi.cardAccent(AcademyUi.GOLD);
        priv.getChildren().add(AcademyUi.section("\uD83D\uDD12 PRIVATE ROOM", AcademyUi.GOLD));
        priv.getChildren().add(AcademyUi.text("Generate an invite code, share it with a friend, and duel in a locked room.", 12));
        HBox privRow = new HBox(10);
        TextField roomName = new TextField("My Private Duel");
        roomName.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");
        Button createBtn = AcademyUi.button("\uD83C\uDF10 CREATE", "#1f6feb", "#ffffff");
        Label codeLab = new Label("");
        codeLab.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");
        privRow.getChildren().addAll(roomName, createBtn, codeLab);
        createBtn.setOnAction(e -> {
            String code = "UC-" + String.format("%04X", new java.util.Random().nextInt(0xFFFF));
            codeLab.setText("Invite code: " + code + "  \u2022  " + "KaliQueen accepted!");
            startPvpMatch(roomName.getText().trim().isEmpty() ? "PRIVATE ROOM" : roomName.getText().trim(), randomOpponent());
        });
        priv.getChildren().add(privRow);
        main.getChildren().add(priv);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private void startPvpMatch(String room, String opponent) {
        stopPvpTimer();
        pvpRoom = room;
        pvpOpponent = opponent;
        playerScore = 0;
        oppScore = 0;
        pvpRound = 0;
        pvpRounds = 3;
        pvpActive = true;
        nextPvpRound();
    }

    private void nextPvpRound() {
        if (!pvpActive) return;
        if (pvpRound >= pvpRounds) { endPvpMatch(); return; }
        pvpRound++;
        pvpChallenge = academy.generateChallenge("EASY");
        pvpSecondsLeft = 60;
        pvpAnswered = false;
        renderPvpRound();
        scheduleOpponentMove();
    }

    private void scheduleOpponentMove() {
        int delayMs = 8000 + new java.util.Random().nextInt(14000);
        Thread t = new Thread(() -> {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { return; }
            Platform.runLater(() -> {
                if (!pvpActive || pvpAnswered || pvpSecondsLeft <= 0) return;
                boolean oppGot = new java.util.Random().nextInt(100) < 70;
                if (oppGot) {
                    int pts = 80 + Math.max(0, pvpSecondsLeft);
                    oppScore += pts;
                    addStep(pvpStepsBox, "\u26A1 " + pvpOpponent + " solved it \u2014 +" + pts + " pts", "#f78166");
                    addLog("[PVP] " + pvpOpponent + " answered correctly.");
                }
                nextPvpRound();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void renderPvpRound() {
        academyActive = true;
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Button quitBtn = AcademyUi.button("\u274C FORFEIT", "#da3633", "#ffffff");
        quitBtn.setOnAction(e -> { pvpActive = false; stopPvpTimer(); showMultiplayer(); });

        Label roomLab = AcademyUi.neon("\uD83C\uDFAE " + pvpRoom + "  \u2014  Round " + pvpRound + "/" + pvpRounds, AcademyUi.BLUE, 16);
        pvpScoreLab = AcademyUi.neon("\uD83D\uDC64 YOU " + playerScore + "   \u2014   " + oppScore + " " + pvpOpponent, AcademyUi.GOLD, 15);
        pvpClockLab = new Label("60s");
        pvpClockLab.setStyle("-fx-text-fill: #f85149; -fx-font-size: 26px; -fx-font-weight: bold;");

        VBox chCard = AcademyUi.cardAccent(AcademyUi.GREEN);
        chCard.getChildren().add(AcademyUi.pill(pvpChallenge.diff + " \u2022 " + pvpChallenge.family, AcademyUi.GREEN));
        Label descr = AcademyUi.text(pvpChallenge.descr, 13);
        descr.setStyle(descr.getStyle() + " -fx-font-family: 'Courier New';");
        chCard.getChildren().add(descr);

        pvpAnswer = new TextField();
        pvpAnswer.setPromptText("Enter flag...");
        pvpAnswer.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-font-family: 'Courier New';");
        pvpAnswer.setOnAction(e -> submitPvpAnswer());

        Button submitBtn = AcademyUi.button("\u26A1 SUBMIT", "#1f6feb", "#ffffff");
        submitBtn.setOnAction(e -> submitPvpAnswer());

        pvpStepsBox = new VBox(6);
        pvpStepsBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 14; -fx-border-color: #30363d; -fx-border-radius: 6;");

        HBox row = new HBox(10, pvpAnswer, submitBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(12, quitBtn, roomLab, pvpScoreLab, pvpClockLab, chCard, row, pvpStepsBox);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(700);
        main.getChildren().add(scroll);
        setCenter(main);

        startPvpClock();
    }

    private void startPvpClock() {
        stopPvpTimer();
        pvpTimer = new AnimationTimer() {
            private long last = -1;
            @Override public void handle(long now) {
                if (last < 0) { last = now; return; }
                long deltaMs = (now - last) / 1_000_000;
                last = now;
                if (!pvpActive) { stop(); return; }
                if (deltaMs >= 1000) {
                    pvpSecondsLeft = Math.max(0, pvpSecondsLeft - (int) (deltaMs / 1000));
                    if (pvpClockLab != null) {
                        pvpClockLab.setText(pvpSecondsLeft + "s");
                        pvpClockLab.setStyle("-fx-text-fill: " + (pvpSecondsLeft <= 10 ? "#f85149" : "#39FF14")
                            + "; -fx-font-size: 26px; -fx-font-weight: bold;");
                    }
                    if (pvpSecondsLeft <= 0) {
                        stop();
                        addStep(pvpStepsBox, "\u23F0 Time up \u2014 no points this round.", "#8b949e");
                        nextPvpRound();
                    }
                }
            }
        };
        pvpTimer.start();
    }

    private void stopPvpTimer() {
        if (pvpTimer != null) { pvpTimer.stop(); pvpTimer = null; }
    }

    private void submitPvpAnswer() {
        if (!pvpActive || pvpAnswered || pvpChallenge == null) return;
        String ans = pvpAnswer.getText().trim().toUpperCase();
        if (ans.isEmpty()) return;
        pvpAnswered = true;
        stopPvpTimer();
        if (ans.equals(pvpChallenge.flag)) {
            int pts = 100 + Math.max(0, pvpSecondsLeft);
            playerScore += pts;
            addStep(pvpStepsBox, "\u2705 CORRECT \u2014 +" + pts + " pts (" + pvpSecondsLeft + "s left)", "#39FF14");
            academy.onSolve(pvpChallenge.id, pvpChallenge.xp);
            computeBadges();
            addLog("[PVP] Correct! +" + pts + " points.");
        } else {
            addStep(pvpStepsBox, "\u274C WRONG \u2014 " + pvpOpponent + " takes the round.", "#f85149");
            oppScore += 60;
        }
        Thread t = new Thread(() -> {
            try { Thread.sleep(1300); } catch (InterruptedException e) { return; }
            Platform.runLater(this::nextPvpRound);
        });
        t.setDaemon(true);
        t.start();
    }

    private void endPvpMatch() {
        pvpActive = false;
        stopPvpTimer();
        boolean win = playerScore > oppScore;
        boolean tie = playerScore == oppScore;
        if (win) {
            academy.addPvpWin();
            academy.awardXp(80 + pvpRounds * 20);
            academy.addCoins(25 + pvpRounds * 5);
            totalXP = academy.getTotalXp();
            computeBadges();
        } else if (tie) {
            academy.addCoins(15);
        } else {
            academy.addCoins(10);
        }
        showPvpWinner(win, tie);
    }

    private void showPvpWinner(boolean win, boolean tie) {
        academyActive = true;
        VBox main = new VBox(16);
        main.setPadding(new Insets(30));
        main.setStyle(BG_DARK);
        main.setAlignment(Pos.CENTER);

        Label trophy = new Label(win ? "\uD83C\uDFC6" : tie ? "\uD83E\uDD1D" : "\uD83D\uDC4D");
        trophy.setStyle("-fx-font-size: 84px;");
        Label verdict = AcademyUi.neon(
            win ? "VICTORY" : tie ? "DRAW" : "DEFEAT",
            win ? AcademyUi.GOLD : tie ? AcademyUi.BLUE : AcademyUi.RED, 34);
        Label scoreLab = AcademyUi.text("YOU " + playerScore + "  \u2014  " + oppScore + " " + pvpOpponent, 18);
        Label rewardLab = AcademyUi.caption(win
            ? "Rewards: +" + (80 + pvpRounds * 20) + " XP, +" + (25 + pvpRounds * 5) + " coins"
            : tie ? "Rewards: +15 coins (draw bonus)"
                  : "Rewards: +10 coins (participation)", 13);

        Button rematch = AcademyUi.button("\uD83D\uDD04 REMATCH", "#1f6feb", "#ffffff");
        rematch.setOnAction(e -> startPvpMatch(pvpRoom, randomOpponent()));
        Button back = AcademyUi.button("\u2B05 BACK TO ARENA", "#30363d", AcademyUi.LIGHT);
        back.setOnAction(e -> showMultiplayer());

        HBox btns = new HBox(12, rematch, back);
        btns.setAlignment(Pos.CENTER);
        main.getChildren().addAll(trophy, verdict, scoreLab, rewardLab, btns);
        AcademyUi.glow(main, javafx.scene.paint.Color.web(win ? AcademyUi.GOLD : AcademyUi.RED, 0.3));
        setCenter(main);
    }

    // ============================================================
    // TOURNAMENTS (ITEM 18)
    // ============================================================

    private void showTournaments() {
        academyActive = true;
        stopCupTimer();
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO ACADEMY", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAcademyDashboard());
        Label title = AcademyUi.neon("\uD83C\uDFC6 TOURNAMENTS", AcademyUi.GOLD, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.GOLD, 0.3));
        Label sub = AcademyUi.caption(
            "Weekly Cups \u2022 Monthly Cups \u2022 Season Championships \u2014 climb the bracket and collect XP, badges, coins and certificates.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        HBox rewards = new HBox(12);
        rewards.getChildren().add(AcademyUi.statTile("\u26A1", String.valueOf(academy.getTotalXp()), "TOTAL XP", AcademyUi.GOLD));
        rewards.getChildren().add(AcademyUi.statTile("\uD83C\uDFC1", String.valueOf(academy.getTournamentWins()), "CUPS WON", AcademyUi.PURPLE));
        rewards.getChildren().add(AcademyUi.statTile("\uD83E\uDE99", String.valueOf(academy.getCoins()), "COINS", AcademyUi.GOLD));
        rewards.getChildren().add(AcademyUi.statTile("\uD83C\uDF96\uFE0F", String.valueOf(academy.getCertPoints()), "CERT POINTS", AcademyUi.BLUE));
        main.getChildren().add(rewards);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(buildCupCard("WEEKLY", "\uD83C\uDFC6", "Weekly Cup", "3 qualifying challenges. Top 3 win XP + coins.", "#1f6feb"), 0, 0);
        grid.add(buildCupCard("MONTHLY", "\uD83C\uDF1F", "Monthly Cup", "5 challenges, stiffer rivals, bigger rewards.", "#8957e5"), 1, 0);
        grid.add(buildCupCard("SEASON", "\uD83D\uDC51", "Season Championship", "3-stage bracket. Champion earns a badge + certificate points.", "#FFD700"), 2, 0);
        main.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node buildCupCard(String type, String icon, String name, String descr, String accent) {
        VBox card = AcademyUi.cardAccent(accent);
        card.setPrefWidth(330);
        card.getChildren().add(AcademyUi.section(icon + " " + name, accent));
        card.getChildren().add(AcademyUi.text(descr, 12));
        Button enter = AcademyUi.button("\uD83C\uDFC6 ENTER " + name.toUpperCase(), accent, "#ffffff");
        enter.setOnAction(e -> startCup(type));
        card.getChildren().add(enter);
        return card;
    }

    private void startCup(String type) {
        stopCupTimer();
        cupType = type;
        cupScore = 0;
        cupRound = 0;
        cupRounds = switch (type) {
            case "WEEKLY" -> 3;
            case "MONTHLY" -> 5;
            default -> 3;
        };
        cupActive = true;
        nextCupRound();
    }

    private void nextCupRound() {
        if (!cupActive) return;
        if (cupRound >= cupRounds) { endCup(); return; }
        cupRound++;
        cupChallenge = academy.generateChallenge("MEDIUM");
        cupSecondsLeft = 60;
        renderCupRound();
    }

    private void renderCupRound() {
        academyActive = true;
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle(BG_DARK);

        Button quitBtn = AcademyUi.button("\u274C FORFEIT", "#da3633", "#ffffff");
        quitBtn.setOnAction(e -> { cupActive = false; stopCupTimer(); showTournaments(); });

        Label cupLab = AcademyUi.neon("\uD83C\uDFC6 " + cupType + " CUP \u2014 Challenge " + cupRound + "/" + cupRounds, AcademyUi.GOLD, 16);
        Label scoreLab = AcademyUi.neon("\uD83C\uDFAF Cup score: " + cupScore, AcademyUi.GREEN, 15);
        Label clockLab = new Label("60s");
        clockLab.setStyle("-fx-text-fill: #f85149; -fx-font-size: 26px; -fx-font-weight: bold;");

        VBox chCard = AcademyUi.cardAccent(AcademyUi.GOLD);
        chCard.getChildren().add(AcademyUi.pill(cupChallenge.diff + " \u2022 " + cupChallenge.family, AcademyUi.GOLD));
        Label descr = AcademyUi.text(cupChallenge.descr, 13);
        descr.setStyle(descr.getStyle() + " -fx-font-family: 'Courier New';");
        chCard.getChildren().add(descr);

        TextField answer = new TextField();
        answer.setPromptText("Enter flag...");
        answer.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #30363d; -fx-font-family: 'Courier New';");
        answer.setOnAction(e -> submitCupAnswer(answer));

        Button submitBtn = AcademyUi.button("\u26A1 SUBMIT", "#1f6feb", "#ffffff");
        submitBtn.setOnAction(e -> submitCupAnswer(answer));

        VBox stepsBox = new VBox(6);
        stepsBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 14; -fx-border-color: #30363d; -fx-border-radius: 6;");

        HBox row = new HBox(10, answer, submitBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(12, quitBtn, cupLab, scoreLab, clockLab, chCard, row, stepsBox);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(700);
        main.getChildren().add(scroll);
        setCenter(main);

        Label clockRef = clockLab;
        stopCupTimer();
        cupTimer = new AnimationTimer() {
            private long last = -1;
            @Override public void handle(long now) {
                if (last < 0) { last = now; return; }
                long deltaMs = (now - last) / 1_000_000;
                last = now;
                if (!cupActive) { stop(); return; }
                if (deltaMs >= 1000) {
                    cupSecondsLeft = Math.max(0, cupSecondsLeft - (int) (deltaMs / 1000));
                    clockRef.setText(cupSecondsLeft + "s");
                    clockRef.setStyle("-fx-text-fill: " + (cupSecondsLeft <= 10 ? "#f85149" : "#39FF14")
                        + "; -fx-font-size: 26px; -fx-font-weight: bold;");
                    if (cupSecondsLeft <= 0) {
                        stop();
                        addStep(stepsBox, "\u23F0 Time up \u2014 no points this round.", "#8b949e");
                        nextCupRound();
                    }
                }
            }
        };
        cupTimer.start();
    }

    private void stopCupTimer() {
        if (cupTimer != null) { cupTimer.stop(); cupTimer = null; }
    }

    private void submitCupAnswer(TextField answer) {
        if (!cupActive || cupChallenge == null) return;
        stopCupTimer();
        String ans = answer.getText().trim().toUpperCase();
        if (ans.equals(cupChallenge.flag)) {
            int pts = 100 + Math.max(0, cupSecondsLeft);
            cupScore += pts;
            academy.onSolve(cupChallenge.id, cupChallenge.xp);
            computeBadges();
            addLog("[CUP] Correct! +" + pts + " cup points.");
        }
        Thread t = new Thread(() -> {
            try { Thread.sleep(900); } catch (InterruptedException e) { return; }
            Platform.runLater(this::nextCupRound);
        });
        t.setDaemon(true);
        t.start();
    }

    private void endCup() {
        cupActive = false;
        stopCupTimer();
        String day = java.time.LocalDate.now().toString();
        java.util.Random r = new java.util.Random((operatorID + "|" + cupType + "|" + day).hashCode());
        int maxBot = switch (cupType) {
            case "WEEKLY" -> 300;
            case "MONTHLY" -> 500;
            default -> 700;
        };
        java.util.List<Integer> scores = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) scores.add(r.nextInt(maxBot + 1));
        scores.add(cupScore);
        scores.sort(java.util.Collections.reverseOrder());
        int placement = scores.indexOf(cupScore) + 1;

        int[] xpReward = switch (cupType) {
            case "WEEKLY" -> new int[]{300, 150, 80, 30};
            case "MONTHLY" -> new int[]{500, 250, 120, 40};
            default -> new int[]{700, 350, 160, 60};
        };
        int[] coinReward = switch (cupType) {
            case "WEEKLY" -> new int[]{100, 50, 25, 10};
            case "MONTHLY" -> new int[]{200, 100, 50, 20};
            default -> new int[]{300, 150, 75, 30};
        };
        int idx = Math.min(placement, 4) - 1;
        int xp = xpReward[Math.min(idx, 3)];
        int coins = coinReward[Math.min(idx, 3)];
        int cert = 0;
        boolean first = placement == 1;
        if (first) {
            academy.addTournamentWin();
            if (cupType.equals("SEASON")) cert = 50;
            else cert = 20;
        }
        academy.awardXp(xp);
        academy.addCoins(coins);
        if (cert > 0) academy.addCertPoints(cert);
        totalXP = academy.getTotalXp();
        computeBadges();
        showCupResult(placement, xp, coins, cert, first);
    }

    private void showCupResult(int placement, int xp, int coins, int cert, boolean first) {
        academyActive = true;
        VBox main = new VBox(16);
        main.setPadding(new Insets(30));
        main.setStyle(BG_DARK);
        main.setAlignment(Pos.CENTER);

        String placeText = switch (placement) {
            case 1 -> "1st \u2014 CHAMPION";
            case 2 -> "2nd \u2014 RUNNER UP";
            case 3 -> "3rd \u2014 PODIUM";
            default -> placement + "th";
        };
        Label trophy = new Label(first ? "\uD83C\uDFC6" : placement <= 3 ? "\uD83E\uDD47" : "\uD83D\uDCCA");
        trophy.setStyle("-fx-font-size: 84px;");
        Label verdict = AcademyUi.neon(cupType + " CUP " + placeText, first ? AcademyUi.GOLD : AcademyUi.BLUE, 26);
        Label scoreLab = AcademyUi.text("Your final cup score: " + cupScore + " points", 16);
        Label rewardLab = AcademyUi.caption("Rewards: +" + xp + " XP, +" + coins + " coins"
            + (cert > 0 ? ", +" + cert + " cert pts" : "") + (first ? ", cup badge unlocked" : ""), 13);

        Button again = AcademyUi.button("\uD83D\uDD04 PLAY AGAIN", "#1f6feb", "#ffffff");
        again.setOnAction(e -> startCup(cupType));
        Button back = AcademyUi.button("\u2B05 BACK TO TOURNAMENTS", "#30363d", AcademyUi.LIGHT);
        back.setOnAction(e -> showTournaments());
        HBox btns = new HBox(12, again, back);
        btns.setAlignment(Pos.CENTER);
        main.getChildren().addAll(trophy, verdict, scoreLab, rewardLab, btns);
        setCenter(main);
    }

    // ============================================================
    // ADMIN ANALYTICS (ITEM 19)
    // ============================================================

    private void showAdminAnalytics() {
        if (!LoginScreen.USER_ROLE.equalsIgnoreCase("ADMIN")) {
            addLog("[DENIED] Admin Analytics is reserved for ADMIN operators.");
            return;
        }
        academyActive = false;
        stopMissionClock();
        stopChallengeClock();
        VBox main = new VBox(16);
        main.setPadding(new Insets(24));
        main.setStyle(BG_DARK);

        Button backBtn = AcademyUi.button("\u2B05 BACK TO DASHBOARD", "#30363d", AcademyUi.LIGHT);
        backBtn.setOnAction(e -> showAESModule());
        Label title = AcademyUi.neon("\uD83D\uDCCA PROFESSIONAL ADMIN ANALYTICS", AcademyUi.RED, 22);
        AcademyUi.glow(title, javafx.scene.paint.Color.web(AcademyUi.RED, 0.3));
        Label sub = AcademyUi.caption(
            "Platform-wide telemetry: users, challenges, retention, countries, categories and heatmaps.", 12);
        main.getChildren().addAll(backBtn, title, sub);

        HBox tiles = new HBox(12);
        tiles.getChildren().add(AcademyUi.statTile("\uD83D\uDC64", "12,482", "TOTAL USERS", AcademyUi.BLUE));
        tiles.getChildren().add(AcademyUi.statTile("\uD83D\uDDFF\uFE0F", "1,204", "DAILY ACTIVE", AcademyUi.GREEN));
        tiles.getChildren().add(AcademyUi.statTile("\uD83C\uDD95", "186", "NEW TODAY", AcademyUi.GOLD));
        tiles.getChildren().add(AcademyUi.statTile("\uD83D\uDD04", "68%", "RETENTION", AcademyUi.PURPLE));
        tiles.getChildren().add(AcademyUi.statTile("\uD83C\uDFAF", String.format("%.0f%%", academy.getSuccessRate()), "AVG SCORE", AcademyUi.ORANGE));
        tiles.getChildren().add(AcademyUi.statTile("\u23F1\uFE0F", String.format("%.1f s", Math.max(5.0, academy.getPersonalBestMs() / 1000.0)), "AVG SOLVE TIME", AcademyUi.RED));
        main.getChildren().add(tiles);

        GridPane charts = new GridPane();
        charts.setHgap(16);
        charts.setVgap(16);

        charts.add(analyticsChart("DAILY ACTIVE USERS (7 DAYS)",
            new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"},
            new double[]{980, 1120, 1055, 1204, 1380, 1520, 1204}, "#39FF14", "line"), 0, 0);
        charts.add(analyticsChart("MONTHLY ACTIVE USERS (6 MONTHS)",
            new String[]{"Feb", "Mar", "Apr", "May", "Jun", "Jul"},
            new double[]{7200, 8100, 9400, 10200, 11200, 12482}, "#58a6ff", "bar"), 1, 0);
        charts.add(analyticsChart("NEW USERS PER MONTH",
            new String[]{"Feb", "Mar", "Apr", "May", "Jun", "Jul"},
            new double[]{210, 320, 410, 380, 520, 480}, "#FFD700", "bar"), 0, 1);
        charts.add(analyticsChart("CHALLENGE COMPLETION BY DIFFICULTY",
            new String[]{"EASY", "MEDIUM", "HARD", "EXPERT"},
            new double[]{94, 71, 43, 18}, "#8957e5", "bar"), 1, 1);

        main.getChildren().add(charts);

        HBox lower = new HBox(16);
        VBox failed = AcademyUi.cardAccent(AcademyUi.RED);
        failed.setPrefWidth(340);
        failed.getChildren().add(AcademyUi.section("\uD83D\uDCA9 MOST FAILED CHALLENGES", AcademyUi.RED));
        failed.getChildren().add(failRow("hill_5x5_advanced", 87));
        failed.getChildren().add(failRow("rsa_large_prime", 81));
        failed.getChildren().add(failRow("aes_cbc_decrypt", 74));
        failed.getChildren().add(failRow("stego_lsb_extract", 68));
        failed.getChildren().add(failRow("transposition_route", 61));

        VBox lb = AcademyUi.cardAccent(AcademyUi.GOLD);
        lb.setPrefWidth(340);
        lb.getChildren().add(AcademyUi.section("\uD83C\uDFC6 LEADERBOARD ANALYTICS", AcademyUi.GOLD));
        java.util.List<Standing> standings = academy.getGlobalStandings(6);
        for (Standing s : standings) {
            Label l = new Label(String.format("#%d  %s %-20s %d XP",
                s.rank, s.avatar, s.name, s.xp));
            l.setStyle("-fx-text-fill: " + (s.me ? "#39FF14" : "#c9d1d9") + "; -fx-font-size: 11px; -fx-font-family: 'Courier New';");
            lb.getChildren().add(l);
        }

        VBox countries = AcademyUi.cardAccent(AcademyUi.BLUE);
        countries.setPrefWidth(340);
        countries.getChildren().add(AcademyUi.section("\uD83D\uDDFA\uFE0F COUNTRY STATISTICS", AcademyUi.BLUE));
        String[] ctry = {"Tanzania", "Kenya", "Nigeria", "USA", "UK", "India", "Germany", "Brazil"};
        int[] ctryVal = {1840, 1520, 1310, 980, 820, 640, 510, 460};
        for (int i = 0; i < ctry.length; i++) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label c = new Label(countryFlag(ctry[i]) + " " + ctry[i]);
            c.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 11px;");
            ProgressBar pb = new ProgressBar(ctryVal[i] / 2000.0);
            pb.setPrefWidth(120);
            pb.setStyle("-fx-accent: #58a6ff;");
            Label v = new Label(String.valueOf(ctryVal[i]));
            v.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px;");
            row.getChildren().addAll(c, pb, v);
            countries.getChildren().add(row);
        }
        lower.getChildren().addAll(failed, lb, countries);
        main.getChildren().add(lower);

        VBox heat = AcademyUi.cardAccent(AcademyUi.GREEN);
        heat.getChildren().add(AcademyUi.section("\uD83D\uDD22 ACTIVITY HEATMAP \u2014 7 DAYS \u00D7 24 HOURS", AcademyUi.GREEN));
        heat.getChildren().add(buildHeatmap());
        main.getChildren().add(heat);

        ScrollPane scroll = new ScrollPane(main);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefViewportHeight(720);
        setCenter(scroll);
    }

    private javafx.scene.Node failRow(String name, int pct) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label c = new Label(name);
        c.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 10px; -fx-font-family: 'Courier New';");
        ProgressBar pb = new ProgressBar(pct / 100.0);
        pb.setPrefWidth(110);
        pb.setStyle("-fx-accent: #f85149;");
        Label v = new Label(pct + "% fail");
        v.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px;");
        row.getChildren().addAll(c, pb, v);
        return row;
    }

    private javafx.scene.Node analyticsChart(String caption, String[] labels, double[] values, String color, String kind) {
        VBox card = AcademyUi.cardAccent(color);
        card.getChildren().add(AcademyUi.section(caption, color));
        javafx.scene.canvas.Canvas cv = new javafx.scene.canvas.Canvas(420, 150);
        javafx.scene.canvas.GraphicsContext g = cv.getGraphicsContext2D();
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, cv.getWidth(), cv.getHeight());
        double max = 1;
        for (double v : values) max = Math.max(max, v);
        if (kind.equals("line")) {
            g.setStroke(Color.web(color));
            g.setLineWidth(2);
            double step = cv.getWidth() / (labels.length - 1);
            for (int i = 0; i < values.length; i++) {
                double x = 10 + i * step;
                double y = 130 - (values[i] / max) * 110;
                if (i == 0) { g.beginPath(); g.moveTo(x, y); } else g.lineTo(x, y);
                g.setFill(Color.web(color));
                g.fillOval(x - 3, y - 3, 6, 6);
            }
            g.stroke();
            g.setFill(Color.web("#8b949e"));
            g.setFont(javafx.scene.text.Font.font("Monospace", 10));
            for (int i = 0; i < labels.length; i++) {
                double x = 10 + i * step;
                g.fillText(labels[i], x - 12, 145);
            }
        } else {
            double slot = cv.getWidth() / labels.length;
            double bw = slot * 0.6;
            for (int i = 0; i < values.length; i++) {
                double x = i * slot + (slot - bw) / 2;
                double h = (values[i] / max) * 110;
                g.setFill(Color.web(color));
                g.fillRoundRect(x, 130 - h, bw, h, 4, 4);
                g.setFill(Color.web("#8b949e"));
                g.setFont(javafx.scene.text.Font.font("Monospace", 10));
                g.fillText(labels[i], x + bw / 2 - 12, 145);
                g.fillText(String.valueOf((int) values[i]), x + bw / 2 - 10, 125 - h);
            }
        }
        card.getChildren().add(cv);
        return card;
    }

    private javafx.scene.Node buildHeatmap() {
        javafx.scene.canvas.Canvas cv = new javafx.scene.canvas.Canvas(760, 96);
        javafx.scene.canvas.GraphicsContext g = cv.getGraphicsContext2D();
        java.util.Random r = new java.util.Random((operatorID + "|heatmap").hashCode());
        g.setFill(Color.web("#0d1117"));
        g.fillRect(0, 0, cv.getWidth(), cv.getHeight());
        int cell = 20;
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 24; col++) {
                double base = r.nextDouble();
                int hot = (col >= 8 && col <= 22) ? 1 : 0;
                int level = (int) (base * 0.75 + hot * 0.25 * 10);
                if (row == 5 && col == 12) level = 10;
                String colr = switch (Math.min(10, level)) {
                    case 10, 9 -> "#39FF14";
                    case 8, 7 -> "#4cbb17";
                    case 6, 5 -> "#6a9955";
                    case 4, 3 -> "#2b463c";
                    default -> "#141b24";
                };
                g.setFill(Color.web(colr));
                g.fillRoundRect(16 + col * (cell + 4), 10 + row * (cell + 4), cell, cell, 4, 4);
            }
        }
        g.setFill(Color.web("#8b949e"));
        g.setFont(javafx.scene.text.Font.font("Monospace", 9));
        for (int col = 0; col < 24; col += 4) g.fillText(col + ":00", 16 + col * 24, 8);
        return cv;
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
            createMenuBtn("🎓 ACADEMY", e -> showAcademyDashboard()),
            createMenuBtn("\uD83D\uDCC2 CATEGORIES", e -> showCategoryPage()),
            createMenuBtn("\uD83C\uDFC5 CERTIFICATES", e -> showCertificates()),
            createMenuBtn("\uD83E\uDDEA CRYPTO LAB", e -> showCryptoLab()),
            createMenuBtn("\uD83C\uDFAC VISUAL LEARNING", e -> showVisualLearning()),
            createMenuBtn("\uD83E\uDD16 AI MENTOR", e -> showAiMentor()),
            new Separator(),
            createMenuBtn("\uD83D\uDC79 ATTACK SIMULATOR", e -> showAttackSimulator()),
            createMenuBtn("\uD83C\uDFC6 CAREER MODE", e -> showCareerMode()),
            createMenuBtn("\uD83C\uDFAE MULTIPLAYER", e -> showMultiplayer()),
            createMenuBtn("\uD83C\uDFC6 TOURNAMENTS", e -> showTournaments()),
            createMenuBtn("\uD83D\uDC64 PROFILE", e -> showProfile())
        );

        sidebar.getChildren().add(new Separator());

if (LoginScreen.USER_ROLE.equalsIgnoreCase("ADMIN")) {
        
        // Ongeza mstari wa kutenganisha (Separator)
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363d;");
        
        Button adminPanelBtn = new Button("🛠 ADMIN ANALYTICS");
        adminPanelBtn.setMaxWidth(Double.MAX_VALUE);
        adminPanelBtn.setPrefHeight(40);
        // Rangi nyekundu ili ionekane ni sehemu ya hatari/nguvu (Power)
        adminPanelBtn.setStyle("-fx-background-color: #f85149; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        // Hapa ndipo unapofungua hiyo dashboard ya analytics
        adminPanelBtn.setOnAction(e -> { academyActive = false; showAdminAnalytics(); }); 
        
        sidebar.getChildren().addAll(sep, adminPanelBtn);
    }

    return sidebar;
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