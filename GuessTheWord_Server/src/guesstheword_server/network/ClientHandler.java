package guesstheword_server.network;

import guesstheword_server.db.UtenteDAO;
import guesstheword_server.model.Utente;
import static guesstheword_server.utils.PasswordUtils.hashPassword;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;

/**
 * Gestisce la sessione di comunicazione con un singolo client su un thread separato.
 * Intercetta le richieste di rete, esegue le operazioni di database richieste 
 * e instrada le azioni di gioco verso il ServerManager.
 * 
 */
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final ServerManager serverManager;
    
    // Canali di comunicazione per leggere (Input) e scrivere (Output) sulla rete
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    // Variabili di stato per la sessione corrente
    private boolean inAscolto = true;
    private String usernameUtente = null; 
    private long idUtente = -1;
    private final UtenteDAO utenteDAO;

    /**
     * Inizializza un nuovo gestore per il client appena connesso.
     * * @param socket Il socket generato dall'accettazione della connessione.
     * @param serverManager Il riferimento al gestore centrale del server.
     */
    public ClientHandler(Socket socket, ServerManager serverManager) {
        this.socket = socket;
        this.serverManager = serverManager;
        this.utenteDAO = new UtenteDAO(); 
    }

    /**
     * Metodo eseguito all'avvio del Thread.
     * Mantiene attiva la connessione e resta in ascolto continuo dei messaggi inviati dal client.
     */
    @Override
    public void run() {
        try {
            // 1. Inizializziamo prima l'Output e facciamo un flush per evitare blocchi del buffer
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();    
            // 2. Inizializziamo l'Input per metterci in ascolto
            in = new ObjectInputStream(socket.getInputStream());
            
            Object objRicevuto;
            
            // Il ciclo continua finché inAscolto è true e ci sono dati in arrivo
            while (inAscolto && (objRicevuto = in.readObject()) != null) {
                if (objRicevuto instanceof String) {
                    String rigaRicevuta = (String) objRicevuto;
                    System.out.println("[CLIENT " + socket.getRemoteSocketAddress() + "] ha inviato: " + rigaRicevuta);
                
                    // Passiamo la stringa ricevuta al metodo per gestire i messaggi
                    elaboraMessaggio(rigaRicevuta);
                }
            }

        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER] Connessione interrotta con il client: " + e.getMessage());
        } finally {
            // In caso di errore o di uscita volontaria dal ciclo, chiudiamo tutto in sicurezza
            chiudiRisorse();
        }
    }

    /**
     * Motore principale di parsing del Protocollo di Rete.
     * Interpreta il comando (prima dei due punti) ed esegue l'azione associata.
     * * @param messaggio La stringa grezza ricevuta dal client (es. "LOGIN:player1:player123").
     */
    private void elaboraMessaggio(String messaggio) {
        // Dividiamo la stringa al massimo in 2 parti, garantendo che se ci sono altri ":" nei dati non vengano spezzati
        String[] parti = messaggio.split(":", 2);
        if (parti.length == 0) return;
        
        // Il comando è sempre la prima parola, convertita in maiuscolo per sicurezza
        String comando = parti[0].toUpperCase();

        switch (comando) {
            case "LOGIN":
                String[] datiLogin = parti[1].split(":");
                if (datiLogin.length == 2) {
                    gestisciLogin(datiLogin[0], datiLogin[1]);
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
        
                    try {
                        // 1. Controllo preventivo per evitare duplicati nel DB
                        if (utenteDAO.esisteUsername(rUser)) {
                            System.out.println("[REGISTRAZIONE NEGATA] Username già occupato: " + rUser);
                            inviaMessaggio("REG_FAIL:Impossibile registrarsi. Username già esistente.");
                            break; 
                        }
                        
                        // 2. Protezione della password prima dell'inserimento
                        String passwordCifrata = hashPassword(rPass, rUser);
                        
                        // 3. Salvataggio definitivo
                        Utente nuovoUtente = new Utente(rUser, passwordCifrata, rRuolo);
                        utenteDAO.insert(nuovoUtente);
            
                        inviaMessaggio("REG_SUCCESS:Account creato! Adesso puoi fare il login.");
            
                    } catch (Exception e) {
                        System.err.println("[SERVER] Errore critico durante la registrazione di " + rUser + ": " + e.getMessage());
                        e.printStackTrace(); 
                        inviaMessaggio("REG_FAIL:Errore interno del server durante la registrazione.");
                    }
                } else {
                    inviaMessaggio("REG_FAIL:Formato dati registrazione errato.");
                }
                break;
                
            case "AVVIA_SFIDA":
                System.out.println("[CODA] L'utente " + getUsernameUtente() + " ha richiesto di avviare una sfida dalla Lobby.");
                // Verifica di sicurezza prima di inserire il client nel Matchmaking
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
                    // Passa il tentativo al gestore globale della partita
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
                // Questo ferma il ciclo while nel run(), innescando la chiusura delle risorse
                inAscolto = false;
                break;

            default:
                inviaMessaggio("ERRORE: Comando sconosciuto.");
                break;
        }
    }

    /**
     * Gestisce l'autenticazione di un utente.
     * Implementa una politica di blocco per i login multipli simultanei.
     */
    private void gestisciLogin(String user, String pass) {
        Optional<Utente> utenteAutenticato = utenteDAO.login(user, pass);

        if (utenteAutenticato.isPresent()) {
            Utente u = utenteAutenticato.get();
            
            // Verifichiamo che l'utente non sia già loggato da un altro PC/finestra
            if (serverManager.isGiocatoreGiaConnesso(u.getUsername())) {
                System.out.println("[LOGIN NEGATO] L'utente '" + u.getUsername() + "' è già connesso da un altro client.");
                inviaMessaggio("LOGIN_FAIL:Questo account è già correntemente connesso.");
                return; 
            }
            
            // Salviamo i dati nella sessione corrente di questo Thread
            this.usernameUtente = u.getUsername();
            this.idUtente = u.getIdUtente();
            
            inviaMessaggio("LOGIN_SUCCESS:" + u.getRuolo() + ":" + u.getUsername());
            
            if (u.getRuolo().equalsIgnoreCase("PLAYER")) {
                System.out.println("[SERVER] Il giocatore '" + usernameUtente + "' è entrato nella Lobby di attesa.");
            } else {
                System.out.println("[SERVER] L'amministratore '" + usernameUtente + "' si è connesso.");
            }
        } else {
            inviaMessaggio("LOGIN_FAIL:Username o Password errati.");
        }
    }

    /**
     * Recupera e invia la classifica globale di tutti i giocatori.
     */
    private void gestisciRichiestaClassifica() {
        try {
            String datiClassifica = utenteDAO.getClassificaGlobaleFormattata(); 
            if (datiClassifica == null || datiClassifica.trim().isEmpty()) {
                System.out.println("[SERVER] Classifica globale ancora vuota.");
                inviaMessaggio("DATI_CLASSIFICA:VUOTO");
            } else {
                inviaMessaggio("DATI_CLASSIFICA:" + datiClassifica);
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Errore nel recupero della classifica: " + e.getMessage());
            inviaMessaggio("ERRORE: Impossibile recuperare la classifica.");
        }
    }

    /**
     * Recupera e invia lo storico delle partite dell'utente attualmente connesso.
     */
    private void gestisciRichiestaStorico() {
        if (usernameUtente == null) {
            inviaMessaggio("ERRORE: Devi prima effettuare il login.");
            return;
        }

        try {
            guesstheword_server.db.RisultatoDAO risultatoDAO = new guesstheword_server.db.RisultatoDAO();
            String datiStorico = risultatoDAO.getStoricoFormattato(this.idUtente);
            inviaMessaggio("DATI_STORICO:" + datiStorico);
        } catch (Exception e) {
            System.err.println("[SERVER] Errore nel recupero dello storico per " + usernameUtente + ": " + e.getMessage());
            inviaMessaggio("ERRORE: Impossibile recuperare lo storico partite.");
        }
    }
    
    /**
     * Invia una singola stringa di testo al client.
     * * @param messaggio Il testo da inviare secondo il protocollo (es. "NOTIFICA:Hai indovinato!").
     */
    public void inviaMessaggio(String messaggio) {
        try {
            if (out != null) {
                out.writeObject(messaggio);
                out.flush();
                out.reset(); // Evita problemi di caching della memoria
            }
        } catch (IOException e) {
            System.err.println("Errore invio stringa: " + e.getMessage());
        }
    }

    /**
     * Invia un oggetto serializzato complesso al client.
     * Utilizzato principalmente per inoltrare il PacchettoSfida durante il gioco.
     * * @param obj L'oggetto Java da trasferire (deve implementare Serializable).
     */
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

    /**
     * Restituisce il nome utente del client corrente, utile per log e identificazione.
     * @return Il nome utente o "Anonimo" se non ha ancora effettuato il login.
     */
    public String getUsernameUtente() {
        return usernameUtente != null ? usernameUtente : "Anonimo";
    }

    /**
     * Restituisce l'ID univoco assegnato all'utente dal database.
     * @return L'ID dell'utente.
     */
    public long getIdUtente() { 
        return this.idUtente; 
    }

    /**
     * Procedura di spegnimento sicuro della sessione.
     * Rimuove il client dal ServerManager e chiude i socket per evitare memory leak.
     */
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