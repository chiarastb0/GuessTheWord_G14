/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.db.UtenteDAO;
import guesstheword_server.model.Utente;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;


// Gestisce la sessione di comunicazione con un singolo client su un thread separato.
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final ServerManager serverManager;
    
    // Canali per leggere e scrivere sulla rete
    private BufferedReader in;
    private PrintWriter out;
    
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // 'true' forza l'invio immediato senza buffer (autoflush)

            String rigaRicevuta;
            // Il thread resta confinato in questo ciclo leggendo ogni stringa inviata dal client
            while (inAscolto && (rigaRicevuta = in.readLine()) != null) {
                System.out.println("[CLIENT " + socket.getRemoteSocketAddress() + "] ha inviato: " + rigaRicevuta);
                
                // Analizza ed esegue il comando ricevuto secondo il protocollo stabilito
                elaboraMessaggio(rigaRicevuta);
            }

        } catch (IOException e) {
            System.err.println("[CLIENT HANDLER] Connessione interrotta con il client: " + e.getMessage());
        } finally {
            // Se il ciclo si interrompe (es. disconnessione o logout), puliamo le risorse
            chiudiRisorse();
        }
    }

    /**
     * Parsing del Protocollo di Rete (Definisce come interpretare le stringhe del client).
     * Gestisce i comandi separati dal carattere speciale ":" (Es. LOGIN:username:password)
     */
    private void elaboraMessaggio(String messaggio) {
        String[] parti = messaggio.split(":");
        if (parti.length == 0) return;
        
        String comando = parti[0].toUpperCase();

        switch (comando) {
            case "LOGIN":
                if (parti.length == 3) {
                    String user = parti[1];
                    String pass = parti[2];
                    gestisciLogin(user, pass);
                } else {
                    inviaMessaggio("LOGIN_FAIL: Formato comando non valido.");
                }
                break;
                
            case "RISPOSTA":
                // Questo comando servirà nella Fase 3/4 per ricevere i tentativi di gioco delle parole
                if (parti.length == 2) {
                    String parolaTentata = parti[1];
                    System.out.println("[GIOCO] L'utente " + getUsernameUtente() + " ha tentato: " + parolaTentata);
                    // QUI in futuro vi collegherete al controllo vincitore del Compagno 3
                }
                break;

            case "DISCONNECT":
                inAscolto = false;
                break;

            default:
                inviaMessaggio("ERRORE: Comando sconosciuto.");
                break;
        }
    }

    /**
     * Esegue la verifica delle credenziali interfacciandosi con l'UtenteDAO.
     */
    private void gestisciLogin(String user, String pass) {
        // Usiamo il metodo di login del tuo UtenteDAO 
        Optional<Utente> utenteAutenticato = utenteDAO.login(user, pass);

        if (utenteAutenticato.isPresent()) {
            Utente u = utenteAutenticato.get();
            this.usernameUtente = u.getUsername();
            
            // Inviamo la risposta di successo includendo Ruolo e Username (Requisito fondamentale del Bando!)
            inviaMessaggio("LOGIN_SUCCESS:" + u.getRuolo() + ":" + u.getUsername());
            
            // Se è un normale giocatore ("PLAYER"), lo registriamo nel ServerManager per il matchmaking
            if (u.getRuolo().equalsIgnoreCase("PLAYER")) {
                serverManager.giocatoreAutenticato(this);
            } else {
                System.out.println("[SERVER] L'amministratore '" + usernameUtente + "' si è connesso.");
            }
        } else {
            
            inviaMessaggio("LOGIN_FAIL:Username o Password errati.");
        }
    }


    public void inviaMessaggio(String messaggio) {
        if (out != null) {
            out.println(messaggio);
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
