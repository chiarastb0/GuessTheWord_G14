/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.ConfigManager;
import guesstheword_server.model.PacchettoSfida;
import guesstheword_server.utils.CifrarioUtils;
import guesstheword_server.model.Partita;
import guesstheword_server.model.Risultato;
import guesstheword_server.db.PartitaDAO;
import guesstheword_server.db.RisultatoDAO;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.*;

public class ServerManager {
    private boolean inEsecuzione = true;
    private final List<ClientHandler> clientConnessi = new ArrayList<>();
    private final List<ClientHandler> giocatoriPronti = new ArrayList<>();
    
    //VARIABILI PER LA GESTIONE DELLA SFIDA
    private Map<String, Long> dizionarioAttivo;
    private String testoIntegraleAttivo; // Contiene l'intero file .txt
    
    // Stato della partita corrente
    private String difficoltaCorrente = "Facile"; // Impostato dall'Admin
    private final Map<String, String> mappaParoleSegrete = new HashMap<>(); // Chiave: Parola Chiara, Valore: Parola Cifrata
    // Mappa che associa a ogni giocatore il SUO set di parole indovinate
    private final Map<ClientHandler, Set<String>> progressiGiocatori = new HashMap<>();
    
    private boolean partitaInCorso = false;
    private Timer timerPartita;
    private long timestampInizioSfida;
    private final int DURATA_TIMER = 60; // 60 secondi standard

    /**
     * Riceve la configurazione iniziale dall'interfaccia Admin
     */
    public void setDatiSfida(Map<String, Long> dizionario, String testoIntegrale) {
        this.dizionarioAttivo = dizionario;
        this.testoIntegraleAttivo = testoIntegrale;
        System.out.println("[SERVER] ServerManager pronto. Parole dizionario: " + dizionario.size());
    }

    /**
     * Permette all'interfaccia Admin di impostare la difficoltà prima del matchmaking
     */
    public void setDifficoltaCorrente(String difficolta) {
        this.difficoltaCorrente = difficolta;
        System.out.println("[SERVER] Difficoltà impostata a: " + difficoltaCorrente);
    }

