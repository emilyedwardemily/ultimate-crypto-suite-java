package ui;

import crypto.*;
import storage.MongoManager;
import steganography.StegTool;
import app.DatabaseManager;
import app.LicenseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import javafx.scene.control.Separator;

// --- JSON & COLLECTIONS IMPORTS ---
import org.json.JSONObject;
import org.json.JSONArray;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
// ----------------------------------

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

public class Dashboard extends BorderPane {
    private TextArea inputArea, outputArea;
    private PasswordField keyField;
    private TextField mfaField; 
    private Label signatureLabel, mfaStatusLabel;
    private ProgressBar strengthMeter;
    private VBox terminalLogs;
    private String operatorID;
    private File selectedImageFile; 
    private Thread activeStegThread; 
    private TableView<JSONObject> auditTable;
    
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
            try {
                Runtime.getRuntime().exec("explorer.exe /select," + selectedImageFile.getAbsolutePath());
            } catch (Exception ex) { addLog("[ERROR] Explorer link failed."); }
        });

        stegTask.setOnFailed(event -> {
            main.getChildren().remove(btnAbort);
            strengthMeter.progressProperty().unbind();
            addLog("[CRITICAL] Process failed.");
        });

        activeStegThread = new Thread(stegTask);
        activeStegThread.setDaemon(true);
        activeStegThread.start();
    }

    private void setupLayout(String titleStr, String inputHint) {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #050505;");

        Label title = new Label(titleStr);
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        HBox mfaBar = new HBox(12);
        mfaBar.setAlignment(Pos.CENTER_LEFT);
        mfaField = new TextField();
        mfaField.setPromptText("SECURE OTP");
        mfaField.setPrefWidth(110);
        mfaField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #f85149;");
        mfaStatusLabel = new Label("LOCKED: MFA REQUIRED");
        mfaStatusLabel.setTextFill(Color.web("#f85149"));
        mfaBar.getChildren().addAll(new Label("MFA GATE:"), mfaField, mfaStatusLabel);

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
        outputArea.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-border-width: 0.3;");

        terminalLogs = new VBox(3);
        terminalLogs.setPadding(new Insets(10));
        terminalLogs.setStyle("-fx-background-color: #000000;"); 
        ScrollPane logScroll = new ScrollPane(terminalLogs);
        logScroll.setPrefHeight(110);
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: #000000; -fx-background-color: #000000; -fx-border-color: #39FF14; -fx-border-width: 0.3;");

        HBox actionBox = new HBox(20);
        Button btnSync = new Button("🛡️ QUANTUM SYNC");
        btnSync.setStyle("-fx-background-color: #238636; -fx-text-fill: white;");
        btnSync.setOnAction(e -> handleSecureSync(titleStr));

        Button btnSign = new Button("🖋️ RSA SIGN");
        btnSign.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white;");
        btnSign.setOnAction(e -> handleDigitalSignature());
        actionBox.getChildren().addAll(btnSign, btnSync);

        main.getChildren().addAll(title, mfaBar, new Label("PLAINTEXT / CIPHERTEXT:"), inputArea, 
                                  new Label("SECURITY KEY:"), keyField, strengthMeter, 
                                  signatureLabel, new Label("RESULT MATRICES:"), outputArea, 
                                  actionBox, new Label("REAL-TIME FORENSICS:"), logScroll);
        
        main.getChildren().forEach(n -> { if (n instanceof Label && n != title) ((Label) n).setTextFill(Color.web("#8b949e")); });
        setCenter(main);
    }

    // --- MODULE VIEWS ---

    private void showAESModule() {
        setupLayout("AES-256 CASCADING ENGINE", "Enter intelligence for hybrid encryption...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button enc = new Button("ENCRYPT"); Button dec = new Button("DECRYPT");
        enc.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
        dec.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
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
        enc.setStyle("-fx-background-color: #238636; -fx-text-fill: white;");
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
        btnCert.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
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
        gen.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white;");
        enc.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
        gen.setOnAction(e -> executePythonService("/rsa-keygen", "[RSA] Initiating..."));
        enc.setOnAction(e -> executePythonService("/rsa-encrypt", "[RSA] Applying Cipher..."));
        hb.getChildren().addAll(gen, enc);
        main.getChildren().add(9, hb);
    }

    private void showXORModule() {
        setupLayout("XOR BITWISE ENGINE", "Fast bitwise transformation...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button enc = new Button("XOR ENCRYPT"); Button dec = new Button("XOR DECRYPT");
        enc.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
        dec.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white;");
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

    private void showStegModule() {
        setupLayout("IMAGE STEGANOGRAPHY", "Hide intelligence inside carrier pixels...");
        VBox main = (VBox) getCenter();
        HBox hb = new HBox(10);
        Button btnSel = new Button("📸 SELECT IMAGE");
        Button btnHide = new Button("🔒 HIDE DATA");
        Button btnExt = new Button("🔓 EXTRACT DATA");
        Button btnSave = new Button("💾 SAVE AS");

        btnSave.setStyle("-fx-background-color: #238636; -fx-text-fill: white;");
        btnSave.setOnAction(e -> {
            if (selectedImageFile != null) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save Secure Intelligence");
                fc.setInitialFileName("stego_result.png");
                File file = fc.showSaveDialog(null);
                if (file != null) {
                    try {
                        java.nio.file.Files.copy(selectedImageFile.toPath(), file.toPath(), 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        addLog("[SAVED] Image stored at: " + file.getAbsolutePath());
                    } catch (Exception ex) { addLog("[ERROR] Save failed."); }
                }
            } else { addLog("[DENIED] No image to save."); }
        });
        
        btnSel.setOnAction(e -> {
            selectedImageFile = new FileChooser().showOpenDialog(null);
            if(selectedImageFile != null) addLog("[FILE] Loaded: " + selectedImageFile.getName());
        });

        btnHide.setOnAction(e -> handleHideData());

        btnExt.setOnAction(e -> {
            if (selectedImageFile == null) {
                addLog("[INFO] Select carrier image first.");
                selectedImageFile = new FileChooser().showOpenDialog(null);
            }
            if(selectedImageFile != null) {
                new Thread(() -> {
                    try {
                        addLog("[SCAN] Analyzing: " + selectedImageFile.getName());
                        String recovered = StegTool.decode(selectedImageFile, keyField.getText());
                        Platform.runLater(() -> { 
                            outputArea.setText(recovered); 
                            addLog("[SUCCESS] Intelligence recovered."); 
                            sendAuditLog("STEG_EXTRACT", "STEGANOGRAPHY");
                        });
                    } catch (Exception ex) { Platform.runLater(() -> addLog("[ERROR] " + ex.getMessage())); }
                }).start();
            }
        });
        hb.getChildren().addAll(btnSel, btnHide, btnExt, btnSave);
        main.getChildren().add(9, hb);
    }

   
   private void showCaesarModule() {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #050505;");

        Label title = new Label("CAESAR & LEGACY ENGINES");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

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
        sl.valueProperty().addListener((obs, old, newVal) -> {
            shiftValLabel.setText("CURRENT SHIFT: " + newVal.intValue());
        });

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
        outputArea.setStyle("-fx-control-inner-background: #010409; -fx-text-fill: #39FF14; -fx-border-color: #39FF14; -fx-border-width: 0.3;");

        HBox controls = new HBox(10);
        Button btnEnc = new Button("🔐 ENCRYPT");
        Button btnDec = new Button("🔓 DECRYPT");
        btnEnc.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDec.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;");

        // 4. Update Actions kutuma pia Secret Key
        btnEnc.setOnAction(e -> handleLegacyCrypto(caesarIn.getText(), (int)sl.getValue(), cipherType.getValue(), true, keyInput.getText()));
        btnDec.setOnAction(e -> handleLegacyCrypto(caesarIn.getText(), (int)sl.getValue(), cipherType.getValue(), false, keyInput.getText()));

        controls.getChildren().addAll(btnEnc, btnDec);
        main.getChildren().addAll(title, new Label("SELECT ALGORITHM:"), cipherType, 
                                  new Label("INPUT:"), caesarIn, 
                                  keyInput, // Sehemu ya neno la siri
                                  shiftValLabel, sl, 
                                  controls, new Label("RESULT MATRICES:"), outputArea);
        
        main.getChildren().forEach(n -> { if (n instanceof Label && n != title) ((Label) n).setTextFill(Color.web("#8b949e")); });
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
                String result = new JSONObject(res).getString("result");
                
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
        
        btnHash.setStyle("-fx-background-color: #58a6ff; -fx-text-fill: white;");
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
                    String res = callSecurePython("/secure-wipe", p);
                    addLog("[VAPORIZED] " + f.getName() + " removed securely.");
                    sendAuditLog("SECURE_WIPE", "FORENSICS");
                } catch (Exception ex) { addLog("[ERROR] Wipe engine failed."); }
            }
        });

        hb.getChildren().addAll(btnHash, btnWipe);
        main.getChildren().add(9, hb);
    }

   private void showHistoryModule() {
        VBox main = new VBox(12);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #050505;");

        Label title = new Label("SYSTEM FORENSIC HISTORY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 24px; -fx-font-weight: bold;");

        // --- SEARCH BOX (NEW) ---
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search by Action or Module (e.g. CAESAR, SYSTEM_BOOT)...");
        searchField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #39FF14; -fx-border-width: 0.5;");

        TableView<JSONObject> table = new TableView<>();
        table.setStyle("-fx-background-color: #0d1117; -fx-control-inner-background: #0d1117;");
        
        // --- COLUMNS (REORDERABLE FALSE) ---
        TableColumn<JSONObject, String> colTime = new TableColumn<>("TIMESTAMP");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().optString("timestamp")));
        colTime.setReorderable(false);
        colTime.setPrefWidth(200);

        TableColumn<JSONObject, String> colAction = new TableColumn<>("ACTION");
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().optString("action", "SYSTEM_EVENT")));
        colAction.setReorderable(false);
        colAction.setPrefWidth(180);

        TableColumn<JSONObject, String> colModule = new TableColumn<>("MODULE");
        colModule.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().optString("module", "CORE_KERNEL")));
        colModule.setReorderable(false);
        colModule.setPrefWidth(150);

        table.getColumns().addAll(colTime, colAction, colModule);

        // --- SEARCH LOGIC ---
        ObservableList<JSONObject> masterData = FXCollections.observableArrayList();
        FilteredList<JSONObject> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(log -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerCaseFilter = newVal.toLowerCase();
                if (log.optString("action").toLowerCase().contains(lowerCaseFilter)) return true;
                if (log.optString("module").toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        // --- BUTTONS ---
        HBox btnBox = new HBox(10);
        Button refreshBtn = new Button("REFRESH FROM ATLAS");
        Button pdfBtn = new Button("GENERATE REPORT (PDF)");
        
        refreshBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;");
        pdfBtn.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;");

        refreshBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String res = callSecurePython("/get-audit-logs", new JSONObject());
                    JSONArray logs = new JSONArray(res);
                    masterData.clear();
                    for (int i = 0; i < logs.length(); i++) masterData.add(logs.getJSONObject(i));
                    Platform.runLater(() -> table.setItems(filteredData));
                } catch (Exception ex) { addLog("[ERROR] Audit sync failed."); }
            }).start();
        });

        pdfBtn.setOnAction(e -> exportToPDF(masterData));

        btnBox.getChildren().addAll(refreshBtn, pdfBtn);
        main.getChildren().addAll(title, searchField, table, btnBox);
        setCenter(main);
    }

