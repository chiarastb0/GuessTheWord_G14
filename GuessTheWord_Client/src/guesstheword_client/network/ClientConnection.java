/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.network;

import guesstheword_client.controller.*;
import guesstheword_server.model.PacchettoSfida;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.Platform;

/**
 * @class ClientConnection
 * @brief Gestisce la connessione Socket lato Client e l'ascolto asincrono dei messaggi del Server.
 * Implementa l'interfaccia Runnable per poter eseguire la lettura dei dati provenienti 
 * dal Server all'interno di un thread dedicato, evitando così di bloccare l'interfaccia grafica.
 */
public class ClientConnection implements Runnable {

    /** 
     * @brief Indirizzo IP del Server a cui connettersi. 
    */
    private final String ip;
    
    /** 
     * @brief Porta del Server sulla quale stabilire la sessione. 
    */
    private final int porta;
    
    /** 
     * @brief Il socket effettivo della connessione corrente. 
     */
    private Socket socket;
    
    /** 
     * @brief Canale di input per la ricezione di oggetti serializzati dal Server. 
    */
    private ObjectInputStream in;
    
    /** 
     * @brief Canale di output per l'invio di oggetti serializzati al Server. 
    */
    private ObjectOutputStream out;
    
    /** 
     * @brief Flag booleano per controllare il ciclo di vita del thread di ascolto. 
    */
    private boolean inAscolto = true;

    /** 
     * @brief Riferimento al controller della schermata di gioco principale (JavaFX). 
    */
    private GameController controllerGioco;
    
    /** 
     * @brief Riferimento al controller della schermata di autenticazione/registrazione (JavaFX). 
    */
    private AuthController controllerAuth;
    
    /** 
     * @brief Riferimento al controller della lobby principale e gestione classifiche (JavaFX). 
    */
    private LobbyController controllerLobby;
    
    /** 
     * @brief Cassetto temporaneo (cache) per memorizzare i dati dello storico nel caso in cui la schermata della Lobby non sia ancora pronta. 
    */
    private String cacheStorico = null;
    
    /** 
     * @brief Memorizza lo username dell'utente loggato su questa sessione client. 
    */
    private String usernameLoggato = "";

    /**
     * @brief Restituisce lo username dell'utente attualmente loggato.
     * @return String Lo username salvato nel client.
     */
    public String getUsernameLoggato() {
        return this.usernameLoggato;
    }
    
    /**
     * @brief Costruttore della classe ClientConnection.
     * Configura i parametri di rete essenziali senza avviare la connessione.
     * @param ip    L'indirizzo IP del Server.
     * @param porta La porta del Server.
     */
    public ClientConnection(String ip, int porta) {
        this.ip = ip;
        this.porta = porta;
    }

