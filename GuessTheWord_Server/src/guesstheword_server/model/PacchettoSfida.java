package guesstheword_server.model;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author angel
 */
import java.io.Serializable;

/**
 * Rappresenta i dati della sfida inviati dal Server al Client.
 * Implementa Serializable per poter essere trasmesso sui Socket.
 */
public class PacchettoSfida implements Serializable {
    
    // Questo ID è il "timbro" di conformità. Server e Client devono avere 
    // lo stesso ID per questo oggetto, altrimenti la ricezione fallisce.
    private static final long serialVersionUID = 1L;

    private String parolaCifrata;
    private int durataTimerSecondi;

    public PacchettoSfida(String parolaCifrata, int durataTimerSecondi) {
        this.parolaCifrata = parolaCifrata;
        this.durataTimerSecondi = durataTimerSecondi;
    }

    public String getParolaCifrata() {
        return parolaCifrata;
    }

    public int getDurataTimerSecondi() {
        return durataTimerSecondi;
    }
}
