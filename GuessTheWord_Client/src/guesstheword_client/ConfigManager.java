/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @class ConfigManager
 * @brief Gestore della configurazione del client.
 * * Questa classe si occupa di caricare le impostazioni dell'applicazione (IP e porta del server)
 * da un file di configurazione esterno, se il file non esiste, definisce dei valori di fallback.
 **/

public class ConfigManager {
    /**
     * @brief Nome del file di configurazione da caricare.
     */
    private static final String CONFIG_FILE = "client.properties";
    /**
     * @brief Oggetto Properties che memorizza le coppie chiave-valore della configurazione.
     */
    private static final Properties properties = new Properties();

    /**
     * @brief Blocco di inizializzazione statico.
     * * Viene eseguito automaticamente al caricamento della classe in memoria, tenta di leggere il file "client.properties".
     * * @pre Il file "client.properties" deve essere presente nella root del progetto.
     * @post Se il file esiste, le proprietà vengono caricate, se non esiste (lancia IOException), 
     * vengono impostati i valori di default: IP "127.0.0.1" e Porta "5000".
     */
    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("[CLIENT WARN] File client.properties non trovato! Verranno usati i valori di default.");
            // Valori di paracadute nel caso in cui qualcuno cancelli il file
            properties.setProperty("server.ip", "127.0.0.1");
            properties.setProperty("server.port", "5000");
        }
    }

    /**
     * @brief Restituisce l'indirizzo IP del server.
     * * @pre La classe deve essere inizializzata (avviene automaticamente).
     * @post Il valore restituito non sarà mai null, poiché è garantito un valore di fallback.
     * * @return String Rappresentante l'indirizzo IP del server ("127.0.0.1").
     */
    public static String getServerIp() {
        return properties.getProperty("server.ip");
    }

    /**
     * @brief Restituisce la porta del server come valore intero.
     * * Estrae la stringa della porta dalle proprietà e tenta di convertirla in un intero.
     * Nel caso in cui la stringa non sia un numero valido, intercetta l'eccezione e 
     * restituisce una porta di default.
     * * @post Il valore restituito è un intero positivo valido per una porta di rete.
     * * @return int Il numero di porta del server (5000).
     */
    public static int getServerPort() {
        try {
            return Integer.parseInt(properties.getProperty("server.port"));
        } catch (NumberFormatException e) {
            System.err.println("Errore di formato per la porta nel file properties. Uso la 5000.");
            return 5000;
        }
    }
}
