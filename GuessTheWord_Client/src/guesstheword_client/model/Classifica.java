/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.model;

/**
 *
 * @author admin
 */

public class Classifica {
    private int posizione;
    private String utente;
    private int puntiTotali;

    public Classifica(int posizione, String utente, int puntiTotali) {
        this.posizione = posizione;
        this.utente = utente;
        this.puntiTotali = puntiTotali;
    }

    // I Getter usati da JavaFX per riempire le colonne della classifica
    public int getPosizione() { return posizione; }
    public String getUtente() { return utente; }
    public int getPuntiTotali() { return puntiTotali; }
}