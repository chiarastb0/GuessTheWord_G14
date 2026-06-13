package guesstheword_server.db;

import guesstheword_server.model.Partita;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object per la gestione delle operazioni CRUD sull'entità Partita.
 * Interagisce con il database SQLite per salvare, recuperare, aggiornare e contare le sfide giocate.
 * 
 */
public class PartitaDAO implements DAO<Partita> {

    /**
     * Cerca una singola partita nel database tramite il suo ID.
     * * @param id L'identificativo univoco della partita.
     * @return Un Optional contenente la Partita se trovata, altrimenti un Optional vuoto.
     */
    @Override
    public Optional<Partita> selectById(long id) {
        Optional<Partita> result = Optional.empty();
        String sql = "SELECT * FROM PARTITA WHERE id_partita = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
                
            pst.setLong(1, id);
            ResultSet rs = pst.executeQuery();
            Partita partita = null;
            
            if (rs.next()) {
                partita = getPartita(rs);
            }
            result = Optional.ofNullable(partita);
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la selectById (Partita)", e);
        }
        return result;
    }

    /**
     * Recupera l'elenco di tutte le partite registrate nel database.
     * * @return Una lista contenente tutte le partite.
     */
    @Override
    public List<Partita> selectAll() {
        List<Partita> partite = new ArrayList<>();
        String sql = "SELECT * FROM PARTITA";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {
                
            while (rs.next()) {
                Partita partita = getPartita(rs);
                partite.add(partita);
            }
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la selectAll (Partita)", e);
        }
        return partite;    
    }

    /**
     * Inserisce una nuova partita nel database (senza recuperare l'ID generato).
     * * @param t L'oggetto Partita da salvare.
     */
    @Override
    public void insert(Partita t) {
        String sql = "INSERT INTO PARTITA (data_ora, parola_nascosta, difficolta) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, t.getDataOra());
            pst.setString(2, t.getParolaNascosta());
            pst.setString(3, t.getDifficolta()); 
            pst.executeUpdate();
            
            System.out.println("Partita inserita con successo.");
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la insert (Partita)", e);
        }
    }
    
    /**
     * Inserisce una nuova partita nel database e restituisce immediatamente l'ID univoco 
     * assegnato dal motore SQLite. Utile per il collegamento con la tabella Risultati.
     * * @param t L'oggetto Partita da salvare.
     * @return L'ID autogenerato dal database, oppure -1 in caso di errore.
     */
    public long inserisciERestituisciId(Partita t) {
        String sql = "INSERT INTO PARTITA (data_ora, parola_nascosta, difficolta) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, t.getDataOra());
            pst.setString(2, t.getParolaNascosta());
            pst.setString(3, t.getDifficolta()); 
            pst.executeUpdate();
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1); 
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRORE SQLITE DETTAGLIATO]: " + e.getMessage());
            throw new DBException("Errore durante la insert con ID (Partita)", e);
        }
        return -1; 
    }

    /**
     * Aggiorna i dati di una partita già esistente.
     * Nota: Modifica solo la data e la parola nascosta.
     * * @param t L'oggetto Partita con i dati aggiornati e l'ID corrispondente.
     */
    @Override
    public void update(Partita t) {
        String sql = "UPDATE PARTITA SET data_ora = ?, parola_nascosta = ? WHERE id_partita = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
                
            pst.setString(1, t.getDataOra());
            pst.setString(2, t.getParolaNascosta());
            pst.setLong(3, t.getIdPartita());
            pst.executeUpdate();
            
            System.out.println("Partita aggiornata con successo.");
            
        } catch (SQLException e) {
            throw new DBException("Errore durante l'update (Partita)", e);
        }
    }

    /**
     * Elimina definitivamente una partita dal database tramite il suo ID.
     * * @param t L'oggetto Partita da eliminare.
     */
    @Override
    public void delete(Partita t) {
        String sql = "DELETE FROM PARTITA WHERE id_partita = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
                
            pst.setLong(1, t.getIdPartita());
            pst.executeUpdate();
                
        } catch (SQLException e) {
            throw new DBException("Errore durante il delete (Partita)", e);
        }
    }
    
    /**
     * Metodo di supporto (Utility) per estrarre i dati di una partita dal ResultSet.
     * * @param rs Il ResultSet posizionato sulla riga da leggere.
     * @return L'oggetto Partita istanziato con i dati del database.
     * @throws SQLException Se si verifica un errore durante la lettura dei campi.
     */
    public Partita getPartita(ResultSet rs) throws SQLException {
        long idPartita = rs.getLong("id_partita");
        String dataOra = rs.getString("data_ora");
        String parolaNascosta = rs.getString("parola_nascosta"); 
        String difficolta = rs.getString("difficolta"); 
        
        return new Partita(idPartita, dataOra, parolaNascosta, difficolta);
    }
    
    /**
     * Calcola e restituisce il numero totale di partite disputate sul server.
     * Questo metodo viene utilizzato principalmente per alimentare le statistiche della Dashboard Admin.
     * * @return Il conteggio totale delle righe nella tabella PARTITA.
     */
    public int getNumeroPartiteDisputate() {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM PARTITA";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery(sql)) {

            if (rs.next()) {
                count = rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new DBException("Errore conteggio partite disputate", e);
        }
        return count;
    }
}
