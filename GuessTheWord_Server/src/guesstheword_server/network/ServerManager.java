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
    
    // VARIABILI PER LA GESTIONE DELLA SFIDA DINAMICA
    private Map<String, Long> dizionarioAttivo;
    private String testoIntegraleAttivo; // Contiene l'intero file .txt caricato
    private String parolaSegretaCorrente; 

    /**
     * Riceve sia la mappa delle frequenze che il testo integrale dall'interfaccia Admin
     */
    public void setDatiSfida(Map<String, Long> dizionario, String testoIntegrale) {
        this.dizionarioAttivo = dizionario;
        this.testoIntegraleAttivo = testoIntegrale;
        System.out.println("[SERVER] ServerManager configurato con " + dizionario.size() + " parole e testo integrale pronti.");
    }

    public String getParolaSegretaCorrente() {
        return parolaSegretaCorrente;
    }

    public void start() {
        int port = ConfigManager.getServerPort(); 
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato correttamente sulla porta: " + port);
           
            while (inEsecuzione) {
                // Il server rimane in ascolto delle connessioni dei client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso da: " + clientSocket.getRemoteSocketAddress());
                
                // Istanziamo il ClientHandler passando il socket e questo ServerManager
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientConnessi.add(handler);
                
                // Avviamo il thread dedicato al client
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel ServerSocket: " + e.getMessage());
        }
    }

    /**
     * Registra un giocatore che ha effettuato il login con successo e verifica il matchmaking
     */
    public synchronized void aggiungiGiocatorePronto(ClientHandler handler) {
        if (!giocatoriPronti.contains(handler)) {
            giocatoriPronti.add(handler);
            System.out.println("[SERVER] Giocatore pronto: " + handler.getUsernameUtente() + " (" + giocatoriPronti.size() + "/2)");
            
            // Requisito Matchmaking: Appena ci sono esattamente 2 giocatori, la sfida parte in automatico
            if (giocatoriPronti.size() == 2) {
                avviaSfidaDinamica();
            }
        }
    }

    /**
     * Avvia la sfida estraendo una parola, cifrandola e distribuendo il PacchettoSfida
     */
    private void avviaSfidaDinamica() {
        if (dizionarioAttivo == null || dizionarioAttivo.isEmpty() || testoIntegraleAttivo == null) {
            System.err.println("[SERVER ERRORE] Impossibile avviare la sfida: Dati del dizionario o testo mancanti!");
            return;
        }

        System.out.println("[SERVER] Matchmaking completato! Estrazione della parola in corso...");

        // 1. Estrazione di una parola casuale dal dizionario attivo
        List<String> listaParole = new ArrayList<>(dizionarioAttivo.keySet());
        this.parolaSegretaCorrente = listaParole.get(new Random().nextInt(listaParole.size()));

        // 2. Generazione dello shift casuale per il Cifrario di Cesare (tra 1 e 10)
        int spostamentoCasuale = new Random().nextInt(10) + 1;
        
        // 3. Cifratura della sola parola scelta mediante Chiara (CifrarioUtils)
        String parolaCifrata = CifrarioUtils.cifratura(this.parolaSegretaCorrente, spostamentoCasuale);
        
        // 4. Sostituzione dinamica nel testo originale rispettando i confini della parola (\b)
        // (?i) rende il matching case-insensitive
        String testoModificatoConContesto = this.testoIntegraleAttivo.replaceAll("(?i)\\b" + this.parolaSegretaCorrente + "\\b", "[ " + parolaCifrata + " ]");
        
        int durataTimer = 60; 

        System.out.println("[SERVER] Parola da indovinare: " + this.parolaSegretaCorrente + " | Inserita nel contesto cifrato.");

        // 5. Creazione del pacchetto serializzato contenente il testo modificato e il tempo della timeline
        PacchettoSfida pacchetto = new PacchettoSfida(testoModificatoConContesto, durataTimer);

        // 6. Comunica a entrambi i client l'inizio del gioco inviando l'oggetto serializzato
        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaOggetto(pacchetto); 
        }
        
        System.out.println("[SERVER] PacchettoSfida contestuale inviato in tempo reale ai client.");
    }
    
    /**
     * Rimuove in sicurezza un client se si disconnette prima o durante la partita
     */
    public synchronized void disconnettiClient(ClientHandler handler) {
        clientConnessi.remove(handler);
        giocatoriPronti.remove(handler);
        System.out.println("[SERVER] Client rimosso. Giocatori pronti rimanenti: " + giocatoriPronti.size());
    }
    
    public void fermaServer() {
        this.inEsecuzione = false;
    }
}