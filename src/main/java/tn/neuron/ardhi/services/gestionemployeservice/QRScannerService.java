package tn.neuron.ardhi.services.gestionemployeservice;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Service de scan QR Code via webcam integree.
 *
 * Implementation hybride auto-detectee :
 *   Mode 1 : com.github.sarxos.webcam (JitPack) si disponible
 *   Mode 2 : AWT Robot fallback (100% JDK17, zero dependance externe)
 *
 * L'API publique est identique dans les deux modes.
 */
public class QRScannerService {

    private static final long COOLDOWN_MS  = 3000;
    private static final int  FPS_DELAY_MS = 100;

    private Thread          scanThread;
    private final AtomicBoolean scanning   = new AtomicBoolean(false);
    private final AtomicBoolean paused     = new AtomicBoolean(false);
    private long            dernierScanTs  = 0;

    private enum Mode { SARXOS, ROBOT }
    private Mode   mode;
    private Object webcamInstance; // sarxos Webcam charge par reflexion

    // Callbacks
    private Consumer<String> onQRDetected;
    private Consumer<Image>  onFrameReady;
    private Consumer<String> onError;

    public QRScannerService setOnQRDetected(Consumer<String> cb) { this.onQRDetected = cb; return this; }
    public QRScannerService setOnFrameReady(Consumer<Image>  cb) { this.onFrameReady = cb; return this; }
    public QRScannerService setOnError(Consumer<String>      cb) { this.onError      = cb; return this; }

    // ─────────────────────────────────────────────────────────────────────

    public boolean demarrer() {
        try {
            Class.forName("com.github.sarxos.webcam.Webcam");
            mode = Mode.SARXOS;
        } catch (ClassNotFoundException e) {
            mode = Mode.ROBOT;
            System.out.println("[QRScanner] sarxos absent -> mode Robot (capture ecran)");
        }
        return mode == Mode.SARXOS ? demarrerSarxos() : demarrerRobot();
    }

    // ── Mode SARXOS ───────────────────────────────────────────────────────

    private boolean demarrerSarxos() {
        try {
            Class<?> wc  = Class.forName("com.github.sarxos.webcam.Webcam");
            Class<?> res = Class.forName("com.github.sarxos.webcam.WebcamResolution");

            webcamInstance = wc.getMethod("getDefault").invoke(null);
            if (webcamInstance == null) { notifyError("Aucune webcam detectee."); return false; }

            Object vga  = res.getField("VGA").get(null);
            Dimension d = (Dimension) vga.getClass().getMethod("getSize").invoke(vga);
            wc.getMethod("setViewSize", Dimension.class).invoke(webcamInstance, d);
            wc.getMethod("open", boolean.class).invoke(webcamInstance, true);

            scanning.set(true); paused.set(false);
            scanThread = new Thread(() -> boucleSarxos(wc), "QR-Sarxos");
            scanThread.setDaemon(true);
            scanThread.start();
            System.out.println("[QRScanner] Mode SARXOS demarre.");
            return true;
        } catch (Exception e) {
            System.err.println("[QRScanner] Echec sarxos: " + e.getMessage() + " -> fallback Robot");
            mode = Mode.ROBOT;
            return demarrerRobot();
        }
    }

    private void boucleSarxos(Class<?> wc) {
        MultiFormatReader reader = new MultiFormatReader();
        while (scanning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                BufferedImage img = (BufferedImage) wc.getMethod("getImage").invoke(webcamInstance);
                if (img != null) traiterFrame(reader, img);
                Thread.sleep(FPS_DELAY_MS);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            catch (Exception ignored) {}
        }
        try { if (webcamInstance != null) wc.getMethod("close").invoke(webcamInstance); }
        catch (Exception ignored) {}
    }

    // ── Mode ROBOT (fallback 100% JDK) ───────────────────────────────────

    private boolean demarrerRobot() {
        if (GraphicsEnvironment.isHeadless()) {
            notifyError("Mode Robot impossible (environnement headless).");
            return false;
        }
        scanning.set(true); paused.set(false);
        scanThread = new Thread(this::boucleRobot, "QR-Robot");
        scanThread.setDaemon(true);
        scanThread.start();
        System.out.println("[QRScanner] Mode ROBOT demarre (capture ecran centrale 640x480).");
        return true;
    }

    private void boucleRobot() {
        try {
            Robot robot = new Robot();
            MultiFormatReader reader = new MultiFormatReader();
            Dimension ecran = Toolkit.getDefaultToolkit().getScreenSize();
            int w = 640, h = 480;
            Rectangle zone = new Rectangle((ecran.width - w) / 2, (ecran.height - h) / 2, w, h);

            while (scanning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    traiterFrame(reader, robot.createScreenCapture(zone));
                    Thread.sleep(FPS_DELAY_MS);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                catch (Exception ignored) {}
            }
        } catch (AWTException e) { notifyError("AWT Robot: " + e.getMessage()); }
    }

    // ── Traitement commun ─────────────────────────────────────────────────

    private void traiterFrame(MultiFormatReader reader, BufferedImage image) {
        if (onFrameReady != null) {
            Image fx = toFXImage(image);
            if (fx != null) Platform.runLater(() -> onFrameReady.accept(fx));
        }
        if (!paused.get()) {
            String qr = decode(reader, image);
            if (qr != null) {
                long now = System.currentTimeMillis();
                if (now - dernierScanTs > COOLDOWN_MS) {
                    dernierScanTs = now;
                    pauserTemporairement();
                    System.out.println("[QRScanner] Detecte: " + qr);
                    if (onQRDetected != null) Platform.runLater(() -> onQRDetected.accept(qr));
                }
            }
        }
    }

    private String decode(MultiFormatReader reader, BufferedImage img) {
        try {
            return reader.decodeWithState(
                    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)))
            ).getText();
        } catch (NotFoundException e) { return null; }
        catch (Exception e)           { return null; }
        finally                        { reader.reset(); }
    }

    // ── Arret / Pause ─────────────────────────────────────────────────────

    public void arreter() {
        scanning.set(false);
        if (scanThread != null) { scanThread.interrupt(); scanThread = null; }
        if (mode == Mode.SARXOS && webcamInstance != null) {
            try {
                Class.forName("com.github.sarxos.webcam.Webcam")
                        .getMethod("close").invoke(webcamInstance);
            } catch (Exception ignored) {}
        }
    }

    public void pauserTemporairement() {
        paused.set(true);
        Thread t = new Thread(() -> {
            try { Thread.sleep(COOLDOWN_MS); } catch (InterruptedException ignored) {}
            paused.set(false);
        });
        t.setDaemon(true); t.start();
    }

    // ── Utils ─────────────────────────────────────────────────────────────

    private Image toFXImage(BufferedImage awt) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(awt, "png", out);
            return new javafx.scene.image.Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) { return null; }
    }

    private void notifyError(String msg) {
        System.err.println("[QRScanner] " + msg);
        if (onError != null) Platform.runLater(() -> onError.accept(msg));
    }

    public boolean isScanning()    { return scanning.get(); }
    public boolean isPaused()      { return paused.get(); }
    public String  getModeActif()  { return mode != null ? mode.name() : "N/A"; }
}