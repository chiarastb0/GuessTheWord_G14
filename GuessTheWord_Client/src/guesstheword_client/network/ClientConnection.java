/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.network;

/**
 *
 * @author admin
 */

import guesstheword_client.controller.*;
import guesstheword_server.model.PacchettoSfida;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.Platform;

/**
 * Gestisce la connessione Socket lato Client e l'ascolto asincrono dei messaggi del Server.
 */
public class ClientConnection implements Runnable {

    private final String ip;
    private final int porta;
    private Socket socket;
    
    // Canali per comunicare con il Server
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private boolean inAscolto = true;

    // Riferimento al controller della schermata di gioco 
    private ScreenGameController controllerGioco;
    private AuthController controllerAuth;

    public ClientConnection(String ip, int porta) {
        this.ip = ip;
        this.porta = porta;
    }

    /**
     * Tenta la connessione iniziale con il Server.
     * @return true se la connessione è riuscita, false altrimenti.
     */
    public boolean connetti() {
        try {
            this.socket = new Socket(ip, porta);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[CLIENT] Connesso al server " + ip + ":" + porta);
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Impossibile connettersi al server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ciclo continuo di ascolto dei messaggi provenienti dal ClientHandler del Server.
     */
    @Override
    public void run() {
        try {
            Object objRicevuto;
            while (inAscolto && (objRicevuto = in.readObject()) != null) {
                
                // CASO 1: Stringhe di testo e Protocollo comandi
                if (objRicevuto instanceof String) {
                    String rigaRicevuta = (String) objRicevuto;
                    System.out.println("[SERVER DICE]: " + rigaRicevuta);
                    elaboraMessaggioServer(rigaRicevuta); 
                }
                
                // CASO 2: Serializzazione Oggetto PacchettoSfida
                else if (objRicevuto instanceof PacchettoSfida) {
                    PacchettoSfida pacchetto = (PacchettoSfida) objRicevuto;
                    
                    //TEST RICEZIONE PAcchetto Sfida
                    System.out.println("==================================================");
                    System.out.println("[TEST RICEZIONE CLIENT]");
                    System.out.println("Testo della sfida: " + pacchetto.getParolaCifrata());
                    System.out.println("==================================================");
                    
                    if (controllerGioco != null) {
                        Platform.runLater(() -> {
                            controllerGioco.inizializzaPartita(String.valueOf(pacchetto.getDurataTimerSecondi()), pacchetto.getParolaCifrata());
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Connessione con il server interrotta: " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            System.err.println("[CLIENT] Errore di sincronizzazione classi con il server: " + e.getMessage());
        } finally {
            chiudiConnessione();
        }
    }


    /**
     * Parsing del protocollo speculare a quello del Server.
     */
    private void elaboraMessaggioServer(String messaggio) {
        
        // Rimuoviamo eventuali spazi o ritorni a capo trasmessi dal flusso di rete all'inizio/fine
        messaggio = messaggio.trim();
        
        // Separiamo l'intestazione del comando dal resto dei dati
        String[] parti = messaggio.split(":", 2); 
        if (parti.length == 0) return;

        String comando = parti[0].toUpperCase().trim();

        switch (comando) {
            case "LOGIN_SUCCESS":
                System.out.println("[LOGIN] Accesso eseguito con successo.");
                
                String[] pezzi = messaggio.split(":");
                final String ruolo = (pezzi.length >= 2) ? pezzi[1].trim() : "PLAYER";
                
                if (controllerAuth != null) {
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreLogin("✅Accesso riuscito!");
                        // Nota: qui potrai inserire la logica del cambio di scena
                        // verso l'interfaccia di gioco effettiva
                        controllerAuth.gestisciLoginSuccess(this, ruolo);
                    });
                }
                break;
              
            case "LOGIN_FAIL": 
                if (parti.length >= 2 && controllerAuth != null) {
                    String errore = parti[1];
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreLogin("❌ " + errore);
                    });
                }
                break;
                
            case "REG_SUCCESS":
                System.out.println("[REGISTRAZIONE] Risposta di successo elaborata correttamente.");
                if (controllerAuth != null) {
                // Recuperiamo il messaggio reale inviato dal server (se presente), altrimenti usiamo uno di fallback
                    final String messaggioServer = (parti.length >= 2) ? parti[1].trim() : "Registrazione completata!";
                
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreReg(""); // Svuota l'errore nel pannello reg
                        controllerAuth.mostraPannelloLogin(null);   // Scambia i pannelli VBox
                    
                        // Mostriamo a schermo il vero messaggio che arriva dal server!
                        controllerAuth.mostraMessaggioErroreLogin("✅ " + messaggioServer);
                    });
                }
                break;
            
            case "REG_FAIL":
                if (parti.length >= 2 && controllerAuth != null) {
                    String errore = parti[1];
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreReg("❌ " + errore);
                    });
                }
                break;

            case "START_GAME":
                // Il server invia il comando vecchio stile: START_GAME:tempo:testoCifrato
                // Gestito per retrocompatibilità se non usa l'oggetto PacchettoSfida
                String[] sottoParti = parti[1].split(":", 2);
                if (sottoParti.length >= 2 && controllerGioco != null) {
                    String tempoInSecondi = sottoParti[0];
                    String testoCifrato = sottoParti[1];

                    Platform.runLater(() -> {
                        controllerGioco.inizializzaPartita(tempoInSecondi, testoCifrato);
                    });
                }
                break;
            
            case "FINE_PARTITA":
                System.out.println("[RETE CLIENT] Esito ricevuto: " + messaggio);
                if (controllerGioco != null) {
                    // Passiamo l'intero messaggio al controller grafico
                    controllerGioco.gestisciFinePartita(messaggio); 
                }
                break;

            case "DATI_CLASSIFICA":
                // Formato atteso dal Server: DATI_CLASSIFICA:posizione,username,punti;posizione,username,punti;...
                if (parti.length >= 2 && controllerGioco != null) {
                    String contenuto = parti[1];
                    String[] righeGiocatori = contenuto.split(";");

                    // Usiamo Platform.runLater per aggiornare in sicurezza la TableView
                    Platform.runLater(() -> {
                        controllerGioco.svuotaClassifica(); // Resetta i vecchi dati
                        for (String riga : righeGiocatori) {
                            if (!riga.trim().isEmpty()) {
                                String[] dati = riga.split(",");
                                int pos = Integer.parseInt(dati[0]);
                                String username = dati[1];
                                int punti = Integer.parseInt(dati[2]);
                                
                                // Inietta la riga nel controller
                                controllerGioco.aggiungiGiocatoreAClassifica(pos, username, punti);
                            }
                        }
                    });
                }
                break;

            case "DATI_STORICO":
                // Formato atteso dal Server: DATI_STORICO:data,parola,esito,punti;data,parola,esito,punti;...
                if (parti.length >= 2 && controllerGioco != null) {
                    String contenutoStorico = parti[1];
                    String[] righeStorico = contenutoStorico.split(";");

                    Platform.runLater(() -> {
                        // Nota: Lo storico di solito mostra i dati cumulativi, non serve svuotarlo 
                        // a meno che non si voglia ricaricare completamente da zero la lista.
                        for (String riga : righeStorico) {
                            if (!riga.trim().isEmpty()) {
                                String[] dati = riga.split(",");
                                String data = dati[0];
                                String parola = dati[1];
                                String esito = dati[2];
                                int punteggio = Integer.parseInt(dati[3]);
                                
                                // Inietta il match giocato nella tabella dello Storico personale
                                controllerGioco.aggiungiPartitaAStorico(data, parola, esito, punteggio);
                            }
                        }
                    });
                }
                break;

            case "ERRORE":
                System.err.println("[SERVER ERRORE] " + parti[1]);
                break;

            default:
                System.out.println("[CLIENT] Comando non riconosciuto o gestito altrove: " + comando);
                break;
        }
    }

    /**
     * Invia un messaggio testuale (comando) al ClientHandler del Server.
     */
    public void spedisciMessaggio(String messaggio) {
        try {
            if (out != null) {
                out.writeObject(messaggio);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Errore spedizione messaggio client: " + e.getMessage());
        }
    }

        
    public void setControllerAuth(AuthController controller) {
        this.controllerAuth = controller;
    }
    
    /**
     * Permette al tuo SchermataGiocoController di "registrarsi" così da poter ricevere
     * i testi cifrati non appena la partita comincia.
     */
    public void setControllerGioco(ScreenGameController controller) {
        this.controllerGioco = controller;
    }

    /**
     * Chiude i flussi in modo pulito.
     */
    public void chiudiConnessione() {
        this.inAscolto = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[CLIENT] Risorse di rete chiuse correttamente.");
        } catch (IOException e) {
            System.err.println("[CLIENT] Errore durante la chiusura delle risorse: " + e.getMessage());
        }
    }
}