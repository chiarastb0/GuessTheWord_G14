/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestisce il caricamento e la lettura dei parametri di configurazione del Client.
 * Rispetta i vincoli di percorso relativo e la struttura delle cartelle richiesta.
 */
public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        // 1. Tenta di caricare il file dalla cartella 'properties/' richiesta dalla traccia
        File propFile = new File("properties/client.properties");
        
        // Fallback: se la cartella properties non esiste (es. durante i test nell'IDE), lo cerca nella root
        if (!propFile.exists()) {
            propFile = new File("client.properties");
        }

        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
            System.out.println("[CLIENT] Configurazione caricata con successo da: " + propFile.getPath());
        } catch (IOException e) {
            System.err.println("[CLIENT] Impossibile caricare il file delle proprietà: " + e.getMessage());
            System.err.println("[CLIENT] Verranno utilizzati i valori di default (localhost:5000).");
        }
    }

    /**
     * Recupera l'IP del server dal file di configurazione.
     * Se non presente, restituisce il default '127.0.0.1'.
     */
    public static String getServerIp() {
        return props.getProperty("server.ip", "127.0.0.1");
    }

    /**
     * Recupera la porta del server dal file di configurazione.
     * Se non presente, restituisce il default '5000' come da traccia.
     */
    public static int getServerPort() {
        try {
            return Integer.parseInt(props.getProperty("server.port", "5000"));
        } catch (NumberFormatException e) {
            System.err.println("[CLIENT] Porta non valida nel file di configurazione. Uso il default 5000.");
            return 5000;
        }
    }
}
