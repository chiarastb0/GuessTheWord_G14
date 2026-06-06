/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.utils;

/**
 *
 * @author admin
 */
public class CifrarioUtils {
     //Applica il Cifrario di Cesare a una stringa.
     //Mantiene le lettere maiuscole/minuscole e ignora i caratteri non alfabetici (es. punteggiatura).
    public static String cifratura(String testo, int shift) {
        StringBuilder risultato = new StringBuilder();
        
        // Normalizza lo shift nell'intervallo 0-25
        shift = (shift % 26 + 26) % 26; 

        for (char carattere : testo.toCharArray()) {
            if (Character.isLetter(carattere)) {
                char base = Character.isLowerCase(carattere) ? 'a' : 'A';
                // Calcola la nuova posizione nell'alfabeto ciclico
                carattere = (char) (base + (carattere - base + shift) % 26);
            }
            risultato.append(carattere);
        }
        return risultato.toString();
    }
}
    

