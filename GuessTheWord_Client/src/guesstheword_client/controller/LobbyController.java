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
import guesstheword_client.model.Classifica;
import guesstheword_client.model.PartitaStorico;
import guesstheword_client.network.ClientConnection;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class LobbyController implements Initializable {

    @FXML private Label lblBenvenuto;
    @FXML private Label lblStatoLobby;
    @FXML private Button btnAvviaSfida;

    // Elementi Storico Partite
    @FXML private TableView<PartitaStorico> tabellaStorico;
    @FXML private TableColumn<PartitaStorico, String> colData;
    @FXML private TableColumn<PartitaStorico, String> colParola;
    @FXML private TableColumn<PartitaStorico, String> colEsito;
    @FXML private TableColumn<PartitaStorico, Integer> colPunteggio;

    // Elementi Classifica Globale
    @FXML private TableView<Classifica> tabellaClassifica;
    @FXML private TableColumn<Classifica, Integer> colPosizione;
    @FXML private TableColumn<Classifica, Integer> colUtente;
    @FXML private TableColumn<Classifica, Integer> colPuntiTotali;
    
    private final ObservableList<PartitaStorico> datiStorico = FXCollections.observableArrayList();
    private final ObservableList<Classifica> datiClassifica = FXCollections.observableArrayList();

    private ClientConnection clientConnection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Inizializzazione Tabella Storico
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colParola.setCellValueFactory(new PropertyValueFactory<>("parola"));
        colEsito.setCellValueFactory(new PropertyValueFactory<>("esito"));
        colPunteggio.setCellValueFactory(new PropertyValueFactory<>("punteggio"));
        tabellaStorico.setItems(datiStorico);
        
        // 2. Inizializzazione Tabella Classifica (Mappiamo i get di RigaClassifica)
        colPosizione.setCellValueFactory(new PropertyValueFactory<>("posizione"));
        colUtente.setCellValueFactory(new PropertyValueFactory<>("utente"));
        colPuntiTotali.setCellValueFactory(new PropertyValueFactory<>("puntiTotali"));
        tabellaClassifica.setItems(datiClassifica);
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
            lblStatoLobby.setText("In coda... Ricerca di un avversario in corso...");
            
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
    
        /**
     * Permette alla rete di iniettare un record della classifica alla volta
     */
    public void aggiungiGiocatoreAClassifica(int posizione, String utente, int puntiTotali) {
        Classifica nuovaRiga = new Classifica(posizione, utente, puntiTotali);
        datiClassifica.add(nuovaRiga);
    }
    
        /**
     * Permette alla rete di iniettare dati storici personali nella tabella storico
     */
    public void aggiungiPartitaAStorico(String data, String parola, String esito, int punteggio) {
        PartitaStorico nuovaRiga = new PartitaStorico(data, parola, esito, punteggio);
        datiStorico.add(nuovaRiga);
    }


}