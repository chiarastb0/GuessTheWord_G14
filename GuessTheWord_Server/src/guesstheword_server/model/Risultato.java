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
    
}
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
    
    private int idRisultato;
    private int idPartita;
    private int idUtente;
    private String esito;
    private int tempoRispostaMs;

    public Risultato(int idRisultato, int idPartita, int idUtente, String esito, int tempoRispostaMs) {
        this.idRisultato = idRisultato;
        this.idPartita = idPartita;
        this.idUtente = idUtente;
        this.esito = esito;
        this.tempoRispostaMs = tempoRispostaMs;
    }

    public int getIdRisultato() {
        return idRisultato;
    }

    public void setIdRisultato(int idRisultato) {
        this.idRisultato = idRisultato;
    }

    public int getIdPartita() {
        return idPartita;
    }

    public void setIdPartita(int idPartita) {
        this.idPartita = idPartita;
    }

    public int getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(int idUtente) {
        this.idUtente = idUtente;
    }

    public String getEsito() {
        return esito;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public int getTempoRispostaMs() {
        return tempoRispostaMs;
    }

    public void setTempoRispostaMs(int tempoRispostaMs) {
        this.tempoRispostaMs = tempoRispostaMs;
    }
    
}
