package tn.neuron.ardhi.utils.UserAndDiag;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    // 1. Variable statique pour stocker l'instance unique
    private static MyDatabase instance;
    private Connection cnx;

    private final String url = AppConfig.get("db.url", "jdbc:mysql://localhost:3306/ardhi?createDatabaseIfNotExist=true");
    private final String user = AppConfig.get("db.user", "root");
    private final String password = AppConfig.get("db.password", "");

    // 2. Constructeur privé : Personne ne peut faire "new MyDatabase()" de
    // l'extérieur
    private MyDatabase() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            // Initialiser le schéma de la base de données (si ce n'est pas déjà fait)
            // 1. User & Diag (Core - creates 'user' table)
            tn.neuron.ardhi.utils.UserAndDiag.DatabaseInitializer.initialize(cnx);

            // 2. Evenement
            tn.neuron.ardhi.utils.Evenement.DatabaseInitializer.initialize(cnx);

            // 3. Materiel & Maintenance
            tn.neuron.ardhi.utils.MaterielEtMaintenance.DatabaseInitializer.initialize(cnx);

            // 4. Parcelle & Cultures
            tn.neuron.ardhi.utils.Parcelle_Cultures.DatabaseInitializer.initialize(cnx);

            // 5. Gestion Employe
            tn.neuron.ardhi.utils.gestionemployeutils.DatabaseInitializer.initialize(cnx);

            // 6. Marketplace
            tn.neuron.ardhi.utils.marketplace.DatabaseInitializer.initialize(cnx);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException(
                    "Impossible de se connecter à la base de données. Vérifiez que MySQL est lancé et que la base 'ardhi' existe.\nErreur: "
                            + e.getMessage(),
                    e);
        }
    }
    // 3. Méthode publique statique pour récupérer l'instance
    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}