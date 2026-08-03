package academy;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * UC-FORTRESS ACADEMY - visual polish toolkit.
 *
 * <p>Transient overlays (particles, confetti, toasts, popups, splash) and
 * lightweight FX helpers (fades, glow pulses, floating XP text, synthesized
 * sound). Everything is stateless and safe on the JavaFX thread; all effects
 * degrade gracefully when animations are disabled or audio is unavailable.</p>
 */
public final class AcademyFx {

    private AcademyFx() { }

    /** Mirrors the persisted "sound" setting; checked before any audio plays. */
    public static volatile boolean soundOn = true;

    private static final Random RND = new Random();

    // ----------------------------------------------------------------
    // OVERLAY ATTACHMENT
    // ----------------------------------------------------------------

    /**
     * Adds {@code n} as a floating overlay on top of {@code layer} (typically the
     * Dashboard BorderPane). Unmanaged children are ignored by layout but still
     * painted, so they can be positioned freely above the existing UI.
     */
    public static void attach(Pane layer, Node n, boolean blocking) {
        n.setManaged(false);
        n.setMouseTransparent(!blocking);
        if (n instanceof Region r) {
            r.setMinSize(0, 0);
        }
        layer.getChildren().add(n);
    }

    private static void bindFull(Region n, Pane layer) {
        Runnable apply = () -> {
            n.setPrefSize(Math.max(1, layer.getWidth()), Math.max(1, layer.getHeight()));
            n.setLayoutX(0);
            n.setLayoutY(0);
        };
        apply.run();
        layer.widthProperty().addListener((o, a, b) -> apply.run());
        layer.heightProperty().addListener((o, a, b) -> apply.run());
    }

    // ----------------------------------------------------------------
    // TRANSITIONS
    // ----------------------------------------------------------------

