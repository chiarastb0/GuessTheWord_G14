/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.db.UtenteDAO;
import guesstheword_server.model.Utente;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;

// Gestisce la sessione di comunicazione con un singolo client su un thread separato.
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final ServerManager serverManager;
    
    // Canali per leggere e scrivere sulla rete
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private boolean inAscolto = true;
    private String usernameUtente = null; 
    private final UtenteDAO utenteDAO;

    public ClientHandler(Socket socket, ServerManager serverManager) {
        this.socket = socket;
        this.serverManager = serverManager;
        this.utenteDAO = new UtenteDAO(); 
    }

    @Override
    public void run() {
        try {
            // Inizializziamo i flussi di Input (ascolto) e Output (invio)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();    // forza l'invio immediato senza buffer (autoflush)
            in = new ObjectInputStream(socket.getInputStream());
            
            Object objRicevuto;
            
            // --- INIZIO BYPASS PER IL TEST ---
            this.usernameUtente = "Tester_" + socket.getPort(); // Assegna un nome finto
            serverManager.aggiungiGiocatorePronto(this);        // Usa il metodo col nome corretto!
            // --- FINE BYPASS --
            
            // Il thread resta confinato in questo ciclo leggendo ogni stringa inviata dal client
            while (inAscolto && (objRicevuto = in.readObject()) != null) {
                if (objRicevuto instanceof String) {
                    String rigaRicevuta = (String) objRicevuto;
                    System.out.println("[CLIENT " + socket.getRemoteSocketAddress() + "] ha inviato: " + rigaRicevuta);
                
                    // Analizza ed esegue il comando ricevuto secondo il protocollo stabilito
                    elaboraMessaggio(rigaRicevuta);
                }
            }

        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER] Connessione interrotta con il client: " + e.getMessage());
        } finally {
            // Se il ciclo si interrompe (es. disconnessione o logout), puliamo le risorse
            chiudiRisorse();
        }
    }

    /**
     * Parsing del Protocollo di Rete (Definisce come interpretare le stringhe del client).
     */
    private void elaboraMessaggio(String messaggio) {
        // Usiamo un limite nello split per non rompere i messaggi contenenti ":"
        String[] parti = messaggio.split(":", 2);
        if (parti.length == 0) return;
        
        String comando = parti[0].toUpperCase();

        switch (comando) {
            case "LOGIN":
                String[] datiLogin = parti[1].split(":");
                if (datiLogin.length == 2) {
                    String user = datiLogin[0];
                    String pass = datiLogin[1];
                    gestisciLogin(user, pass);
                } else {
                    inviaMessaggio("LOGIN_FAIL: Formato comando non valido.");
                }
                break;
                
            case "RISPOSTA":
                if (parti.length == 2) {
                    String parolaTentata = parti[1];
                    System.out.println("[GIOCO] L'utente " + getUsernameUtente() + " ha tentato: " + parolaTentata);
                }
                break;

            case "RICHIEDI_CLASSIFICA":
                System.out.println("[RICHIESTA] L'utente " + getUsernameUtente() + " ha richiesto la classifica globale.");
                gestisciRichiestaClassifica();
                break;

            case "RICHIEDI_STORICO":
                System.out.println("[RICHIESTA] L'utente " + getUsernameUtente() + " ha richiesto il proprio storico partite.");
                gestisciRichiestaStorico();
                break;

            case "DISCONNECT":
                inAscolto = false;
                break;

            default:
                inviaMessaggio("ERRORE: Comando sconosciuto.");
                break;
        }
    }

    private void gestisciLogin(String user, String pass) {
        Optional<Utente> utenteAutenticato = utenteDAO.login(user, pass);

        if (utenteAutenticato.isPresent()) {
            Utente u = utenteAutenticato.get();
            this.usernameUtente = u.getUsername();
            
            // Inviamo la risposta di successo includendo Ruolo e Username
            inviaMessaggio("LOGIN_SUCCESS:" + u.getRuolo() + ":" + u.getUsername());
            
            // Se è un normale giocatore ("PLAYER"), lo registriamo nel ServerManager per il matchmaking
            if (u.getRuolo().equalsIgnoreCase("PLAYER")) {
                serverManager.aggiungiGiocatorePronto(this); // Nome del metodo corretto!
            } else {
                System.out.println("[SERVER] L'amministratore '" + usernameUtente + "' si è connesso.");
            }
        } else {
            inviaMessaggio("LOGIN_FAIL:Username o Password errati.");
        }
    }

    private void gestisciRichiestaClassifica() {
        try {
            String datiClassifica = utenteDAO.getClassificaGlobaleFormattata(); 
            inviaMessaggio("DATI_CLASSIFICA:" + datiClassifica);
        } catch (Exception e) {
            System.err.println("[SERVER] Errore nel recupero della classifica: " + e.getMessage());
            inviaMessaggio("ERRORE: Impossibile recuperare la classifica.");
        }
    }

    private void gestisciRichiestaStorico() {
        if (usernameUtente == null) {
            inviaMessaggio("ERRORE: Devi prima effettuare il login.");
            return;
        }
        try {
            String datiStorico = utenteDAO.getStoricoPartiteFormattato(this.usernameUtente);
            inviaMessaggio("DATI_STORICO:" + datiStorico);
        } catch (Exception e) {
            System.err.println("[SERVER] Errore nel recupero dello storico per " + usernameUtente + ": " + e.getMessage());
            inviaMessaggio("ERRORE: Impossibile recuperare lo storico partite.");
        }
    }

    public void inviaMessaggio(String messaggio) {
        try {
            if (out != null) {
                out.writeObject(messaggio);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Errore invio stringa: " + e.getMessage());
        }
    }

    public void inviaOggetto(Object obj) {
        try {
            if (out != null) {
                out.writeObject(obj);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Errore invio oggetto: " + e.getMessage());
        }
    }

    public String getUsernameUtente() {
        return usernameUtente != null ? usernameUtente : "Anonimo";
    }

    private void chiudiRisorse() {
        System.out.println("[CLIENT HANDLER] Chiusura connessione per l'utente: " + getUsernameUtente());
        serverManager.disconnettiClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[CLIENT HANDLER] Errore chiusura socket stream: " + e.getMessage());
        }
    }
}