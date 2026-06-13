package guesstheword_server.model;

import java.io.Serializable;

/**
 * @class PacchettoSfida
 * @brief Definizione della classe PacchettoSfida per il modello server.
 * Implementa l'interfaccia Serializable, permettendo all'istanza 
 * di essere convertita in un flusso di byte e inviata attraverso i canali Socket (ObjectOutputStream).
 * Viene decodificata specularmente sul Client per aggiornare l'interfaccia grafica del gioco.
 */
public class PacchettoSfida implements Serializable {
    /** 
     * @brief ID di controllo della serializzazione per garantire la compatibilità tra classi Client/Server.
     */
    private static final long serialVersionUID = 1L;
    
    /** 
     * @brief Il testo completo contenente la parola segreta parzialmente cifrata.
     */
    private String parolaCifrata;
    
    /** 
     * @brief La durata del timer di gioco espressa in secondi.
     */
    private int durataTimerSecondi;
    
    /** 
     * @brief Il livello di difficoltà impostato per la sfida (FACILE, MEDIO, DIFFICILE).
     */
    private String difficolta;
    
    /**
     * @brief Costruttore completo della classe PacchettoSfida.
     * Inizializza un nuovo pacchetto dati per essere spedito in rete ai giocatori.
     * @param parolaCifrata       La stringa di testo nascosta.
     * @param durataTimerSecondi  Il tempo concesso per indovinare, in secondi.
     * @param difficolta         La stringa rappresentante la difficoltà del match.
     */
    public PacchettoSfida(String parolaCifrata, int durataTimerSecondi, String difficolta) {
        this.parolaCifrata = parolaCifrata;
        this.durataTimerSecondi = durataTimerSecondi;
        this.difficolta = difficolta;
    }

    /**
     * @brief Restituisce la parola cifrata.
     * @return String La parola nascosta da indovinare.
     */
    public String getParolaCifrata() { 
        return parolaCifrata; 
    }
    
    /**
     * @brief Restituisce la durata del timer della sfida.
     * @return int Il tempo totale in secondi.
     */
    public int getDurataTimerSecondi() { 
        return durataTimerSecondi; 
    }
    
    /**
     * @brief Restituisce il livello di difficoltà della sfida.
     * @return String La difficoltà visualizzabile dal client.
     */
    public String getDifficolta() { 
        return difficolta; 
    }
}
