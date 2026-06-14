/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.model;

/**
 * @class PartitaStorico
 * @brief Definizione della classe PartitaStorico per il modello client.
 * Questo file definisce la struttura dati utilizzata per memorizzare 
 * e visualizzare i dettagli di una singola partita giocata nello storico dell'utente.
 */
public class PartitaStorico {
    /** 
     * @brief La data e l'ora in cui è stata effettuata la partita (formattata come stringa).
     */
    private String data;
    
    /** 
     * @brief La parola segreta che doveva essere indovinata durante la partita.
     */
    
    private String parola;
    
    /** 
     * @brief L'esito finale della partita.
     */
    private String esito;
    
    /** 
     * @brief Il punteggio totalizzato dal giocatore in questa specifica partita.
     */
    private int punteggio;

    /**
     * @brief Costruttore completo per la classe PartitaStorico.
     * Inizializza un nuovo record dello storico con tutti i dettagli della partita conclusa.
     * @param data      La data della partita.
     * @param parola    La parola da indovinare.
     * @param esito     L'esito (vittoria/sconfitta/pareggio).
     * @param punteggio Il punteggio assegnato.
     */
    public PartitaStorico(String data, String parola, String esito, int punteggio) {
        this.data = data;
        this.parola = parola;
        this.esito = esito;
        this.punteggio = punteggio;
    }

    /**
     * @brief Restituisce la data della partita.
     * @return String La data nel formato testuale memorizzato.
     */
    public String getData() { 
        return data; 
    }
    
    /**
     * @brief Restituisce la parola segreta del match.
     * @return String La parola giocata.
     */
    public String getParola() { 
        return parola; 
    }
    
    /**
     * @brief Restituisce l'esito della partita.
     * @return String L'esito della partita.
     */
    public String getEsito() { 
        return esito; 
    }
    
    /**
     * @brief Restituisce il punteggio ottenuto.
     * @return int Il punteggio della partita.
     */
    public int getPunteggio() { 
        return punteggio; 
    }
}
