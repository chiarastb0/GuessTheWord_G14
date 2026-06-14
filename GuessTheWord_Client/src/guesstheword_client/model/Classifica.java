/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.model;

/**
 * @class Classifica
 * @brief Definizione della classe Classifica per il modello client.
 * Questo file contiene la struttura dati per gestire una singola voce 
 * all'interno della classifica del gioco "Guess The Word".
 */
public class Classifica {
    /** 
     * @brief La posizione occupata dall'utente nella classifica (es. 1°, 2°, ecc.).
     */
    private int posizione;
    
    /** 
     * @brief Il nome identificativo dell'utente.
     */
    private String utente;
    
    /** 
     * @brief Il totale dei punti accumulati dall'utente.
     */
    private int puntiTotali;

    /**
     * @brief Costruttore della classe Classifica.
     * Inizializza un nuovo oggetto Classifica con tutti i dati necessari 
     * per visualizzare la riga in tabella.
     * @param posizione   La posizione in classifica.
     * @param utente      Il nome dell'utente.
     * @param puntiTotali I punti totali dell'utente.
     */
    public Classifica(int posizione, String utente, int puntiTotali) {
        this.posizione = posizione;
        this.utente = utente;
        this.puntiTotali = puntiTotali;
    }

    /**
     * @brief Restituisce la posizione in classifica.
     * Questo getter è utilizzato da JavaFX per popolare la relativa colonna della TableView.
     * @return int La posizione dell'utente.
     */
    public int getPosizione() { 
        return posizione; 
    }
    
    /**
     * @brief Restituisce il nome dell'utente.
     * Questo getter è utilizzato da JavaFX per popolare la relativa colonna della TableView.
     * @return String Il nome utente.
     */
    public String getUtente() { 
            return utente; 
    }
    
    /**
     * @brief Restituisce i punti totali accumulati.
     * Questo getter è utilizzato da JavaFX per popolare la relativa colonna della TableView.
     * @return int Il punteggio totale.
     */
    public int getPuntiTotali() { 
        return puntiTotali; 
    }
}