private void initiateMpesaPayment() {
    TextInputDialog dialog = new TextInputDialog("255");
    dialog.setTitle("PREMIUM ACTIVATION");
    dialog.setHeaderText("Unlock Military Grade Protection");
    dialog.setContentText("Enter M-Pesa Number:");

    dialog.showAndWait().ifPresent(phone -> {
        addLog("[WAIT] Contacting M-Pesa Gateway...");
        new Thread(() -> {
            try {
                JSONObject payLoad = new JSONObject();
                payLoad.put("phoneNumber", phone);
                payLoad.put("amount", "5000"); // Bei ya Premium

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://ultimate-crypto-node-gateway.onrender.com/api/payments/stkpush"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payLoad.toString()))
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                
                Platform.runLater(() -> {
                    if(res.statusCode() == 200) {
                        addLog("[PAYMENT] STK Push Sent! Check your phone.");
                        // Hapa mteja akilipa, ndo tunampa 'PRO' status kwenye Atlas
                    } else {
                        addLog("[ERROR] Payment Gateway Timeout.");
                    }
                });
            } catch (Exception e) { addLog("[CRITICAL] Payment System Error."); }
        }).start();
    });
}

    private void showLearningModule() {
        VBox main = new VBox(20);
        main.setPadding(new Insets(30));
        main.setStyle("-fx-background-color: #050505;");
        
        Label title = new Label("🎓 UC-FORTRESS ACADEMY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 24px; -fx-font-weight: bold;");

        Accordion academy = new Accordion();
        
        TitledPane p1 = new TitledPane("1. AES-256 Encryption", new Label("The gold standard in symmetric encryption. \nIt uses 256-bit keys to scramble data."));
        TitledPane p2 = new TitledPane("2. Steganography (LSB)", new Label("Hiding data in the Least Significant Bit of pixels. \nInvisible to the naked eye."));
        TitledPane p3 = new TitledPane("3. RSA Asymmetric Gate", new Label("Uses a Public Key for encryption and a \nPrivate Key for decryption."));

        academy.getPanes().addAll(p1, p2, p3);
        main.getChildren().addAll(title, academy);
        setCenter(main);
    }

    // --- FORENSIC HISTORY & ATLAS INTEGRATION ---

    private void showAuditHistory() {
        VBox main = new VBox(15);
        main.setPadding(new Insets(20));
        main.setStyle("-fx-background-color: #050505;");

        Label title = new Label("SYSTEM FORENSIC HISTORY");
        title.setStyle("-fx-text-fill: #39FF14; -fx-font-size: 26px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        auditTable = new TableView<>();
        auditTable.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d;");
        
        TableColumn<JSONObject, String> colTime = new TableColumn<>("TIMESTAMP");
        colTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString("timestamp", "N/A")));
        colTime.setPrefWidth(180);

        TableColumn<JSONObject, String> colAction = new TableColumn<>("ACTION");
        colAction.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString("action", "UNKNOWN")));
        colAction.setPrefWidth(200);

        TableColumn<JSONObject, String> colModule = new TableColumn<>("MODULE");
        colModule.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().optString("module", "GENERAL")));
        colModule.setPrefWidth(150);

        auditTable.getColumns().addAll(colTime, colAction, colModule);
        auditTable.setPrefHeight(450);

        HBox controls = new HBox(15);
        Button btnRefresh = new Button("🔄 REFRESH FROM ATLAS");
        btnRefresh.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;");
        btnRefresh.setOnAction(e -> refreshAuditLogs());

        Button btnExport = new Button("📄 GENERATE REPORT");
        btnExport.setStyle("-fx-background-color: #8957e5; -fx-text-fill: white; -fx-font-weight: bold;");
        btnExport.setOnAction(e -> generateForensicReport());

        controls.getChildren().addAll(btnRefresh, btnExport);
        main.getChildren().addAll(title, auditTable, controls);
        setCenter(main);
        
        refreshAuditLogs();
    }

    private void exportToPDF(ObservableList<JSONObject> data) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("🛡️ GENERATE SECURE PDF REPORT");
        
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        
        String cleanTimestamp = java.time.LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        fileChooser.setInitialFileName("Forensic_Report_" + cleanTimestamp + ".pdf");
        
        File selectedFile = fileChooser.showSaveDialog(null);

        if (selectedFile != null) {
            new Thread(() -> {
                String absolutePath = selectedFile.getAbsolutePath();
                
                if (absolutePath.toLowerCase().endsWith(".txt")) {
                    absolutePath = absolutePath.substring(0, absolutePath.length() - 4) + ".pdf";
                } else if (!absolutePath.toLowerCase().endsWith(".pdf")) {
                    absolutePath = absolutePath + ".pdf";
                }
                
                File finalFile = new File(absolutePath);

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(finalFile)) {
                    com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                    com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, fos);
                    
                    writer.setCloseStream(true);
                    document.open();
                    
                    com.itextpdf.text.Font headFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 14, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.GREEN);
                    document.add(new com.itextpdf.text.Paragraph("ULTIMATE CRYPTO SUITE - FORENSIC AUDIT", headFont));
                    document.add(new com.itextpdf.text.Paragraph("OPERATOR: " + operatorID));
                    document.add(new com.itextpdf.text.Paragraph("DATE: " + java.time.LocalDateTime.now()));
                    document.add(new com.itextpdf.text.Paragraph("--------------------------------------------------\n\n"));

                    com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(3);
                    pdfTable.setWidthPercentage(100);
                    
                    pdfTable.addCell("TIMESTAMP");
                    pdfTable.addCell("ACTION");
                    pdfTable.addCell("MODULE");

                    for (JSONObject log : data) {
                        pdfTable.addCell(log.optString("timestamp", "N/A"));
                        pdfTable.addCell(log.optString("action", "EVENT"));
                        pdfTable.addCell(log.optString("module", "CORE"));
                    }

                    document.add(pdfTable);
                    document.close();
                    
                    Platform.runLater(() -> {
                        addLog("[SUCCESS] Forensic PDF Created: " + finalFile.getName());
                        try { Runtime.getRuntime().exec("xdg-open " + finalFile.getParent()); } catch (Exception e) {}
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> addLog("[ERROR] PDF Engine Fail: " + ex.getMessage()));
                }
            }).start();
        }
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

                for (JSONObject log : auditTable.getItems()) {
                    sb.append(String.format("[%s] -> %-18s | MOD: %s\n", 
                        log.optString("timestamp"), log.optString("action"), log.optString("module")));
                }
                
                sb.append("\n[END OF REPORT - INTEGRITY SEALED]");
                java.nio.file.Files.writeString(file.toPath(), sb.toString());
                addLog("[EXPORTED] Report saved successfully.");
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    Runtime.getRuntime().exec("notepad.exe " + file.getAbsolutePath());
                }
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
                        auditTable.getItems().clear();
                        json.getJSONArray("logs").forEach(item -> auditTable.getItems().add((JSONObject) item));
                        addLog("[SUCCESS] Forensic history synced.");
                    });
                }
            } catch (Exception e) { Platform.runLater(() -> addLog("[ERROR] Atlas Sync Fail.")); }
        }).start();
    }

    private String callSecurePythonGet(String ep) throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(PYTHON_URL + ep))
                .header("X-API-KEY", API_SECRET_KEY).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private void syncWithCloud(String encryptedData, String serviceType) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("userId", operatorID); 
            payload.put("service", serviceType);
            payload.put("encryptedData", encryptedData);
            payload.put("type", "Vault");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                   .uri(URI.create("https://ultimate-crypto-node-gateway.onrender.com/api/vault/sync"))
