package tn.neuron.ardhi.services.UserAndDiag;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.Base64;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

public class JavaCVFaceRecognizer implements FaceRecognitionService {

    private CascadeClassifier faceCascade;

    public JavaCVFaceRecognizer() {
        try {
            // Load the Haar Cascade for face detection
            File file = new File(System.getProperty("java.io.tmpdir"), "haarcascade_frontalface_alt.xml");
            if (!file.exists()) {
                System.out.println("Downloading Haar Cascade model...");
                URL url = new URL(
                        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_alt.xml");
                file = Loader.cacheResource(url);
            }
            faceCascade = new CascadeClassifier(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String extractFaceSignature(Image image) {
        Mat mat = javaFxImageToMat(image);
        Mat faceCrop = detectAndCropFace(mat);
        if (faceCrop == null || faceCrop.empty())
            return null;

        // Convert cropped face to Base64 String (PNG format)
        try {
            BytePointer bytePointer = new BytePointer();
            imencode(".png", faceCrop, bytePointer);
            byte[] bytes = new byte[(int) bytePointer.limit()];
            bytePointer.get(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean verifyFace(Image capturedImage, String storedSignature) {
        if (storedSignature == null || capturedImage == null)
            return false;

        Mat capturedMat = javaFxImageToMat(capturedImage);
        Mat capturedFace = detectAndCropFace(capturedMat);
        if (capturedFace == null || capturedFace.empty())
            return false;

        try {
            // Decode stored signature
            byte[] storedBytes = Base64.getDecoder().decode(storedSignature);
            Mat storedFace = imdecode(new Mat(storedBytes), IMREAD_GRAYSCALE);

            // Resize both to standard size for comparison
            Size size = new Size(150, 150);
            resize(capturedFace, capturedFace, size);
            resize(storedFace, storedFace, size);

            // Train LBPH on the stored face
            FaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.setThreshold(80.0); // Strictness threshold

            MatVector images = new MatVector(1);
            images.put(0, storedFace);

            Mat labels = new Mat(1, 1, CV_32SC1);
            IntIndexer idx = labels.createIndexer();
            idx.put(0, 0, 1); // Label for our single enrolled face is 1

            recognizer.train(images, labels);

            // Predict the captured face
            int[] label = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(capturedFace, label, confidence);

            System.out.println("Face recognition distance (confidence): " + confidence[0]);

            // In LBPH, lower confidence equals a closer distance/match
            return label[0] == 1 && confidence[0] < 80.0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Mat javaFxImageToMat(Image image) {
        BufferedImage bImg = SwingFXUtils.fromFXImage(image, null);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImg, "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            return imdecode(new Mat(imageInByte), IMREAD_GRAYSCALE);
        } catch (Exception e) {
            return new Mat();
        }
    }

    private Mat detectAndCropFace(Mat grayMat) {
        if (faceCascade == null || faceCascade.empty() || grayMat.empty())
            return null;

        RectVector faces = new RectVector();

        // Equalize hist to improve detection
        Mat eqMat = new Mat();
        equalizeHist(grayMat, eqMat);

        faceCascade.detectMultiScale(eqMat, faces);

        if (faces.size() == 0)
            return null;

        // Return the largest face
        Rect largestFace = faces.get(0);
        for (long i = 1; i < faces.size(); i++) {
            Rect f = faces.get(i);
            if (f.width() * f.height() > largestFace.width() * largestFace.height()) {
                largestFace = f;
            }
        }

        return new Mat(grayMat, largestFace);
    }
}
