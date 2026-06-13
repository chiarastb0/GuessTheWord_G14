package guesstheword_server.model;

/**
 * Rappresenta il risultato finale ottenuto da un singolo giocatore in una determinata partita.
 * Memorizza l'esito della sfida, il tempo impiegato e i punti calcolati.
 * 
 */
public class Risultato {
    
    private long idRisultato;
    private long idPartita; 
    private long idUtente;   
    private String esito;
    private int tempoRispostaMs;
    private int punteggio;
    
    /**
     * Costruttore completo utilizzato per mappare un risultato già esistente nel database.
     * * @param idRisultato L'ID univoco del risultato.
     * @param idPartita L'ID della partita a cui si riferisce.
     * @param idUtente L'ID dell'utente che ha giocato.
     * @param esito L'esito della partita (es. "VITTORIA", "SCONFITTA", "PAREGGIO").
     * @param tempoRispostaMs Il tempo impiegato dal giocatore, espresso in millisecondi.
     * @param punteggio Il punteggio totale calcolato e assegnato.
     */
    public Risultato(long idRisultato, long idPartita, long idUtente, String esito, int tempoRispostaMs, int punteggio) {
        this.idRisultato = idRisultato;
        this.idPartita = idPartita;
        this.idUtente = idUtente;
        this.esito = esito;
        this.tempoRispostaMs = tempoRispostaMs;
        this.punteggio = punteggio;
    }

    /**
     * Costruttore parziale utilizzato per creare un nuovo risultato da inserire nel database (senza ID autogenerato).
     * * @param idPartita L'ID della partita a cui si riferisce.
     * @param idUtente L'ID dell'utente che ha giocato.
     * @param esito L'esito della partita.
     * @param tempoRispostaMs Il tempo impiegato dal giocatore in millisecondi.
     * @param punteggio Il punteggio assegnato a fine partita.
     */
    public Risultato(long idPartita, long idUtente, String esito, int tempoRispostaMs, int punteggio) {
        this.idPartita = idPartita;
        this.idUtente = idUtente;
        this.esito = esito;
        this.tempoRispostaMs = tempoRispostaMs;
        this.punteggio = punteggio;
    }

    /**
     * Restituisce l'ID del risultato.
     * * @return L'ID univoco del risultato.
     */
    public long getIdRisultato() {
        return idRisultato;
    }

    /**
     * Restituisce l'ID della partita associata.
     * * @return L'ID della partita.
     */
    public long getIdPartita() {
        return idPartita;
    }

    /**
     * Restituisce l'ID dell'utente associato a questo risultato.
     * * @return L'ID dell'utente.
     */
    public long getIdUtente() {
        return idUtente;
    }

    /**
     * Restituisce l'esito della partita per il giocatore.
     * * @return Una stringa rappresentante l'esito (es. "VITTORIA").
     */
    public String getEsito() {
        return esito;
    }

    /**
     * Restituisce il tempo di risposta del giocatore.
     * * @return Il tempo impiegato espresso in millisecondi.
     */
    public int getTempoRispostaMs() {
        return tempoRispostaMs;
    }

    /**
     * Imposta l'ID del risultato.
     * * @param idRisultato Il nuovo ID del risultato.
     */
    public void setIdRisultato(long idRisultato) {
        this.idRisultato = idRisultato;
    }

    /**
     * Imposta l'ID della partita associata.
     * * @param idPartita Il nuovo ID della partita.
     */
    public void setIdPartita(long idPartita) {
        this.idPartita = idPartita;
    }

    /**
     * Imposta l'ID dell'utente associato.
     * * @param idUtente Il nuovo ID dell'utente.
     */
    public void setIdUtente(long idUtente) {
        this.idUtente = idUtente;
    }

    /**
     * Imposta l'esito della partita.
     * * @param esito Il nuovo esito da assegnare.
     */
    public void setEsito(String esito) {
        this.esito = esito;
    }

    /**
     * Imposta il tempo di risposta del giocatore.
     * * @param tempoRispostaMs Il tempo impiegato in millisecondi.
     */
    public void setTempoRispostaMs(int tempoRispostaMs) {
        this.tempoRispostaMs = tempoRispostaMs;
    }
    
    /**
     * Restituisce il punteggio guadagnato nella partita.
     * * @return I punti totalizzati.
     */
    public int getPunteggio() {
        return punteggio;
    }

    /**
     * Imposta il punteggio per il risultato.
     * * @param punteggio I nuovi punti da assegnare.
     */
    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    /**
     * Restituisce una rappresentazione testuale del risultato.
     * * @return Una stringa contenente i valori principali dell'oggetto.
     */
    @Override
    public String toString() {
        return "Risultato{" + "idRisultato=" + idRisultato + ", idPartita=" + idPartita + ", idUtente=" + idUtente + ", esito=" + esito + ", tempoRispostaMs=" + tempoRispostaMs + '}';
    }
}