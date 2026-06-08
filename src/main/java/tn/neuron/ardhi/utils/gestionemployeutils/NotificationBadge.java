package tn.neuron.ardhi.utils.gestionemployeutils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * 🔔 Badge de notification animé avec compteur
 */
public class NotificationBadge extends StackPane {

    private Circle badge;
    private Label countLabel;
    private int count = 0;

    public NotificationBadge() {
        this.setAlignment(Pos.TOP_RIGHT);
        this.setPickOnBounds(false);

        // Badge circulaire rouge
        badge = new Circle(10);
        badge.setFill(Color.web("#e74c3c"));
        badge.setStroke(Color.WHITE);
        badge.setStrokeWidth(2);
        badge.setVisible(false);

        // Label avec le nombre
        countLabel = new Label("0");
        countLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 10px;"
        );
        countLabel.setVisible(false);

        this.getChildren().addAll(badge, countLabel);

        // Positionnement
        this.setTranslateX(15);
        this.setTranslateY(-5);
    }

    /**
     * Mettre à jour le compteur
     */
    public void setCount(int count) {
        int oldCount = this.count;
        this.count = count;

        if (count > 0) {
            countLabel.setText(count > 99 ? "99+" : String.valueOf(count));

            // Adapter la taille du badge
            double radius = count > 99 ? 12 : (count > 9 ? 11 : 10);
            badge.setRadius(radius);

            badge.setVisible(true);
            countLabel.setVisible(true);

            // Animation seulement si le compte a augmenté
            if (count > oldCount) {
                animateBadge();
            }
        } else {
            badge.setVisible(false);
            countLabel.setVisible(false);
        }
    }

    /**
     * Obtenir le compteur actuel
     */
    public int getCount() {
        return count;
    }

    /**
     * Animation d'entrée du badge
     */
    private void animateBadge() {
        // Animation de scale (rebond)
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), this);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        // Animation de rotation
        RotateTransition rotate = new RotateTransition(Duration.millis(300), this);
        rotate.setFromAngle(-15);
        rotate.setToAngle(15);
        rotate.setCycleCount(2);
        rotate.setAutoReverse(true);

        // Jouer les animations
        ParallelTransition parallel = new ParallelTransition(scale, rotate);
        parallel.play();
    }

    /**
     * Animation de pulsation (pour attirer l'attention)
     */
    public void pulse() {
        if (count == 0) return;

        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), badge);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.3);
        pulse.setToY(1.3);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    /**
     * Réinitialiser le badge
     */
    public void reset() {
        setCount(0);
    }

    /**
     * Incrémenter le compteur
     */
    public void increment() {
        setCount(count + 1);
    }

    /**
     * Décrémenter le compteur
     */
    public void decrement() {
        setCount(Math.max(0, count - 1));
    }
}