package guesstheword_server.model;

/**
 * Rappresenta un utente registrato nel sistema.
 * Contiene le credenziali di accesso (username e password) e il ruolo all'interno del gioco.
 * 
 */
public class Utente {
    
    private long idUtente;
    private String username;
    private String password;
    private String ruolo;
    
    /**
     * Costruttore parziale per creare un nuovo utente prima dell'inserimento nel database (senza ID).
     * * @param username Il nome utente scelto.
     * @param password La password dell'utente (preferibilmente già in formato hash).
     * @param ruolo Il livello di permessi (es. "PLAYER" o "ADMIN").
     */
    public Utente(String username, String password, String ruolo) {
        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
    }
    
    /**
     * Costruttore completo per istanziare un utente recuperato dal database.
     * * @param idUtente L'ID univoco dell'utente nel sistema.
     * @param username Il nome utente.
     * @param password La password dell'utente.
     * @param ruolo Il livello di permessi associato all'account.
     */
    public Utente(long idUtente, String username, String password, String ruolo) {
        this.idUtente = idUtente;
        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
    }

    /**
     * Restituisce l'ID univoco dell'utente.
     * * @return L'ID dell'utente.
     */
    public long getIdUtente() {
        return idUtente;
    }

    /**
     * Restituisce il nome utente dell'account.
     * * @return L'username dell'utente.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce la password associata all'account.
     * * @return La password dell'utente.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Restituisce il ruolo dell'utente nel sistema.
     * * @return Il ruolo (es. "PLAYER").
     */
    public String getRuolo() {
        return ruolo;
    }

    /**
     * Imposta l'ID univoco dell'utente.
     * * @param idUtente Il nuovo ID da assegnare.
     */
    public void setIdUtente(long idUtente) {
        this.idUtente = idUtente;
    }

    /**
     * Imposta il nome utente.
     * * @param username Il nuovo username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Imposta la password dell'utente.
     * * @param password La nuova password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Imposta il ruolo dell'utente.
     * * @param ruolo Il nuovo ruolo da assegnare.
     */
    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }
    
    /**
     * Restituisce una rappresentazione testuale dell'oggetto Utente.
     * * @return Una stringa contenente i valori principali dell'account.
     */
    @Override
    public String toString() {
        return "Utente{" + "idUtente=" + idUtente + ", username=" + username + ", password=" + password + ", ruolo=" + ruolo + '}';
    }
}

