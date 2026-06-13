/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.db.UtenteDAO;
import guesstheword_server.model.Utente;
import static guesstheword_server.utils.PasswordUtils.hashPassword;
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
    private long idUtente = -1;
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
            
            /* --- INIZIO BYPASS PER IL TEST ---
            this.usernameUtente = "Tester_" + socket.getPort(); // Assegna un nome finto
            serverManager.giocatoreAutenticato(this);        // Usa il metodo col nome corretto!
            */
            
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
            
            case "REGISTRAZIONE":
                String[] datiReg = parti[1].split(":");
                if (datiReg.length == 3) {
                    String rUser = datiReg[0].trim();
                    String rPass = datiReg[1].trim();
                    String rRuolo = datiReg[2].trim();
        
                    // Unico blocco try per l'intera transazione di registrazione
                    try {
                        // 1. Controllo preventivo: l'username esiste?
                        if (utenteDAO.esisteUsername(rUser)) {
                            System.out.println("[REGISTRAZIONE NEGATA] Username già occupato: " + rUser);
                            inviaMessaggio("REG_FAIL:Impossibile registrarsi. Username già esistente.");
                            break; 
                        }
                        
                        String passwordCifrata = hashPassword(rPass, rUser);
                        
                        // 2. Se non esiste, procediamo direttamente all'inserimento
                        Utente nuovoUtente = new Utente(rUser, passwordCifrata, rRuolo);
                        utenteDAO.insert(nuovoUtente);
            
                        // 3. Successo!
                        inviaMessaggio("REG_SUCCESS:Account creato! Adesso puoi fare il login.");
            
                    } catch (Exception e) {
                    // Qualsiasi cosa vada storta (SQL, Driver, tabelle errate), la catturiamo qui
                    System.err.println("[SERVER] Errore critico durante la registrazione di " + rUser + ": " + e.getMessage());
                    e.printStackTrace(); // Ti stampa in console l'errore reale per fare debug!
            
                    inviaMessaggio("REG_FAIL:Errore interno del server durante la registrazione.");
                }
            } else {
                inviaMessaggio("REG_FAIL:Formato dati registrazione errato.");
            }
            break;
                
            case "AVVIA_SFIDA":
                System.out.println("[CODA] L'utente " + getUsernameUtente() + " ha richiesto di avviare una sfida dalla Lobby.");
        
                // Spostiamo qui la registrazione per il matchmaking!
                if (this.usernameUtente != null) {
                    serverManager.aggiungiGiocatorePronto(this);
                } else {
                    inviaMessaggio("ERRORE: Utente non autenticato.");
                }
                break;
                
            case "RISPOSTA":
                if (parti.length == 2) {
                    String parolaTentata = parti[1];
                    System.out.println("[GIOCO] L'utente " + getUsernameUtente() + " ha tentato: " + parolaTentata);
                    serverManager.verificaTentativo(this, parolaTentata);
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
            
            if (serverManager.isGiocatoreGiaConnesso(u.getUsername())) {
                System.out.println("[LOGIN NEGATO] L'utente '" + u.getUsername() + "' è già connesso da un altro client.");
                inviaMessaggio("LOGIN_FAIL:Questo account è già correntemente connesso.");
                return; // Interrompe il login
            
            }
            this.usernameUtente = u.getUsername();
            this.idUtente = u.getIdUtente();
            // Inviamo la risposta di successo includendo Ruolo e Username
            inviaMessaggio("LOGIN_SUCCESS:" + u.getRuolo() + ":" + u.getUsername());
            
            // Se è un normale giocatore ("PLAYER"), lo registriamo nel ServerManager per il matchmaking
            if (u.getRuolo().equalsIgnoreCase("PLAYER")) {
                System.out.println("[SERVER] Il giocatore '" + usernameUtente + "' è entrato nella Lobby di attesa.");
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
        
            // Controlliamo se la classifica restituita dal DAO è null o vuota
            if (datiClassifica == null || datiClassifica.trim().isEmpty()) {
                System.out.println("[SERVER] Classifica globale ancora vuota.");
                // Inviamo un messaggio specifico di classifica vuota al client
                inviaMessaggio("DATI_CLASSIFICA:VUOTO");
            } else {
                // Se ci sono dati, li inviamo normalmente
                inviaMessaggio("DATI_CLASSIFICA:" + datiClassifica);
            }
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
            // Usiamo il RisultatoDAO per interrogare il DB usando il VERO id dell'utente connesso
            guesstheword_server.db.RisultatoDAO risultatoDAO = new guesstheword_server.db.RisultatoDAO();
            String datiStorico = risultatoDAO.getStoricoFormattato(this.idUtente);
            
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
                out.reset();
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
    
    public long getIdUtente() { return this.idUtente; }
}
