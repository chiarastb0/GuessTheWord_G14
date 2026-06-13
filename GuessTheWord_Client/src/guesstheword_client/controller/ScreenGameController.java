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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ScreenGameController implements Initializable {

    // --- COMPONENTI SCHEDA GIOCO ---
    @FXML private Label lblTimer;
    @FXML private Label lblNotifica; 
    @FXML private TextArea txtAreaSfida;
    @FXML private TextField txtRisposta;
    
    // --- NUOVI COMPONENTI AGGIUNTI PER OVERLAY ---
    @FXML private TabPane mainGameContainer;       // Per disabilitare i click sulla scheda sotto
    @FXML private VBox paneRisultato;              // Il contenitore scuro a schermo intero
    @FXML private Label lblEsitoTitolo;            // Scritta grande Vittoria/Sconfitta
    @FXML private Label lblEsitoDescrizione;       // Testo descrittivo dei punti

    private ClientConnection clientConnection;
    private Timeline timeline;
    private int secondiRimanenti;
    private Timeline timelineErrore; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inizializzazione pulita
    }    
    
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        this.clientConnection.setControllerGioco(this);
    }

    public void inizializzaPartita(String tempo, String testoCifrato) {
        // Ripristiniamo lo stato della UI in caso di partite multiple consecutive
        paneRisultato.setVisible(false);
        mainGameContainer.setDisable(false);
        
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
     * Gestisce la fine della partita mostrando l'overlay grafico in-game
     */
    public void gestisciFinePartita(String messaggioRete) {
        String[] parti = messaggioRete.split(":", 3); 
        if (parti.length < 3) return;

        String esito = parti[1]; // VITTORIA, SCONFITTA o PAREGGIO
        String testoAvviso = parti[2]; // La spiegazione del server

        Platform.runLater(() -> {
            if (timeline != null) {
                timeline.stop(); // Blocchiamo il tempo residuo
            }
            
            // 1. Congeliamo l'interfaccia sotto
            mainGameContainer.setDisable(true);
            
            // 2. Personalizziamo i testi dell'overlay
            lblEsitoDescrizione.setText(testoAvviso);
            
            switch (esito) {
                case "VITTORIA":
                    lblEsitoTitolo.setText("HAI VINTO!");
                    break;
                case "SCONFITTA":
                    lblEsitoTitolo.setText("HAI PERSO");
                    break;
                case "PAREGGIO":
                    lblEsitoTitolo.setText("PAREGGIO: tempo scaduto");
                    break;
            }

            // 3. Mostriamo l'overlay oscurando la UI
            paneRisultato.setVisible(true);
        });
    }
    
    /**
     * Azione collegata al bottone dell'overlay per ritornare alla lobby
     */
    @FXML
    private void ritornoAllaLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/LobbyView.fxml"));
            Parent lobbyRoot = loader.load();

            LobbyController controllerLobby = loader.getController();
            
            // Ripristiniamo i puntatori di rete verso il controller lobby
            controllerLobby.setClientConnection(clientConnection);
            clientConnection.setControllerLobby(controllerLobby);
            clientConnection.setControllerGioco(null); // Rilasciamo il gioco attuale

            // Cambiamo scena sulla finestra attuale
            Stage stage = (Stage) paneRisultato.getScene().getWindow();
            Scene scenaLobby = new Scene(lobbyRoot);
            stage.setScene(scenaLobby);
            stage.setTitle("Guess The Word - Lobby di Attesa");
            stage.centerOnScreen();
            stage.show();

            System.out.println("[GUI] Ritorno alla lobby completato con successo via Overlay.");
            
            // Aggiorniamo istantaneamente i dati delle classifiche/storici della lobby ricaricata
            clientConnection.spedisciMessaggio("RICHIEDI_STORICO");
            clientConnection.spedisciMessaggio("RICHIEDI_CLASSIFICA");

        } catch (Exception e) {
            System.err.println("Errore durante il ritorno alla lobby dall'overlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void aggiornaTestoDinamicamente(String nuovoTesto) {
        Platform.runLater(() -> {
            txtAreaSfida.setText(nuovoTesto);
            txtRisposta.clear(); 
        });
    }

    public void mostraNotifica(String messaggio) {
        // Questo metodo mostrava un alert.show() asincrono fastidioso, lo convertiamo
        // per usare la label interna temporanea così da non interrompere l'esperienza utente.
        mostraMessaggioErroreTemporaneo(messaggio);
    }
    
    public void mostraMessaggioErroreTemporaneo(String messaggio) {
        if (timelineErrore != null) {
            timelineErrore.stop();
        }

        lblNotifica.setText(messaggio);

        timelineErrore = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            lblNotifica.setText(""); 
        }));
        timelineErrore.setCycleCount(1);
        timelineErrore.play();
    }
}