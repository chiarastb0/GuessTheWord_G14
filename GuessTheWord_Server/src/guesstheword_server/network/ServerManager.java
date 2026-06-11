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
        
        // 3. Cifratura della sola parola scelta mediante Chiara
        String parolaCifrata = guesstheword_server.utils.CifrarioUtils.cifratura(this.parolaSegretaCorrente, spostamentoCasuale);
        
        // 4. Sostituzione dinamica nel testo originale rispettando i confini della parola (\b)
        // (?i) rende il matching case-insensitive
        String testoModificatoConContesto = this.testoIntegraleAttivo.replaceAll("(?i)\\b" + this.parolaSegretaCorrente + "\\b", "[ " + parolaCifrata + " ]");
        
        int durataTimer = 60; 

        System.out.println("[SERVER] Parola da indovinare: " + this.parolaSegretaCorrente + " | Inserita nel contesto cifrato.");

        // 5. Creazione del pacchetto serializzato contenente il testo d'estratto completo modificato
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
}
