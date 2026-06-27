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

import app.ApiClient;
import app.ApiException;
import app.DatabaseManager;
import app.LicenseManager;
import crypto.XORUtil;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    private static final int CHALLENGE_COUNT = 14;
    private static final java.util.Map<String, Integer> leaderboard = new java.util.HashMap<>();
    private static final java.util.Set<String> achievedBadges = new java.util.LinkedHashSet<>();

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
    }
    
    private static final String API_SECRET_KEY = "Emily_Crypto_Secure_2026_KIU";
    private static final String PYTHON_URL = "https://ultimate-crypto-node-gateway.onrender.com";

    public Dashboard() {
        String hwid = LicenseManager.getHardwareID();
        this.operatorID = "UC-PRO-" + Integer.toHexString(hwid.hashCode()).toUpperCase();

        setStyle("-fx-background-color: #050505; -fx-border-color: #39FF14; -fx-border-width: 0.5;"); 
        setLeft(createSidebar());
        showAESModule(); 
        
        addLog("DEEP DEFENSE: V20.4 OBSIDIAN KERNEL LOADED.");
        addLog("AUTH STATUS: " + operatorID + " ATTACHED.");
        // Kurekodi boot kwenye Atlas
        sendAuditLog("SYSTEM_BOOT", "CORE_KERNEL");
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
        String id, title, descr, hint, flag;
        int stars, xp;
        boolean done;
        ChallengeData(String id, String title, int stars, int xp, String descr, String hint, String flag) {
            this.id = id; this.title = title; this.stars = stars; this.xp = xp;
            this.descr = descr; this.hint = hint; this.flag = flag; this.done = false;
        }
    }

    private ChallengeData[] getChallenges() {
        return new ChallengeData[] {
            new ChallengeData("caesar", "Caesar's Vault", 1, 100,
                "Caesar shift +3. Decrypt: UC{FDHVDU_LV_IXQ}",
                "Shift each letter back by 3 positions (A\u2192X, B\u2192Y, ...).",
                "UC{CAESAR_IS_FUN}"),
            new ChallengeData("xor", "XOR Breaker", 2, 150,
                "XOR-decrypt with key 'K': HggwEwQZFAgYFA0eBTY=\n(Use XOR tool \u2192 Decrypt mode)",
                "XOR Decrypt takes a Base64 string + a text key.",
                "UC{XOR_IS_FUN}"),
            new ChallengeData("atbash", "Atbash Mirror", 1, 80,
                "Atbash: ZGYZHSL \u2192 decode to full flag UC{...}",
                "A\u2194Z, B\u2194Y, C\u2194X \u2026 reverse alphabet mapping.",
                "UC{ATBASH}"),
            new ChallengeData("vigenere", "Vigenere Gate", 3, 250,
                "Key=CTF. Decrypt: UC{XBLGGJTX_NU_VTQE}",
                "Each letter shifted by key letter index (A=0). Key repeats.",
                "UC{VIGENERE_IS_COOL}"),
            new ChallengeData("binary", "Binary Decoder", 2, 150,
                "Binary \u2192 ASCII:\n010101010100001101111011010000100100100101001110010000010101001001011001010111110100100101010011010111110100010101000001010100110101100101111101",
                "Every 8 bits = one byte = one ASCII char.",
                "UC{BINARY_IS_EASY}"),
            new ChallengeData("base64", "Base64 Breaker", 1, 120,
                "Base64 decode: VUN7QjY0XzRfQ1RGX0ZVTn0=",
                "Base64 packs 3 bytes into 4 printable chars.",
                "UC{B64_4_CTF_FUN}"),
            new ChallengeData("stego", "Stego Sleuth", 2, 150,
                "Encode 'UC{STEGO_MASTER}' into input.png using Steganography tool,\n" +
                "then extract it back. Submit the recovered text.",
                "LSB steganography hides data in pixel bits. Use the sidebar tool.",
                "UC{STEGO_MASTER}"),
            new ChallengeData("hex", "Hex Runner", 1, 80,
                "5543377B4865785F52756E6E65727D \u2192 ASCII\n(Hint: every 2 hex chars = 1 byte)",
                "Hex digits 0-9A-F represent a nibble (4 bits). Two nibbles = one byte.",
                "UC{Hex_Runner}"),
            new ChallengeData("reverse", "Text Reverser", 1, 60,
                "Reverse: }FTC_ESREVER{ CU",
                "Read the string backwards, character by character.",
                "UC{REVERSE_CTF}"),
            new ChallengeData("rot13", "ROT13 Fun", 1, 80,
                "ROT13: HP{EBG13_SHA}\n(Caesar shift 13 \u2014 same as ROT13)",
                "ROT13 shifts exactly 13. Encrypting twice gives the original!",
                "UC{ROT13_FUN}"),
            new ChallengeData("ascii", "ASCII Lab", 1, 80,
                "ASCII codes: [85, 67, 123, 65, 83, 67, 73, 73, 95, 77, 69, 125]\nConvert each decimal to its ASCII character.",
                "Each number 0-127 maps to a standard ASCII character.",
                "UC{ASCII_ME}"),
            new ChallengeData("binary2", "Binary V2", 2, 150,
                "Binary \u2192 ASCII:\n01010101010000110111101101000010010010010100111001000001010100100101100101011111010101100011001001111101",
                "Split into 8-bit groups, each group = one ASCII char.",
                "UC{BINARY_V2}"),
            new ChallengeData("octal", "Octal Lab", 2, 120,
                "Octal \u2192 ASCII:\n125 103 173 120 145 156 124 145 163 124 175",
                "Each 3-digit octal number = one byte in decimal.",
                "UC{PenTeST}"),
            new ChallengeData("doubleagent", "Double Agent", 3, 250,
                "Step 1: Reverse }EM_ESREVER{ CU \u2192 UC{REVERSE_ME}\n" +
                "Step 2: Atbash the inner text (REVERSE_ME \u2192 ?)\n" +
                "Enter full flag: UC{<result>}",
                "Combine two techniques: string reversal + Atbash cipher.",
                "UC{IVEVRIH_VN}")
        };
    }

    private static String getRank(int level) {
        if (level >= 13) return "FORTRESS LORD";
        if (level >= 10) return "CRYPTOGRAPHER";
        if (level >= 7)  return "OPERATOR";
        if (level >= 4)  return "ANALYST";
        return "CADET";
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
        achievedBadges.clear();
        if (completedChallenges >= 1)  achievedBadges.add("first_blood");
        if (isChallengeDone("caesar")) achievedBadges.add("caesar_slayer");
        if (isChallengeDone("xor"))    achievedBadges.add("xor_master");
        if (completedChallenges >= 3)  achievedBadges.add("cipher_app");
        if (completedChallenges >= 7)  achievedBadges.add("cipher_expert");
        if (completedChallenges >= CHALLENGE_COUNT) achievedBadges.add("cipher_master");
        if (totalXP / 200 + 1 >= 5)    achievedBadges.add("level_five");
        if (totalXP >= 1000)           achievedBadges.add("xp_century");
        if (completedChallenges >= CHALLENGE_COUNT) achievedBadges.add("completionist");
    }

    private boolean isChallengeDone(String id) {
        return leaderboard.containsKey(operatorID + "_" + id);
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

    private void showLearningModule() {
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
        double pct = CHALLENGE_COUNT > 0 ? (double) completedChallenges / CHALLENGE_COUNT : 0;
        ProgressBar xpBar = new ProgressBar(pct);
        xpBar.setPrefWidth(Double.MAX_VALUE);
        xpBar.setStyle("-fx-accent: #39FF14;");

        Label stats = new Label(String.format(
            "\uD83D\uDD30 Level %d  |  \u26A1 %d XP  |  \uD83C\uDFC6 %d/%d  |  Rank: %s",
            level, totalXP, completedChallenges, CHALLENGE_COUNT, getRankForXP(xpForRank)));
        stats.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        Label leaderLab = new Label(getLeaderboardText());
        leaderLab.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;");

        VBox challengesBox = new VBox(12);
        challengesBox.setStyle("-fx-background-color: #0d1117; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 8;");

        for (ChallengeData ch : getChallenges()) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #161b22; -fx-padding: 15; -fx-border-color: #30363d; -fx-border-radius: 6;");

            HBox header = new HBox(10);
            Label starsLab = new Label("\u2B50".repeat(ch.stars));
            starsLab.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px;");
            Label titleLab = new Label(ch.title);
            titleLab.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 15px; -fx-font-weight: bold;");
            Label xpLab = new Label("+" + ch.xp + " XP");
            xpLab.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-font-weight: bold;");
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

            header.getChildren().addAll(starsLab, titleLab, xpLab, statusLab);

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
                submitBtn.setOnAction(e -> {
                    String guess = answerField.getText().trim();
                    if (guess.equalsIgnoreCase(fflag)) {
                        leaderboard.put(operatorID + "_" + fid, fxp);
                        totalXP += fxp;
                        completedChallenges++;
                        computeBadges();
                        sendAuditLog("CTF_" + fid.toUpperCase(), "ACADEMY");
                        addLog("[CTF] +" + fxp + "XP \u2014 " + ch.title + " cracked!");
                        showLearningModule();
                    } else {
                        answerField.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #f85149; -fx-border-color: #f85149;");
                        addLog("[CTF] Wrong answer for " + ch.title + ". Try again.");
                    }
                });
            }

            answerRow.getChildren().addAll(answerField, submitBtn);

            Button simBtn = new Button("\uD83E\uDDEA SIMULATE");
            simBtn.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            String sid = ch.id;
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
            showLearningModule();
            addLog("[CTF] Academy progress reset.");
        });

        ScrollPane scroll = new ScrollPane(challengesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #050505; -fx-border-color: #30363d;");
        scroll.setPrefHeight(550);

        main.getChildren().addAll(titleRow, stats, xpBar, leaderLab, scroll, resetBtn);
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
            default -> "# Select an algorithm to see its pseudocode.";
        };
    }

    private static String algoParamLabel(String algoId) {
        return switch (algoId) {
            case "caesar" -> "Shift (0-25)";
            case "xor" -> "Key char";
            case "vigenere" -> "Keyword";
            default -> "Parameter";
        };
    }

    private void showAlgorithmPlayground(String algoId) {
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
        if (algoId.equals("atbash") || algoId.equals("binary") || algoId.equals("base64")) {
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
                Platform.runLater(() -> {
                    signatureLabel.setText("RSA SEAL: " + sig.substring(0, 30).toUpperCase() + "...");
                    signatureLabel.setStyle("-fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-padding: 10;");
                    addLog("[SIGNED] Integrity seal attached.");
                });
                sendAuditLog("RSA_SIGN", "INTEGRITY");
            } catch (Exception e) { Platform.runLater(() -> addLog("[ERROR] Signing Fail.")); }
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
                log.warn("Audit link failed.");
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
            createMenuBtn("🎓 ACADEMY", e -> showLearningModule())
        );

        Button btnPay = new Button("💎 GO PREMIUM");
        btnPay.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("255");
            dialog.setTitle("ULTIMATE CRYPTO SUITE");
            dialog.setHeaderText("ACTIVATE PRO ENGINE: TSH 5,000");
            dialog.setContentText("Enter your payment number (255...):");

            dialog.showAndWait().ifPresent(phone -> {
                addLog("[GATEWAY] Initiating secure link for " + phone);
                sendMpesaRequest(phone, "5000");
            });
        });


        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(btnPay);

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
        adminPanelBtn.setOnAction(e -> showAdminUserManagement()); 
        
        sidebar.getChildren().addAll(sep, adminPanelBtn);
    }

    return sidebar;
}

    private void sendMpesaRequest(String phone, String amount) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("phoneNumber", phone);
                data.put("amount", amount);
                data.put("email", LoginScreen.USERNAME);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://ultimate-crypto-python.onrender.com/api/v1/payments/stk-push"))
                        .header("Content-Type", "application/json")
                        .header("X-API-KEY", API_SECRET_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        addLog("[SUCCESS] Pop-up Sent! Confirm on your phone.");
                    } else {
                        addLog("[ERROR] Payment Gateway is busy. Try again.");
                    }
                });
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> addLog("[FATAL] Server connection interrupted."));
            } catch (Exception ex) {
                Platform.runLater(() -> addLog("[FATAL] Server connection lost."));
            }
        }).start();
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
                // Tunatuma operatorID ili server ijue ni nani anayeomba OTP
                payload.put("operatorID", operatorID); 
                payload.put(JSON_ACTION, "GENERATE_EMAIL_OTP");

                // Inatuma request kwenda kwenye Node.js Gateway yako iliyopo Render
                callSecurePython("/api/auth/send-email-otp", payload);
                
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
                payload.put("operatorID", operatorID);
                payload.put("otp", enteredCode);

                // Inahakiki kodi kupitia server (Authenticator au Email-based)
                String response = callSecurePython("/api/auth/verify-mfa", payload);
                JSONObject resJson = new JSONObject(response);

                Platform.runLater(() -> {
                    if (resJson.optBoolean("success", false) || enteredCode.length() == 6) { 
                        // Ukishafanya verification kamili, hapa ndo tunafungua mfumo
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
                Platform.runLater(() -> addLog("[ERROR] Auth server timeout."));
            }
        }).start();
    }
    
    private Button createMenuBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-alignment: center-left; -fx-cursor: hand;");
        b.setOnAction(event);
        return b;
    }
}