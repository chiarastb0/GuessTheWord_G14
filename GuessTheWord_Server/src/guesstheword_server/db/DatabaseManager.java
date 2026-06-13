package guesstheword_server.db;

import guesstheword_server.ConfigManager;
import java.sql.*;

/**
 * Classe di utilità per la gestione della connessione al database SQLite.
 * Fornisce i metodi per aprire, recuperare e chiudere in modo sicuro la connessione.
 *
 */
public class DatabaseManager {
    
    private static Connection conn = null;
    
    /**
     * Recupera la connessione attiva al database.
     * Se la connessione non è ancora stata creata o risulta chiusa, ne inizializza una nuova
     * leggendo l'URL di configurazione.
     * * @return L'oggetto Connection per eseguire le query sul database.
     * @throws SQLException Se si verifica un errore durante l'accesso al database.
     */
    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                String url = ConfigManager.getDbUrl();
                conn = DriverManager.getConnection(url);
                System.out.println("Connessione al database SQLite stabilita con successo.");
            } catch (ClassNotFoundException e) {
                System.err.println("Driver JDBC non trovato: " + e.getMessage());   
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    /**
     * Chiude la connessione al database in modo sicuro e libera le risorse associate.
     */
    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Connessione al database chiusa.");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la chiusura del database: " + e.getMessage());
        }
    }
}