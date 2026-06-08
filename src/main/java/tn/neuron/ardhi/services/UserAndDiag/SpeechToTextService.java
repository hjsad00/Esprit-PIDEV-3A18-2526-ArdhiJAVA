package tn.neuron.ardhi.services.UserAndDiag;

import javafx.application.Platform;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.neuron.ardhi.utils.UserAndDiag.AppConfig;

/**
 * Speech-to-Text service using the Groq Whisper API.
 * Records audio from the microphone and transcribes it to French text.
 */
public class SpeechToTextService {

    private static final String API_KEY = AppConfig.get("groq.api.key");
    private static final String TRANSCRIPTION_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "whisper-large-v3";

    private TargetDataLine microphone;
    private ByteArrayOutputStream audioBuffer;
    private volatile boolean recording = false;
    private Thread recordingThread;
    private AudioFormat activeFormat;

    // Formats to try, in order of preference
    private static final AudioFormat[] FORMATS_TO_TRY = {
            new AudioFormat(44100, 16, 1, true, false), // 44.1kHz 16-bit mono LE signed
            new AudioFormat(44100, 16, 2, true, false), // 44.1kHz 16-bit stereo
            new AudioFormat(48000, 16, 1, true, false), // 48kHz mono
            new AudioFormat(48000, 16, 2, true, false), // 48kHz stereo
            new AudioFormat(16000, 16, 1, true, false), // 16kHz mono
            new AudioFormat(8000, 16, 1, true, false), // 8kHz mono (telephony)
    };

    // Known Whisper hallucination patterns for silence
    private static final String[] SILENCE_HALLUCINATIONS = {
            "sous-titres", "sous-titrage", "société radio", "radio-canada",
            "amara.org", "communauté d'amara", "merci d'avoir regardé",
            "st'", "copyright", "www.", ".com", ".org"
    };

    public static boolean isMicrophoneAvailable() {
        for (AudioFormat fmt : FORMATS_TO_TRY) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
            if (AudioSystem.isLineSupported(info))
                return true;
        }
        return false;
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * Finds the best working microphone — sorts devices by priority to pick
     * real hardware (earbuds, headsets, named mics) over generic Windows drivers.
     */
    private TargetDataLine findWorkingMicrophone() throws LineUnavailableException {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        // Priority tiers for device selection
        List<Mixer.Info> tier1_headset = new ArrayList<>(); // casque, earbuds, headset
        List<Mixer.Info> tier2_mic = new ArrayList<>(); // microphone, mic
        List<Mixer.Info> tier3_hardware = new ArrayList<>(); // other named hardware
        List<Mixer.Info> tier4_generic = new ArrayList<>(); // "Pilote de capture", "Primary"

        for (Mixer.Info mi : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mi);
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            if (targetLines.length == 0)
                continue;

            String name = mi.getName().toLowerCase();
            String desc = mi.getDescription().toLowerCase();

            // Skip known virtual/loopback devices
            if (name.contains("stereo mix") || name.contains("what u hear") ||
                    name.contains("loopback") || name.contains("virtual")) {
                System.out.println("[STT] Skipping virtual device: " + mi.getName());
                continue;
            }

            // Skip "Port Mixer" devices — they don't do TargetDataLine recording
            if (desc.contains("port mixer")) {
                System.out.println("[STT] Skipping port mixer: " + mi.getName());
                continue;
            }

            // Classify into priority tiers
            if (name.contains("casque") || name.contains("earbuds") || name.contains("headset") ||
                    name.contains("honor") || name.contains("airpod") || name.contains("bluetooth")) {
                tier1_headset.add(mi);
            } else if (name.contains("microphone") || name.contains("mic") || name.contains("réseau de micro")) {
                tier2_mic.add(mi);
            } else if (name.contains("pilote") || name.contains("primary") || name.contains("principal")) {
                tier4_generic.add(mi);
            } else {
                tier3_hardware.add(mi);
            }
        }

        // Merge in priority order
        List<Mixer.Info> sorted = new ArrayList<>();
        sorted.addAll(tier1_headset);
        sorted.addAll(tier2_mic);
        sorted.addAll(tier3_hardware);
        sorted.addAll(tier4_generic);

        System.out.println("[STT] " + sorted.size() + " candidate(s): " +
                tier1_headset.size() + " headset, " + tier2_mic.size() + " mic, " +
                tier3_hardware.size() + " other, " + tier4_generic.size() + " generic");

        // Try each candidate with each format
        for (Mixer.Info mi : sorted) {
            Mixer mixer = AudioSystem.getMixer(mi);
            for (AudioFormat fmt : FORMATS_TO_TRY) {
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, fmt);
                if (mixer.isLineSupported(lineInfo)) {
                    try {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                        line.open(fmt);
                        activeFormat = fmt;
                        LogUtils.info(getClass(),
                                String.format("Using microphone: %s @ %.0fHz", mi.getName(), fmt.getSampleRate()));
                        return line;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Absolute fallback
        for (AudioFormat fmt : FORMATS_TO_TRY) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
            if (AudioSystem.isLineSupported(info)) {
                try {
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                    line.open(fmt);
                    activeFormat = fmt;
                    LogUtils.info(getClass(), "Using default AudioSystem line @ " + fmt.getSampleRate() + "Hz");
                    return line;
                } catch (Exception ignored) {
                }
            }
        }

        throw new LineUnavailableException("No working microphone found.");
    }

    /**
     * Computes RMS (Root Mean Square) audio level from 16-bit PCM samples.
     * Returns a value between 0.0 (silence) and 1.0 (max volume).
     */
    private double computeRMS(byte[] audioData) {
        long sum = 0;
        int sampleCount = audioData.length / 2; // 16-bit = 2 bytes per sample
        for (int i = 0; i < audioData.length - 1; i += 2) {
            // Little-endian 16-bit signed
            short sample = (short) ((audioData[i] & 0xFF) | (audioData[i + 1] << 8));
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / sampleCount);
        return rms / 32768.0; // Normalize to 0.0-1.0
    }

    public void startRecording() {
        if (recording)
            return;

        try {
            microphone = findWorkingMicrophone();
            microphone.start();

            audioBuffer = new ByteArrayOutputStream();
            recording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (recording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioBuffer.write(buffer, 0, bytesRead);
                    }
                }
            }, "STT-Recording-Thread");
            recordingThread.setDaemon(true);
            recordingThread.start();

