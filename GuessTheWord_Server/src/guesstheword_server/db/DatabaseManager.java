/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.db;
import java.sql.*;

/**
 *
 * @author angel
 */
public class DatabaseManager {
    //Da Modificare aggiungendo lettura da file .properties
    private static final String URL = "jdbc:sqlite:db/database.db";
    private static Connection conn = null;
    
    public static Connection  getConnection() throws SQLException {
        if(conn == null || conn.isClosed()) {
            try{
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(URL);
            } catch (ClassNotFoundException e) {
                System.err.println("Driver non trovato: " + e.getMessage());   
            }
        }
        return conn;
    }
}
