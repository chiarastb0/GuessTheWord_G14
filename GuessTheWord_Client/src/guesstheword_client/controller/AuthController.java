/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.controller;

import guesstheword_client.ConfigManager;
import guesstheword_client.network.ClientConnection;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
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

/**
 * @class AuthController
 * @brief Gestisce le interazioni dell'interfaccia utente per l'accesso e la registrazione.
 * Implementa l'interfaccia Initializable di JavaFX, si occupa di scambiare i pannelli 
 * di login e registrazione all'interno dello stesso stage, inviare le credenziali al server 
 * tramite il protocollo predefinito e, in caso di successo, effettuare lo switch di scena verso la lobby.
 */

public class AuthController {

    //LOGIN
    /** 
     * @brief Contenitore principale del form di Login (collegato via FXML). 
     */
    @FXML private VBox paneLogin;
    
    /** 
     * @brief Campo di testo per l'inserimento dello username di login. 
    */
    @FXML private TextField txtLoginUser;
    
    /** 
     * @brief Campo mascherato per l'inserimento della password di login. 
    */
    @FXML private PasswordField txtLoginPass;
    
    /** 
     * @brief Etichetta di testo usata per mostrare messaggi di errore o di stato nel Login. 
    */
    @FXML private Label lblErroreLogin;

    //REGISTRAZIONE
    /** 
     * @brief Contenitore principale del form di Registrazione (collegato via FXML). 
    */
    @FXML private VBox paneRegistrazione;
    
    /** 
     * @brief Campo di testo per l'inserimento del nuovo username in fase di registrazione. 
    */
    @FXML private TextField txtRegUser;
    
    /** 
     * @brief Campo mascherato per l'inserimento della password in fase di registrazione. 
    */
    @FXML private PasswordField txtRegPass;
    
    /** 
     * @brief Campo mascherato per la conferma della password inserita. 
    */
    @FXML private PasswordField txtRegPassConfirm;
    
    /** 
     * @brief Etichetta descrittiva contenente il ruolo preimpostato (PLAYER). 
    */
    @FXML private Label comboRuolo;
    
    /** 
     * @brief Etichetta di testo usata per mostrare messaggi di errore o di stato nella Registrazione. 
    */
    @FXML private Label lblErroreReg;

    /** 
     * @brief Riferimento alla connessione di rete attiva per l'invio dei comandi. 
    */
    private ClientConnection clientConnection;
    
    /**
     * @brief Inizializza la connessione di rete con il server in modo asincrono.
     * * Il metodo crea ed avvia un thread separato (background) per gestire il tentativo 
     * di connessione al server. In questo modo si evita il congelamento (freezing) dell'interfaccia 
     * grafica (UI) di JavaFX. Se la connessione ha successo, il riferimento viene associato al controller 
     * in modo thread-safe e viene avviato il thread di ascolto dei messaggi.
     * * @pre Il ConfigManager deve essere configurato correttamente con l'IP e la porta del server.
     * @post L'interfaccia grafica rimane reattiva; se la connessione fallisce viene mostrato un 
     * messaggio di errore nella console, se ha successo l'applicazione è pronta a scambiare dati.
     */
    public void inizializzaConnessione() {
        Thread reteThread = new Thread(() -> {
            try {
                String ipServer = ConfigManager.getServerIp();
                int portaServer = ConfigManager.getServerPort();
            
                ClientConnection connessione = new ClientConnection(ipServer, portaServer);
            
                if (connessione.connetti()) {
                    Platform.runLater(() -> {
                        this.setClientConnection(connessione);
                    });

                // Avvio del thread di ascolto dei messaggi del server
                Thread t = new Thread(connessione);
                t.start();
                } else {
                    System.out.println("Server offline!");
                }
            } catch (Exception e) {
                System.out.println("Errore di connessione: " + e.getMessage());
            }
        });
    
        //Stop del thread al chiudersi della finestra
        reteThread.setDaemon(true); // Se chiudi la finestra, il thread si stoppa da solo
        reteThread.start();
    }
    
