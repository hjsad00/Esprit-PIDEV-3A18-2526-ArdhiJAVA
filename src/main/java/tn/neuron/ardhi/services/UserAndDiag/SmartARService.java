package tn.neuron.ardhi.services.UserAndDiag;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.ArrayList;
import java.util.List;

public class SmartARService {

    public static class ARResult {
        public double plantCentroidX = -1;
        public double plantCentroidY = -1;
        public double vegetationCoverage = 0; // 0.0 to 1.0
        public List<Point> stressPoints = new ArrayList<>();
        public boolean targetLocked = false;
    }

    public static class Point {
        public double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Optimization: Process a downscaled version for performance
    private static final int PROCESS_WIDTH = 320;
    private static final int PROCESS_HEIGHT = 240;

    // EMA State for Anti-Jitter
    private double smoothedCentroidX = -1;
    private double smoothedCentroidY = -1;
    private double smoothedCoverage = 0;
    private static final double EMA_ALPHA = 0.2; // Smoothing factor (0.0 - 1.0)

    /**
     * Analyzes the video frame to detect vegetation and stress zones.
     * Uses "Excess Green" (ExG = 2g - r - b) for robust plant segmentation.
     */
    public ARResult processFrame(Image input) {
        ARResult result = new ARResult();
        if (input == null)
            return result;

        PixelReader reader = input.getPixelReader();
        double width = input.getWidth();
        double height = input.getHeight();

        // Steps for scaling coordinates
        double stepX = width / PROCESS_WIDTH;
        double stepY = height / PROCESS_HEIGHT;

        long sumX = 0;
        long sumY = 0;
        long plantPixelCount = 0;

        // Thresholds
        // ExG > 20 is usually green vegetation
        // Stress: High Red/Green ratio or specific yellow/brown hues in plant area

        int cols = PROCESS_WIDTH / 15;
        int rows = PROCESS_HEIGHT / 15;
        int[][] stressGrid = new int[cols][rows];

        for (int y = 0; y < PROCESS_HEIGHT; y++) {
            for (int x = 0; x < PROCESS_WIDTH; x++) {
                // Map to source coordinates
                int srcX = (int) (x * stepX);
                int srcY = (int) (y * stepY);

                // Bounds check
                if (srcX >= width)
                    srcX = (int) width - 1;
                if (srcY >= height)
                    srcY = (int) height - 1;

                int argb = reader.getArgb(srcX, srcY);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // 1. Vegetation Index (ExG)
                // ExG = 2*G - R - B
                double exG = (2 * g) - r - b;

                // 2. Fruit/Flower Detection (Warm Colors)
                // Heuristic: Significant Red component, Red > Blue, Red >= Green (or close to
                // it)
                // For orange/yellow: R is high, G is med-high, B is low.
                boolean isFruit = (r > 150) && (g > 100) && (b < 100) && (r > b + 30);

                // Also check for deep reds (tomatoes, apples) where R >> G
                boolean isRedFruit = (r > 130) && (r > g + 20) && (r > b + 20);

                if (exG > 20 || isFruit || isRedFruit) {
                    // It's a plant or fruit!
                    sumX += srcX;
                    sumY += srcY;
                    plantPixelCount++;

                    // Stress Detection Update
                    // If it's detected via ExG but has low ExG, it might be stressed edge
                    if (exG > 20 && exG < 45) {
                        int gridX = x / 15;
                        int gridY = y / 15;
                        if (gridX < cols && gridY < rows) {
                            stressGrid[gridX][gridY]++;
                        }
                    }
                    // For fruits, we don't necessarily mark them as stress,
                    // but we could mark distinct color variations if needed.
                }

            }
        }

        // Aggregate stress points (Clustering)
        for (int gridX = 0; gridX < cols; gridX++) {
            for (int gridY = 0; gridY < rows; gridY++) {
                if (stressGrid[gridX][gridY] > 3) { // Threshold for stress density
                    // Map center of grid cell back to source coordinates
                    int srcX = (int) ((gridX * 15 + 7.5) * stepX);
                    int srcY = (int) ((gridY * 15 + 7.5) * stepY);
                    result.stressPoints.add(new Point(srcX, srcY));
                }
            }
        }

        if (plantPixelCount > 0) {
            double rawCentroidX = (double) sumX / plantPixelCount;
            double rawCentroidY = (double) sumY / plantPixelCount;
            double rawCoverage = (double) plantPixelCount / (PROCESS_WIDTH * PROCESS_HEIGHT); // Approx

            if (smoothedCentroidX == -1) {
                // Initialize EMA
                smoothedCentroidX = rawCentroidX;
                smoothedCentroidY = rawCentroidY;
                smoothedCoverage = rawCoverage;
            } else {
                // Apply EMA
                smoothedCentroidX = smoothedCentroidX + EMA_ALPHA * (rawCentroidX - smoothedCentroidX);
                smoothedCentroidY = smoothedCentroidY + EMA_ALPHA * (rawCentroidY - smoothedCentroidY);
                smoothedCoverage = smoothedCoverage + EMA_ALPHA * (rawCoverage - smoothedCoverage);
            }

            result.plantCentroidX = smoothedCentroidX;
            result.plantCentroidY = smoothedCentroidY;
            result.vegetationCoverage = smoothedCoverage;

            // Lock target if coverage is significant (> 5%)
            if (result.vegetationCoverage > 0.05) {
                result.targetLocked = true;
            }
        } else {
            // Reset EMA if target lost completely
            smoothedCentroidX = -1;
            smoothedCentroidY = -1;
            smoothedCoverage = 0;
        }

        return result;
    }
}
