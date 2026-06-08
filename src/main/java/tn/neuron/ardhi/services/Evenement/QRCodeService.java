package tn.neuron.ardhi.services.Evenement;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

/**
 * Service de gestion des QR codes pour le check-in des participants.
 * Génère un QR code unique par participation et valide le scan.
 */
public class QRCodeService {

    private static final int QR_SIZE = 300;
    private static final String QR_FORMAT = "PNG";
    private static final String QR_DIR = "uploads/qrcodes/";
    private Connection connection;

    public QRCodeService() {
        this.connection = MyDatabase.getInstance().getCnx();
        ensureQRDirectory();
        ensureQRColumn();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void ensureQRDirectory() {
        try {
            Path dir = Paths.get(QR_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.err.println("Erreur création dossier QR: " + e.getMessage());
        }
    }

    /**
     * Ajoute la colonne qr_code_token si elle n'existe pas encore.
     */
    private void ensureQRColumn() {
        try {
            // Vérifier si la colonne existe
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "participation", "qr_code_token");
            if (!rs.next()) {
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(
                        "ALTER TABLE participation ADD COLUMN qr_code_token VARCHAR(64) UNIQUE NULL"
                );
                stmt.close();
                System.out.println("✓ Colonne qr_code_token ajoutée à la table participation");
            }
            rs.close();
        } catch (SQLException e) {
            // Colonne déjà existante ou autre erreur non critique
            System.out.println("Info colonne QR: " + e.getMessage());
        }
    }

    // ── Génération ───────────────────────────────────────────────────────────

    /**
     * Génère un token unique et un QR code PNG pour une participation.
     * Sauvegarde le token en base et le fichier image sur disque.
     *
     * @return chemin du fichier QR code, ou null en cas d'erreur
     */
    public String genererQRCodeParticipation(int participationId, int evenementId, String nomParticipant) {
        try {
            // 1. Générer un token unique
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

            // 2. Construire le contenu du QR (format structuré)
            String qrContent = buildQRContent(token, participationId, evenementId);

            // 3. Générer l'image QR
            BufferedImage qrImage = generateQRImage(qrContent);
            if (qrImage == null) return null;

            // 4. Sauvegarder sur disque
            String filename = "qr_" + participationId + "_" + token.substring(0, 8) + ".png";
            String filePath = QR_DIR + filename;
            Path outputPath = Paths.get(filePath);
            ImageIO.write(qrImage, QR_FORMAT, outputPath.toFile());

            // 5. Sauvegarder le token en base
            saveTokenToDatabase(participationId, token, filePath);

            System.out.println("✓ QR Code généré pour participation #" + participationId + ": " + filePath);
            return filePath;

        } catch (Exception e) {
            System.err.println("Erreur génération QR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Génère l'image QR et retourne un Base64 (pour affichage direct dans JavaFX).
     */
    public String genererQRCodeBase64(int participationId, int evenementId) {
        try {
            String token = getOrCreateToken(participationId, evenementId);
            String qrContent = buildQRContent(token, participationId, evenementId);
            BufferedImage qrImage = generateQRImage(qrContent);
            if (qrImage == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, QR_FORMAT, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            System.err.println("Erreur génération QR Base64: " + e.getMessage());
            return null;
        }
    }

    private String getOrCreateToken(int participationId, int evenementId) {
        // Chercher un token existant
        String existing = getTokenForParticipation(participationId);
        if (existing != null) return existing;

        // Créer un nouveau token
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        saveTokenToDatabase(participationId, token, null);
        return token;
    }

    private String buildQRContent(String token, int participationId, int evenementId) {
        return "ARDHI_CHECKIN|" + token + "|P" + participationId + "|E" + evenementId;
    }

    private BufferedImage generateQRImage(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (WriterException e) {
            System.err.println("Erreur écriture QR: " + e.getMessage());
            return null;
        }
    }

    // ── Base de données ──────────────────────────────────────────────────────

    private void saveTokenToDatabase(int participationId, String token, String filePath) {
        String sql = "UPDATE participation SET qr_code_token = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setInt(2, participationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde token QR: " + e.getMessage());
        }
    }

    public String getTokenForParticipation(int participationId) {
        String sql = "SELECT qr_code_token FROM participation WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("qr_code_token");
        } catch (SQLException e) {
            System.err.println("Erreur lecture token QR: " + e.getMessage());
        }
        return null;
    }

    // ── Validation / Check-in ────────────────────────────────────────────────

    /**
     * Résultat d'un scan QR.
     */
    public static class CheckInResult {
        private final boolean success;
        private final String message;
        private final Participation participation;
        private final Evenement evenement;

        public CheckInResult(boolean success, String message, Participation participation, Evenement evenement) {
            this.success = success;
            this.message = message;
            this.participation = participation;
            this.evenement = evenement;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Participation getParticipation() { return participation; }
        public Evenement getEvenement() { return evenement; }
    }

    /**
     * Traite le contenu scanné d'un QR code.
     * Parse le token, vérifie la participation et marque PRESENT.
     */
    public CheckInResult processQRScan(String qrContent) {
        try {
            // Parser le contenu
            if (!qrContent.startsWith("ARDHI_CHECKIN|")) {
                return new CheckInResult(false, "QR code invalide — format non reconnu", null, null);
            }

            String[] parts = qrContent.split("\\|");
            if (parts.length != 4) {
                return new CheckInResult(false, "QR code invalide — données incomplètes", null, null);
            }

            String token = parts[1];
            int participationId = Integer.parseInt(parts[2].substring(1));
            int evenementId = Integer.parseInt(parts[3].substring(1));

            // Vérifier le token en base
            Participation participation = findParticipationByToken(token, participationId);
            if (participation == null) {
                return new CheckInResult(false, "QR code invalide — token non trouvé", null, null);
            }

            // Vérifier l'événement
            EvenementService evenementService = new EvenementService();
            Evenement evenement = evenementService.getEvenementById(evenementId);
            if (evenement == null) {
                return new CheckInResult(false, "Événement introuvable", null, null);
            }

            // Vérifier le statut de la participation
            if ("PRESENT".equals(participation.getStatut())) {
                return new CheckInResult(false,
                        "⚠️ " + participation.getNomComplet() + " est déjà marqué(e) présent(e) !",
                        participation, evenement);
            }

            if ("ANNULE".equals(participation.getStatut())) {
                return new CheckInResult(false,
                        "❌ L'inscription de " + participation.getNomComplet() + " est annulée",
                        participation, evenement);
            }

            // Marquer présent
            ParticipationService participationService = new ParticipationService();
            boolean updated = participationService.updateStatut(participationId, "PRESENT");

            if (updated) {
                participation.setStatut("PRESENT");
                return new CheckInResult(true,
                        "✅ " + participation.getNomComplet() + " — Check-in réussi !",
                        participation, evenement);
            } else {
                return new CheckInResult(false, "Erreur lors du marquage de présence", participation, evenement);
            }

        } catch (Exception e) {
            System.err.println("Erreur traitement QR: " + e.getMessage());
            return new CheckInResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    private Participation findParticipationByToken(String token, int participationId) {
        String sql = "SELECT p.*, u.nom, u.prenom, u.email " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "WHERE p.qr_code_token = ? AND p.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setInt(2, participationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Participation p = new Participation();
                p.setId(rs.getInt("id"));
                p.setIdEvenement(rs.getInt("id_evenement"));
                p.setIdUtilisateur(rs.getInt("id_utilisateur"));
                p.setStatut(rs.getString("statut"));
                p.setCommentaire(rs.getString("commentaire"));
                p.setNomUtilisateur(rs.getString("nom"));
                p.setPrenomUtilisateur(rs.getString("prenom"));
                p.setEmailUtilisateur(rs.getString("email"));
                Timestamp ts = rs.getTimestamp("date_inscription");
                if (ts != null) p.setDateInscription(ts.toLocalDateTime());
                return p;
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche par token: " + e.getMessage());
        }
        return null;
    }

    /**
     * Valide directement un token (pour compatibilité scanner mobile).
     */
    public CheckInResult validateToken(String token) {
        String sql = "SELECT id, id_evenement FROM participation WHERE qr_code_token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int participationId = rs.getInt("id");
                int evenementId = rs.getInt("id_evenement");
                String qrContent = "ARDHI_CHECKIN|" + token + "|P" + participationId + "|E" + evenementId;
                return processQRScan(qrContent);
            }
        } catch (SQLException e) {
            System.err.println("Erreur validation token: " + e.getMessage());
        }
        return new CheckInResult(false, "Token inconnu", null, null);
    }
}