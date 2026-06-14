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

/**
 * @class GameController
 * @brief Controlla i componenti grafici e la logica interna durante una partita.
 * Riceve aggiornamenti in tempo reale dal thread 
 * di rete, aggiorna la label del timer tramite una Timeline e mostra i risultati finali oscurando 
 * l'interfaccia principale.
 */
public class GameController {

    // SCHEDA GIOCO 
    /** 
     * @brief Etichetta per la visualizzazione del countdown dei secondi rimanenti. 
    */
    @FXML private Label lblTimer;
    
    /** 
     * @brief Etichetta per mostrare notifiche temporanee. 
    */
    @FXML private Label lblNotifica; 
    
    /** 
     * @brief Area di testo contenente il testo. 
    */
    @FXML private TextArea txtAreaSfida;
    
    /** 
     * @brief Campo di input in cui l'utente digita i propri tentativi di risposta. 
    */
    @FXML private TextField txtRisposta;
    
    // COMPONENTI OVERLAY 
    /** 
     * @brief Contenitore principale delle schede di gioco, disabilitato alla fine del match per bloccare gli input sottostanti. 
    */
    @FXML private TabPane mainGameContainer;
    
    /** 
     * @brief Contenitore VBox posizionato in overlay per mostrare la schermata di riepilogo a fine partita. 
    */
    @FXML private VBox paneRisultato;
    
    /** 
     * @brief Etichetta dell'overlay che mostra il titolo dell'esito. 
     */
    @FXML private Label lblEsitoTitolo;
    
    /** 
     * @brief Etichetta dell'overlay che mostra i dettagli testuali. 
    */
    @FXML private Label lblEsitoDescrizione;

    /** 
     * @brief Riferimento alla connessione di rete client attiva. 
    */
    private ClientConnection clientConnection;
    
    /** 
     * @brief Timeline di JavaFX utilizzata per gestire il countdown dei secondi della sfida. 
    */
    private Timeline timeline;
    
    /** 
     * @brief Contatore intero dei secondi rimanenti prima della scadenza del tempo. 
    */
    private int secondiRimanenti;
    
    /** 
     * @brief Timeline secondaria utilizzata per far scomparire le notifiche flash dopo un timeout prestabilito. 
    */
    private Timeline timelineErrore; 

    /**
     * @brief Associa la connessione di rete corrente al controller e si registra all'interno di essa.
     * @param connessione L'istanza di ClientConnection attiva.
     */
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        this.clientConnection.setControllerGioco(this);
    }

    /**
     * @brief Predispone l'interfaccia grafica e avvia il timer per una nuova sessione di gioco.
     * Resetta la visibilità dell'overlay, riabilita il contenitore principale, inserisce il 
     * testo cifrato iniziale e configura una Timeline per aggiornare il countdown grafico.
     * @param tempo         La durata totale della sfida espressa in secondi (sotto forma di stringa).
     * @param testoCifrato  La stringa oscurata iniziale da indovinare.
     */
    public void inizializzaPartita(String tempo, String testoCifrato) {
        // Ripristino dello stato della UI in caso di partite multiple consecutive
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

    /**
     * @brief Estrae la risposta inserita dall'utente e la inoltra al server.
     * Metodo attivato alla pressione del tasto Invio nel campo di testo, se l'input non è vuoto,
     * trasmette il comando nel formato `RISPOSTA:testo_inserito` e svuota il campo.
     */
    @FXML
    private void gestisciInvioRisposta() {
        String invio = txtRisposta.getText().trim();
        if (!invio.isEmpty() && clientConnection != null) {
            clientConnection.spedisciMessaggio("RISPOSTA:" + invio);
            txtRisposta.clear(); 
        }
    }
    
    /**
     * @brief Interrompe la partita e mostra l'overlay grafico con l'esito finale.
     * Esegue il parsing del messaggio di fine partita inviato dal server (`FINE_PARTITA:ESITO:DESCRIZIONE`).
     * Attraverso Platform.runLater() ferma il timer, disabilita l'interfaccia di gioco sottostante e personalizza 
     * i testi dell'overlay a seconda del risultato (VITTORIA, SCONFITTA, PAREGGIO).
     * @param messaggioRete Il messaggio di protocollo ricevuto dal server.
     */
    public void gestisciFinePartita(String messaggioRete) {
        String[] parti = messaggioRete.split(":", 3); 
        if (parti.length < 3) return;

        String esito = parti[1]; 
        String testoAvviso = parti[2]; 

        Platform.runLater(() -> {
            if (timeline != null) {
                timeline.stop(); 
            }
            
            mainGameContainer.setDisable(true);
            
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

            paneRisultato.setVisible(true);
        });
    }
    
    /**
     * @brief Gestisce il cambio di scena per ritornare alla Lobby principale del client.
     * Viene invocato al click sul pulsante presente all'interno dell'overlay di fine partita. 
     * Carica il file FXML `LobbyView.fxml`, istanzia il relativo controller riassegnando i puntatori di rete 
     * ed azzerando il riferimento al gioco corrente, infine richiede al server un aggiornamento immediato 
     * di storico e classifica per la schermata appena caricata.
     */
    @FXML
    private void ritornoAllaLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/LobbyView.fxml"));
            Parent lobbyRoot = loader.load();

            LobbyController controllerLobby = loader.getController();

            controllerLobby.setClientConnection(clientConnection);
            clientConnection.setControllerLobby(controllerLobby);
            clientConnection.setControllerGioco(null);

            Stage stage = (Stage) paneRisultato.getScene().getWindow();
            Scene scenaLobby = new Scene(lobbyRoot);
            stage.setScene(scenaLobby);
            stage.setTitle("Guess The Word - Lobby di Attesa");
            stage.centerOnScreen();
            stage.show();

            System.out.println("[GUI] Ritorno alla lobby completato con successo via Overlay.");
            
            clientConnection.spedisciMessaggio("RICHIEDI_STORICO");
            clientConnection.spedisciMessaggio("RICHIEDI_CLASSIFICA");

        } catch (Exception e) {
            System.err.println("Errore durante il ritorno alla lobby dall'overlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * @brief Aggiorna dinamicamente l'area di testo della sfida con la nuova stringa cifrata.
     *  Richiamato dal thread di rete quando un qualunque utente indovina una parola, 
     * aggiorna il testo visibile e pulisce il campo di input della risposta all'interno del thread JavaFX.
     * @param nuovoTesto Il nuovo testo parzialmente oscurata inviata dal server.
     */
    public void aggiornaTestoDinamicamente(String nuovoTesto) {
        Platform.runLater(() -> {
            txtAreaSfida.setText(nuovoTesto);
            txtRisposta.clear(); 
        });
    }
    
    /**
     * @brief Visualizza una notifica testuale nell'interfaccia di gioco.
     * @param messaggio Il testo della notifica da mostrare.
     */
    public void mostraNotifica(String messaggio) {
        mostraMessaggioErroreTemporaneo(messaggio);
    }
    
    /**
     * @brief Mostra un messaggio di errore temporaneo sulla label delle notifiche.
     * Gestisce la comparsa del testo e configura una Timeline dedicata della durata di 2 secondi 
     * per provvedere alla sua cancellazione automatica.
     * @param messaggio Il testo dell'errore da visualizzare provvisoriamente.
     */
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