/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package guesstheword_server.db;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author angel
 */

/* Interfaccia per implementare pattern DAO
   Separa logica di gestione DB dal resto
*/
public interface DAO<T>{
    Optional<T> selectById(long id); //Trova un elemento per ID
    List<T> selectAll();    //Prende tutti gli elementi
    void insert(T t);
    void update(T t);
    void delete(T t);
}
