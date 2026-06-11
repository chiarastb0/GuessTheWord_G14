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

import guesstheword_client.controller.ScreenGameController;
import guesstheword_server.model.PacchettoSfida;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
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

    // Riferimento al TUO controller della schermata di gioco 
    private ScreenGameController controllerGioco;

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
                
                // CASO 1: Vecchie Stringhe (Login, ecc.)
                if (objRicevuto instanceof String) {
                    String rigaRicevuta = (String) objRicevuto;
                    System.out.println("[SERVER DICE]: " + rigaRicevuta);
                    elaboraMessaggioServer(rigaRicevuta); 
                }
                
                // CASO 2:Serializzazione 
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
        } finally {
            chiudiConnessione(); // Chiama intatto il metodo del tuo compagno
        }
    }

    /**
     * Parsing del protocollo speculare a quello del Server.
     */
    private void elaboraMessaggioServer(String messaggio) {
        String[] parti = messaggio.split(":");
        if (parti.length == 0) return;

        String comando = parti[0].toUpperCase();

        switch (comando) {
            case "LOGIN_SUCCESS":
                // Gestito inizialmente dal compagno 3 per cambiare schermata
                System.out.println("[LOGIN] Accesso eseguito come: " + parti[1]);
                break;

            case "START_GAME":
                // QUESTO TOCCA A TE! Il server invia il testo cifrato (es. START_GAME:tempo:testoCifrato)
                if (parti.length >= 3 && controllerGioco != null) {
                    String tempoInSecondi = parti[1];
                    String testoCifrato = parti[2];

                    // TRUCCONE JAVAFX MANDATORIO: Siccome siamo in un Thread di rete, 
                    // per modificare elementi grafici (Label, TextArea) dobbiamo usare Platform.runLater
                    Platform.runLater(() -> {
                        controllerGioco.inizializzaPartita(tempoInSecondi, testoCifrato);
                    });
                }
                break;

            case "ERRORE":
                System.err.println("[SERVER ERRORE] " + parti[1]);
                break;

            default:
                System.out.println("[CLIENT] Comando non riconosciuto: " + comando);
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