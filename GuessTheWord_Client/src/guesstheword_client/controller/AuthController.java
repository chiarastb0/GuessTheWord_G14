/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.controller;

import guesstheword_client.network.ClientConnection;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuthController implements Initializable {

    // PANNELLO LOGIN
    @FXML private VBox paneLogin;
    @FXML private TextField txtLoginUser;
    @FXML private PasswordField txtLoginPass;
    @FXML private Label lblErroreLogin;

    // PANNELLO REGISTRAZIONE
    @FXML private VBox paneRegistrazione;
    @FXML private TextField txtRegUser;
    @FXML private PasswordField txtRegPass;
    @FXML private Label comboRuolo;
    @FXML private Label lblErroreReg;

    private ClientConnection clientConnection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
           // Lascia vuoto: il testo "PLAYER" è già impostato nel file FXML
    }
    
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        if (connessione != null) {
            connessione.setControllerAuth(this);
        }
    }

    @FXML
    void mostraPannelloRegistrazione(ActionEvent event) {
        paneLogin.setVisible(false);
        paneRegistrazione.setVisible(true);
        lblErroreLogin.setText("");
    }

    @FXML
    public void mostraPannelloLogin(ActionEvent event) {
        paneRegistrazione.setVisible(false);
        paneLogin.setVisible(true);
        lblErroreReg.setText("");
    }

    @FXML
    void gestisciLogin(ActionEvent event) {
        String user = txtLoginUser.getText().trim();
        String pass = txtLoginPass.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblErroreLogin.setText("⚠️ Compila tutti i campi!");
            return;
        }

        if (clientConnection != null) {
            lblErroreLogin.setText("Tentativo di accesso in corso...");
            // Spediamo il comando al ClientHandler del Server seguendo il protocollo
            clientConnection.spedisciMessaggio("LOGIN:" + user + ":" + pass);
        } else {
            lblErroreLogin.setText("❌ Errore: Nessuna connessione al server.");
        }
    }

    @FXML
    void gestisciRegistrazione(ActionEvent event) {
        String user = txtRegUser.getText().trim();
        String pass = txtRegPass.getText().trim();
        String ruolo = comboRuolo.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || ruolo.isEmpty()) {// 
            lblErroreReg.setText("⚠️ Compila tutti i campi!");
            return;
        }

        // Evitiamo caratteri separatori pericolosi per il protocollo di rete
        if (user.contains(":") || pass.contains(":")) {
            lblErroreReg.setText("❌ L'uso dei due punti ':' non è consentito.");
            return;
        }

        if (clientConnection != null) {
            lblErroreReg.setText("Registrazione in corso...");
            // Spediamo il nuovo comando di registrazione al server
            clientConnection.spedisciMessaggio("REGISTRAZIONE:" + user + ":" + pass + ":" + ruolo);
        } else {
            lblErroreReg.setText("❌ Errore: Nessuna connessione al server.");
        }
    }
    
    public void gestisciLoginSuccess(ClientConnection connessione, String ruolo) {
    try {

            // Se l'utente è un ADMIN, blocchiamo l'accesso!
            if (ruolo.equals("ADMIN")) {
                mostraMessaggioErroreLogin("❌ Accesso negato: l'acesso è riservato ai Player.");
                return; // Interrompe il metodo ed evita il cambio di scena
            }
        
        // 1. Carica il file FXML della schermata di gioco
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/LobbyView.fxml"));
            Parent giocoRoot = loader.load();
        
        // 2. Recupera il controller della lobby
            LobbyController controllerLobby = loader.getController();
            
            controllerLobby.setClientConnection(connessione);
        // 2. Recupera il controller della schermata di gioco
             
        // Aggiorna il riferimento del controller dentro ClientConnection 
        // in modo che i messaggi successivi (es. START_GAME, CLASSIFICA) vadano alla nuova schermata
            connessione.setControllerLobby(controllerLobby);
        
        // 4. Recupera lo Stage corrente
        // Puoi farlo usando un qualsiasi nodo grafico presente nella tua AuthView (es. txtUsername o un bottone)
            Stage stage = (Stage) paneLogin.getScene().getWindow(); 
        
        // 5. Crea la nuova scena e mostra lo stage aggiornato
            Scene scenaGioco = new Scene(giocoRoot);
            stage.setScene(scenaGioco);
            stage.setTitle("Guess The Word - Lobby di Attesa");
            stage.centerOnScreen(); // Opzionale: centra la nuova finestra sullo schermo
            stage.show();
        
        } catch (Exception e) {
            System.err.println("Errore durante il cambio di scena verso la lobby: " + e.getMessage());
            e.printStackTrace();
            mostraMessaggioErroreLogin("❌ Errore nel caricamento della lobby di attesa.");
        }
    }
    
    /**
     * Metodo di utility richiamabile dalla rete se la registrazione fallisce o ha successo
     */
    public void mostraMessaggioErroreReg(String msg) {
        lblErroreReg.setText(msg);
    }
    
    public void mostraMessaggioErroreLogin(String msg) {
        lblErroreLogin.setText(msg);
    }
}