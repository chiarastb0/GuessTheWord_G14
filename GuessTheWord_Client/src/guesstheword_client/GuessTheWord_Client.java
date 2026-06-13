package guesstheword_client;

import guesstheword_client.controller.*;
import guesstheword_client.network.ClientConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuessTheWord_Client extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Carica il file FXML della schermata di gioco
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/AuthView.fxml"));
        Parent root = loader.load();
        
        // Recupera il controller 
        AuthController controllerAuth = loader.getController();
        
        // Fa partire la connessione di rete verso il server (es. localhost, porta 1234)
        ClientConnection connessione = new ClientConnection("127.0.0.1", 5000);
        
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

    public static void main(String[] args) {
        launch(args);
    }
}