    /**
     * @brief Configura la connessione di rete corrente e vi si registra come controller.
     * @param connessione L'istanza attiva di ClientConnection.
     */
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        if (connessione != null) {
            connessione.setControllerAuth(this);
        }
    }

    /**
     * @brief Event handler per mostrare graficamente il pannello di Registrazione.
     * Nasconde il pannello di login, rende visibile quello di registrazione e resetta i messaggi d'errore passati.
     * @param event L'evento di click sul pulsante generato da JavaFX.
     */
    @FXML
    void mostraPannelloRegistrazione(ActionEvent event) {
        paneLogin.setVisible(false);
        paneRegistrazione.setVisible(true);
        lblErroreLogin.setText("");
    }

    /**
     * @brief Event handler per mostrare graficamente il pannello di Login.
     * Nasconde il pannello di registrazione, rende visibile quello di login e resetta i messaggi d'errore passati.
     * @param event L'evento di click sul pulsante generato da JavaFX.
     */
    @FXML
    public void mostraPannelloLogin(ActionEvent event) {
        paneRegistrazione.setVisible(false);
        paneLogin.setVisible(true);
        lblErroreReg.setText("");
    }

    /**
     * @brief Gestisce la logica della richiesta di Login.
     * @details Estrae l'input dai campi grafici, verifica localmente che non siano vuoti 
     * e spedisce al server il messaggio nel formato di protocollo `LOGIN:username:password`.
     * @param event L'evento di click sul pulsante Accedi.
     */
    @FXML
    void gestisciLogin(ActionEvent event) {
        String user = txtLoginUser.getText().trim();
        String pass = txtLoginPass.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblErroreLogin.setText("Compila tutti i campi!");
            return;
        }

        if (clientConnection != null) {
            lblErroreLogin.setText("Tentativo di accesso in corso...");
            clientConnection.spedisciMessaggio("LOGIN:" + user + ":" + pass);
        } else {
            lblErroreLogin.setText("Errore: Nessuna connessione al server.");
        }
    }

    /**
     * @brief Gestisce la logica e le validazioni locali per la richiesta di Registrazione.
     * Estrae i parametri dai textfield ed esegue tre controlli di validazione prima dell'invio:
     * - Presenza di campi vuoti.
     * - Coerenza tra password e conferma password.
     * - Assenza del carattere speciale dei due punti `:`, poichè agisce da delimitatore del protocollo di rete.
     * Se i test passano, inoltra il comando `REGISTRAZIONE:username:password:ruolo`.
     * @param event L'evento di click sul pulsante Registrati.
     */
    @FXML
    void gestisciRegistrazione(ActionEvent event) {
        String user = txtRegUser.getText().trim();
        String pass = txtRegPass.getText().trim();
        String passConfirm = txtRegPassConfirm.getText().trim();
        String ruolo = comboRuolo.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || passConfirm.isEmpty() || ruolo.isEmpty()) {
            lblErroreReg.setText("Compila tutti i campi!");
            return;
        }

        if (!pass.equals(passConfirm)) {
            lblErroreReg.setText("Le password inserite non corrispondono.");
            return;
        }

        if (user.contains(":") || pass.contains(":")) {
            lblErroreReg.setText("L'uso dei due punti ':' non è consentito.");
            return;
        }

        if (clientConnection != null) {
            lblErroreReg.setText("Registrazione in corso...");
            clientConnection.spedisciMessaggio("REGISTRAZIONE:" + user + ":" + pass + ":" + ruolo);
        } else {
            lblErroreReg.setText("Errore: Nessuna connessione al server.");
        }
    }
    
    /**
     * @brief Elabora l'esito positivo del login comunicato dalla rete per effettuare il cambio scena.
     * @details Svolge le seguenti operazioni sequenziali:
     * 1. Verifica il ruolo trasmesso dal Server; se l'utente risulta un "ADMIN" l'accesso viene bloccato localmente.
     * 2. Esegue il caricamento del file FXML `LobbyView.fxml`.
     * 3. Recupera il relativo `LobbyController` e gli passa il riferimento alla connessione di rete corrente.
     * 4. Registra il nuovo controller dentro `ClientConnection`.
     * 5. Estrae lo `Stage` corrente a partire dai nodi grafici e vi imposta la nuova scena della Lobby ricentrandola.
     * @param connessione La connessione di rete di tipo ClientConnection attualmente attiva.
     * @param ruolo Il ruolo del profilo utente convalidato dal server.
     */
    public void gestisciLoginSuccess(ClientConnection connessione, String ruolo) {
    try {
            if (ruolo.equals("ADMIN")) {
                mostraMessaggioErroreLogin("Accesso negato: l'acesso è riservato ai Player.");
                return; 
            }
 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/LobbyView.fxml"));
            Parent giocoRoot = loader.load();
        
            LobbyController controllerLobby = loader.getController();
            
            controllerLobby.setClientConnection(connessione);
  
            connessione.setControllerLobby(controllerLobby);
        
            Stage stage = (Stage) paneLogin.getScene().getWindow(); 
        
            Scene scenaGioco = new Scene(giocoRoot);
            stage.setScene(scenaGioco);
            stage.setTitle("Guess The Word - Lobby di Attesa");
            stage.centerOnScreen(); // Opzionale: centra la nuova finestra sullo schermo
            stage.show();
        
        } catch (Exception e) {
            System.err.println("Errore durante il cambio di scena verso la lobby: " + e.getMessage());
            e.printStackTrace();
            mostraMessaggioErroreLogin("Errore nel caricamento della lobby di attesa.");
        }
    }
    
    /**
     * @brief Metodo di utility per impostare un testo d'errore nel form di registrazione.
     * @param msg Il messaggio testuale da visualizzare.
     */
    public void mostraMessaggioErroreReg(String msg) {
        lblErroreReg.setText(msg);
    }
    
    /**
     * @brief Metodo di utility per impostare un testo d'errore nel form di login.
     * @param msg Il messaggio testuale da visualizzare.
     */
    public void mostraMessaggioErroreLogin(String msg) {
        lblErroreLogin.setText(msg);
    }
}