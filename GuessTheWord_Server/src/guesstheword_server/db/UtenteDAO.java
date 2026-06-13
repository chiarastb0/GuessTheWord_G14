/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.db;
import guesstheword_server.model.Utente;
import java.sql.*;
import java.util.*;

/**
 *
 * @author admin
 */
public class UtenteDAO implements DAO<Utente> {
    
    @Override
    public Optional<Utente> selectById(long id) {
        Optional<Utente> result = Optional.empty();
        String sql = "SELECT * FROM UTENTE WHERE id_utente = ?";
        try (Connection connection = DatabaseManager.getConnection(); 
            PreparedStatement cmd = connection.prepareStatement(sql)) {
            cmd.setLong(1, id);

            try (ResultSet rs = cmd.executeQuery()) {
                if (rs.next()) {
                    Utente utente = getUtente(rs);
                    result = Optional.ofNullable(utente);
                }
            }
        } catch (SQLException exc) {
            throw new DBException("Errore durante la select by Id (Utente)", exc);
        }   
        return result;
}

    @Override
    public List<Utente> selectAll() {
        List<Utente> utenti = new ArrayList<>();
        String sql ="SELECT * FROM UTENTE";
        
        try(Connection connection = DatabaseManager.getConnection();
            Statement cmd = connection.createStatement();
            ResultSet rs = cmd.executeQuery(sql)){
                Utente utente=null;
                while(rs.next()){
                    utente= getUtente(rs);
                    utenti.add(utente);
                }
            
        } catch (SQLException e){
            throw new DBException("Errore durante la selectAll (Utente)", e);
        }
        return utenti;    
    }

    @Override
    public void insert(Utente u) {
        try(Connection connection = DatabaseManager.getConnection(); 
            PreparedStatement cmd = connection.prepareStatement("INSERT INTO UTENTE(username, password, ruolo) VALUES (?,?,?)"); ) {
            cmd.setString(1, u.getUsername());
            cmd.setString(2, u.getPassword());
            cmd.setString(3, u.getRuolo());
            cmd.executeUpdate();
            
            System.out.println("Utente inserito con successo.");
            
        } catch(SQLException exc) {
            throw new DBException("Errore durante la insert (Utente)", exc);
        }   
    }

    @Override
    public void update(Utente u) {
        
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement cmd = connection.prepareStatement("UPDATE UTENTE SET username = ?, password = ?, ruolo = ? WHERE id_utente = ?")) {
            cmd.setString(1, u.getUsername());
            cmd.setString(2, u.getPassword());
            cmd.setString(3, u.getRuolo());
            cmd.setLong(4, u.getIdUtente()); 

            cmd.executeUpdate();
            System.out.println("Utente aggiornato con successo.");

        } catch (SQLException exc) {
            throw new DBException("Errore durante la update (Utente)", exc);
        }
    }
    
    @Override
    public void delete(Utente u) {

        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement cmd = connection.prepareStatement("DELETE FROM UTENTE WHERE id_utente = ?")) {

            cmd.setLong(1, u.getIdUtente());
            cmd.executeUpdate();

        } catch (SQLException exc) {
            throw new DBException("Errore durante la delete (Utente)", exc);
        }
    }
    
    public Utente getUtente(ResultSet rs) throws SQLException {
        long id = rs.getLong("id_utente");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String ruolo = rs.getString("ruolo"); 
        
        return new Utente(id, username, password, ruolo);
    }
    
    public Optional<Utente> login(String username, String password) {
        String sql = "SELECT * FROM UTENTE WHERE username = ? AND password = ?";
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement cmd = connection.prepareStatement(sql)) {
        
            cmd.setString(1, username);
            cmd.setString(2, password);
        
            try (ResultSet rs = cmd.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(getUtente(rs));
                }
            }
        } catch (SQLException exc) {
            throw new DBException("Errore nel login", exc);
        }
        return Optional.empty();
    }
    
    public boolean esisteUsername(String username) throws SQLException {
        // Adatta il nome della tabella (es. utenti) e della colonna (es. username) al tuo database
        String query = "SELECT COUNT(*) FROM UTENTE WHERE username = ?";
    
        try (Connection conn = DatabaseManager.getConnection(); 
         PreparedStatement ps = conn.prepareStatement(query)) {
        
        ps.setString(1, username);
        
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                // Se il conteggio è maggiore di 0, l'username esiste già
                return rs.getInt(1) > 0;
                }
            }   
        }
        return false;
    }
        
     /**
     * Recupera la classifica globale di tutti gli utenti ordinati per punteggio decrescente.
     * Formato: posizione,username,punti;posizione,username,punti;...
     */
    /**
     * Calcola la somma dei punti di ogni giocatore e restituisce la classifica formattata.
     */
    public String getClassificaGlobaleFormattata() {
        StringBuilder sb = new StringBuilder();
        
        // Usiamo SUM() per sommare i punti e GROUP BY per raggrupparli per utente.
        // I nomi completi delle tabelle evitano il famoso errore di sintassi SQLite.
        String sql = "SELECT UTENTE.username, SUM(RISULTATO.punteggio) AS punti_totali " +
                     "FROM UTENTE " +
                     "JOIN RISULTATO ON UTENTE.id_utente = RISULTATO.id_utente " +
                     "GROUP BY UTENTE.id_utente, UTENTE.username " +
                     "ORDER BY punti_totali DESC";

        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = pstmt.executeQuery()) {

            int posizione = 1;
            boolean hasData = false;
            
            while (rs.next()) {
                hasData = true;
                String username = rs.getString("username");
                int puntiTotali = rs.getInt("punti_totali");

                // Assembliamo la stringa nel formato: posizione,username,punti;
                sb.append(posizione).append(",")
                  .append(username).append(",")
                  .append(puntiTotali).append(";");
                  
                posizione++;
            }
            
            if (!hasData) {
                return "VUOTO";
            }
            
        } catch (java.sql.SQLException e) {
            System.err.println("Errore query classifica: " + e.getMessage());
            return "VUOTO";
        }
        return sb.toString();
    }

    /**
     * Recupera la cronologia delle sfide giocate da uno specifico utente dal database.
     * Formato: data,parola,esito,punti;data,parola,esito,punti;...
     */
    public String getStoricoPartiteFormattato(String username) {
        StringJoiner sj = new StringJoiner(";");
        
        // Query sulla tabella delle partite collegate all'utente tramite username o FK
        String sql = "SELECT data_partita, parola_segreta, esito, punteggio_ottenuto " +
                     "FROM PARTITA WHERE fk_username = ? ORDER BY data_partita DESC";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement cmd = connection.prepareStatement(sql)) {
            
            cmd.setString(1, username);
            
            try (ResultSet rs = cmd.executeQuery()) {
                while (rs.next()) {
                    String data = rs.getString("data_partita");
                    String parola = rs.getString("parola_segreta");
                    String esito = rs.getString("esito");
                    int punti = rs.getInt("punteggio_ottenuto");
                    
                    sj.add(data + "," + parola + "," + esito + "," + punti);
                }
            }
            
        } catch (SQLException exc) {
            throw new DBException("Errore nel recupero dello storico partite per l'utente " + username, exc);
        }
        
        // Se l'utente non ha mai giocato, restituisce una riga fittizia di benvenuto
        return sj.length() > 0 ? sj.toString() : "---,Nessuna partita giocata,---,0";
    }
        
}
