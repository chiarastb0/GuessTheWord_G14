/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.network;

import guesstheword_server.ConfigManager;
import guesstheword_server.model.PacchettoSfida;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.*;

public class ServerManager {
    private boolean inEsecuzione = true;
    // Lista di tutti i gestori dei client attualmente connessi alla rete
    private final List<ClientHandler> clientConnessi = new ArrayList<>();
    // Lista specifica dei soli sfidanti che hanno superato il login con successo (Fase 3)
    private final List<ClientHandler> giocatoriPronti = new ArrayList<>();
    
    public void start() {
        int port = ConfigManager.getServerPort(); 
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server avviato correttamente sulla porta: " + port);
           
            while (inEsecuzione) {
                // Il server si blocca qui in attesa di una chiamata dal Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso da: " + clientSocket.getRemoteSocketAddress());
                
                // Istanziamo il ClientHandler passando il socket e questo ServerManager
                ClientHandler handler = new ClientHandler(clientSocket, this);
                
                synchronized (clientConnessi) {
                    clientConnessi.add(handler);
                }
                
                // Crea e fa partire il Thread indipendente.
                Thread t = new Thread(handler);
                t.start();
                
                // CORRETTO: Rimosse le righe di forcing del test che lanciavano la sfida senza login!
            }
            
        } catch (IOException e) {
            System.err.println("Impossibile avviare il server sulla porta " + port);
        }
    }
    
    public synchronized void giocatoreAutenticato(ClientHandler handler) {
        // Accettiamo giocatori solo se non abbiamo già una sfida al completo
        if (giocatoriPronti.size() < 2) {
            giocatoriPronti.add(handler);
            System.out.println("[MATCHMAKING] Giocatore '" + handler.getUsernameUtente() + "' pronto! (" + giocatoriPronti.size() + "/2)");
            
            // Logica Matchmaking (Fase 3): Quando DUE giocatori hanno fatto il login con successo 
            if (giocatoriPronti.size() == 2) {
                avviaSfida();
            }
        } else {
            // Se un terzo giocatore prova ad accodarsi a partita in corso, inviamo una notifica di rifiuto
            handler.inviaMessaggio("ERRORE: Partita già in corso. Riprova più tardi.");
        }
    }
    
    /**
     * Attiva la sfida notificando i client e chiamando la logica di gioco (Fase 3)
     */
    private void avviaSfida() {
        System.out.println("[SERVER] Entrambi i giocatori sono pronti! Avvio della sfida in tempo reale..."); 
        
        // 1. Definiamo i dati della sfida (Per ora statici, poi integrati con il Compagno 2)
        int durataTimer = 60; 
        String testoCifratoFittizio = "Il _ _ _ _ _ è sul tavolo."; 

        // 2. Prepariamo l'oggetto serializzabile
        PacchettoSfida pacchetto = new PacchettoSfida(testoCifratoFittizio, durataTimer);

        // 3. Comunica a entrambi i client l'inizio del gioco inviando l'oggetto
        for (ClientHandler giocatore : giocatoriPronti) {
            giocatore.inviaOggetto(pacchetto); 
        }
        
        System.out.println("[SERVER] Pacchetto dati di gioco serializzato e inviato con successo.");
        
        // CORRETTO: Resettiamo la lista dei giocatori pronti così il server è pronto 
        // ad accogliere la prossima coppia di sfidanti senza conflitti!
        giocatoriPronti.clear();
    }
    
    /**
     * Rimuove in sicurezza un client se si disconnette o chiude l'app prima dell'inizio
     */
    public synchronized void disconnettiClient(ClientHandler handler) {
        clientConnessi.remove(handler);
        giocatoriPronti.remove(handler);
        System.out.println("[SERVER] Un client si è disconnesso. Liste aggiornate.");
    }
    
    public void ferma() {
        this.inEsecuzione = false;
    }
}