    /** Fades a node in from transparent (no-op target state when animations are off). */
    public static void fadeIn(Node node, double ms) {
        if (node == null) return;
        if (!AcademyUi.ANIMATIONS) {
            node.setOpacity(1);
            return;
        }
        node.setOpacity(0);
        KeyValue kv = new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_OUT);
        new Timeline(new KeyFrame(Duration.millis(ms), kv)).play();
    }

    // ----------------------------------------------------------------
    // FLOATING XP / PARTICLES / GLOW
    // ----------------------------------------------------------------

    /** Spawns a neon "+X XP" label that rises and fades. x or y &lt; 0 centers on that axis. */
    public static void floatUp(Pane layer, String text, double x, double y, String color) {
        if (!AcademyUi.ANIMATIONS) return;
        Label lab = new Label(text);
        lab.setStyle(AcademyUi.FONT
            + "-fx-text-fill: " + color + ";"
            + "-fx-font-size: 30px;"
            + "-fx-font-weight: bold;");
        lab.setOpacity(0);
        attach(layer, lab, true);
        double w = layer.getWidth();
        double h = layer.getHeight();
        lab.setLayoutX(x < 0 ? Math.max(10, w / 2 - 60) : x);
        lab.setLayoutY(y < 0 ? Math.max(10, h / 2) : y);
        lab.applyCss();
        lab.autosize();
        if (x < 0) lab.setLayoutX(Math.max(10, w / 2 - lab.getWidth() / 2));
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(lab.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(160), new KeyValue(lab.translateYProperty(), -12)),
            new KeyFrame(Duration.millis(950), new KeyValue(lab.translateYProperty(), -84)),
            new KeyFrame(Duration.millis(950), new KeyValue(lab.opacityProperty(), 0.0)));
        tl.setOnFinished(e -> layer.getChildren().remove(lab));
        tl.play();
    }

    /** A brief radial burst of glowing dots from the centre of the layer. */
    public static void sparkles(Pane layer, String colorHex, int count) {
        if (!AcademyUi.ANIMATIONS) return;
        Pane fx = new Pane();
        fx.setMouseTransparent(true);
        attach(layer, fx, true);
        bindFull(fx, layer);
        double cx = Math.max(50, layer.getWidth() / 2);
        double cy = Math.max(50, layer.getHeight() / 2);
        Color c = Color.web(colorHex);
        for (int i = 0; i < count; i++) {
            double angle = RND.nextDouble() * Math.PI * 2;
            double dist = 60 + RND.nextDouble() * 220;
            Circle dot = new Circle(1.5 + RND.nextDouble() * 2.5, c);
            dot.setCenterX(cx);
            dot.setCenterY(cy);
            fx.getChildren().add(dot);
            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(dot.translateXProperty(), 0),
                    new KeyValue(dot.translateYProperty(), 0),
                    new KeyValue(dot.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(700 + RND.nextDouble() * 400),
                    new KeyValue(dot.translateXProperty(), Math.cos(angle) * dist),
                    new KeyValue(dot.translateYProperty(), Math.sin(angle) * dist),
                    new KeyValue(dot.opacityProperty(), 0.0)));
            tl.play();
        }
        PauseTransition rem = new PauseTransition(Duration.millis(1400));
        rem.setOnFinished(e -> layer.getChildren().remove(fx));
        rem.play();
    }

    /** Confetti rain across the whole layer, used for victory moments. */
    public static void confetti(Pane layer, String... colors) {
        if (!AcademyUi.ANIMATIONS) return;
        Pane fx = new Pane();
        fx.setMouseTransparent(true);
        attach(layer, fx, true);
        bindFull(fx, layer);
        double w = layer.getWidth();
        double h = layer.getHeight();
        String[] palette = (colors == null || colors.length == 0)
            ? new String[]{AcademyUi.GOLD, AcademyUi.GREEN, AcademyUi.BLUE, AcademyUi.PURPLE, AcademyUi.ORANGE}
            : colors;
        for (int i = 0; i < 90; i++) {
            double x = RND.nextDouble() * Math.max(w, 400);
            double size = 4 + RND.nextDouble() * 6;
            Rectangle piece = new Rectangle(size, size * (0.5 + RND.nextDouble() * 0.7),
                Color.web(palette[RND.nextInt(palette.length)]));
            piece.setLayoutX(x);
            piece.setLayoutY(-30 - RND.nextDouble() * 120);
            piece.setRotate(RND.nextDouble() * 360);
            fx.getChildren().add(piece);
            double fall = 2200 + RND.nextDouble() * 1200;
            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(piece.translateYProperty(), 0),
                    new KeyValue(piece.rotateProperty(), piece.getRotate()),
                    new KeyValue(piece.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(fall * 0.8),
                    new KeyValue(piece.translateYProperty(), h * 0.7),
                    new KeyValue(piece.rotateProperty(), piece.getRotate() + 360)),
                new KeyFrame(Duration.millis(fall),
                    new KeyValue(piece.translateYProperty(), h + 40),
                    new KeyValue(piece.opacityProperty(), 0.0)));
            tl.play();
        }
        PauseTransition rem = new PauseTransition(Duration.millis(3600));
        rem.setOnFinished(e -> layer.getChildren().remove(fx));
        rem.play();
    }

    /** Pulsing neon glow around a node; stops automatically when the node leaves the scene. */
    public static void glowPulse(Node node, String colorHex) {
        if (node == null) return;
        if (!AcademyUi.ANIMATIONS) {
            AcademyUi.glow(node, Color.web(colorHex, 0.35));
            return;
        }
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(colorHex, 0.4));
        shadow.setRadius(14);
        shadow.setSpread(0.15);
        node.setEffect(shadow);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(shadow.radiusProperty(), 12),
                new KeyValue(shadow.spreadProperty(), 0.12)),
            new KeyFrame(Duration.millis(450),
                new KeyValue(shadow.radiusProperty(), 26),
                new KeyValue(shadow.spreadProperty(), 0.22)));
        tl.setAutoReverse(true);
        tl.setCycleCount(Timeline.INDEFINITE);
        node.sceneProperty().addListener((o, a, b) -> {
            if (b == null) tl.stop();
        });
        tl.play();
    }

    // ----------------------------------------------------------------
    // POPUPS + TOASTS
    // ----------------------------------------------------------------

    /** Slim, modern notification card that slides in at the top-right. */
    public static void toast(Pane layer, String message, String accent) {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.TOP_RIGHT);
        wrap.setPadding(new Insets(18));
        attach(layer, wrap, true);
        bindFull(wrap, layer);

        Label lab = new Label(message);
        lab.setWrapText(true);
        lab.setMaxWidth(340);
        lab.setStyle(AcademyUi.FONT
            + "-fx-text-fill: " + AcademyUi.LIGHT + ";"
            + "-fx-font-size: 13px;"
            + "-fx-background-color: " + AcademyUi.CARD_2 + ";"
            + "-fx-background-radius: 10;"
            + "-fx-border-color: " + accent + ";"
            + "-fx-border-radius: 10;"
            + "-fx-border-width: 1.2;"
            + "-fx-padding: 12 16 12 16;");
        AcademyUi.glow(lab, Color.web(accent, 0.35));
        wrap.getChildren().add(lab);

        if (!AcademyUi.ANIMATIONS) {
            PauseTransition hold = new PauseTransition(Duration.millis(1500));
            hold.setOnFinished(e -> layer.getChildren().remove(wrap));
            hold.play();
            return;
        }
        lab.setTranslateX(320);
        Timeline slide = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(lab.translateXProperty(), 320)),
            new KeyFrame(Duration.millis(320), new KeyValue(lab.translateXProperty(), 0, Interpolator.EASE_OUT)));
        slide.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.millis(1500));
            FadeTransition out = new FadeTransition(Duration.millis(280), wrap);
            out.setToValue(0);
            hold.setOnFinished(ev -> out.play());
            out.setOnFinished(ev -> layer.getChildren().remove(wrap));
            hold.play();
        });
        slide.play();
    }

    /** Blocking modal card on a dimmed backdrop with an OK button. */
    public static void popup(Pane layer, String title, String body, String accent, Runnable onOk) {
        StackPane wrap = new StackPane();
        wrap.setStyle("-fx-background-color: #00000066;");
        attach(layer, wrap, false);
        bindFull(wrap, layer);
        wrap.setOnMouseClicked(e -> {
            if (e.getTarget() == wrap) layer.getChildren().remove(wrap);
        });

        VBox card = new VBox(14);
        card.setMaxWidth(420);
        card.setMaxHeight(600);
        card.setStyle(AcademyUi.FONT
            + "-fx-background-color: " + AcademyUi.CARD_2 + ";"
            + "-fx-background-radius: 14;"
            + "-fx-border-color: " + accent + ";"
            + "-fx-border-radius: 14;"
            + "-fx-border-width: 1.4;"
            + "-fx-padding: 24 26 24 26;");
        AcademyUi.glow(card, Color.web(accent, 0.3));
        Label t = AcademyUi.neon(title, accent, 18);
        Label b = AcademyUi.text(body, 13);
        Button ok = AcademyUi.button("OK", accent, "#0d1117");
        ok.setOnAction(e -> {
            layer.getChildren().remove(wrap);
            if (onOk != null) onOk.run();
        });
        HBox row = new HBox(ok);
        row.setAlignment(Pos.CENTER);
        card.getChildren().addAll(t, b, row);
        StackPane.setAlignment(card, Pos.CENTER);
        wrap.getChildren().add(card);

        if (!AcademyUi.ANIMATIONS) return;
        card.setScaleX(0.85);
        card.setScaleY(0.85);
        card.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(260), card);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition ft = new FadeTransition(Duration.millis(220), card);
        ft.setToValue(1);
        st.play();
        ft.play();
    }

    // ----------------------------------------------------------------
    // SPLASH / LOADING SCREEN
    // ----------------------------------------------------------------

    /** Full-screen boot overlay: brand + animated progress bar, then fades out. */
    public static void splash(Pane layer, String title, String sub) {
        StackPane wrap = new StackPane();
        wrap.setStyle("-fx-background-color: #050505;");
        attach(layer, wrap, true);
        bindFull(wrap, layer);

        Label brand = new Label(title);
        brand.setStyle(AcademyUi.FONT
            + "-fx-text-fill: " + AcademyUi.GREEN + ";"
            + "-fx-font-size: 30px;"
            + "-fx-font-weight: bold;");
        AcademyUi.glow(brand, Color.web(AcademyUi.GREEN, 0.45));
        Label tagline = AcademyUi.caption(sub == null ? "INITIALIZING SECURE KERNEL" : sub, 13);
        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(300);
        bar.setStyle("-fx-accent: " + AcademyUi.GREEN + ";");
        VBox box = new VBox(14, brand, tagline, bar);
        box.setAlignment(Pos.CENTER);
        wrap.getChildren().add(box);

        Timeline prog = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), 0)),
            new KeyFrame(Duration.millis(1100), new KeyValue(bar.progressProperty(), 1.0)));
        prog.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(360), wrap);
            out.setToValue(0);
            out.setOnFinished(ev -> layer.getChildren().remove(wrap));
            out.play();
        });
        prog.play();
    }

    // ----------------------------------------------------------------
    // SYNTHESIZED SOUND
    // ----------------------------------------------------------------

    /**
     * Plays a short synthesized chime for the given event kind. Never throws:
     * audio failures (headless boxes, missing media) are swallowed silently.
     */
    public static void playSound(String kind) {
        if (!soundOn) return;
        try {
            double[] f = freqsFor(kind);
            byte[] wav = synthWav(f);
            Path tmp = Files.createTempFile("ucsfx", ".wav");
            Files.write(tmp, wav);
            MediaPlayer mp = new MediaPlayer(new Media(tmp.toUri().toString()));
            mp.setVolume(0.5);
            mp.setOnEndOfMedia(() -> {
                try { mp.dispose(); } catch (Throwable ignored) { }
                try { Files.deleteIfExists(tmp); } catch (Throwable ignored) { }
            });
            mp.setOnError(() -> {
                try { mp.dispose(); } catch (Throwable ignored) { }
            });
            mp.play();
        } catch (Throwable t) {
            // audio unavailable -> silent
        }
    }

    private static double[] freqsFor(String kind) {
        if ("win".equals(kind)) return new double[]{659.25, 783.99, 1046.50, 1318.51};
        if ("lose".equals(kind)) return new double[]{392.00, 329.63, 261.63};
        if ("xp".equals(kind)) return new double[]{1046.50, 1318.51};
        if ("coin".equals(kind)) return new double[]{1318.51};
        if ("tie".equals(kind)) return new double[]{523.25, 659.25};
        return new double[]{659.25};
    }

    private static byte[] synthWav(double[] freqs) {
        int sampleRate = 22050;
        int perNote = (int) (sampleRate * 0.13);
        int total = perNote * freqs.length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int dataLen = total * 2;
        writeInt(bos, 0x46464952);
        writeInt(bos, 36 + dataLen);
        writeInt(bos, 0x45564157);
        writeInt(bos, 0x20746D66);
        writeInt(bos, 16);
        writeShort(bos, 1);
        writeShort(bos, 1);
        writeInt(bos, sampleRate);
        writeInt(bos, sampleRate * 2);
        writeShort(bos, 2);
        writeShort(bos, 16);
        writeInt(bos, 0x61746164);
        writeInt(bos, dataLen);
        for (double f : freqs) {
            for (int i = 0; i < perNote; i++) {
                double env = Math.min(1.0, Math.min((double) i / 20, (double) (perNote - i) / 120));
                double s = Math.sin(2 * Math.PI * f * i / sampleRate) * env * 0.5;
                short v = (short) (s * 32767);
                bos.write(v & 0xFF);
                bos.write((v >> 8) & 0xFF);
            }
        }
        return bos.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream b, int v) {
        b.write(v & 0xFF);
        b.write((v >> 8) & 0xFF);
        b.write((v >> 16) & 0xFF);
        b.write((v >> 24) & 0xFF);
    }

    private static void writeShort(ByteArrayOutputStream b, int v) {
        b.write(v & 0xFF);
        b.write((v >> 8) & 0xFF);
    }
}
