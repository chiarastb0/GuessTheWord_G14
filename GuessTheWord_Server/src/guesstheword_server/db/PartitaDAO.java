/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.db;
import java.sql.*;
import java.util.*;
import guesstheword_server.model.Partita;
/**
 *
 * @author angel
 */
public class PartitaDAO implements DAO<Partita> {
    
    

    @Override
    public Optional<Partita> selectById(long id) {
        Optional<Partita> result = Optional.empty();
        String sql = "SELECT * FROM PARTITA WHERE id_partita = ?";
        
        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql)){
                
                pst.setLong(1, id);
                ResultSet rs = pst.executeQuery();
                Partita partita=null;
                if(rs.next()){
                    partita = getPartita(rs);
                }
                result = Optional.ofNullable(partita);
        } catch (SQLException e){
            throw new DBException("Errore durante la selectById (Partita)", e);
        }
        return result;
    }

    @Override
    public List<Partita> selectAll() {
        List<Partita> partite = new ArrayList<>();
        String sql ="SELECT * FROM PARTITA";
        
        try(Connection conn = DatabaseManager.getConnection();
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql)){
                Partita partita=null;
                while(rs.next()){
                    partita= getPartita(rs);
                    partite.add(partita);
                }
            
        } catch (SQLException e){
            throw new DBException("Errore durante la selectAll (Partita)", e);
        }
        return partite;    
    }

    @Override
    public void insert(Partita t) {
        String sql = "INSERT INTO PARTITA (data_ora, parola_nascosta) VALUES (?,?)";
        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql)){
            
            pst.setString(1, t.getDataOra());
            pst.setString(2, t.getParolaNascosta());
            pst.executeUpdate();
            
            System.out.println("Partita inserita con successo.");
            
        } catch (SQLException e){
            throw new DBException("Errore durante la insert (Partita)", e);
        }
    }

    @Override
    public void update(Partita t) {
        String sql = "UPDATE PARTITA SET data_ora = ?, parola_nascosta = ? WHERE id_partita = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)){
                
                pst.setString(1, t.getDataOra());
                pst.setString(2, t.getParolaNascosta());
                pst.setLong(3, t.getIdPartita());
                pst.executeUpdate();
                
                System.out.println("Partita aggiornata con successo.");
            
        } catch (SQLException e){
            throw new DBException("Errore durante l'update (Partita)", e);
        }
    }

    @Override
    public void delete(Partita t) {
        String sql = "DELETE FROM PARTITA WHERE id_partita = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)){
                
                pst.setLong(1, t.getIdPartita());
                pst.executeUpdate();
                
        } catch (SQLException e){
            throw new DBException("Errore durante il delate (Partita)", e);
        }
    }
    
    public Partita getPartita(ResultSet rs) throws SQLException {
        long  idPartita = rs.getLong("id_partita");
        String dataOra = rs.getString("data_ora");
        String parolaNascosta = rs.getString("parola_nascosta"); 
        
        return new Partita(idPartita, dataOra, parolaNascosta);
    }
    
    
    
}
