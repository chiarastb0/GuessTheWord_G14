package guesstheword_server.db;

import guesstheword_server.model.Utente;
import static guesstheword_server.utils.PasswordUtils.hashPassword;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object per la gestione delle operazioni CRUD sull'entità Utente.
 * Interagisce con il database SQLite per gestire registrazioni, login e il calcolo della classifica globale.
 * 
 */
public class UtenteDAO implements DAO<Utente> {
    
    /**
     * Cerca un singolo utente nel database tramite il suo ID.
     * * @param id L'identificativo univoco dell'utente.
     * @return Un Optional contenente l'Utente se trovato, altrimenti un Optional vuoto.
     */
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

    /**
     * Recupera l'elenco di tutti gli utenti registrati nel database.
     * * @return Una lista contenente tutti gli utenti.
     */
    @Override
    public List<Utente> selectAll() {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT * FROM UTENTE";
        
        try (Connection connection = DatabaseManager.getConnection();
             Statement cmd = connection.createStatement();
             ResultSet rs = cmd.executeQuery(sql)) {
                
            while (rs.next()) {
                Utente utente = getUtente(rs);
                utenti.add(utente);
            }
            
        } catch (SQLException e) {
            throw new DBException("Errore durante la selectAll (Utente)", e);
        }
        return utenti;    
    }

    /**
     * Inserisce un nuovo utente nel database (fase di registrazione).
     * * @param u L'oggetto Utente contenente le credenziali da salvare.
     */
    @Override
    public void insert(Utente u) {
        String sql = "INSERT INTO UTENTE(username, password, ruolo) VALUES (?, ?, ?)";
        
        try (Connection connection = DatabaseManager.getConnection(); 
             PreparedStatement cmd = connection.prepareStatement(sql)) {
            
            cmd.setString(1, u.getUsername());
            cmd.setString(2, u.getPassword());
            cmd.setString(3, u.getRuolo());
            cmd.executeUpdate();
            
            System.out.println("Utente inserito con successo.");
            
        } catch (SQLException exc) {
            throw new DBException("Errore durante la insert (Utente)", exc);
        }   
    }

    /**
     * Aggiorna i dati di un utente esistente (username, password, ruolo).
     * * @param u L'oggetto Utente con i dati aggiornati e l'ID corrispondente.
     */
    @Override
    public void update(Utente u) {
        String sql = "UPDATE UTENTE SET username = ?, password = ?, ruolo = ? WHERE id_utente = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement cmd = connection.prepareStatement(sql)) {
            
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
    
    /**
     * Elimina definitivamente un utente dal database.
     * * @param u L'oggetto Utente da eliminare.
     */
    @Override
    public void delete(Utente u) {
        String sql = "DELETE FROM UTENTE WHERE id_utente = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement cmd = connection.prepareStatement(sql)) {

            cmd.setLong(1, u.getIdUtente());
            cmd.executeUpdate();

        } catch (SQLException exc) {
            throw new DBException("Errore durante la delete (Utente)", exc);
        }
    }
    
    /**
     * Metodo di supporto (Utility) per estrarre i dati di un utente dal ResultSet.
     * * @param rs Il ResultSet posizionato sulla riga da leggere.
     * @return L'oggetto Utente istanziato con i dati del database.
     * @throws SQLException Se si verifica un errore durante la lettura dei campi.
     */
    public Utente getUtente(ResultSet rs) throws SQLException {
        long id = rs.getLong("id_utente");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String ruolo = rs.getString("ruolo"); 
        
        return new Utente(id, username, password, ruolo);
    }
    
    /**
     * Verifica le credenziali fornite per l'accesso al sistema.
     * Applica l'algoritmo di hashing sulla password inserita prima del confronto.
     * * @param username L'username digitato dal client.
     * @param password La password in chiaro digitata dal client.
     * @return Un Optional contenente l'Utente se le credenziali sono corrette, altrimenti vuoto.
     */
    public Optional<Utente> login(String username, String password) {
        String sql = "SELECT * FROM UTENTE WHERE username = ? AND password = ?";
        
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement cmd = connection.prepareStatement(sql)) {
            
            String passwordCifrata = hashPassword(password, username);

            cmd.setString(1, username);
            cmd.setString(2, passwordCifrata);
        
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
    
    /**
     * Verifica preventivamente se un nome utente è già in uso nel database.
     * * @param username L'username da controllare.
     * @return true se l'username esiste già, false se è disponibile.
     * @throws SQLException Se si verifica un errore durante la query.
     */
    public boolean esisteUsername(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM UTENTE WHERE username = ?";
    
        try (Connection conn = DatabaseManager.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
        
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }   
        }
        return false;
    }
        
    /**
     * Calcola la somma dei punti di ogni giocatore e restituisce la classifica formattata.
     * Formato risultante: posizione,username,punti;posizione,username,punti;...
     * * @return Una stringa contenente la classifica globale, oppure "VUOTO" se non ci sono dati.
     */
    public String getClassificaGlobaleFormattata() {
        StringBuilder sb = new StringBuilder();
        
        String sql = "SELECT UTENTE.username, SUM(RISULTATO.punteggio) AS punti_totali " +
                     "FROM UTENTE " +
                     "JOIN RISULTATO ON UTENTE.id_utente = RISULTATO.id_utente " +
                     "GROUP BY UTENTE.id_utente, UTENTE.username " +
                     "ORDER BY punti_totali DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int posizione = 1;
            boolean hasData = false;
            
            while (rs.next()) {
                hasData = true;
                String username = rs.getString("username");
                int puntiTotali = rs.getInt("punti_totali");

                sb.append(posizione).append(",")
                  .append(username).append(",")
                  .append(puntiTotali).append(";");
                  
                posizione++;
            }
            
            if (!hasData) {
                return "VUOTO";
            }
            
        } catch (SQLException e) {
            System.err.println("Errore query classifica: " + e.getMessage());
            return "VUOTO";
        }
        return sb.toString();
    }
}