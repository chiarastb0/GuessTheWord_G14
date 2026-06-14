package guesstheword_server.utils;

/**
 * Classe di utilità che fornisce i metodi per l'offuscamento delle parole.
 * Implementa la logica crittografica basata sul Cifrario di Cesare.
 * 
 */
public class CifrarioUtils {
    
    /**
     * Applica il Cifrario di Cesare a una stringa di testo.
     * Mantiene inalterate le lettere maiuscole e minuscole e ignora i caratteri 
     * non alfabetici (come punteggiatura, numeri o spazi).
     * * @param testo La parola o frase in chiaro da cifrare.
     * @param shift Il numero di posizioni (chiave) di cui far scorrere le lettere nell'alfabeto.
     * @return La stringa cifrata e offuscata.
     */
    public static String cifratura(String testo, int shift) {
        StringBuilder risultato = new StringBuilder();
        
        // Normalizza lo shift nell'intervallo 0-25 per gestire senza errori 
        // eventuali valori negativi o maggiori di 26
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

