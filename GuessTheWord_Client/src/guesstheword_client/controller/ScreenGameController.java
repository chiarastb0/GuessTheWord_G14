package guesstheword_client.controller;

import guesstheword_client.network.ClientConnection;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ScreenGameController implements Initializable {

    // --- COMPONENTI SCHEDA GIOCO ---
    @FXML
    private Label lblTimer;
    @FXML
    private TextArea txtAreaSfida;
    @FXML
    private TextField txtRisposta;

    private ClientConnection clientConnection;
    private Timeline timeline;
    private int secondiRimanenti;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Metodo lasciato intenzionalmente vuoto: non vi sono più tabelle locali da mappare
    }    
    
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        this.clientConnection.setControllerGioco(this);
    }

    public void inizializzaPartita(String tempo, String testoCifrato) {
        txtAreaSfida.setText(testoCifrato);
        txtRisposta.setDisable(false);
        txtRisposta.clear();
        
        this.secondiRimanenti = Integer.parseInt(tempo);
        lblTimer.setText(secondiRimanenti + " secondi");

        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondiRimanenti--;
            lblTimer.setText(secondiRimanenti + " secondi");
        
            if (secondiRimanenti <= 0) {
                timeline.stop();
                txtRisposta.setDisable(true); 
                lblTimer.setText("Tempo Scaduto!");
                System.out.println("[GUI] Tempo esaurito per questa sfida.");
            }
        }));
    
        timeline.setCycleCount(secondiRimanenti);
        timeline.play();
    }

    @FXML
    private void gestisciInvioRisposta() {
        String invio = txtRisposta.getText().trim();
        if (!invio.isEmpty() && clientConnection != null) {
            clientConnection.spedisciMessaggio("RISPOSTA:" + invio);
            txtRisposta.clear(); 
        }
    }
    
    /**
     * Riceve il comando di fine partita dalla rete e mostra un Pop-up grafico
     */
    public void gestisciFinePartita(String messaggioRete) {
        // Il messaggio è tipo "FINE_PARTITA:VITTORIA:Hai indovinato la parola..."
        String[] parti = messaggioRete.split(":", 3); 
        if (parti.length < 3) return;

        String esito = parti[1]; // VITTORIA, SCONFITTA o PAREGGIO
        String testoAvviso = parti[2]; // La spiegazione

        // Platform.runLater forza l'aggiornamento sulla coda grafica di JavaFX
        Platform.runLater(() -> {
            if (timeline != null) {
                timeline.stop(); // Fermiamo il timer locale per sicurezza
            }
            Alert alert = new Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setHeaderText("Partita Terminata!");
            alert.setContentText(testoAvviso);

            // Personalizziamo il titolo della finestrella in base all'esito
            switch (esito) {
                case "VITTORIA":
                    alert.setTitle("Hai Vinto!");
                    break;
                case "SCONFITTA":
                    alert.setTitle("Hai Perso!");
                    break;
                case "PAREGGIO":
                    alert.setTitle("⏱️ Pareggio!");
                    break;
            }

            // Mostriamo il pop-up e aspettiamo che l'utente clicchi OK
            alert.showAndWait();
            // 🔥 SEZIONE RITORNO ALLA LOBBY DINAMICO
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/LobbyView.fxml"));
                Parent lobbyRoot = loader.load();

                LobbyController controllerLobby = loader.getController();
                
                // Rincrociamo i riferimenti di ascolto di ClientConnection alla lobby
                controllerLobby.setClientConnection(clientConnection);
                clientConnection.setControllerLobby(controllerLobby);
                clientConnection.setControllerGioco(null); // Sganciamo il gioco vecchio

                // Recuperiamo lo Stage attuale partendo da un nodo della schermata di gioco
                Stage stage = (Stage) txtAreaSfida.getScene().getWindow();
                Scene scenaLobby = new Scene(lobbyRoot);
                stage.setScene(scenaLobby);
                stage.setTitle("Guess The Word - Lobby di Attesa");
                stage.centerOnScreen();
                stage.show();

                System.out.println("[GUI] Ritorno alla lobby completato con successo.");
                
                clientConnection.spedisciMessaggio("RICHIESTA_STORICO");
                clientConnection.spedisciMessaggio("RICHIESTA_CLASSIFICA");

            } catch (Exception e) {
                System.err.println("Errore durante il ritorno automatico alla lobby: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Aggiorna la casella di testo in tempo reale quando una parola viene svelata
     */
    public void aggiornaTestoDinamicamente(String nuovoTesto) {
        javafx.application.Platform.runLater(() -> {
            txtAreaSfida.setText(nuovoTesto);
            // Svuota la casella di input per prepararsi alla prossima parola
            txtRisposta.clear(); 
        });
    }

    /**
     * Mostra un popup rapido (non bloccante) per informare sui progressi
     */
    public void mostraNotifica(String messaggio) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Aggiornamento Partita");
            alert.setHeaderText(null);
            alert.setContentText(messaggio);
            alert.show(); // Usiamo show() invece di showAndWait() per non bloccare il timer!
        });
    }
}