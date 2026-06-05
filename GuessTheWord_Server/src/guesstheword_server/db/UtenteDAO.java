/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.db;
import guesstheword_server.model.Utente;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    
}
