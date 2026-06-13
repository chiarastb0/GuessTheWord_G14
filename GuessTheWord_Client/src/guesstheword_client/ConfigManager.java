/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "client.properties";
    private static final Properties properties = new Properties();

    // Questo blocco viene eseguito automaticamente una sola volta all'avvio
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

    public static String getServerIp() {
        return properties.getProperty("server.ip");
    }

    public static int getServerPort() {
        try {
            return Integer.parseInt(properties.getProperty("server.port"));
        } catch (NumberFormatException e) {
            System.err.println("Errore di formato per la porta nel file properties. Uso la 5000.");
            return 5000;
        }
    }
}
