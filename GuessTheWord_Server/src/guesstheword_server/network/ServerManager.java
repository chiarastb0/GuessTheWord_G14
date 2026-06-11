/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.ConfigManager;
import guesstheword_server.model.PacchettoSfida;
import guesstheword_server.utils.CifrarioUtils;
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
    private String parolaSegretaCorrente;
    
    //VARIABILI PER GESTIRE STATO PARTITA
    private boolean partitaInCorso = false;
    private Timer timerPartita;

    // Riceve sia la mappa che il testo integrale dall'interfaccia Admin
    public void setDatiSfida(Map<String, Long> dizionario, String testoIntegrale) {
        this.dizionarioAttivo = dizionario;
        this.testoIntegraleAttivo = testoIntegrale;
        System.out.println("[SERVER] ServerManager configurato con " + dizionario.size() + " parole e testo integrale pronti.");
    }

    public String getParolaSegretaCorrente() {
        return this.parolaSegretaCorrente;
    }
    
    public void start() {
        int port = ConfigManager.getServerPort(); 
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato correttamente sulla porta: " + port);
           
            while (inEsecuzione) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso da: " + clientSocket.getRemoteSocketAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, this);
                
                synchronized (clientConnessi) {
                    clientConnessi.add(handler);
                }
                
                Thread t = new Thread(handler);
                t.start();
            }
            
        } catch (IOException e) {
            System.err.println("Impossibile avviare il server sulla porta " + port);
        }
    }
    
    public synchronized void giocatoreAutenticato(ClientHandler handler) {
        if (giocatoriPronti.size() < 2) {
            giocatoriPronti.add(handler);
            System.out.println("[MATCHMAKING] Giocatore '" + handler.getUsernameUtente() + "' pronto! (" + giocatoriPronti.size() + "/2)");
            
            if (giocatoriPronti.size() == 2) {
                avviaSfida();
            }
        } else {
            handler.inviaMessaggio("ERRORE: Partita già in corso. Riprova più tardi.");
        }
    }
    
    private void avviaSfida() {
        System.out.println("[SERVER] Entrambi i giocatori sono pronti! Preparazione della sfida..."); 
        
        // 1. Selezione della parola e fallback di sicurezza
        if (dizionarioAttivo == null || dizionarioAttivo.isEmpty() || testoIntegraleAttivo == null) {
            System.err.println("[SERVER WARNING] Dati incompleti! Uso dati di default.");
            this.parolaSegretaCorrente = "AVVOCATO";
            this.testoIntegraleAttivo = "Non fare l' AVVOCATO delle cause perse";
        } else {
            List<String> listaParole = new ArrayList<>(dizionarioAttivo.keySet());
            Random random = new Random();
            this.parolaSegretaCorrente = listaParole.get(random.nextInt(listaParole.size())).toUpperCase();
        }

        // 2. Generazione dello spostamento per il Cifrario (es. tra 1 e 10)
        int spostamentoCasuale = new Random().nextInt(10) + 1;
        
        // 3. Cifratura della sola parola scelta mediante 
        String parolaCifrata = guesstheword_server.utils.CifrarioUtils.cifratura(this.parolaSegretaCorrente, spostamentoCasuale);
        
        // 4. Sostituzione dinamica nel testo originale rispettando i confini della parola (\b)
        // (?i) rende il matching case-insensitive
        String testoModificatoConContesto = this.testoIntegraleAttivo.replaceAll("(?i)\\b" + this.parolaSegretaCorrente + "\\b", "[ " + parolaCifrata + " ]");
        
        int durataTimer = 60; 
        this.partitaInCorso = true; // Dichiariamo la partita ufficialmente aperta!

        // Creiamo il timer del Server: se scade senza vincitori, chiama il Pareggio
        if (timerPartita != null) timerPartita.cancel();
        timerPartita = new Timer();
        timerPartita.schedule(new TimerTask() {
            @Override
            public void run() {
                terminaPartitaPareggio();
            }
        }, durataTimer * 1000L); // Convertito in millisecondi

        System.out.println("[SERVER] Parola da indovinare: " + this.parolaSegretaCorrente + " | Timer avviato: " + durataTimer + "s");

        PacchettoSfida pacchetto = new PacchettoSfida(testoModificatoConContesto, durataTimer);
        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaOggetto(pacchetto); 
        }
        
        System.out.println("[SERVER] PacchettoSfida contestuale inviato in tempo reale.");
    }
    
    public synchronized void disconnettiClient(ClientHandler handler) {
        clientConnessi.remove(handler);
        giocatoriPronti.remove(handler);
        System.out.println("[SERVER] Un client si è disconnesso. Liste aggiornate.");
    }
    
    public void ferma() {
        this.inEsecuzione = false;
    }
    
    /**
     * Riceve il tentativo dal ClientHandler e verifica se è corretto
     */
    public synchronized void verificaTentativo(ClientHandler giocatore, String tentativo) {
        if (!partitaInCorso) return; // Se il tempo è scaduto o qualcuno ha già vinto, ignora

        // Confronto ignorando maiuscole/minuscole e spazi accidentali
        if (tentativo.trim().equalsIgnoreCase(this.parolaSegretaCorrente)) {
            terminaPartitaVittoria(giocatore);
        } else {
            // (Opzionale ma utile) Diciamo al client che ha sbagliato così può riprovare
            giocatore.inviaMessaggio("RISPOSTA_ERRATA:Ritenta!");
        }
    }

    /**
     * Ferma il gioco e decreta il vincitore assoluto
     */
    private synchronized void terminaPartitaVittoria(ClientHandler vincitore) {
        if (!partitaInCorso) return;
        partitaInCorso = false;
        timerPartita.cancel(); // Spegniamo il timer!

        System.out.println("[FINE PARTITA] Ha vinto " + vincitore.getUsernameUtente());

        // Inviamo a tutti i client il responso finale
        for (ClientHandler giocatore : giocatoriPronti) {
            if (giocatore == vincitore) {
                giocatore.inviaMessaggio("FINE_PARTITA:VITTORIA:Hai indovinato la parola segreta: " + parolaSegretaCorrente);
            } else {
                giocatore.inviaMessaggio("FINE_PARTITA:SCONFITTA:Il giocatore " + vincitore.getUsernameUtente() + " ha indovinato: " + parolaSegretaCorrente);
            }
        }
        resetPartita();
    }

    /**
     * Se il timer scade, nessuno ha vinto
     */
    private synchronized void terminaPartitaPareggio() {
        if (!partitaInCorso) return;
        partitaInCorso = false;

        System.out.println("[FINE PARTITA] Tempo scaduto. Pareggio!");

        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaMessaggio("FINE_PARTITA:PAREGGIO:Tempo scaduto! La parola era: " + parolaSegretaCorrente);
        }
        resetPartita();
    }

    /**
     * Svuota la lista dei giocatori attuali per permettere l'inizio di una nuova sfida
     */
    private void resetPartita() {
        // Qui in futuro metteremo anche la logica di salvataggio sul Database (PartitaDAO)
        giocatoriPronti.clear();
        System.out.println("[SERVER] Campo da gioco resettato. In attesa di nuove sfide...");
    }
}
