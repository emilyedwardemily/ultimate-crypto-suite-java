package academy;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * UC-FORTRESS ACADEMY - reusable cyber-neon UI toolkit.
 *
 * <p>Keeps the visual design consistent across the Academy views (dark cyber
 * theme, neon green / blue / purple accents, rounded cards, hover states and
 * entrance animations) while leaving the rest of the legacy UI untouched.</p>
 *
 * <p>Every helper is stateless and safe to call from the JavaFX thread.</p>
 */
public final class AcademyUi {

    private AcademyUi() { }

    // ----------------------------------------------------------------
    // COLOR CONSTANTS
    // ----------------------------------------------------------------

    public static final String GREEN = "#39FF14";
    public static final String BLUE = "#58a6ff";
    public static final String PURPLE = "#8957e5";
    public static final String GOLD = "#FFD700";
    public static final String ORANGE = "#f78166";
    public static final String RED = "#f85149";
    public static final String LIGHT = "#c9d1d9";
    public static final String DIM = "#8b949e";
    public static final String CARD_BG = "#0d1117";
    public static final String CARD_2 = "#161b22";
    public static final String BORDER = "#30363d";

    public static final String FONT = "-fx-font-family: 'DejaVu Sans', 'SansSerif';";

    // ----------------------------------------------------------------
    // COMPONENTS
    // ----------------------------------------------------------------

    /** Rounded, subtly glowing card container. */
    public static VBox card() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: " + CARD_2 + ";"
            + "-fx-background-radius: 12;"
            + "-fx-border-color: " + BORDER + ";"
            + "-fx-border-radius: 12;"
            + "-fx-border-width: 1;");
        return card;
    }

    /** Accent-bordered card that glows with the given accent colour. */
    public static VBox cardAccent(String accent) {
        VBox card = card();
        card.setStyle(
            "-fx-background-color: " + CARD_2 + ";"
            + "-fx-background-radius: 12;"
            + "-fx-border-color: " + accent + ";"
            + "-fx-border-radius: 12;"
            + "-fx-border-width: 1.2;");
        glow(card, Color.web(accent, 0.25));
        return card;
    }

    /** Neon headline label. */
    public static Label neon(String text, String color, double size) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-text-fill: " + color + "; -fx-font-size: " + size + "px; -fx-font-weight: bold;");
        return l;
    }

    /** Secondary label (light grey body text). */
    public static Label text(String text, double size) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-text-fill: " + LIGHT + "; -fx-font-size: " + size + "px;");
        l.setWrapText(true);
        return l;
    }

    /** Muted caption label. */
    public static Label caption(String text, double size) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-text-fill: " + DIM + "; -fx-font-size: " + size + "px;");
        l.setWrapText(true);
        return l;
    }

    /** Rounded action button with hover brightening. */
    public static Button button(String text, String bg, String fg) {
        Button b = new Button(text);
        styleButton(b, bg, fg);
        return b;
    }

    public static void styleButton(Button b, String bg, String fg) {
        b.setStyle(FONT
            + "-fx-background-color: " + bg + ";"
            + "-fx-text-fill: " + fg + ";"
            + "-fx-font-size: 12px;"
            + "-fx-font-weight: bold;"
            + "-fx-background-radius: 8;"
            + "-fx-background-insets: 0;"
            + "-fx-cursor: hand;");
        hover(b, bg);
    }

    /** Applies a brightening hover effect to a button. */
    public static void hover(Button b, String baseBg) {
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle() + "-fx-background-color: " + lighten(baseBg, 0.18) + ";"));
        b.setOnMouseExited(e -> b.setStyle(b.getStyle().replace(
            "-fx-background-color: " + lighten(baseBg, 0.18) + ";", "-fx-background-color: " + baseBg + ";")));
    }

    /** Neon-glow drop shadow. */
    public static void glow(Node node, Color color) {
        DropShadow shadow = new DropShadow();
        shadow.setColor(color);
        shadow.setRadius(14);
        shadow.setSpread(0.15);
        node.setEffect(shadow);
    }

    /** Section title used inside cards. */
    public static Label section(String text, String accent) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-text-fill: " + accent + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        return l;
    }

    /** Entrance animation: fade + gentle rise. */
    public static void animateIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(14);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setToValue(1.0);
        TranslateTransition rise = new TranslateTransition(Duration.millis(450), node);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
        rise.play();
    }

    /** Smoothly animates a progress bar to the target value. */
    public static void animateProgress(ProgressBar bar, double target, String accent) {
        bar.setStyle("-fx-accent: " + accent + ";");
        double t = Math.max(0, Math.min(1, target));
        KeyValue kv = new KeyValue(bar.progressProperty(), t);
        KeyFrame kf = new KeyFrame(Duration.millis(900), kv);
        new Timeline(kf).play();
    }

    /**
     * A compact dashboard stat tile:
     * icon / large value / caption, with an accent border and glow.
     */
    public static VBox statTile(String icon, String value, String caption, String accent) {
        VBox tile = cardAccent(accent);
        tile.setSpacing(4);
        tile.setPrefWidth(150);
        tile.setAlignment(Pos.CENTER_LEFT);

        Label iconLab = new Label(icon);
        iconLab.setStyle(FONT + "-fx-font-size: 18px;");

        Label valueLab = new Label(value);
        valueLab.setStyle(FONT + "-fx-text-fill: " + accent + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label cap = new Label(caption);
        cap.setStyle(FONT + "-fx-text-fill: " + DIM + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        tile.getChildren().addAll(iconLab, valueLab, cap);
        return tile;
    }

    /** A pill-shaped status/difficulty badge. */
    public static Label pill(String text, String color) {
        Label l = new Label(text);
        l.setStyle(FONT
            + "-fx-text-fill: " + color + ";"
            + "-fx-font-size: 10px;"
            + "-fx-font-weight: bold;"
            + "-fx-background-color: " + CARD_BG + ";"
            + "-fx-padding: 3 10 3 10;"
            + "-fx-border-color: " + color + ";"
            + "-fx-border-radius: 20;"
            + "-fx-background-radius: 20;");
        return l;
    }

    /** Horizontal filler that pushes siblings apart. */
    public static Region spacer() {
        Region r = new Region();
        r.setMinWidth(0);
        r.setPrefWidth(1);
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------

    /** Blends a hex colour toward white. f=0 -> same, f=1 -> white. */
    public static String lighten(String hex, double f) {
        try {
            Color c = Color.web(hex);
            double r = c.getRed() + (1 - c.getRed()) * f;
            double g = c.getGreen() + (1 - c.getGreen()) * f;
            double b = c.getBlue() + (1 - c.getBlue()) * f;
            return String.format("#%02X%02X%02X",
                (int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));
        } catch (Exception e) {
            return hex;
        }
    }
}
