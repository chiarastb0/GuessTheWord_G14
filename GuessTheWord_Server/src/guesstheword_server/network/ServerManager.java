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

/**
 * Gestore centrale del Server.
 * Accetta le connessioni in entrata, gestisce la coda di matchmaking (Lobby) 
 * e orchestra l'intera logica di gioco (estrazione parole, timer, calcolo punteggi).
 * 
 */
public class ServerManager {
    
    private boolean inEsecuzione = true;
    private final List<ClientHandler> clientConnessi = new ArrayList<>();
    private final List<ClientHandler> giocatoriPronti = new ArrayList<>();
    
    // VARIABILI DI CONFIGURAZIONE GLOBALE (Gestite dall'Admin)
    private Map<String, Long> dizionarioAttivo;
    private String testoIntegraleAttivo; 
    
    // STATO DELLA PARTITA CORRENTE
    private String difficoltaCorrente = "Facile"; 
    
    // Mappa per associare la parola in chiaro alla sua versione cifrata con il Cifrario di Cesare
    private final Map<String, String> mappaParoleSegrete = new HashMap<>(); 
    
    // Mappa vitale per isolare i progressi: associa ad ogni singolo socket il SUO cesto di parole indovinate
    private final Map<ClientHandler, Set<String>> progressiGiocatori = new HashMap<>();
    
    private boolean partitaInCorso = false;
    private Timer timerPartita;
    private long timestampInizioSfida;
    private final int DURATA_TIMER = 60; // Durata fissa della partita in secondi

    /**
     * Riceve e memorizza il dizionario e il testo caricati dall'interfaccia dell'Amministratore.
     * * @param dizionario Mappa contenente le parole e la loro frequenza assoluta.
     * @param testoIntegrale Il contenuto testuale grezzo del file .txt caricato.
     */
    public void setDatiSfida(Map<String, Long> dizionario, String testoIntegrale) {
        this.dizionarioAttivo = dizionario;
        this.testoIntegraleAttivo = testoIntegrale;
        System.out.println("[SERVER] ServerManager pronto. Parole dizionario: " + dizionario.size());
    }

    /**
     * Aggiorna la difficoltà globale del server in tempo reale.
     * * @param difficolta La stringa rappresentante il livello (es. "Media").
     */
    public void setDifficoltaCorrente(String difficolta) {
        this.difficoltaCorrente = difficolta;
        System.out.println("[SERVER] Difficoltà impostata a: " + difficoltaCorrente);
    }

