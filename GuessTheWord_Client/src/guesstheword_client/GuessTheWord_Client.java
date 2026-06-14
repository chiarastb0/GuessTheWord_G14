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
     * @brief Inizializza e mostra la finestra principale di autenticazione del client.
     * * Il metodo si occupa esclusivamente della gestione dell'interfaccia grafica: carica il 
     * file FXML, imposta la scena sullo stage principale e mostra la finestra all'utente. 
     * Successivamente, recupera il controller associato alla vista e gli delega l'avvio 
     * della connessione di rete in background, garantendo che la UI rimanga immediatamente reattiva.
     * * @param stage Lo stage principale dell'applicazione JavaFX.
     * * @pre Il file "AuthView.fxml" deve essere presente nel percorso specificato.
     * @post La finestra grafica viene mostrata a schermo all'istante e viene avviata la 
     * procedura asincrona di connessione al server.
     */

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/AuthView.fxml"));
        Parent root = loader.load();
    
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Guess The Word - Autenticazione");
        stage.show(); 
    
        AuthController controllerAuth = loader.getController();
        controllerAuth.inizializzaConnessione(); 
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