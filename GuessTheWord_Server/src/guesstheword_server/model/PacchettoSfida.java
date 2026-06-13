package guesstheword_server.model;

import java.io.Serializable;

/**
 * Rappresenta il pacchetto di dati inviato dal Server al Client 
 * contenente le informazioni e lo stato della sfida in corso.
 * 
 */
public class PacchettoSfida implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String parolaCifrata; 
    private int durataTimerSecondi;
    private String difficolta;    
    
    /**
     * Costruisce un nuovo pacchetto di sfida.
     * * @param parolaCifrata Il testo completo della sfida con le parole cifrate.
     * @param durataTimerSecondi Il tempo rimanente espresso in secondi.
     * @param difficolta Il livello di difficoltà attuale della partita.
     */
    public PacchettoSfida(String parolaCifrata, int durataTimerSecondi, String difficolta) {
        this.parolaCifrata = parolaCifrata;
        this.durataTimerSecondi = durataTimerSecondi;
        this.difficolta = difficolta;
    }

    /**
     * Restituisce il testo della sfida.
     * * @return Il testo contenente le parole cifrate o già indovinate.
     */
    public String getParolaCifrata() { 
        return parolaCifrata; 
    }
    
    /**
     * Restituisce il tempo rimanente per la sfida in corso.
     * * @return I secondi rimanenti alla fine della partita.
     */
    public int getDurataTimerSecondi() { 
        return durataTimerSecondi; 
    }
    
    /**
     * Restituisce il livello di difficoltà impostato per la sfida.
     * * @return Una stringa che rappresenta la difficoltà (es. "Facile", "Media").
     */
    public String getDifficolta() { 
        return difficolta; 
    }
}
