/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.model;

/**
 *
 * @author Chiara
 */

public class PartitaStorico {
    private String data;
    private String parola;
    private String esito;
    private int punteggio;

    public PartitaStorico(String data, String parola, String esito, int punteggio) {
        this.data = data;
        this.parola = parola;
        this.esito = esito;
        this.punteggio = punteggio;
    }

    public String getData() { 
        return data; 
    }
    
    public String getParola() { 
        return parola; 
    }
    
    public String getEsito() { 
        return esito; 
    }
    
    public int getPunteggio() { 
        return punteggio; 
    }
}