.header("Content-Type", "application/json")
.header("X-API-KEY", API_SECRET_KEY)
.POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
.build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201) {
                        System.out.println("✅ Data Imehifadhiwa Cloud!");
                    } else {
                        System.out.println("❌ Cloud Sync Failed: " + response.body());
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
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

        new Thread(() -> {
            try {
                Platform.runLater(() -> addLog(encrypt ? "[JAVA] Initiating XOR + AES Layering..." : "[JAVA] Reversing Hybrid Layers..."));

                // XOR Layer ya kwanza (Inafanyika hapa hapa Kali Linux)
                String xorProcessed = encrypt ? XORUtil.encrypt(inputData, securityKey) : inputData;
                
                JSONObject p = new JSONObject(); 
                p.put("data", xorProcessed); 
                p.put("key", securityKey);
                p.put("ts", java.time.Instant.now().getEpochSecond());

                // Kuitisha jibu toka Render
                String res = callSecurePython(encrypt ? "/encrypt" : "/decrypt", p);
                
                if (res != null && !res.isEmpty()) {
                    JSONObject resJson = new JSONObject(res);
                    String pythonResult = resJson.optString("result", "");

                    if (!pythonResult.isEmpty()) {
                        // Kama tunadecrypt, XOR Layer ya mwisho inafanyika hapa
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
        }).start();
    }

  private void executePythonService(String endpoint, String logMsg) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> addLog(logMsg));
                
                JSONObject p = new JSONObject(); 
                p.put("data", inputArea.getText().trim()); 
                p.put("key", keyField.getText().trim());
                
                // Tunapiga API ya Render
                String res = callSecurePython(endpoint, p);
                
                Platform.runLater(() -> {
                    try {
                        JSONObject responseJson = new JSONObject(res);
                        String finalOutput = "";

                        // Inakagua kama kuna 'result', 'message' au 'data'
                        if (responseJson.has("result")) {
                            finalOutput = responseJson.getString("result");
                        } else if (responseJson.has("message")) {
                            finalOutput = responseJson.getString("message");
                        } else {
                            finalOutput = res; // Kama si JSON, chukua text yote
                        }

                        // MSTARI WA USHINDI: Jibu linaandikwa hapa
                        outputArea.setText(finalOutput);
                        
                        addLog("[SUCCESS] Gateway handshaking finalized.");
                        sendAuditLog("PYTHON_SERVICE", endpoint);
                        
                    } catch (Exception e) {
                        outputArea.setText(res); // Kama response si JSON, weka jibu kama lilivyo
                        addLog("[INFO] RAW Output displayed.");
                    }
                });
            } catch (Exception ex) { 
                Platform.runLater(() -> {
                    addLog("[ERROR] Bridge failed: " + ex.getMessage());
                    outputArea.setText("🌐 SERVER OFFLINE: Render is still waking up...");
                });
            }
        }).start();
    }

    private String callSecurePython(String ep, JSONObject p) throws Exception {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(7)).build();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(PYTHON_URL + ep))
                .header("Content-Type", "application/json").header("X-API-KEY", API_SECRET_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(p.toString())).build();
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
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
                JSONObject log = new JSONObject();
                log.put("operator_id", this.operatorID); 
                log.put("action", action);
                log.put("module", module);
                callSecurePython("/audit-log", log);
            } catch (Exception e) {
                System.out.println("Audit link failed.");
            }
        }).start();
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
            createMenuBtn("🖼️ STEGANOGRAPHY", e -> showStegModule()),
            createMenuBtn("🏛️ CAESAR LEGACY", e -> showCaesarModule()),
            createMenuBtn("🔍 FORENSIC AUDIT", e -> showIntegrityModule()),
            createMenuBtn("📊 VIEW HISTORY", e -> showAuditHistory()),
            new Separator(),
            createMenuBtn("🎓 ACADEMY", e -> showLearningModule())
        );

        Button btnPay = new Button("💎 GO PREMIUM");
        btnPay.setOnAction(e -> {
    TextInputDialog dialog = new TextInputDialog("255");
    dialog.setTitle("ULTIMATE CRYPTO SUITE"); // Jina la mradi wako
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
//Helper method kwa ajili ya kutengeneza button za pembeni kirahisi
private Button createSidebarBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
    Button b = new Button(text);
    b.setMaxWidth(Double.MAX_VALUE);
    b.setAlignment(Pos.CENTER_LEFT);
    b.setStyle("-fx-background-color: transparent; -fx-text-fill: #c9d1d9; -fx-cursor: hand;");
    b.setOnAction(event);
    return b;
}

    private void sendMpesaRequest(String phone, String amount) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("phoneNumber", phone);
                data.put("amount", amount);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://ultimate-crypto-node-gateway.onrender.com/api/payments/stkpush"))
.header("Content-Type", "application/json")
.POST(HttpRequest.BodyPublishers.ofString(data.toString()))
.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        addLog("[SUCCESS] Pop-up Sent! Confirm on your phone.");
                    } else {
                        addLog("[ERROR] M-Pesa Gateway is busy. Try again.");
                    }
                });
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
        adminView.setStyle("-fx-background-color: #050505;");

        Label title = new Label("🛠️ SYSTEM ADMINISTRATION: USER DATABASE");
        title.setStyle("-fx-text-fill: #f85149; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");

        // Sehemu ya kuonyesha list ya watumiaji
        TextArea userDisplay = new TextArea();
        userDisplay.setEditable(false);
        userDisplay.setPrefHeight(450);
        userDisplay.setText("--- [ UC-FORTRESS ADMIN CONSOLE ] ---\n" +
                           "FETCHING DATA FROM MONGODB ATLAS...\n\n");
        userDisplay.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #39FF14; -fx-border-color: #39FF14;");

        Button btnRefresh = new Button("🔄 REFRESH USER LIST");
        btnRefresh.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Hapa baadaye tutaweka kodi ya kuvuta data halisi
        btnRefresh.setOnAction(e -> userDisplay.appendText("[SCAN] User fetch initiated...\n[LOG] Access granted to: " + LoginScreen.USERNAME + "\n"));

        adminView.getChildren().addAll(title, btnRefresh, userDisplay);
        
        // Hii itabadilisha eneo la katikati la software yako kuwa muonekano wa Admin
        setCenter(adminView);
        
        // Log kwenye terminal yako ya pembeni
        addLog("[CRITICAL] Admin Dashboard Accessed.");
    }
    

    
    private Button createMenuBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> event) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e; -fx-alignment: center-left; -fx-cursor: hand;");
        b.setOnAction(event);
        return b;
    }
}