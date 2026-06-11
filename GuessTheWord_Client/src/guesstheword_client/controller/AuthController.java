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
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

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
    @FXML private ComboBox<String> comboRuolo;
    @FXML private Label lblErroreReg;

    private ClientConnection clientConnection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Popoliamo la combo box con i ruoli richiesti dal bando d'esame
        comboRuolo.getItems().addAll("PLAYER", "ADMIN");
        comboRuolo.setValue("PLAYER"); // Valore di default
    }    
    
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
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
        String ruolo = comboRuolo.getValue();

        if (user.isEmpty() || pass.isEmpty() || ruolo == null) {
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