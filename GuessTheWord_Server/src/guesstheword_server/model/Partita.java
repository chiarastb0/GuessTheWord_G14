package guesstheword_server.model;

/**
 * Rappresenta una singola partita giocata sul server.
 * Contiene le informazioni su quando è stata giocata, la parola (o le parole) nascosta e la difficoltà.
 * 
 */
public class Partita {
    
    private long idPartita;
    private String dataOra;
    private String parolaNascosta; 
    private String difficolta;     
   
    /**
     * Costruttore vuoto di default.
     */
    public Partita() {
    }
   
    /**
     * Costruttore completo per inizializzare una partita esistente (es. recuperata dal database).
     * * @param idPartita L'ID univoco della partita.
     * @param dataOra La data e l'ora in cui si è svolta la partita.
     * @param parolaNascosta La parola (o l'elenco di parole) da indovinare.
     * @param difficolta Il livello di difficoltà della partita.
     */
    public Partita(long idPartita, String dataOra, String parolaNascosta, String difficolta) {
        this.idPartita = idPartita;
        this.dataOra = dataOra;
        this.parolaNascosta = parolaNascosta;
        this.difficolta = difficolta;
    }
   
    /**
     * Costruttore parziale usato per creare una nuova partita da salvare nel database (senza ID assegnato).
     * * @param dataOra La data e l'ora in cui si è svolta la partita.
     * @param parolaNascosta La parola (o l'elenco di parole) da indovinare.
     * @param difficolta Il livello di difficoltà della partita.
     */
    public Partita(String dataOra, String parolaNascosta, String difficolta) {
        this.dataOra = dataOra;
        this.parolaNascosta = parolaNascosta;
        this.difficolta = difficolta;
    }

    /**
     * Restituisce l'ID univoco della partita.
     * * @return L'ID della partita.
     */
    public long getIdPartita() { 
        return idPartita; 
    }

    /**
     * Imposta l'ID univoco della partita.
     * * @param id_partita Il nuovo ID da assegnare.
     */
    public void setIdPartita(long id_partita) { 
        this.idPartita = id_partita; 
    }

    /**
     * Restituisce la data e l'ora in cui si è svolta la partita.
     * * @return Una stringa formattata con data e ora.
     */
    public String getDataOra() { 
        return dataOra; 
    }

    /**
     * Imposta la data e l'ora in cui si è svolta la partita.
     * * @param dataOra La nuova stringa con data e ora.
     */
    public void setDataOra(String dataOra) { 
        this.dataOra = dataOra; 
    }

    /**
     * Restituisce la parola nascosta associata alla partita.
     * * @return La parola o le parole da indovinare.
     */
    public String getParolaNascosta() { 
        return parolaNascosta; 
    }

    /**
     * Imposta la parola nascosta per la partita.
     * * @param parolaNascosta La nuova parola da indovinare.
     */
    public void setParolaNascosta(String parolaNascosta) { 
        this.parolaNascosta = parolaNascosta; 
    }

    /**
     * Restituisce il livello di difficoltà della partita.
     * * @return Una stringa rappresentante la difficoltà (es. "Facile", "Media").
     */
    public String getDifficolta() { 
        return difficolta; 
    }

    /**
     * Imposta il livello di difficoltà della partita.
     * * @param difficolta Il nuovo livello di difficoltà.
     */
    public void setDifficolta(String difficolta) { 
        this.difficolta = difficolta; 
    }
   
    /**
     * Restituisce una rappresentazione testuale dell'oggetto Partita.
     * * @return Una stringa contenente ID, data e parola nascosta.
     */
    @Override
    public String toString() {
        return "Partita{" + "id=" + idPartita + ", data='" + dataOra + "', parola='" + parolaNascosta + "'}";
    }
}

