package guesstheword_server.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe di utilità per la sicurezza e la crittografia delle credenziali.
 * Fornisce metodi per generare in modo sicuro l'hash delle password prima del salvataggio 
 * o della verifica nel database, applicando lo standard SHA-256.
 */
public class PasswordUtils {

    /**
     * Genera l'hash SHA-256 di una password combinata con un parametro "salt" (es. l'username).
     * L'aggiunta del salt garantisce che due utenti che scelgono la stessa password 
     * abbiano comunque due hash completamente diversi all'interno del database, 
     * proteggendoli da attacchi informatici basati su "Rainbow Tables".
     * * @param password La password in chiaro inserita dall'utente.
     * @param salt Il valore univoco da combinare con la password per renderla unica.
     * @return La stringa esadecimale di 64 caratteri che rappresenta l'hash calcolato.
     * @throws RuntimeException Se l'algoritmo SHA-256 non è supportato dal sistema corrente.
     */
    public static String hashPassword(String password, String salt) {
        try {
            // Inizializziamo l'algoritmo di crittografia unidirezionale SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Uniamo la password con il salt (l'username) per renderla unica
            String passwordConSalt = password + salt;
            
            // Calcoliamo l'hash generando un array di byte
            byte[] hashBytes = md.digest(passwordConSalt.getBytes());
            
            // Convertiamo l'array di byte in una stringa esadecimale facilmente leggibile e memorizzabile
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore critico: Algoritmo di hashing non trovato nel sistema", e);
        }
    }
}