package guesstheword_server.db;

/**
 * Eccezione personalizzata a runtime utilizzata per gestire in modo uniforme 
 * gli errori relativi alle operazioni sul database (es. query fallite, disconnessioni).
 * Estende RuntimeException, quindi non obbliga a blocchi try-catch espliciti ovunque.
 * * @author admin
 */
public class DBException extends RuntimeException {

    /**
     * Costruisce una nuova eccezione del database con il messaggio di dettaglio specificato.
     * * @param msg Il messaggio che descrive l'errore o il motivo dell'eccezione.
     */
    public DBException(String msg) {
        super(msg);
    }
    
    /**
     * Costruisce una nuova eccezione del database con un messaggio di dettaglio 
     * e la causa originaria (utile per il concatenamento delle eccezioni).
     * * @param message Il messaggio che descrive l'errore.
     * @param cause L'eccezione originaria che ha scatenato questo errore (es. SQLException).
     */
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
    
}