    /**
     * @brief Tenta la connessione iniziale con il Server e inizializza gli stream di Input/Output.
     * L'ObjectOutputStream viene flushato immediatamente per evitare deadlock di inizializzazione dello stream Socket.
     * @return true se la connessione è riuscita e i flussi sono pronti, false altrimenti.
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
     * @brief Ciclo continuo di ascolto dei messaggi provenienti dal ClientHandler del Server.
     * Legge gli oggetti in arrivo dal canale di input in modo asincrono. 
     * Gestisce due casistiche principali di messaggi:
     * - Caso 1 (String): Messaggi di testo ordinari delegati a elaboraMessaggioServer().
     * - Caso 2 (PacchettoSfida): Ricezione di aggiornamenti della parola cifrata e timer.
     */
    @Override
    public void run() {
        try {
            Object objRicevuto;
            while (inAscolto && (objRicevuto = in.readObject()) != null) {
                
                //Caso 1
                if (objRicevuto instanceof String) {
                    String rigaRicevuta = (String) objRicevuto;
                    System.out.println("[SERVER DICE]: " + rigaRicevuta);
                    elaboraMessaggioServer(rigaRicevuta); 
                }
                
                // Caso 2
                else if (objRicevuto instanceof PacchettoSfida) {
                    PacchettoSfida pacchetto = (PacchettoSfida) objRicevuto;
                    
 
                    System.out.println("[TEST RICEZIONE CLIENT]");
                    System.out.println("Testo della sfida: " + pacchetto.getParolaCifrata());
           
                    if (controllerGioco != null) {
                        controllerGioco.aggiornaTestoDinamicamente(pacchetto.getParolaCifrata());
                    }
                    else if (controllerLobby != null) {
                        controllerLobby.avviaSchermataGioco(
                            String.valueOf(pacchetto.getDurataTimerSecondi()), 
                            pacchetto.getParolaCifrata()
                        );
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
     * @brief Parsing del protocollo speculare a quello del Server.
     * Analizza i comandi testuali in arrivo dal Server ed esegue le relative azioni grafiche 
     * incapsulandole all'interno di Platform.runLater() per rispettare il thread-safety di JavaFX.
     * I comandi gestiti dallo switch-case sono:
     * - LOGIN_SUCCESS: Estrae ruolo e username, notificando l'accesso riuscito a AuthController.
     * - LOGIN_FAIL: Mostra il messaggio d'errore del server sul pannello di login.
     * - REG_SUCCESS: Gestisce la registrazione completata e torna al login.
     * - REG_FAIL: Visualizza l'errore specifico (es. utente duplicato) nel form di registrazione.
     * - START_GAME: Estrae i parametri di inizializzazione del match e avvia la schermata di gioco dalla Lobby.
     * - FINE_PARTITA: Notifica l'esito della partita per mostrare il resoconto finale a schermo.
     * - DATI_CLASSIFICA: Pulisce e ricompone riga per riga la TableView della classifica.
     * - DATI_STORICO: Riceve lo storico del giocatore; lo stampa direttamente se la lobby esiste, altrimenti popola la cache locale.
     * - RISPOSTA_ERRATA: Mostra un feedback grafico temporaneo di errore senza interrompere la digitazione.
     * - ERRORE: Stampa sul log di sistema gli errori imprevisti notificati dal Server.
     * * @param messaggio Il messaggio testuale grezzo inviato dal Server.
     */
    private void elaboraMessaggioServer(String messaggio) {
        
        messaggio = messaggio.trim();
        
        // Separazione dell'intestazione del comando dal resto dei dati
        String[] parti = messaggio.split(":", 2); 
        if (parti.length == 0) return;

        String comando = parti[0].toUpperCase().trim();

        switch (comando) {
            case "LOGIN_SUCCESS":
                System.out.println("[LOGIN] Accesso eseguito con successo.");
                
                String[] pezzi = messaggio.split(":");
                final String ruolo = (pezzi.length >= 2) ? pezzi[1].trim() : "PLAYER";
                
                if (pezzi.length >= 3) {
                    this.usernameLoggato = pezzi[2].trim();
                }
                
                if (controllerAuth != null) {
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreLogin("✅Accesso riuscito!");
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
                    final String messaggioServer = (parti.length >= 2) ? parti[1].trim() : "Registrazione completata!";
                
                    Platform.runLater(() -> {
                        controllerAuth.mostraMessaggioErroreReg("");
                        controllerAuth.mostraPannelloLogin(null);   
                    
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
                String[] sottoParti = parti[1].split(":", 2);
                if (sottoParti.length >= 2) {
                    String tempoInSecondi = sottoParti[0];
                    String testoCifrato = sottoParti[1];

                    if (controllerLobby != null) {

                        controllerLobby.avviaSchermataGioco(tempoInSecondi, testoCifrato);
                    }
                }
                break;
            
                
            case "FINE_PARTITA":
                System.out.println("[RETE CLIENT] Esito ricevuto: " + messaggio);
                if (controllerGioco != null) {
                    final String messaggioFinale = messaggio;
                    Platform.runLater(() -> {
                        controllerGioco.gestisciFinePartita(messaggioFinale);
                    });
                }
                break;

            case "DATI_CLASSIFICA":
                if (parti.length >= 2) {
                    String contenuto = parti[1];
                    if (controllerLobby != null) {
                        Platform.runLater(() -> {
                            controllerLobby.svuotaClassifica(); 
                            if (!contenuto.equalsIgnoreCase("VUOTO")) {
                                String[] righeGiocatori = contenuto.split(";");
                                for (String riga : righeGiocatori) {
                                    if (!riga.trim().isEmpty()) {
                                        String[] dati = riga.split(",");
                                        int pos = Integer.parseInt(dati[0]);
                                        String username = dati[1];
                                        int punti = Integer.parseInt(dati[2]);
                                        controllerLobby.aggiungiGiocatoreAClassifica(pos, username, punti);
                                    }
                                }
                            }
                        });
                    }
                }
                break;

            case "DATI_STORICO":
             if (parti.length >= 2) { 
                 String contenutoStorico = parti[1];

                 if (controllerLobby != null) {
                    Platform.runLater(() -> {
                            controllerLobby.svuotaStorico(); 
                            elaboraStoricoGrafica(contenutoStorico);
                        });
                 } else {
                     this.cacheStorico = contenutoStorico;
                 }
             }
             break;
             
            case "RISPOSTA_ERRATA":
                if (controllerGioco != null) {
                    Platform.runLater(() -> {
                        controllerGioco.mostraMessaggioErroreTemporaneo("Sbagliato, ritenta!");
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
     * @brief Invia un messaggio testuale (comando) al ClientHandler del Server.
     * @details Serializza una stringa e la trasmette immediatamente lungo il canale di output effettuando un flush().
     * @param messaggio Il comando testuale da inoltrare al Server.
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
     * @brief Registra il riferimento dell'AuthController.
     * @param controller Il controller della schermata di autenticazione.
     */
    public void setControllerAuth(AuthController controller) {
        this.controllerAuth = controller;
    }
    
    /**
     * @brief Registra il riferimento del controller di gioco principale.
     * Permette allo ScreenGameController di "registrarsi" così da poter ricevere 
     * i testi cifrati non appena la partita comincia o subisce modifiche in tempo reale.
     * @param controller Il controller della schermata di gioco.
     */
    public void setControllerGioco(GameController controller) {
        this.controllerGioco = controller;
    }
    
    /**
     * @brief Registra il riferimento del LobbyController e gestisce l'eventuale svuotamento della cache.
     * Se al momento della configurazione della lobby sono presenti dati dello storico salvati in precedenza 
     * nella `cacheStorico`, questi vengono processati immediatamente all'interno del Thread JavaFX.
     * @param controller Il controller della schermata Lobby.
     */
    public void setControllerLobby(LobbyController controller) {
     this.controllerLobby = controller;

     // Controllo di dati "in attesa"
     if (this.cacheStorico != null) {
         String datiInSospeso = this.cacheStorico;
         Platform.runLater(() -> elaboraStoricoGrafica(datiInSospeso));
         this.cacheStorico = null; 
     }
    }
    
    /**
     * @brief Analizza la stringa dello storico ed effettua la ricostruzione dei record.
     * Suddivide i record delimitati da ';' e ricompone i dati delimitati da ','. 
     * Prevede una logica flessibile basata sugli indici inversi per ricostruire frasi o parole complesse 
     * contenenti virgole, assumendo che l'esito sia sempre al penultimo posto e il punteggio all'ultimo.
     * @param contenutoStorico La stringa grezza ricevuta contenente i dati dello storico.
     */
    private void elaboraStoricoGrafica(String contenutoStorico) {
        if (contenutoStorico.equalsIgnoreCase("VUOTO") || controllerLobby == null) 
            return;

        String[] righeStorico = contenutoStorico.split(";");
        for (String riga : righeStorico) {
            if (!riga.trim().isEmpty()) {
                String[] dati = riga.split(",");
                
                // Verifica della presenza di almeno Data, Parola, Esito e Punteggio
                if (dati.length >= 4) {
                    String data = dati[0];
                    String punteggioStr = dati[dati.length - 1];
                    String esito = dati[dati.length - 2];        
                    
                    // Ricostruzione delle parole
                    StringBuilder parolaRicostruita = new StringBuilder();
                    for (int i = 1; i < dati.length - 2; i++) {
                        parolaRicostruita.append(dati[i]);
                        if (i < dati.length - 3) {
                            parolaRicostruita.append(", ");
                        }
                    }
                    
                    try {
                        controllerLobby.aggiungiPartitaAStorico(
                            data, 
                            parolaRicostruita.toString(), 
                            esito, 
                            Integer.parseInt(punteggioStr.trim())
                        );
                    } catch (NumberFormatException e) {
                        System.err.println("[CLIENT] Errore di lettura punteggio nello storico.");
                    }
                }
            }
        }
    }
    

    /**
     * @brief Chiude i flussi I/O e il socket in modo pulito.
     * @details Arresta il loop di ascolto impostando `inAscolto` a false e procede alla chiusura 
     * sequenziale di ObjectInputStream, ObjectOutputStream e del Socket se ancora aperti.
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