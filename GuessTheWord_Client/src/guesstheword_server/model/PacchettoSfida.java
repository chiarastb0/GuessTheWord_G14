package guesstheword_server.model;

import java.io.Serializable;

public class PacchettoSfida implements Serializable {
    
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
