package guesstheword_server.model;

/**
 *
 * @author angel
 */

import java.io.Serializable;

public class PacchettoSfida implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String parolaCifrata; // È il testo completo con le parole cifrate
    private int durataTimerSecondi;
    private String difficolta;    // NUOVO CAMPO per far visualizzare la diff. sul client
    
    public PacchettoSfida(String parolaCifrata, int durataTimerSecondi, String difficolta) {
        this.parolaCifrata = parolaCifrata;
        this.durataTimerSecondi = durataTimerSecondi;
        this.difficolta = difficolta;
    }

    public String getParolaCifrata() { return parolaCifrata; }
    public int getDurataTimerSecondi() { return durataTimerSecondi; }
    public String getDifficolta() { return difficolta; }
}
