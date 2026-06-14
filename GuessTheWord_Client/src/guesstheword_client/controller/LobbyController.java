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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * @class LobbyController
 * @brief Gestisce le interazioni della UI per lo storico, la classifica e l'avvio della sfida.
 * Implementa Initializable per mappare i datisulle tabelle JavaFX.
 */
public class LobbyController implements Initializable {

    /** 
     * @brief Etichetta per il messaggio di benvenuto. 
    */
    @FXML private Label lblBenvenuto;
    
    /** 
     * @brief Etichetta che mostra lo stato corrente nella lobby. 
    */
    @FXML private Label lblStatoLobby;
    
    /** 
     * @brief Pulsante per richiedere l'avvio di una nuova partita. 
    */
    @FXML private Button btnAvviaSfida;

    // STORICO PARTITE
    
    /** 
     * @brief Tabella grafica dello storico partite. 
    */
    @FXML private TableView<PartitaStorico> tabellaStorico;
    
    /** 
     * @brief Colonna data dello storico. 
    */
    @FXML private TableColumn<PartitaStorico, String> colData;
    
    /** 
     * @brief Colonna parola dello storico. 
    */
    @FXML private TableColumn<PartitaStorico, String> colParola;
    
    /** 
     * @brief Colonna esito dello storico. 
    */
    @FXML private TableColumn<PartitaStorico, String> colEsito;
    
    /** 
     * @brief Colonna punteggio dello storico. 
    */
    @FXML private TableColumn<PartitaStorico, Integer> colPunteggio;

    //CLASSIFICA GLOBALE
    
    /** 
     * @brief Tabella della classifica globale. 
    */
    @FXML public TableView<Classifica> tabellaClassifica;
    
    /** 
     * @brief Colonna posizione della classifica. 
    */
    @FXML private TableColumn<Classifica, Integer> colPosizione;
    
    /** 
     * @brief Colonna nome utente della classifica. 
    */
    @FXML private TableColumn<Classifica, String> colUtente;
    
    /** 
     * @brief Colonna punti totali della classifica. 
    */
    @FXML private TableColumn<Classifica, Integer> colPuntiTotali;
    
    /** 
     * @brief Lista osservabile per i record dello storico. 
    */
    private final ObservableList<PartitaStorico> datiStorico = FXCollections.observableArrayList();
    
    /** 
     * @brief Lista osservabile per i record della classifica. 
    */
    private final ObservableList<Classifica> datiClassifica = FXCollections.observableArrayList();

    /** 
     * @brief Riferimento alla connessione di rete client. 
    */
    private ClientConnection clientConnection;

    /**
     * @brief Inizializza il controller, configurando il mapping delle TableView.
     * @param url La locazione usata per risolvere i percorsi relativi dell'oggetto radice.
     * @param rb Le risorse usate per localizzare l'oggetto radice.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colParola.setCellValueFactory(new PropertyValueFactory<>("parola"));
        colEsito.setCellValueFactory(new PropertyValueFactory<>("esito"));
        colPunteggio.setCellValueFactory(new PropertyValueFactory<>("punteggio"));
        tabellaStorico.setItems(datiStorico);
        tabellaStorico.setSelectionModel(null); 
        
        colPosizione.setCellValueFactory(new PropertyValueFactory<>("posizione"));
        colUtente.setCellValueFactory(new PropertyValueFactory<>("utente"));
        colPuntiTotali.setCellValueFactory(new PropertyValueFactory<>("puntiTotali"));
        tabellaClassifica.setItems(datiClassifica);
        tabellaClassifica.setSelectionModel(null); 

    }

    /**
     * @brief Associa la connessione di rete e richiede i dati iniziali al server.
     * @param connessione L'istanza di ClientConnection attiva.
     */
    public void setClientConnection(ClientConnection connessione) {
        this.clientConnection = connessione;
        if (connessione != null) {
            connessione.setControllerLobby(this); 
            
            clientConnection.spedisciMessaggio("RICHIEDI_STORICO");
            clientConnection.spedisciMessaggio("RICHIEDI_CLASSIFICA");
        }
    }

    /**
     * @brief Gestisce l'azione di click per mettersi in coda di gioco.
     * @param event L'evento generato dal click sul pulsante.
     */
    @FXML
    void gestisciAvviaSfida(ActionEvent event) {
        if (clientConnection != null) {
            btnAvviaSfida.setDisable(true);
            lblStatoLobby.setText("In coda... Ricerca di un avversario in corso...");

            clientConnection.spedisciMessaggio("AVVIA_SFIDA");
        }
    }

    /**
     * @brief Carica la scena di gioco inserendo i parametri iniziali ricevuti.
     * @param tempo Il tempo totale della sfida in stringa.
     * @param testoCifrato La parola oscurata iniziale.
     */
    public void avviaSchermataGioco(String tempo, String testoCifrato) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_client/view/ScreenGameView.fxml"));
                Parent giocoRoot = loader.load();

                GameController controllerGioco = loader.getController();
                
                controllerGioco.setClientConnection(clientConnection);
                clientConnection.setControllerGioco(controllerGioco);

                controllerGioco.inizializzaPartita(tempo, testoCifrato);

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
     * @brief Aggiunge una riga di dati alla lista della classifica.
     * @param posizione La posizione occupata.
     * @param utente Lo username del giocatore.
     * @param puntiTotali I punti totalizzati.
     */
    public void aggiungiGiocatoreAClassifica(int posizione, String utente, int puntiTotali) {
        Classifica nuovaRiga = new Classifica(posizione, utente, puntiTotali);
        datiClassifica.add(nuovaRiga);
    }
    
    /**
     * @brief Aggiunge una riga di dati alla lista dello storico partite.
     * @param data La data del match.
     * @param parola La parola del match.
     * @param esito L'esito finale.
     * @param punteggio Il punteggio ottenuto.
     */
    public void aggiungiPartitaAStorico(String data, String parola, String esito, int punteggio) {
        PartitaStorico nuovaRiga = new PartitaStorico(data, parola, esito, punteggio);
        datiStorico.add(nuovaRiga);
    }
    
    /**
     * @brief Svuota la lista dello storico delle partite.
     */
    public void svuotaStorico() {
        this.datiStorico.clear();
    }

    /**
     * @brief Svuota la lista della classifica globale.
     */
    public void svuotaClassifica() {
        this.datiClassifica.clear();
    }


}