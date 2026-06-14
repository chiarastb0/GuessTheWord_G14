package guesstheword_server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Classe di utilità per il caricamento delle configurazioni di base del server.
 * Legge i parametri dal file "server.properties" all'avvio dell'applicazione.
 * 
 */
public class ConfigManager {
    
    private static final Properties props = new Properties();

    // Blocco statico eseguito automaticamente una sola volta al caricamento della classe in memoria
    static {
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Impossibile caricare server.properties: " + e.getMessage());
        }
    }

    /**
     * Restituisce l'URL di connessione al database.
     * * @return La stringa di connessione letta dalle properties (es. jdbc:sqlite:db/database.db).
     */
    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    /**
     * Restituisce la porta di ascolto del server di rete.
     * * @return Il numero della porta (es. 5000), oppure 8080 come valore di paracadute (default).
     */
    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port", "8080"));
    }
}