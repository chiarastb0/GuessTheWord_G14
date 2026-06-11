/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_client.controller;

/**
 *
 * @author Chiara
 */

import guesstheword_client.model.PartitaStorico; 
import guesstheword_client.model.Classifica; 
import guesstheword_client.network.ClientConnection;
import java.net.URL;
import javafx.util.Duration;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ScreenGameController implements Initializable {

    // --- COMPONENTI SCHEDA GIOCO ---
    @FXML
    private Label lblTimer;
    @FXML
    private TextArea txtAreaSfida;
    @FXML
    private TextField txtRisposta;

    // --- COMPONENTI SCHEDA STORICO ---
    @FXML
    private TableView<PartitaStorico> tabellaStorico;
    @FXML
    private TableColumn<PartitaStorico, String> colData;
    @FXML
    private TableColumn<PartitaStorico, String> colParola;
    @FXML
    private TableColumn<PartitaStorico, String> colEsito;
    @FXML
    private TableColumn<PartitaStorico, Integer> colPunteggio;

    // --- COMPONENTI SCHEDA CLASSIFICHE (Nuovi!) ---
    @FXML
    private TableView<Classifica> tabellaClassifica;
    @FXML
    private TableColumn<Classifica, Integer> colPosizione;
    @FXML
    private TableColumn<Classifica, String> colUtente;
    @FXML
    private TableColumn<Classifica, Integer> colPuntiTotali;

    // Liste di supporto JavaFX per popolare le tabelle
    private final ObservableList<PartitaStorico> datiStorico = FXCollections.observableArrayList();
    private final ObservableList<Classifica> datiClassifica = FXCollections.observableArrayList();

    private ClientConnection clientConnection;
    private Timeline timeline;
    private int secondiRimanenti;

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
     * Permette alla rete di svuotare e aggiornare la classifica globale
     */
    public void svuotaClassifica() {
        datiClassifica.clear();
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