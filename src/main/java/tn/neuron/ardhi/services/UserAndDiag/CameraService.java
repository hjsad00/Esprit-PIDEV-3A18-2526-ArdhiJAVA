package tn.neuron.ardhi.services.UserAndDiag;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class CameraService {

    private Webcam webcam;

    // Static block to register the IP Camera Driver if needed
    // In a real app, you might want a more dynamic way to switch drivers.
    // For now, we will try to use a composite driver or just register IP cam when
    // requested.

    static {
        // Optional: Pre-configuration if we wanted to enforce a specific driver
        // globally
    }

    public List<String> getAvailableCameras() {
        List<String> cameraNames = new ArrayList<>();
        // This will find default USB/Built-in cameras
        try {
            for (Webcam w : Webcam.getWebcams()) {
                cameraNames.add(w.getName());
            }
        } catch (Exception e) {
            System.err.println("Error listing cameras: " + e.getMessage());
        }
        return cameraNames;
    }

    public void startCamera(String cameraName) {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        // Find the webcam by name
        // specific logic to find the right webcam instance
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam w : webcams) {
            if (w.getName().equals(cameraName)) {
                this.webcam = w;
                break;
            }
        }

        if (webcam != null) {
            // Set resolution (optional, using default or trying common sizes)
            // webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
        }
    }

    public boolean connectIpCamera(String name, String url) throws MalformedURLException {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        // Register the IP camera
        IpCamDeviceRegistry.unregisterAll(); // Clear previous
        IpCamDeviceRegistry.register(name, url, IpCamMode.PUSH);

        try {
            Webcam.setDriver(new IpCamDriver());
        } catch (Exception e) {
            // Driver might already be set
        }

        // Now get the specific webcam
        List<Webcam> cams = Webcam.getWebcams();
        this.webcam = null;
        for (Webcam w : cams) {
            if (w.getName().equals(name)) {
                this.webcam = w;
                break;
            }
        }

        if (this.webcam != null) {
            try {
                // Return result of open()
                return this.webcam.open();
            } catch (Exception e) {
                System.err.println("Failed to open IP Camera: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public void stopCamera() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    public Image takeSnapshot() {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage bufferedImage = webcam.getImage();
            return SwingFXUtils.toFXImage(bufferedImage, null);
        }
        return null;
    }

    public BufferedImage takeBufferedSnapshot() {
        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }
        return null;
    }

    public boolean isOpen() {
        return webcam != null && webcam.isOpen();
    }
}