    public void start() {
        int port = ConfigManager.getServerPort(); 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato correttamente sulla porta: " + port);
           
            while (inEsecuzione) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso da: " + clientSocket.getRemoteSocketAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientConnessi.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel ServerSocket: " + e.getMessage());
        }
    }

    public synchronized void aggiungiGiocatorePronto(ClientHandler handler) {
        if (!giocatoriPronti.contains(handler)) {
            giocatoriPronti.add(handler);
            System.out.println("[SERVER] Giocatore pronto: " + handler.getUsernameUtente() + " (" + giocatoriPronti.size() + "/2)");
            
            if (giocatoriPronti.size() == 2) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Pausa di sincronizzazione per JavaFX
                    } catch (InterruptedException e) { }
                    avviaSfidaDinamica();
                }).start();
            }
        }
    }

    /**
     * Algoritmo di estrazione e cifratura basato sui 4 parametri del professore
     */
    private void avviaSfidaDinamica() {
        if (dizionarioAttivo == null || dizionarioAttivo.isEmpty() || testoIntegraleAttivo == null) {
            System.err.println("[SERVER ERRORE] Dati mancanti per avviare la sfida!");
            return;
        }

        // Reset dei contenitori di stato
        mappaParoleSegrete.clear();
        progressiGiocatori.clear();
        
        // Creiamo un cesto vuoto personale per ogni giocatore connesso
        for (ClientHandler gh : giocatoriPronti) {
            progressiGiocatori.put(gh, new HashSet<>());
        }

        // 1. Configurazione dei parametri in base alla difficoltà
        int numeroParoleDaEstrarre = 1;
        int lunghezzaMinima = 4;
        int lunghezzaMassima = 6;
        int shiftMin = 1, shiftMax = 3;
        
        // Determiniamo la soglia di frequenza (rara vs comune) ordinando le parole
        List<Map.Entry<String, Long>> elencoOrdinato = new ArrayList<>(dizionarioAttivo.entrySet());
        elencoOrdinato.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); // Decrescente (più frequenti in cima)

        int dimensioneDizionario = elencoOrdinato.size();
        int indiceInizioSoglia = 0;
        int indiceFineSoglia = dimensioneDizionario;

        switch (difficoltaCorrente.toUpperCase()) {
            case "MEDIA":
                numeroParoleDaEstrarre = 2;
                lunghezzaMinima = 7;
                lunghezzaMassima = 9;
                shiftMin = 4; shiftMax = 7;
                // Prendiamo la fascia media di frequenza (centrale)
                indiceInizioSoglia = (int) (dimensioneDizionario * 0.3);
                indiceFineSoglia = (int) (dimensioneDizionario * 0.7);
                break;
                
            case "DIFFICILE":
                numeroParoleDaEstrarre = 3;
                lunghezzaMinima = 10;
                lunghezzaMassima = 30; // Parole lunghe o composte
                shiftMin = 8; shiftMax = 12;
                // Parole rare (in fondo alla classifica delle frequenze)
                indiceInizioSoglia = (int) (dimensioneDizionario * 0.7);
                break;
                
            case "FACILE":
            default:
                numeroParoleDaEstrarre = 1;
                lunghezzaMinima = 4;
                lunghezzaMassima = 6;
                shiftMin = 1; shiftMax = 3;
                // Parole molto comuni (in cima alla classifica)
                indiceFineSoglia = (int) (dimensioneDizionario * 0.3);
                break;
        }

        // Isoliamo il pool di parole idonee
        List<String> poolIdonee = new ArrayList<>();
        if (indiceInizioSoglia >= indiceFineSoglia || indiceInizioSoglia >= dimensioneDizionario) {
            indiceInizioSoglia = 0;
            indiceFineSoglia = dimensioneDizionario;
        }
        
        for (int i = indiceInizioSoglia; i < indiceFineSoglia; i++) {
            String parola = elencoOrdinato.get(i).getKey();
            if (parola.length() >= lunghezzaMinima && parola.length() <= lunghezzaMassima) {
                poolIdonee.add(parola);
            }
        }

        // Meccanismo di ripiego (Fallback) se il testo è troppo corto e il filtro è vuoto
        if (poolIdonee.size() < numeroParoleDaEstrarre) {
            poolIdonee.clear();
            for (Map.Entry<String, Long> entry : elencoOrdinato) {
                poolIdonee.add(entry.getKey());
            }
        }

        // 2. Estrazione effettiva e applicazione del Cifrario di Cesare
        Collections.shuffle(poolIdonee);
        Random random = new Random();
        
        for (int i = 0; i < Math.min(numeroParoleDaEstrarre, poolIdonee.size()); i++) {
            String parolaChiara = poolIdonee.get(i).toLowerCase();
            int shiftEffettivo = random.nextInt((shiftMax - shiftMin) + 1) + shiftMin;
            String parolaCifrata = CifrarioUtils.cifratura(parolaChiara, shiftEffettivo);
            mappaParoleSegrete.put(parolaChiara, parolaCifrata);
        }

        this.partitaInCorso = true;
        this.timestampInizioSfida = System.currentTimeMillis();

        // 3. Attivazione del Timer asincrono
        if (timerPartita != null) timerPartita.cancel();
        timerPartita = new Timer();
        timerPartita.schedule(new TimerTask() {
            @Override
            public void run() {
                terminaPartitaPareggio();
            }
        }, DURATA_TIMER * 1000L);

        System.out.println("[SERVER] Sfida avviata (" + difficoltaCorrente + "). Parole segrete: " + mappaParoleSegrete.keySet());

        // Invia il pacchetto iniziale generato
        inviaStatoGiocoAiClient();
    }

    /**
     * Cicla i giocatori e invia a ciascuno il SUO stato personale
     */
    private void inviaStatoGiocoAiClient() {
        for (ClientHandler giocatore : giocatoriPronti) {
            inviaStatoGiocoPersonale(giocatore);
        }
    }

    /**
     * Costruisce il testo in base alle parole indovinate DAL SINGOLO GIOCATORE
     */
    private void inviaStatoGiocoPersonale(ClientHandler giocatore) {
        String testoDaInviare = this.testoIntegraleAttivo;
        Set<String> sueParole = progressiGiocatori.get(giocatore);

        for (Map.Entry<String, String> entry : mappaParoleSegrete.entrySet()) {
            String chiara = entry.getKey();
            String cifrata = entry.getValue();

            if (sueParole != null && sueParole.contains(chiara)) {
                testoDaInviare = testoDaInviare.replaceAll("(?i)\\b" + chiara + "\\b", "*" + chiara.toUpperCase() + "*");
            } else {
                testoDaInviare = testoDaInviare.replaceAll("(?i)\\b" + chiara + "\\b", "[ " + cifrata + " ]");
            }
        }

        int secondiTrascorsi = (int) ((System.currentTimeMillis() - timestampInizioSfida) / 1000);
        int secondiRimanenti = Math.max(0, DURATA_TIMER - secondiTrascorsi);

        PacchettoSfida pacchetto = new PacchettoSfida(testoDaInviare, secondiRimanenti, this.difficoltaCorrente);
        giocatore.inviaOggetto(pacchetto); 
    }

    /**
     * Verifica i tentativi inoltrati dai singoli ClientHandler
     */
    public synchronized void verificaTentativo(ClientHandler giocatore, String tentativo) {
        if (!partitaInCorso) return;

        String pulito = tentativo.trim().toLowerCase();
        Set<String> sueParole = progressiGiocatori.get(giocatore);

        // Verifichiamo se la parola è segreta ed è ancora da indovinare PER QUESTO GIOCATORE
        if (sueParole != null && mappaParoleSegrete.containsKey(pulito) && !sueParole.contains(pulito)) {
            sueParole.add(pulito);
            System.out.println("[GIOCO] " + giocatore.getUsernameUtente() + " ha scovato: " + pulito + " (" + sueParole.size() + "/" + mappaParoleSegrete.size() + ")");

            if (sueParole.size() == mappaParoleSegrete.size()) {
                terminaPartitaVittoria(giocatore);
            } else {
                // Notifichiamo SOLO chi ha indovinato la parola
                giocatore.inviaMessaggio("NOTIFICA:Ottimo! Hai decifrato la parola '" + pulito.toUpperCase() + "'!");
                // Aggiorniamo SOLO la sua interfaccia grafica
                inviaStatoGiocoPersonale(giocatore);
            }
        } else {
            giocatore.inviaMessaggio("RISPOSTA_ERRATA:La parola non è corretta o l'hai già trovata.");
        }
    }

    private synchronized void terminaPartitaVittoria(ClientHandler vincitore) {
        if (!partitaInCorso) return;
        partitaInCorso = false;
        timerPartita.cancel();

        String elencoCompletoParole = String.join(", ", mappaParoleSegrete.keySet()).toUpperCase();
        System.out.println("[FINE PARTITA] Ha vinto " + vincitore.getUsernameUtente());

        for (ClientHandler giocatore : giocatoriPronti) {
            if (giocatore == vincitore) {
                giocatore.inviaMessaggio("FINE_PARTITA:VITTORIA:Complimenti! Hai scovato l'ultima parola. Elenco completo: " + elencoCompletoParole);
            } else {
                giocatore.inviaMessaggio("FINE_PARTITA:SCONFITTA:Il giocatore " + vincitore.getUsernameUtente() + " ha completato la sfida prima di te. Le parole erano: " + elencoCompletoParole);
            }
        }
        
        salvaDatiNelDatabase(vincitore);
        resetPartita();
    }

    private synchronized void terminaPartitaPareggio() {
        if (!partitaInCorso) return;
        partitaInCorso = false;

        String elencoCompletoParole = String.join(", ", mappaParoleSegrete.keySet()).toUpperCase();
        System.out.println("[FINE PARTITA] Tempo scaduto.");

        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaMessaggio("FINE_PARTITA:PAREGGIO:Tempo scaduto! Non avete trovato tutte le parole. Soluzione: " + elencoCompletoParole);
        }
        
        salvaDatiNelDatabase(null);
        resetPartita();
    }

    private void salvaDatiNelDatabase(ClientHandler vincitore) {
        try {
            PartitaDAO partitaDAO = new PartitaDAO();
            RisultatoDAO risultatoDAO = new RisultatoDAO();

            String dataOra = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // Aggreghiamo l'elenco delle parole separate da virgola per memorizzarlo nel DB
            String paroleUnite = String.join(",", mappaParoleSegrete.keySet());
            
            // Passiamo anche la difficoltà corrente al costruttore della Partita
            Partita nuovaPartita = new Partita(dataOra, paroleUnite, this.difficoltaCorrente);
            
            long idPartita = partitaDAO.inserisciERestituisciId(nuovaPartita);
            if (idPartita == -1) return;

            for (ClientHandler giocatore : giocatoriPronti) {
                String esito = "PAREGGIO";
                int tempoGiocatore = DURATA_TIMER * 1000; 

                if (vincitore != null) {
                    if (giocatore == vincitore) {
                        esito = "VITTORIA";
                        tempoGiocatore = (int)(System.currentTimeMillis() - timestampInizioSfida); 
                    } else {
                        esito = "SCONFITTA";
                    }
                }

                Risultato r = new Risultato(idPartita, giocatore.getIdUtente(), esito, tempoGiocatore);
                risultatoDAO.insert(r);
            }
            System.out.println("[SERVER DB] Record salvati correttamente con difficoltà: " + difficoltaCorrente);
        } catch (Exception e) {
            System.err.println("[SERVER DB] Errore salvataggio: " + e.getMessage());
        }
    }

    private void resetPartita() {
        giocatoriPronti.clear();
        mappaParoleSegrete.clear();
        progressiGiocatori.clear();
        System.out.println("[SERVER] Lobby ripulita. Pronto per un nuovo set.");
    }
    
    public synchronized void disconnettiClient(ClientHandler handler) {
        clientConnessi.remove(handler);
        giocatoriPronti.remove(handler);
    }
    
    public void fermaServer() {
        this.inEsecuzione = false;
    }
}
