package guesstheword_server.db;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia generica per l'implementazione del pattern Data Access Object (DAO).
 * Definisce i metodi standard per le operazioni CRUD (Create, Read, Update, Delete),
 * separando in modo netto la logica di gestione del database dal resto dell'applicazione.
 * * @param <T> Il tipo di entità (Model) gestita da questo DAO.
 * 
 */
public interface DAO<T> {
    
    /**
     * Cerca e recupera un elemento dal database utilizzando il suo ID univoco.
     * * @param id L'identificativo univoco dell'elemento da cercare.
     * @return Un Optional contenente l'elemento se trovato, altrimenti un Optional vuoto.
     */
    Optional<T> selectById(long id);
    
    /**
     * Recupera tutti gli elementi di questo tipo presenti nel database.
     * * @return Una lista contenente tutti gli elementi trovati.
     */
    List<T> selectAll();
    
    /**
     * Inserisce un nuovo record nel database.
     * * @param t L'oggetto contenente i dati da inserire.
     */
    void insert(T t);
    
    /**
     * Aggiorna i dati di un record già esistente nel database.
     * * @param t L'oggetto contenente i dati aggiornati.
     */
    void update(T t);
    
    /**
     * Elimina in modo definitivo un record dal database.
     * * @param t L'oggetto da eliminare.
     */
    void delete(T t);
}