            System.out.println("[STT] Recording started.");

        } catch (LineUnavailableException e) {
            System.err.println("[STT] ERROR: Could not open microphone: " + e.getMessage());
            e.printStackTrace();
            recording = false;
        }
    }

    public void stopAndTranscribe(Consumer<String> onResult, Consumer<String> onError) {
        if (!recording) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept("Aucun enregistrement en cours."));
            }
            return;
        }

        recording = false;
        try {
            if (recordingThread != null)
                recordingThread.join(1000);
        } catch (InterruptedException ignored) {
        }

        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }

        byte[] rawAudio = audioBuffer.toByteArray();
        AudioFormat fmt = activeFormat;

        // Audio level check
        double rms = computeRMS(rawAudio);

        if (rawAudio.length < 1600) {
            Platform.runLater(() -> {
                if (onError != null)
                    onError.accept("Enregistrement trop court.");
            });
            return;
        }

        new Thread(() -> {
            try {
                byte[] wavData = convertToWav(rawAudio, fmt);
                String text = sendToGroqWhisper(wavData);

                // Filter out known Whisper hallucinations for silence
                if (text != null && isSilenceHallucination(text)) {
                    LogUtils.error(getClass(), "Detected Whisper silence hallucination. Rejecting.");
                    Platform.runLater(() -> {
                        if (onError != null)
                            onError.accept(
                                    "Microphone n'a pas capturé de voix. Vérifiez que vous parlez assez fort.");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    if (text != null && !text.isEmpty()) {
                        onResult.accept(text);
                    } else {
                        if (onError != null)
                            onError.accept("Transcription vide.");
                    }
                });
            } catch (Exception e) {
                LogUtils.error(getClass(), "Transcription failed", e);
                Platform.runLater(() -> {
                    if (onError != null)
                        onError.accept("Erreur: " + e.getMessage());
                });
            }
        }, "STT-Transcribe-Thread").start();
    }

    /**
     * Checks if a transcription is a known Whisper hallucination for silence.
     */
    private boolean isSilenceHallucination(String text) {
        String lower = text.toLowerCase().trim();
        for (String pattern : SILENCE_HALLUCINATIONS) {
            if (lower.contains(pattern))
                return true;
        }
        // Also detect very short generic outputs
        if (lower.length() < 3)
            return true;
        return false;
    }

    public void stopAndTranscribe(Consumer<String> onResult) {
        stopAndTranscribe(onResult, null);
    }

    private byte[] convertToWav(byte[] rawPcm, AudioFormat format) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawPcm);
        AudioInputStream ais = new AudioInputStream(bais, format, rawPcm.length / format.getFrameSize());
        ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
        return wavOut.toByteArray();
    }

    private String sendToGroqWhisper(byte[] wavData) throws IOException {
        String boundary = "----STTBoundary" + System.currentTimeMillis();

        URL url = new URL(TRANSCRIPTION_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);

        try (OutputStream os = conn.getOutputStream()) {
            writeMultipartField(os, boundary, "file", "recording.wav", "audio/wav", wavData);
            writeMultipartTextField(os, boundary, "model", MODEL);
            writeMultipartTextField(os, boundary, "language", "fr");
            writeMultipartTextField(os, boundary, "temperature", "0");
            writeMultipartTextField(os, boundary, "prompt",
                    "Agriculture, diagnostic, traitement, tomate, blé, olives, maladie, fongicide, " +
                            "sol, feuille, plant, récolte, engrais, arrosage, serre, champ, culture, insecte, " +
                            "mildiou, oïdium, rouille, botrytis, pucerons.");
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            InputStream errorStream = conn.getErrorStream();
            String errorBody = errorStream != null ? readStream(errorStream) : "Unknown error";
            LogUtils.error(getClass(), "API error " + responseCode + ": " + errorBody);
            throw new IOException("API error " + responseCode + ": " + errorBody);
        }

        String responseBody = readStream(conn.getInputStream());
        return extractTranscription(responseBody);
    }

    private String extractTranscription(String json) {
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .trim();
        }
        LogUtils.error(getClass(), "Could not extract transcription from JSON");
        return null;
    }

    private void writeMultipartField(OutputStream os, String boundary, String fieldName,
            String fileName, String contentType, byte[] data) throws IOException {
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n";
        os.write(header.getBytes(StandardCharsets.UTF_8));
        os.write(data);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeMultipartTextField(OutputStream os, String boundary, String fieldName, String value)
            throws IOException {
        String part = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n" +
                value + "\r\n";
        os.write(part.getBytes(StandardCharsets.UTF_8));
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            return sb.toString();
        }
    }
}
