/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {

    /**
     * Genera l'hash SHA-256 di una password combinata con un salt (l'username).
     */
    public static String hashPassword(String password, String salt) {
        try {
            // Inizializziamo l'algoritmo SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Uniamo la password con il salt (l'username) per renderla unica
            String passwordConSalt = password + salt;
            
            // Calcoliamo l'hash in byte
            byte[] hashBytes = md.digest(passwordConSalt.getBytes());
            
            // Convertiamo i byte in una stringa esadecimale leggibile
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore critico: Algoritmo di hashing non trovato", e);
        }
    }
}
