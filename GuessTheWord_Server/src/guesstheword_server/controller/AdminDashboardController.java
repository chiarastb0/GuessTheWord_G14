/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.controller;

/**
 *
 * @author angel
 */

import guesstheword_server.db.PartitaDAO;
import guesstheword_server.network.ServerManager;
import guesstheword_server.utils.FileManager;
import java.io.File;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import java.io.File;

public class AdminDashboardController {

    @FXML private Label lblServerStatus;
    @FXML private Button btnStartServer;
    @FXML private Label lblTotalePartite;
    @FXML private Label lblNomeFile;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<Map.Entry<String, Long>> tabellaParole;
    @FXML private TableColumn<Map.Entry<String, Long>, String> colonnaParola;
    @FXML private TableColumn<Map.Entry<String, Long>, Long> colonnaFrequenza;
    @FXML private ComboBox<String> comboStoricoFile;
    
    // La cartella dove salveremo tutti i file binari generati
    private final File cartellaStorico = new File("storico_dizionari");
    // Teniamo traccia dell'ultimo file selezionato per il salvataggio
    private File ultimoFileCaricato;

    private ServerManager serverManager;
    

    @FXML
    public void initialize() {
        // 1. Configurazione delle colonne della tabella 
        colonnaParola.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        colonnaFrequenza.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getValue()).asObject());

        // 2. Caricamento statistiche dal Database
        caricaStatisticheDatabase();
           
        // 3. Se la cartella dello storico non esiste, la crea
        if (!cartellaStorico.exists()) {
            cartellaStorico.mkdir();
        }
        // Popola la tendina all'avvio
        aggiornaTendinaStorico();
    }
    
    /**
     * Legge la cartella dello storico e inserisce i nomi nella tendina
     */
    private void aggiornaTendinaStorico() {
        comboStoricoFile.getItems().clear();
        File[] salvataggi = cartellaStorico.listFiles((dir, name) -> name.endsWith(".dat"));
        
        if (salvataggi != null) {
            for (File f : salvataggi) {
                // Rimuove l'estensione .dat per rendere la scritta più pulita nella tendina
                comboStoricoFile.getItems().add(f.getName().replace(".dat", ""));
            }
        }
    }

    /**
     * Scatta automaticamente quando l'amministratore seleziona una voce dalla tendina
     */
    @FXML
    void caricaDaStorico(ActionEvent event) {
        String nomeSelezionato = comboStoricoFile.getValue();
        if (nomeSelezionato != null) {
            try {
                // Ricostruisce il percorso e carica i dati
                String percorso = cartellaStorico.getPath() + "/" + nomeSelezionato + ".dat";
                Map<String, Long> dizionarioCaricato = FileManager.caricaDizionario(percorso);
                
                tabellaParole.getItems().clear();
                tabellaParole.getItems().addAll(dizionarioCaricato.entrySet());
                
                lblNomeFile.setText("Caricato dallo storico: " + nomeSelezionato);
                System.out.println("[SERVER] Caricato con successo dallo storico: " + nomeSelezionato);
            } catch (Exception e) {
                System.err.println("[SERVER] Errore nel caricamento dallo storico: " + e.getMessage());
            }
        }
    }

    private void caricaStatisticheDatabase() {
        try {
            PartitaDAO partitaDAO = new PartitaDAO();
            int totale = partitaDAO.getNumeroPartiteDisputate();
            lblTotalePartite.setText(String.valueOf(totale));
        } catch (Exception e) {
            lblTotalePartite.setText("Errore DB");
            System.err.println("Impossibile leggere le statistiche: " + e.getMessage());
        }
    }

    @FXML
    void avviaServer(ActionEvent event) {
        if (serverManager == null) {
            serverManager = new ServerManager();
            
            // Avviamo il socket in un Thread separato per NON bloccare l'interfaccia JavaFX
            Thread serverThread = new Thread(() -> {
                serverManager.start();
            });
            serverThread.setDaemon(true); // Il thread si chiude se chiudi la finestra grafica
            serverThread.start();

            // Aggiorniamo la grafica
            lblServerStatus.setText("🟢 In Ascolto...");
            lblServerStatus.setTextFill(javafx.scene.paint.Color.GREEN);
            btnStartServer.setDisable(true); // Evita di avviare due server contemporaneamente
        }
    }

    @FXML
    void caricaFileTesto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona File di Testo per le Sfide");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        // Ricava la finestra attuale per mostrare il popup
        Stage stage = (Stage) btnStartServer.getScene().getWindow();
        File fileSelezionato = fileChooser.showOpenDialog(stage);

        if (fileSelezionato != null) {
            this.ultimoFileCaricato = fileSelezionato; // Salviamo il riferimento
            lblNomeFile.setText("Analisi in corso: " + fileSelezionato.getName());
            avviaAnalisiTestoAsincrona(fileSelezionato.getAbsolutePath());
        }
    }

    private void avviaAnalisiTestoAsincrona(String percorso) {
        Task<Map<String, Long>> taskAnalisi = FileManager.analizzaDocumentoTask(percorso);

        // Mostriamo e colleghiamo la barra di caricamento al progresso del Task
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(taskAnalisi.progressProperty());

        // Dopo che le Stream API finiscono di analizzare i dati
        taskAnalisi.setOnSucceeded(e -> {
            Map<String, Long> risultato = taskAnalisi.getValue();
            
            // Svuota la tabella e inserisce i nuovi risultati
            tabellaParole.getItems().clear();
            tabellaParole.getItems().addAll(risultato.entrySet());
            
            // --- SALVATAGGIO DINAMICO ---
            if (ultimoFileCaricato != null) {
                try {
                    // Prende il nome del .txt originale (es. "spazio")
                    String nomeBase = ultimoFileCaricato.getName().replace(".txt", "");
                    String percorsoSalvataggio = cartellaStorico.getPath() + "/" + nomeBase + ".dat";
                    
                    FileManager.salvaDizionario(risultato, percorsoSalvataggio);
                    
                    // Se non è già nella tendina, lo aggiungiamo in tempo reale
                    if (!comboStoricoFile.getItems().contains(nomeBase)) {
                        comboStoricoFile.getItems().add(nomeBase);
                    }
                    // Lo selezioniamo visivamente
                    comboStoricoFile.setValue(nomeBase);
                    
                    lblNomeFile.setText("Analisi completata e salvata: " + nomeBase);
                } catch (Exception ex) {
                    System.err.println("[SERVER] Errore salvataggio: " + ex.getMessage());
                }
            }
            
            // Nasconde la barra di caricamento
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            
            System.out.println("Analisi file completata. Parole trovate: " + risultato.size());
        });

        taskAnalisi.setOnFailed(e -> {
            progressBar.setVisible(false);
            lblNomeFile.setText("Errore durante la lettura!");
            System.err.println("Errore FileManager: " + taskAnalisi.getException());
        });

        // Lanciamo il task in background
        new Thread(taskAnalisi).start();
    }
}
