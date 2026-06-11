/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.db;
import guesstheword_server.ConfigManager;
import java.sql.*;

/**
 *
 * @author angel
 */
public class DatabaseManager {
    private static Connection conn = null;
    
    public static Connection  getConnection() throws SQLException {
        if(conn == null || conn.isClosed()) {
            try{
                Class.forName("org.sqlite.JDBC");
                String url = ConfigManager.getDbUrl();
                conn = DriverManager.getConnection(url);
                System.out.println("Connessione al database SQLite stabilita con successo.");
            } catch (ClassNotFoundException e) {
                System.err.println("Driver non trovato: " + e.getMessage());   
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    /**
     * Chiude la connessione al database in modo sicuro.
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
