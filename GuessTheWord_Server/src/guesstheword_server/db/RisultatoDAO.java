package guesstheword_server.db;

import guesstheword_server.model.Risultato;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object per la gestione delle operazioni CRUD sull'entità Risultato.
 * Interagisce con il database SQLite per memorizzare gli esiti delle partite, i tempi di risposta
 * e per calcolare statistiche come le vittorie o il tempo medio.
 * 
 */
public class RisultatoDAO implements DAO<Risultato> {
    
    /**
     * Cerca un singolo risultato nel database tramite il suo ID.
     * * @param id L'identificativo univoco del risultato.
     * @return Un Optional contenente il Risultato se trovato, altrimenti un Optional vuoto.
     */
    @Override
    public Optional<Risultato> selectById(long id) {
        Optional<Risultato> result = Optional.empty();
        String sql = "SELECT * FROM RISULTATO WHERE id_risultato = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
                
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();
            Risultato risultato = null;
            
            if (rs.next()) {
                risultato = getRisultato(rs);
            }
            result = Optional.ofNullable(risultato);
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la selectById (Risultato)", e);
        }
        return result;
    }
    
    /**
     * Recupera l'elenco di tutti i risultati registrati nel database.
     * * @return Una lista contenente tutti i risultati.
     */
    @Override
    public List<Risultato> selectAll() {
        List<Risultato> risultati = new ArrayList<>();
        String sql = "SELECT * FROM RISULTATO";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {
                
            while (rs.next()) {
                Risultato risultato = getRisultato(rs);
                risultati.add(risultato);
            }
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la selectAll (Risultato)", e);
        }
        return risultati;    
    }
    
    /**
     * Inserisce un nuovo risultato nel database a fine partita.
     * * @param r L'oggetto Risultato da salvare.
     */
    @Override
    public void insert(Risultato r) {
        String sql = "INSERT INTO RISULTATO (id_partita, id_utente, esito, tempo_risposta_ms, punteggio) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
            
            cmd.setLong(1, r.getIdPartita());
            cmd.setLong(2, r.getIdUtente());
            cmd.setString(3, r.getEsito());
            cmd.setInt(4, r.getTempoRispostaMs());
            cmd.setInt(5, r.getPunteggio());
            
            cmd.executeUpdate();
            System.out.println("Risultato inserito con successo.");
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la insert (Risultato)", e);
        }
    }
    
    /**
     * Aggiorna i dati di un risultato già esistente.
     * * @param r L'oggetto Risultato con i dati aggiornati e l'ID corrispondente.
     */
    @Override
    public void update(Risultato r) {
        String sql = "UPDATE RISULTATO SET id_partita = ?, id_utente = ?, esito = ?, tempo_risposta_ms = ? WHERE id_risultato = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
                
            cmd.setLong(1, r.getIdPartita());
            cmd.setLong(2, r.getIdUtente());
            cmd.setString(3, r.getEsito());
            cmd.setInt(4, r.getTempoRispostaMs());
            cmd.setLong(5, r.getIdRisultato());
            
            cmd.executeUpdate();
            System.out.println("Risultato aggiornato con successo.");
            
        } catch (SQLException e) {
            throw new DBException("Errore durante l'update (Risultato)", e);
        }
    }
        
    /**
     * Elimina definitivamente un risultato dal database tramite il suo ID.
     * * @param r L'oggetto Risultato da eliminare.
     */
    @Override
    public void delete(Risultato r) {
        String sql = "DELETE FROM RISULTATO WHERE id_risultato = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
                
            cmd.setLong(1, r.getIdRisultato());
            cmd.executeUpdate();
                
        } catch (SQLException e) {
            throw new DBException("Errore durante il delete (Risultato)", e);
        }
    }
    
    /**
     * Calcola il numero totale di vittorie ottenute da uno specifico utente.
     * * @param idUtente L'ID dell'utente di cui contare le vittorie.
     * @return Il numero di partite vinte.
     */
    public int getNumeroVittorie(int idUtente) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM RISULTATO WHERE id_utente = ? AND esito = 'VITTORIA'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, idUtente);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new DBException("Errore conteggio vittorie", e);
        }

        return count;
    }
    
    /**
     * Metodo di supporto (Utility) per estrarre i dati di un risultato dal ResultSet.
     * * @param rs Il ResultSet posizionato sulla riga da leggere.
     * @return L'oggetto Risultato istanziato con i dati del database.
     * @throws SQLException Se si verifica un errore durante la lettura dei campi.
     */
    public Risultato getRisultato(ResultSet rs) throws SQLException {
        long idRisultato = rs.getLong("id_risultato");
        long idPartita = rs.getLong("id_partita");
        long idUtente = rs.getLong("id_utente");
        String esito = rs.getString("esito");
        int tempoRispostaMs = rs.getInt("tempo_risposta_ms");
        int punteggio = rs.getInt("punteggio");
        
        return new Risultato(idRisultato, idPartita, idUtente, esito, tempoRispostaMs, punteggio);
    }
    
    /**
     * Calcola il tempo medio di risposta per ciascun giocatore registrato.
     * Utilizzato per popolare la classifica globale nell'interfaccia.
     * * @return Una lista di coppie chiave-valore contenente l'username e il tempo medio in secondi.
     */
    public List<Map.Entry<String, Double>> getTempoMedioPerUtente() {
        List<Map.Entry<String, Double>> lista = new ArrayList<>();
        String sql = "SELECT UTENTE.username, AVG(RISULTATO.tempo_risposta_ms) / 1000.0 AS media " +
                     "FROM UTENTE JOIN RISULTATO ON UTENTE.id_utente = RISULTATO.id_utente " +
                     "GROUP BY UTENTE.username ORDER BY media ASC";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
                 
            while (rs.next()) {
                double media = Math.round(rs.getDouble("media") * 100.0) / 100.0;
                lista.add(new AbstractMap.SimpleEntry<>(rs.getString("username"), media));
            }
            
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        
        return lista;
    }
    
    /**
     * Calcola il numero totale di vittorie per ciascun giocatore registrato.
     * Utilizzato per popolare la classifica globale nell'interfaccia.
     * * @return Una lista di coppie chiave-valore contenente l'username e le vittorie totali.
     */
    public List<Map.Entry<String, Integer>> getVittoriePerUtente() {
        List<Map.Entry<String, Integer>> lista = new ArrayList<>();
        String sql = "SELECT UTENTE.username, COUNT(RISULTATO.id_risultato) AS tot " +
                     "FROM UTENTE JOIN RISULTATO ON UTENTE.id_utente = RISULTATO.id_utente " +
                     "WHERE RISULTATO.esito = 'VITTORIA' " +
                     "GROUP BY UTENTE.username ORDER BY tot DESC";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
                 
            while (rs.next()) {
                lista.add(new AbstractMap.SimpleEntry<>(rs.getString("username"), rs.getInt("tot")));
            }
            
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        
        return lista;
    }
    
    /**
     * Recupera lo storico delle partite di uno specifico giocatore eseguendo 
     * una JOIN tra le tabelle RISULTATO e PARTITA.
     * * @param idUtente L'ID dell'utente di cui richiedere lo storico.
     * @return Una singola stringa formattata con l'elenco delle partite separate da punto e virgola,
     * oppure "VUOTO" se il giocatore non ha ancora disputato partite.
     */
    public String getStoricoFormattato(long idUtente) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT PARTITA.data_ora, PARTITA.parola_nascosta, RISULTATO.esito, RISULTATO.punteggio " +
                     "FROM RISULTATO " +
                     "JOIN PARTITA ON RISULTATO.id_partita = PARTITA.id_partita " +
                     "WHERE RISULTATO.id_utente = ? " +
                     "ORDER BY PARTITA.data_ora DESC";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, idUtente);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    String data = rs.getString("data_ora");
                    String parola = rs.getString("parola_nascosta");
                    String esito = rs.getString("esito");
                    int punteggio = rs.getInt("punteggio");

                    sb.append(data).append(",").append(parola).append(",").append(esito).append(",").append(punteggio).append(";");
                }
                
                if (!hasData) {
                    return "VUOTO";
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore query storico: " + e.getMessage());
            return "VUOTO";
        }
        
        return sb.toString();
    }
}
