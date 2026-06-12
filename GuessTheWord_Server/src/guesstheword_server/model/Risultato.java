/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.model;

/**
 *
 * @author angel
 */
public class Risultato {
    private long idRisultato;
    private long idPartita; 
    private long idUtente;   
    private String esito;
    private int tempoRispostaMs;
    private int punteggio;
    
    public Risultato(long idRisultato, long idPartita, long idUtente, String esito, int tempoRispostaMs, int punteggio) {
        this.idRisultato = idRisultato;
        this.idPartita = idPartita;
        this.idUtente = idUtente;
        this.esito = esito;
        this.tempoRispostaMs = tempoRispostaMs;
        this.punteggio=punteggio;
    }

    public Risultato(long idPartita, long idUtente, String esito, int tempoRispostaMs, int punteggio) {
        this.idPartita = idPartita;
        this.idUtente = idUtente;
        this.esito = esito;
        this.tempoRispostaMs = tempoRispostaMs;
        this.punteggio=punteggio;
    }

    public long getIdRisultato() {
        return idRisultato;
    }

    public long getIdPartita() {
        return idPartita;
    }

    public long getIdUtente() {
        return idUtente;
    }

    public String getEsito() {
        return esito;
    }

    public int getTempoRispostaMs() {
        return tempoRispostaMs;
    }

    public void setIdRisultato(long idRisultato) {
        this.idRisultato = idRisultato;
    }

    public void setIdPartita(long idPartita) {
        this.idPartita = idPartita;
    }

    public void setIdUtente(long idUtente) {
        this.idUtente = idUtente;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public void setTempoRispostaMs(int tempoRispostaMs) {
        this.tempoRispostaMs = tempoRispostaMs;
    }
    
    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    @Override
    public String toString() {
        return "Risultato{" + "idRisultato=" + idRisultato + ", idPartita=" + idPartita + ", idUtente=" + idUtente + ", esito=" + esito + ", tempoRispostaMs=" + tempoRispostaMs + '}';
    }
    
}