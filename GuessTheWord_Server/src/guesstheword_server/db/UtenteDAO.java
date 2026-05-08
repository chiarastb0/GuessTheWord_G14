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
    
    public Optional<Utente> selectById(long id) {
        Optional<Utente> result=Optional.empty();
        try(Connection connection = DatabaseManager.getConnection(); 
            Statement cmd = connection.createStatement(); 
                ResultSet rs = cmd.executeQuery("SELECT * FROM UTENTI WHERE ID_UTENTE = " + id);) {
                    Utente utente=null;
                    if(rs.next()) {
                        utente = getUtente(rs);
                    }
                    result = Optional.ofNullable(utente);
        } catch(SQLException exc) {
            throw new DBException("UtenteDAO.selectByID", exc);
        }   
        return result;
    }

    @Override
    public List<Utente> selectAll() {
        List<Utente> utenti = new ArrayList<>();
        try(Connection connection = DatabaseManager.getConnection(); 
            Statement cmd = connection.createStatement(); 
                ResultSet rs = cmd.executeQuery("SELECT * FROM UTENTI");) {
                    Utente utente=null;
                    while(rs.next()) {
                        utente = getUtente(rs);
                        utenti.add(utente);
                    }
        } catch(SQLException exc) {
            throw new DBException("UtenteDAO.selectAll", exc);
        }   
        return utenti;
    }

    @Override
    public void insert(Utente u) {
        try(Connection connection = DatabaseManager.getConnection(); 
            PreparedStatement cmd = connection.prepareStatement("INSERT INTO ALBUM VALUES (?,?,?)"); ) {
            cmd.setString(1, u.getUsername());
            cmd.setString(2, u.getPassword());
            cmd.setString(3, u.getRuolo());
            cmd.executeUpdate();
        } catch(SQLException exc) {
            throw new DBException("UtenteDAO.insert", exc);
        }   
    }

    @Override
    public void update(Utente u) {
        
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement cmd = connection.prepareStatement("UPDATE UTENTI SET username = ?, password = ?, ruolo = ? WHERE id_utente = ?")) {
            cmd.setString(1, u.getUsername());
            cmd.setString(2, u.getPassword());
            cmd.setString(3, u.getRuolo());
            cmd.setLong(4, u.getIdUtente()); 

            cmd.executeUpdate();

        } catch (SQLException exc) {
            throw new DBException("UtenteDAO.update", exc);
        }
    }
    
    @Override
    public void delete(Utente u) {

        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement cmd = connection.prepareStatement("DELETE FROM UTENTI WHERE id_utente = ?")) {

            cmd.setLong(1, u.getIdUtente());
            cmd.executeUpdate();

        } catch (SQLException exc) {
            throw new DBException("UtenteDAO.delete", exc);
        }
    }
    
    public Utente getUtente(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String password = rs.getString("password");
        String ruolo = rs.getString("ruolo"); 
        
        return new Utente(username, password, ruolo);
    }
    
}
