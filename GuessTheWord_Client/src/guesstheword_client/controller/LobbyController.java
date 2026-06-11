/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.controller;

/**
 *
 * @author admin
 */
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class LobbyController implements Initializable {

    @FXML private Label lblBenvenuto;
    @FXML private Label lblStatoLobby;
    @FXML private Button btnAvviaSfida;

    // Elementi Storico Partite
    @FXML private TableView<?> tabellaStorico;
    @FXML private TableColumn<?, ?> colIdSfida;
    @FXML private TableColumn<?, ?> colData;
    @FXML private TableColumn<?, ?> colAvversario;
    @FXML private TableColumn<?, ?> colPunti;
    @FXML private TableColumn<?, ?> colEsito;

    // Elementi Classifica Globale
    @FXML private TableView<?> tabellaClassifica;
    @FXML private TableColumn<?, ?> colPosizione;
    @FXML private TableColumn<?, ?> colUtente;
    @FXML private TableColumn<?, ?> colPuntiTotali;

    private ClientConnection clientConnection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Qui configurerai le property delle colonne (es. setCellValueFactory) 
        // esattamente come facevi nel vecchio ScreenGameController
    }

    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        if (connessione != null) {
            connessione.setControllerLobby(this); // Importante per ricevere aggiornamenti di coda/tabelle
            
            // Richiedi subito al server i dati per popolare storico e classifica nella lobby
            clientConnection.spedisciMessaggio("RICHIEDI_STORICO");
            clientConnection.spedisciMessaggio("RICHIEDI_CLASSIFICA");
        }
    }

    @FXML
    void gestisciAvviaSfida(ActionEvent event) {
        if (clientConnection != null) {
            btnAvviaSfida.setDisable(true);
            lblStatoLobby.setText("⏳ In coda... Ricerca di un avversario in corso...");
            
            // Inviamo il comando al server per dire che siamo pronti a giocare
            clientConnection.spedisciMessaggio("AVVIA_SFIDA");
        }
    }

    /**
     * Chiamato da ClientConnection quando il server risponde che è stato trovato un avversario
     * e la sfida ha effettivamente inizio.
     */
    public void avviaSchermataGioco() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/ScreenGameView.fxml"));
                Parent giocoRoot = loader.load();

                ScreenGameController controllerGioco = loader.getController();
                
                // Passiamo la connessione attiva e aggiorniamo il riferimento di ascolto rete
                controllerGioco.setClientConnection(clientConnection);
                clientConnection.setControllerGioco(controllerGioco);

                // Effettuiamo il cambio scena
                Stage stage = (Stage) btnAvviaSfida.getScene().getWindow();
                Scene scenaGioco = new Scene(giocoRoot);
                stage.setScene(scenaGioco);
                stage.setTitle("Guess The Word - Partita in Corso");
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                System.err.println("Errore passaggio alla schermata di gioco: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }


}