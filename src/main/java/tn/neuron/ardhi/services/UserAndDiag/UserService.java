package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;

import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    // ggg
    public UserService() {
        checkAndAlterTable();
    }

    private void checkAndAlterTable() {
        try (Statement st = cnx.createStatement()) {
            DatabaseMetaData md = cnx.getMetaData();

            // 1. Check for 2FA columns
            ResultSet rs = md.getColumns(null, null, "user", "two_factor_enabled");
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE user ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE");
                st.executeUpdate("ALTER TABLE user ADD COLUMN two_factor_code VARCHAR(10)");
                st.executeUpdate("ALTER TABLE user ADD COLUMN two_factor_expires_at DATETIME");
                st.executeUpdate("ALTER TABLE user ADD COLUMN reset_password_code VARCHAR(10)");
                st.executeUpdate("ALTER TABLE user ADD COLUMN reset_password_expires_at DATETIME");
                // Use MEDIUMTEXT for fingerprints as they are large Base64 strings
                st.executeUpdate("ALTER TABLE user ADD COLUMN fingerprint_signature MEDIUMTEXT");
                LogUtils.info(getClass(), "Added 2FA, Reset Password and Biometric columns to user table.");
            } else {
                // Check if reset password columns exist (migration)
                rs = md.getColumns(null, null, "user", "reset_password_code");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE user ADD COLUMN reset_password_code VARCHAR(10)");
                    st.executeUpdate("ALTER TABLE user ADD COLUMN reset_password_expires_at DATETIME");
                    LogUtils.info(getClass(), "Added reset password columns to user table.");
                }

                // 2. Check for fingerprint_signature if it was missing
                rs = md.getColumns(null, null, "user", "fingerprint_signature");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE user ADD COLUMN fingerprint_signature MEDIUMTEXT");
                    LogUtils.info(getClass(), "Added fingerprint_signature column to user table.");
                }

                // 2b. Check for face_signature
                rs = md.getColumns(null, null, "user", "face_signature");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE user ADD COLUMN face_signature LONGTEXT");
                    LogUtils.info(getClass(), "Added face_signature column to user table.");
                }

                // 2c. Check for phone and location
                rs = md.getColumns(null, null, "user", "phone");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE user ADD COLUMN phone VARCHAR(20)");
                    LogUtils.info(getClass(), "Added phone column to user table.");
                }
                rs = md.getColumns(null, null, "user", "location");
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE user ADD COLUMN location VARCHAR(255)");
                    LogUtils.info(getClass(), "Added location column to user table.");
                }
            }

            // 3. Fix existing columns: Ensure fingerprint_signature is MEDIUMTEXT (Fixing
            // Data Truncation error)
            // This is safe to run even if it's already MEDIUMTEXT (MySQL handles it)
            try {
                st.executeUpdate("ALTER TABLE user MODIFY COLUMN fingerprint_signature MEDIUMTEXT");
            } catch (SQLException ex) {
                // Ignore if column doesn't exist yet (logic above handles creation) or other
                // non-critical error
                LogUtils.info(getClass(), "Attempted to modify fingerprint_signature: " + ex.getMessage());
            }

        } catch (SQLException e) {
            LogUtils.error(getClass(), "Error checking/altering user table", e);
        }
    }
    // Auto-create table if not exists

    @Override
    public void ajouter(User user) throws SQLException {
        String req = "INSERT INTO user (email, role, password, nom, prenom, face_signature, phone, location) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setString(1, user.getEmail());
        pst.setString(2, user.getRole().name());

        // --- SÉCURITÉ : HASHAGE DU MOT DE PASSE (BCRYPT) ---
        // On génère un salt et on hash le mot de passe
        String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        pst.setString(3, hashed);
        // ---------------------------------------------------

        pst.setString(4, user.getNom());
        pst.setString(5, user.getPrenom());
        pst.setString(6, user.getFaceSignature());
        pst.setString(7, user.getPhone());
        pst.setString(8, user.getLocation());

        pst.executeUpdate();

    }

    @Override
    public void modifier(User user) throws SQLException {
        // Cette méthode met à jour les infos TEXTUELLES (Nom, Email, Rôles, Prénom)
        // Elle NE touche PAS au mot de passe.
        String req = "UPDATE user SET nom=?, prenom=?, email=?, role=?, two_factor_enabled=?, fingerprint_signature=?, face_signature=?, phone=?, location=? WHERE id=?";

        PreparedStatement pst = cnx.prepareStatement(req);

        pst.setString(1, user.getNom());
        pst.setString(2, user.getPrenom());
        pst.setString(3, user.getEmail());
        pst.setString(4, user.getRole().name());
        pst.setBoolean(5, user.isTwoFactorEnabled());
        pst.setString(6, user.getFingerprintSignature());
        pst.setString(7, user.getFaceSignature());
        pst.setString(8, user.getPhone());
        pst.setString(9, user.getLocation());
        pst.setInt(10, user.getId());

        pst.executeUpdate();

    }

    /**
     * Méthode spécifique pour modifier uniquement le mot de passe.
     */
    public void modifierMotDePasse(int idUser, String nouveauMdpEnClair) throws SQLException {
        String req = "UPDATE user SET password=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);

        // On crypte le nouveau mot de passe avant de l'envoyer en base
        String hashed = BCrypt.hashpw(nouveauMdpEnClair, BCrypt.gensalt());

        pst.setString(1, hashed);
        pst.setInt(2, idUser);

        pst.executeUpdate();

    }

    /**
     * Vérifie si le mot de passe fourni correspond au mot de passe actuel de
     * l'utilisateur.
     */
    public boolean verifierMotDePasse(int idUser, String mdpEnClair) throws SQLException {
        String req = "SELECT password FROM user WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, idUser);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            String passwordHashedInDb = rs.getString("password");

            // Fix for PHP/Symfony hashes ($2y$) and newer BCrypt ($2b$) not supported by
            // standard jBCrypt
            if (passwordHashedInDb != null && passwordHashedInDb.length() >= 4) {
                if (passwordHashedInDb.startsWith("$2y$") || passwordHashedInDb.startsWith("$2b$")) {
                    passwordHashedInDb = "$2a$" + passwordHashedInDb.substring(4);
                }
            }

            return BCrypt.checkpw(mdpEnClair, passwordHashedInDb);
        }
        return false;
    }

    public void generate2FACode(User u) throws SQLException {
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(10);

        String req = "UPDATE user SET two_factor_code=?, two_factor_expires_at=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, code);
        pst.setTimestamp(2, java.sql.Timestamp.valueOf(expiresAt));
        pst.setInt(3, u.getId());
        pst.executeUpdate();

        u.setTwoFactorCode(code);
        u.setTwoFactorExpiresAt(expiresAt);
    }

    public boolean verify2FACode(User u, String code) throws SQLException {
        String req = "SELECT two_factor_code, two_factor_expires_at FROM user WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, u.getId());
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            String dbCode = rs.getString("two_factor_code");
            Timestamp dbExpiresAt = rs.getTimestamp("two_factor_expires_at");

            if (dbCode != null && dbCode.equals(code) && dbExpiresAt != null
                    && dbExpiresAt.toLocalDateTime().isAfter(java.time.LocalDateTime.now())) {
                String clearReq = "UPDATE user SET two_factor_code=NULL, two_factor_expires_at=NULL WHERE id=?";
                PreparedStatement clearPst = cnx.prepareStatement(clearReq);
                clearPst.setInt(1, u.getId());
                clearPst.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void generateResetPasswordCode(User u) throws SQLException {
        String code = String.format("%04d", new java.util.Random().nextInt(9999));
        java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(10);

        String req = "UPDATE user SET reset_password_code=?, reset_password_expires_at=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, code);
        pst.setTimestamp(2, java.sql.Timestamp.valueOf(expiresAt));
        pst.setInt(3, u.getId());
        pst.executeUpdate();

        u.setResetPasswordCode(code);
        u.setResetPasswordExpiresAt(expiresAt);
    }

    public boolean verifyResetPasswordCode(User u, String code) throws SQLException {
        String req = "SELECT reset_password_code, reset_password_expires_at FROM user WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, u.getId());
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            String dbCode = rs.getString("reset_password_code");
            Timestamp dbExpiresAt = rs.getTimestamp("reset_password_expires_at");

            if (dbCode != null && dbCode.equals(code) && dbExpiresAt != null
                    && dbExpiresAt.toLocalDateTime().isAfter(java.time.LocalDateTime.now())) {
                String clearReq = "UPDATE user SET reset_password_code=NULL, reset_password_expires_at=NULL WHERE id=?";
                PreparedStatement clearPst = cnx.prepareStatement(clearReq);
                clearPst.setInt(1, u.getId());
                clearPst.executeUpdate();
                return true;
            }
        }
        return false;
    }

    public void toggleTwoFactor(int userId, boolean enabled) throws SQLException {
        String req = "UPDATE user SET two_factor_enabled=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setBoolean(1, enabled);
        pst.setInt(2, userId);
        pst.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM user WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

    }

    @Override
    public List<User> recuperer() throws SQLException {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM user ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            users.add(mapRowToUser(rs));
        }
        return users;
    }

    @Override
    public List<User> rechercher(String keyword) throws SQLException {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM user WHERE LOWER(nom) LIKE ? OR LOWER(prenom) LIKE ? OR LOWER(email) LIKE ? OR LOWER(role) LIKE ? ORDER BY id DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ps.setString(3, pattern);
        ps.setString(4, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            users.add(mapRowToUser(rs));
        }
        return users;
    }

    public User login(String email, String password) throws SQLException {
        String req = "SELECT * FROM user WHERE email = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, email);

        ResultSet rs = pst.executeQuery();

        if (!rs.next()) {
            return null; // Email introuvable
        }

        String passwordHashedInDb = rs.getString("password");

        // Fix for PHP/Symfony hashes ($2y$) and newer BCrypt ($2b$) not supported by
        // standard jBCrypt
        if (passwordHashedInDb != null && passwordHashedInDb.length() >= 4) {
            if (passwordHashedInDb.startsWith("$2y$") || passwordHashedInDb.startsWith("$2b$")) {
                passwordHashedInDb = "$2a$" + passwordHashedInDb.substring(4);
            }
        }

        // Vérification du mot de passe saisi avec le hash en base via BCrypt
        if (BCrypt.checkpw(password, passwordHashedInDb)) {
            User u = mapRowToUser(rs);
            u.setPassword(passwordHashedInDb); // On garde le hash en session
            return u;
        } else {
            return null; // Mot de passe incorrect
        }
    }

    public User chercherParEmail(String email) throws SQLException {
        String req = "SELECT * FROM user WHERE email = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, email);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return mapRowToUser(rs);
        }
        return null;
    }

    public List<User> getAgriculteurs() {
        List<User> agriculteurs = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE role = 'AGRICULTEUR' ORDER BY nom, prenom";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                agriculteurs.add(mapRowToUser(rs));
            }

            System.out.println("✅ " + agriculteurs.size() + " agriculteur(s) récupéré(s)");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des agriculteurs: " + e.getMessage());
            e.printStackTrace();
        }

        return agriculteurs;
    }

    public boolean existe(int id) throws SQLException {
        String req = "SELECT 1 FROM user WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    // Helper method to mapping ResultSet to User object
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setPassword(rs.getString("password"));
        u.setPoints(rs.getInt("points"));
        u.setLevel(rs.getInt("level"));
        try {
            u.setRole(Role.valueOf(rs.getString("role")));
        } catch (IllegalArgumentException | NullPointerException e) {
            u.setRole(Role.CLIENT); // Default fallback
        }
        try {
            u.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        } catch (SQLException e) {
            // Column might not exist yet if table wasn't altered
            u.setTwoFactorEnabled(false);
        }
        try {
            u.setFingerprintSignature(rs.getString("fingerprint_signature"));
        } catch (SQLException e) {
            u.setFingerprintSignature(null);
        }
        try {
            u.setFaceSignature(rs.getString("face_signature"));
        } catch (SQLException e) {
            u.setFaceSignature(null);
        }
        try {
            u.setResetPasswordCode(rs.getString("reset_password_code"));
            Timestamp ts = rs.getTimestamp("reset_password_expires_at");
            if (ts != null) {
                u.setResetPasswordExpiresAt(ts.toLocalDateTime());
            }
        } catch (SQLException e) {
            u.setResetPasswordCode(null);
        }
        try {
            u.setPhone(rs.getString("phone"));
        } catch (SQLException e) {
            u.setPhone(null);
        }
        try {
            u.setLocation(rs.getString("location"));
        } catch (SQLException e) {
            u.setLocation(null);
        }
        return u;
    }

    public int getNombreUtilisateurs() {
        String req = "SELECT COUNT(*) FROM user";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(UserService.class, "Erreur comptage users", e);
        }
        return 0;
    }

    public User chercherParId(int idUser) {
        String req = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            LogUtils.error(UserService.class, "Erreur chercherParId", e);
        }
        return null;
    }
}