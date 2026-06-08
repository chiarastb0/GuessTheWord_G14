/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.db;
import java.sql.*;
import java.util.*;
import guesstheword_server.model.Risultato;

/**
 *
 * @author admin
 */
public class RisultatoDAO implements DAO<Risultato>{
    
    @Override
    public Optional<Risultato> selectById(long id) {
        Optional<Risultato> result = Optional.empty();
        String sql = "SELECT * FROM RISULTATO WHERE id_risultato = ?";
        
        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql)){
                
                pst.setLong(1, id);
                ResultSet rs = pst.executeQuery();
                Risultato risultato=null;
                if(rs.next()){
                    risultato = getRisultato(rs);
                }
                result = Optional.ofNullable(risultato);
        } catch (SQLException e){
            throw new DBException("Errore durante la selectById (Risultato)", e);
        }
        return result;
    }
    
    @Override
    public List<Risultato> selectAll() {
        List<Risultato> risultati = new ArrayList<>();
        String sql ="SELECT * FROM RISULTATO";
        
        try(Connection conn = DatabaseManager.getConnection();
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql)){
                Risultato risultato=null;
                while(rs.next()){
                    risultato= getRisultato(rs);
                    risultati.add(risultato);
                }
            
        } catch (SQLException e){
            throw new DBException("Errore durante la selectAll (Risultato)", e);
        }
        return risultati;    
    }
    
    @Override
    public void insert(Risultato r) {
        String sql = "INSERT INTO RISULTATO (id_partita, id_utente, esito, tempo_risposta) VALUES (?,?, ?, ?)";
        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement cmd = conn.prepareStatement(sql)){
            
            cmd.setLong(1, r.getIdPartita());
            cmd.setLong(2, r.getIdUtente());
            cmd.setString(3, r.getEsito());
            cmd.setInt(4, r.getTempoRispostaMs());
            
            cmd.executeUpdate();
            
            System.out.println("Risultato inserito con successo.");
            
        } catch (SQLException e){
            throw new DBException("Errore durante la insert (Risultato)", e);
        }
    }
    
    @Override
    public void update(Risultato r) {
        String sql = "UPDATE RISULTATO SET id_partita = ?, id_utente = ?, esito = ?, tempo_risposta_ms = ? WHERE id_risultato = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)){
                
                cmd.setLong(1, r.getIdPartita());
                cmd.setLong(2, r.getIdUtente());
                cmd.setString(3, r.getEsito());
                cmd.setInt(4, r.getTempoRispostaMs());
                cmd.setLong(5, r.getIdRisultato());
                cmd.executeUpdate();
                
                System.out.println("Risultato aggiornata con successo.");
            
        } catch (SQLException e){
            throw new DBException("Errore durante l'update (Partita)", e);
        }
    }
        
    @Override
    public void delete(Risultato r) {
        String sql = "DELETE FROM RISULTATO WHERE id_risultato = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)){
                
                cmd.setLong(1, r.getIdRisultato());
                cmd.executeUpdate();
                
        } catch (SQLException e){
            throw new DBException("Errore durante il delate (Partita)", e);
        }
    }
    
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
    
    public Risultato getRisultato(ResultSet rs) throws SQLException {
        long idRisultato = rs.getLong("id_risultato");
        long idPartita = rs.getLong("id_partita");
        long idUtente = rs.getLong("id_utente");
        String esito = rs.getString("esito");
        int tempoRispostaMs = rs.getInt("tempo_risposta_ms");
        
        return new Risultato(idRisultato, idPartita, idUtente, esito, tempoRispostaMs);
    }
    
    // Calcola il tempo medio di risposta per un singolo giocatore
    public double getTempoMedioRisposta(long idUtente) {
        double media = 0;
        String sql = "SELECT AVG(tempo_risposta_ms) FROM RISULTATO WHERE id_utente = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, idUtente);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    media = rs.getDouble(1);
                }
            }

        } catch (SQLException e) {
            throw new DBException("Errore calcolo tempo medio", e);
        }
        return media;
    }
    
}
