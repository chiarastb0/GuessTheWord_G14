/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package guesstheword_server;

/**
 *
 * @author admin
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Impossibile caricare server.properties: " + e.getMessage());
        }
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port", "8080"));
    }
}