    /**
     * Punto di ingresso principale del Server.
     * Apre la porta di rete e resta in ascolto infinito di nuovi client,
     * assegnando ciascuno a un Thread separato (ClientHandler).
     */
    public void start() {
        int port = ConfigManager.getServerPort(); 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato correttamente sulla porta: " + port);
           
            while (inEsecuzione) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso da: " + clientSocket.getRemoteSocketAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientConnessi.add(handler);
                
                // Avvia la gestione di questo specifico client su un percorso asincrono
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel ServerSocket: " + e.getMessage());
        }
    }

    /**
     * Motore di Matchmaking: inserisce gli utenti in attesa in una coda.
     * Non appena la coda raggiunge 2 giocatori, innesca l'inizio della partita.
     * I metodi synchronized prevengono problemi di concorrenza se due client cliccano contemporaneamente.
     * * @param handler Il client che ha richiesto di giocare.
     */
    public synchronized void aggiungiGiocatorePronto(ClientHandler handler) {
        if (!giocatoriPronti.contains(handler)) {
            giocatoriPronti.add(handler);
            System.out.println("[SERVER] Giocatore pronto: " + handler.getUsernameUtente() + " (" + giocatoriPronti.size() + "/2)");
            
            if (giocatoriPronti.size() == 2) {
                // Lanciamo l'avvio della sfida su un thread separato per non bloccare l'accettazione di altri comandi
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Piccola pausa estetica per permettere ai client di caricare la View
                    } catch (InterruptedException e) { }
                    avviaSfidaDinamica();
                }).start();
            }
        }
    }

    /**
     * Cuore logico del gioco. Implementa l'algoritmo di estrazione delle parole, 
     * il calcolo della difficoltà e l'offuscamento tramite crittografia.
     */
    private void avviaSfidaDinamica() {

        // 1. Pulizia totale della RAM per evitare sovrapposizioni con partite precedenti
        mappaParoleSegrete.clear();
        progressiGiocatori.clear();
        
        // Assegna un cesto vuoto (HashSet) a ciascun giocatore per tracciare cosa ha indovinato
        for (ClientHandler gh : giocatoriPronti) {
            progressiGiocatori.put(gh, new HashSet<>());
        }
        
        
        // GESTIONE FALLBACK DI SICUREZZA
        // Se l'admin avvia il server senza caricare file, evitiamo il crash del sistema
        // fornendo una partita di emergenza predefinita.
        if (dizionarioAttivo == null || dizionarioAttivo.isEmpty() || testoIntegraleAttivo == null) {
            this.testoIntegraleAttivo = "Non Fare l'avvocato delle cause perse";
            
            int shiftMin = 1, shiftMax = 3;
            switch (difficoltaCorrente.toUpperCase()) {
                case "MEDIA": shiftMin = 4; shiftMax = 7; break;
                case "DIFFICILE": shiftMin = 8; shiftMax = 12; break;
                case "FACILE": default: shiftMin = 1; shiftMax = 3; break;
            }
            
            Random random = new Random();
            int shiftEffettivo = random.nextInt((shiftMax - shiftMin) + 1) + shiftMin;
            
            String parolaCifrata = CifrarioUtils.cifratura("avvocato", shiftEffettivo);
            mappaParoleSegrete.put("avvocato", parolaCifrata);

            this.partitaInCorso = true;
            this.timestampInizioSfida = System.currentTimeMillis();

            if (timerPartita != null) timerPartita.cancel();
            timerPartita = new Timer();
            timerPartita.schedule(new TimerTask() {
                @Override
                public void run() { terminaPartitaPareggio(); }
            }, DURATA_TIMER * 1000L);

            System.out.println("[SERVER] Nessun file caricato. Avvio partita di fallback (" + difficoltaCorrente + "). Parola fissa: avvocato");
            inviaStatoGiocoAiClient();
            
            return; // Terminiamo qui l'esecuzione per questa partita di emergenza
        }

        // 2. Configurazione Dinamica dei Parametri (Algoritmo principale)
        int numeroParoleDaEstrarre = 1;
        int lunghezzaMinima = 4, lunghezzaMassima = 6;
        int shiftMin = 1, shiftMax = 3;
        
        // Ordina il dizionario per frequenza decrescente (le più usate in cima)
        List<Map.Entry<String, Long>> elencoOrdinato = new ArrayList<>(dizionarioAttivo.entrySet());
        elencoOrdinato.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); 

        int dimensioneDizionario = elencoOrdinato.size();
        int indiceInizioSoglia = 0;
        int indiceFineSoglia = dimensioneDizionario;

        // Modulazione delle regole in base alla scelta dell'Admin
        switch (difficoltaCorrente.toUpperCase()) {
            case "MEDIA":
                numeroParoleDaEstrarre = 2;
                lunghezzaMinima = 7; lunghezzaMassima = 9;
                shiftMin = 4; shiftMax = 7;
                // Parole mediamente utilizzate (taglio del 30% dai due estremi)
                indiceInizioSoglia = (int) (dimensioneDizionario * 0.3);
                indiceFineSoglia = (int) (dimensioneDizionario * 0.7);
                break;
                
            case "DIFFICILE":
                numeroParoleDaEstrarre = 3;
                lunghezzaMinima = 10; lunghezzaMassima = 30; 
                shiftMin = 8; shiftMax = 12;
                // Parole rare (prendiamo solo l'ultimo 30% della classifica)
                indiceInizioSoglia = (int) (dimensioneDizionario * 0.7);
                break;
                
            case "FACILE":
            default:
                numeroParoleDaEstrarre = 1;
                lunghezzaMinima = 4; lunghezzaMassima = 6;
                shiftMin = 1; shiftMax = 3;
                // Parole comunissime (il primo 30% della classifica)
                indiceFineSoglia = (int) (dimensioneDizionario * 0.3);
                break;
        }

        // 3. Estrazione e Filtraggio
        List<String> poolIdonee = new ArrayList<>();
        if (indiceInizioSoglia >= indiceFineSoglia || indiceInizioSoglia >= dimensioneDizionario) {
            indiceInizioSoglia = 0; indiceFineSoglia = dimensioneDizionario;
        }
        
        for (int i = indiceInizioSoglia; i < indiceFineSoglia; i++) {
            String parola = elencoOrdinato.get(i).getKey();
            if (parola.length() >= lunghezzaMinima && parola.length() <= lunghezzaMassima) {
                poolIdonee.add(parola);
            }
        }

        // Meccanismo di sicurezza: se il testo caricato è troppo breve e non ci sono parole idonee ai filtri, 
        // rimuoviamo i filtri per garantire l'avvio della partita.
        if (poolIdonee.size() < numeroParoleDaEstrarre) {
            poolIdonee.clear();
            for (Map.Entry<String, Long> entry : elencoOrdinato) {
                poolIdonee.add(entry.getKey());
            }
        }

        // 4. Crittografia e Salvataggio in Mappa
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

        // 5. Innesco del Time-out (Asincrono)
        if (timerPartita != null) timerPartita.cancel();
        timerPartita = new Timer();
        timerPartita.schedule(new TimerTask() {
            @Override
            public void run() { terminaPartitaPareggio(); }
        }, DURATA_TIMER * 1000L);

        System.out.println("[SERVER] Sfida avviata (" + difficoltaCorrente + "). Parole segrete: " + mappaParoleSegrete.keySet());

        inviaStatoGiocoAiClient();
    }

    /**
     * Funzione ponte: cicla tutti i giocatori attualmente in lobby e inoltra il calcolo visivo.
     */
    private void inviaStatoGiocoAiClient() {
        for (ClientHandler giocatore : giocatoriPronti) {
            inviaStatoGiocoPersonale(giocatore);
        }
    }

    /**
     * Genera la vista "soggettiva" del file di testo per un singolo giocatore.
     * Sostituisce le parole segrete con le parentesi cifrate [ xxxx ] 
     * o le rivela con gli asterischi *XXXX* se il giocatore le ha indovinate.
     * * @param giocatore Il client a cui calcolare e spedire lo stato.
     */
    private void inviaStatoGiocoPersonale(ClientHandler giocatore) {
        String testoDaInviare = this.testoIntegraleAttivo;
        Set<String> sueParole = progressiGiocatori.get(giocatore);

        for (Map.Entry<String, String> entry : mappaParoleSegrete.entrySet()) {
            String chiara = entry.getKey();
            String cifrata = entry.getValue();

            if (sueParole != null && sueParole.contains(chiara)) {
                // Regex (?i)\\b garantisce che venga sostituita la parola esatta (case-insensitive) e non sue sottostringhe
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
     * Intercetta la parola digitata da un client e ne verifica la correttezza.
     * Metodo sincronizzato per evitare che due client vincano nello stesso esatto millisecondo.
     * * @param giocatore Il client che ha tentato la risposta.
     * @param tentativo La parola scritta.
     */
    public synchronized void verificaTentativo(ClientHandler giocatore, String tentativo) {
        if (!partitaInCorso) return;

        String pulito = tentativo.trim().toLowerCase();
        Set<String> sueParole = progressiGiocatori.get(giocatore);

        // Controllo: la parola esiste nella mappa segreta? Ed è la prima volta che la indovina?
        if (sueParole != null && mappaParoleSegrete.containsKey(pulito) && !sueParole.contains(pulito)) {
            sueParole.add(pulito); // Aggiunge la parola al "cesto" del giocatore
            System.out.println("[GIOCO] " + giocatore.getUsernameUtente() + " ha scovato: " + pulito + " (" + sueParole.size() + "/" + mappaParoleSegrete.size() + ")");

            // Verifica Condizione di Vittoria: ha trovato TUTTE le parole?
            if (sueParole.size() == mappaParoleSegrete.size()) {
                terminaPartitaVittoria(giocatore);
            } else {
                // Se mancano ancora parole, aggiorna lo schermo SOLO a chi ha indovinato
                giocatore.inviaMessaggio("NOTIFICA:Ottimo! Hai decifrato la parola '" + pulito.toUpperCase() + "'!");
                inviaStatoGiocoPersonale(giocatore);
            }
        } else {
            giocatore.inviaMessaggio("RISPOSTA_ERRATA:La parola non è corretta o l'hai già trovata.");
        }
    }

    /**
     * Gestisce la chiusura della partita quando qualcuno vince.
     * Ferma i timer, avvisa il vincitore e lo sconfitto, e innesca il salvataggio sul database.
     * * @param vincitore Il client che ha completato per primo il set di parole.
     */
    private synchronized void terminaPartitaVittoria(ClientHandler vincitore) {
        if (!partitaInCorso) return;
        partitaInCorso = false;
        timerPartita.cancel(); // Blocca lo scadere del tempo

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

    /**
     * Gestisce la chiusura della partita in caso di scadenza del Timer.
     */
    private synchronized void terminaPartitaPareggio() {
        if (!partitaInCorso) return;
        partitaInCorso = false;

        String elencoCompletoParole = String.join(", ", mappaParoleSegrete.keySet()).toUpperCase();
        System.out.println("[FINE PARTITA] Tempo scaduto.");

        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaMessaggio("FINE_PARTITA:PAREGGIO:Tempo scaduto! Non avete trovato tutte le parole. Soluzione: " + elencoCompletoParole);
        }
        
        salvaDatiNelDatabase(null); // null indica che non c'è vincitore
        resetPartita();
    }

    /**
     * Calcola i punteggi in base al tempo e salva i record definitivi tramite il DAO.
     * * @param vincitore Il client vincitore (o null in caso di pareggio).
     */
    private void salvaDatiNelDatabase(ClientHandler vincitore) {
        try {
            PartitaDAO partitaDAO = new PartitaDAO();
            RisultatoDAO risultatoDAO = new RisultatoDAO();

            String dataOra = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String paroleUnite = String.join(",", mappaParoleSegrete.keySet());
            
            // Creazione riga madre (Partita) per ottenere la Foreign Key (idPartita)
            Partita nuovaPartita = new Partita(dataOra, paroleUnite, this.difficoltaCorrente);
            long idPartita = partitaDAO.inserisciERestituisciId(nuovaPartita);
            if (idPartita == -1) return;
            
            // Impostazione del moltiplicatore per bilanciare i punti in base alla difficoltà
            int moltiplicatore = 1;
            if(this.difficoltaCorrente.equalsIgnoreCase("Media")) 
                moltiplicatore = 2;
            else if (this.difficoltaCorrente.equalsIgnoreCase("Difficile"))
                moltiplicatore = 3;

            // Creazione riga figlia (Risultato) per ciascun giocatore connesso
            for (ClientHandler giocatore : giocatoriPronti) {
                String esito = "PAREGGIO";
                int tempoGiocatore = DURATA_TIMER * 1000; 
                int puntiAssegnati = 0; // Se perdi o pareggi prendi 0

                if (vincitore != null) {
                    if (giocatore == vincitore) {
                        esito = "VITTORIA";
                        // Calcolo tempo reale in millisecondi
                        tempoGiocatore = (int)(System.currentTimeMillis() - timestampInizioSfida); 
                        
                        // Formula Punteggio: (Secondi Rimanenti) * Moltiplicatore Difficoltà
                        // Esempio: Indovina in 20s in mod. Media. (60 - 20) * 2 = 80 Punti!
                        int tempoTrascorsoSec = tempoGiocatore / 1000;
                        puntiAssegnati = (DURATA_TIMER - tempoTrascorsoSec) * moltiplicatore;
                    } else {
                        esito = "SCONFITTA";
                    }
                }

                Risultato r = new Risultato(idPartita, giocatore.getIdUtente(), esito, tempoGiocatore, puntiAssegnati);
                risultatoDAO.insert(r);
            }
            System.out.println("[SERVER DB] Record salvati correttamente con difficoltà: " + difficoltaCorrente);
        } catch (Exception e) {
            System.err.println("[SERVER DB] Errore salvataggio: " + e.getMessage());
        }
    }
    
    /**
     * Sistema Anti-Duplicazione: Verifica se l'utente che sta tentando il login 
     * ha già una sessione attiva da un'altra finestra o PC.
     * * @param username Il nome utente da controllare.
     * @return true se l'utente è già online, false altrimenti.
     */
    public boolean isGiocatoreGiaConnesso(String username) {
        for (ClientHandler client : clientConnessi) { 
            if (client.getUsernameUtente().equalsIgnoreCase(username)) {
                return true; 
            }
        }
        return false;
    }

    /**
     * Libera la Lobby e la RAM preparando il server per un nuovo giro di Matchmaking.
     */
    private void resetPartita() {
        giocatoriPronti.clear();
        mappaParoleSegrete.clear();
        progressiGiocatori.clear();
        System.out.println("[SERVER] Lobby ripulita. Pronto per un nuovo set.");
    }
    
    /**
     * Rimuove in sicurezza un client disconnesso dalle liste operative.
     * * @param handler Il client da eliminare.
     */
    public synchronized void disconnettiClient(ClientHandler handler) {
        clientConnessi.remove(handler);
        giocatoriPronti.remove(handler);
    }
    
    /**
     * Interrompe il ciclo di ascolto principale per consentire lo spegnimento.
     */
    public void fermaServer() {
        this.inEsecuzione = false;
    }
}