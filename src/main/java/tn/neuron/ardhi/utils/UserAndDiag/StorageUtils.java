package tn.neuron.ardhi.utils.UserAndDiag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utilitaire centralisé pour la gestion du stockage des fichiers.
 */
public class StorageUtils {

    // Constructeur privé pour empêcher l'instanciation
    private StorageUtils() {
    }

    // Force re-index

    // Dossier de stockage centralisé (peut être externalisé dans un fichier de
    // config plus tard)
    private static final String STORAGE_DIR = System.getProperty("user.home") + "/ardhi_images/";

    /**
     * Sauvegarde une copie de l'image sélectionnée dans le dossier géré par
     * l'application.
     * 
     * @param sourceFile Le fichier original sélectionné par l'utilisateur
     * @return L'URI absolue du fichier sauvegardé à stocker en base de données.
     * @throws IOException En cas d'erreur d'écriture (disque plein, permissions...)
     */
    public static String saveImage(File sourceFile) throws IOException {
        if (sourceFile == null)
            return null;

        File dossier = new File(STORAGE_DIR);
        if (!dossier.exists()) {
            if (!dossier.mkdirs()) {
                throw new IOException("Impossible de créer le dossier de stockage : " + STORAGE_DIR);
            }
        }

        // Génération d'un nom unique : timestamp + nom original
        String nomFichier = "diag_" + System.currentTimeMillis() + "_" + sourceFile.getName();
        File destination = new File(dossier, nomFichier);

        // Copie sécurisée
        Files.copy(sourceFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return destination.toURI().toString();
    }
}