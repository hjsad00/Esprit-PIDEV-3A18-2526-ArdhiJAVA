package tn.neuron.ardhi.utils.marketplace;

import javafx.application.Platform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Micro-serveur HTTP local qui écoute sur http://localhost:9000.
 *
 * Stripe redirige vers :
 * - /payment/success → callback(true)
 * - /payment/cancel → callback(false)
 *
 * Le callback est exécuté sur le thread JavaFX (Platform.runLater).
 */
public class LocalPaymentServer {

    private static final int PORT = 9000;

    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean running = false;

    /**
     * Démarre le serveur en arrière-plan.
     * 
     * @param callback appelé avec {@code true} si succès, {@code false} si
     *                 annulation.
     */
    public void start(Consumer<Boolean> callback) {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;

            listenerThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        handleRequest(client, callback);
                    } catch (SocketException e) {
                        // Serveur fermé volontairement – on sort
                        if (running) {
                            System.err.println("[LocalPaymentServer] Erreur socket : " + e.getMessage());
                        }
                    } catch (IOException e) {
                        System.err.println("[LocalPaymentServer] Erreur I/O : " + e.getMessage());
                    }
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.setName("LocalPaymentServer-Thread");
            listenerThread.start();

            System.out.println("[LocalPaymentServer] Démarré sur le port " + PORT);

        } catch (IOException e) {
            System.err.println("[LocalPaymentServer] Impossible de démarrer : " + e.getMessage());
        }
    }

    /** Arrête proprement le serveur. */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[LocalPaymentServer] Erreur à l'arrêt : " + e.getMessage());
        }
        System.out.println("[LocalPaymentServer] Arrêté.");
    }

    // -------------------------------------------------------------------------

    private void handleRequest(Socket client, Consumer<Boolean> callback) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            // Lire la première ligne HTTP (ex. "GET /payment/success HTTP/1.1")
            String requestLine = in.readLine();
            if (requestLine == null)
                return;

            System.out.println("[LocalPaymentServer] Requête : " + requestLine);

            boolean success = requestLine.contains("/payment/success");
            boolean cancel = requestLine.contains("/payment/cancel");

            if (success || cancel) {
                String bodyHtml = success ? buildSuccessPage() : buildCancelPage();
                sendHtmlResponse(out, bodyHtml);

                // Notifier le contrôleur JavaFX sur le FX thread
                stop(); // Une seule notification suffit
                Platform.runLater(() -> callback.accept(success));
            } else {
                // Route inconnue – répondre 404
                out.print("HTTP/1.1 404 Not Found\r\n\r\n");
                out.flush();
            }

        } catch (IOException e) {
            System.err.println("[LocalPaymentServer] Erreur traitement requête : " + e.getMessage());
        }
    }

    private void sendHtmlResponse(PrintWriter out, String html) {
        byte[] bytes;
        try {
            bytes = html.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = html.getBytes();
        }
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Content-Type: text/html; charset=UTF-8\r\n");
        out.print("Content-Length: " + bytes.length + "\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        out.print(html);
        out.flush();
    }

    private String buildSuccessPage() {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>"
                + "<style>body{font-family:Arial,sans-serif;background:#f0fff4;display:flex;"
                + "align-items:center;justify-content:center;height:100vh;margin:0;}"
                + ".card{background:#fff;border-radius:16px;padding:48px;text-align:center;"
                + "box-shadow:0 4px 24px rgba(0,0,0,.12);max-width:400px;}"
                + ".icon{font-size:64px;margin-bottom:16px;}"
                + "h1{color:#2d7a4f;margin:0 0 12px;} p{color:#555;}</style></head>"
                + "<body><div class='card'><div class='icon'>✅</div>"
                + "<h1>Paiement réussi !</h1>"
                + "<p>Votre commande a été confirmée.<br>Vous pouvez fermer cette fenêtre.</p>"
                + "</div></body></html>";
    }

    private String buildCancelPage() {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>"
                + "<style>body{font-family:Arial,sans-serif;background:#fff5f5;display:flex;"
                + "align-items:center;justify-content:center;height:100vh;margin:0;}"
                + ".card{background:#fff;border-radius:16px;padding:48px;text-align:center;"
                + "box-shadow:0 4px 24px rgba(0,0,0,.12);max-width:400px;}"
                + ".icon{font-size:64px;margin-bottom:16px;}"
                + "h1{color:#c62828;margin:0 0 12px;} p{color:#555;}</style></head>"
                + "<body><div class='card'><div class='icon'>❌</div>"
                + "<h1>Paiement annulé</h1>"
                + "<p>Votre panier est intact.<br>Vous pouvez fermer cette fenêtre.</p>"
                + "</div></body></html>";
    }
}
