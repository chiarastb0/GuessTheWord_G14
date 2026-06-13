package guesstheword_client;

import guesstheword_client.controller.*;
import guesstheword_client.network.ClientConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @class GuessTheWord_Client
 * @brief Classe principale dell'applicazione GuessTheWord Client.
 * * Questa classe estende l'applicazione JavaFX e funge da punto di ingresso (Entry Point) 
 * del programma, si occupa di inizializzare la connessione di rete con il server, 
 * caricare l'interfaccia grafica (UI) iniziale e avviare il thread di ascolto.
 */
public class GuessTheWord_Client extends Application {
    
    /**
     * @brief Inizializza e avvia la schermata principale dell'applicazione.
     * * Il metodo carica il file FXML per la vista di autenticazione, recupera i dati di 
     * configurazione di rete tramite ConfigManager, tenta di stabilire una connessione 
     * con il server e, in caso di successo, avvia un thread dedicato alla gestione 
     * dei messaggi di rete in background. Infine, mostra la finestra (Stage) all'utente.
     * * @param stage Rappresenta lo stage principale dell'applicazione JavaFX fornito dalla JVM.
     * * @pre Il file "AuthView.fxml" deve essere presente nel percorso specificato e il 
     * ConfigManager deve essere in grado di fornire IP e porta.
     * * @post L'interfaccia grafica viene mostrata a schermo, se la connessione ha successo, 
     * un thread di ascolto di rete parallelo viene avviato e collegato al controller.
     * * @throws Exception Se si verifica un errore critico durante il caricamento del file FXML 
     * (IOException) o durante l'inizializzazione dei componenti grafici.
     */

    @Override
    public void start(Stage stage) throws Exception {
        // Carica il file FXML della schermata di gioco
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/AuthView.fxml"));
        Parent root = loader.load();
        
        // Recupera il controller 
        AuthController controllerAuth = loader.getController();
        
        String ipServer = ConfigManager.getServerIp();
        int portaServer = ConfigManager.getServerPort();

        ClientConnection connessione = new ClientConnection(ipServer, portaServer);
        
        if (connessione.connetti()) {
            // Collega la rete al controller
            controllerAuth.setClientConnection(connessione);
            
            // Fa partire il thread di ascolto della rete
            Thread t = new Thread(connessione);
            t.start();
        }

        // Mostra la finestra del gioco
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Guess The Word - Autenticazione");
        stage.show();
    }

    /**
     * @brief Il punto di ingresso principale del programma (main classico).
     * * Questo metodo viene invocato al lancio del file JAR/eseguibile, il suo unico scopo 
     * è quello di cedere il controllo al framework JavaFX chiamando il metodo statico launch().
     * * @param args Argomenti passati da riga di comando (opzionali).
     * * @post Viene lanciato il ciclo di vita dell'applicazione JavaFX, portando all'esecuzione 
     * del metodo start().
     */
    public static void main(String[] args) {
        launch(args);
    }
}