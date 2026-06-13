/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.controller;

import guesstheword_server.db.PartitaDAO;
import guesstheword_server.db.RisultatoDAO;
import guesstheword_server.network.ServerManager;
import guesstheword_server.utils.FileManager;
import java.io.File;
import java.util.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


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
    @FXML private ComboBox<String> comboDifficolta;
    
    //COMPONENTI PER LE STATISTICHE
    @FXML private TableView<Map.Entry<String, Integer>> tabellaVittorie;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colVittoriaUser;
    @FXML private TableColumn<Map.Entry<String, Integer>, Integer> colVittoriaCount;

    @FXML private TableView<Map.Entry<String, Double>> tabellaTempi;
    @FXML private TableColumn<Map.Entry<String, Double>, String> colTempoUser;
    @FXML private TableColumn<Map.Entry<String, Double>, Double> colTempoMedia;

    private ServerManager serverManager;
    private String testoIntegraleCorrente = ""; 
    private final File cartellaStorico = new File("storico_dizionari");
    
    private File ultimoFileCaricato;

    @FXML
    public void initialize() {
        colonnaParola.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        colonnaFrequenza.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getValue()).asObject());
        
        //CONFIGURAZIONe TABELLE STATISTICHE
        colVittoriaUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));
        colVittoriaCount.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getValue()).asObject());

        colTempoUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));
        colTempoMedia.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getValue()).asObject());

        
        caricaStatisticheDatabase();
        
        // Popoliamo il menu della difficoltà e impostiamo "Facile" come predefinito
        if (comboDifficolta != null) {
            comboDifficolta.getItems().addAll("Facile", "Media", "Difficile");
            comboDifficolta.setValue("Facile");
        
            // Questo "Listener" scatta automaticamente ogni volta che clicchi un'opzione diversa!
            comboDifficolta.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && serverManager != null) {
                    serverManager.setDifficoltaCorrente(newValue);
                    System.out.println(">>> DIFFICOLTÀ AGGIORNATA IN CORSA A: " + newValue);
                }
            });
        }

        if (!cartellaStorico.exists()) {
            cartellaStorico.mkdir();
        }
        
        // Forza l'auto-adattamento delle colonne eliminando la colonna vuota a destra
        tabellaVittorie.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabellaTempi.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabellaParole.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        aggiornaTendinaStorico();
    }
    
    private void aggiornaTendinaStorico() {
        comboStoricoFile.getItems().clear();
        File[] salvataggi = cartellaStorico.listFiles((dir, name) -> name.endsWith(".dat"));
        
        if (salvataggi != null) {
            for (File f : salvataggi) {
                comboStoricoFile.getItems().add(f.getName().replace(".dat", ""));
            }
        }
    }

    @FXML
    void caricaDaStorico(ActionEvent event) {
        String nomeSelezionato = comboStoricoFile.getValue();
        if (nomeSelezionato != null) {
            try {
                String percorso = cartellaStorico.getPath() + "/" + nomeSelezionato + ".dat";
                Object[] dati = FileManager.caricaDizionarioETesto(percorso);
                Map<String, Long> dizionarioCaricato = (Map<String, Long>) dati[0];
                String testoIntegrale = (String) dati[1];
                
                this.testoIntegraleCorrente = testoIntegrale; // Salviamo il testo in memoria
                
                tabellaParole.getItems().clear();
                tabellaParole.getItems().addAll(dizionarioCaricato.entrySet());
                
                lblNomeFile.setText("Caricato dallo storico: " + nomeSelezionato);
                
                if (serverManager != null) {
                    serverManager.setDatiSfida(dizionarioCaricato, testoIntegrale);
                }
            } catch (Exception e) {
                System.err.println("[SERVER] Errore nel caricamento dallo storico: " + e.getMessage());
            }
        }
    }

    private void caricaStatisticheDatabase() {
        try {
            RisultatoDAO risDAO = new RisultatoDAO();
            PartitaDAO parDAO = new PartitaDAO();
            
            // 1. Partite totali
            lblTotalePartite.setText(String.valueOf(parDAO.getNumeroPartiteDisputate()));

            // 2. Tabella Vittorie
            tabellaVittorie.getItems().setAll(risDAO.getVittoriePerUtente());

            // 3. Tabella Tempi Medi
            tabellaTempi.getItems().setAll(risDAO.getTempoMedioPerUtente());

        } catch (Exception e) {
            System.err.println("Errore caricamento statistiche: " + e.getMessage());
        }
    }
    
    @FXML
    void aggiornaStatisticheClick() {
        caricaStatisticheDatabase();
    }

    @FXML
    void avviaServer(ActionEvent event) {
        if (serverManager == null) {
            serverManager = new ServerManager();
            
            if (!tabellaParole.getItems().isEmpty()) {
                java.util.Map<String, Long> mappaCorrente = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, Long> entry : tabellaParole.getItems()) {
                    mappaCorrente.put(entry.getKey(), entry.getValue());
                }
                serverManager.setDatiSfida(mappaCorrente, testoIntegraleCorrente);
            }
            
            if (comboDifficolta != null && comboDifficolta.getValue() != null) {
                serverManager.setDifficoltaCorrente(comboDifficolta.getValue());
            }
            
            Thread serverThread = new Thread(() -> {
                serverManager.start();
            });
            serverThread.setDaemon(true); 
            serverThread.start();

            lblServerStatus.setText("🟢 In Ascolto...");
            lblServerStatus.setTextFill(javafx.scene.paint.Color.GREEN);
            btnStartServer.setDisable(true); 
        }
    }

    @FXML
    void caricaFileTesto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona File di Testo per le Sfide");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        Stage stage = (Stage) btnStartServer.getScene().getWindow();
        File fileSelezionato = fileChooser.showOpenDialog(stage);

        if (fileSelezionato != null) {
            this.ultimoFileCaricato = fileSelezionato; 
            lblNomeFile.setText("Analisi in corso: " + fileSelezionato.getName());
            avviaAnalisiTestoAsincrona(fileSelezionato.getAbsolutePath());
        }
    }

    private void avviaAnalisiTestoAsincrona(String percorso) {
        Task<Map<String, Long>> taskAnalisi = FileManager.analizzaDocumentoTask(percorso);

        progressBar.setVisible(true);
        progressBar.progressProperty().bind(taskAnalisi.progressProperty());

        taskAnalisi.setOnSucceeded(e -> {
            Map<String, Long> risultato = taskAnalisi.getValue();
            
            tabellaParole.getItems().clear();
            tabellaParole.getItems().addAll(risultato.entrySet());
            
            if (ultimoFileCaricato != null) {
                try {
                    String nomeBase = ultimoFileCaricato.getName().replace(".txt", "");
                    String percorsoSalvataggio = cartellaStorico.getPath() + "/" + nomeBase + ".dat";
                    
                    String testoIntegrale = new String(java.nio.file.Files.readAllBytes(ultimoFileCaricato.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                    
                    this.testoIntegraleCorrente = testoIntegrale; // Salviamo il testo in memoria
                    
                    FileManager.salvaDizionarioETesto(risultato, testoIntegrale, percorsoSalvataggio);
                    
                    if (serverManager != null) {
                        serverManager.setDatiSfida(risultato, testoIntegrale);
                    }
                    
                    if (!comboStoricoFile.getItems().contains(nomeBase)) {
                        comboStoricoFile.getItems().add(nomeBase);
                    }
                    comboStoricoFile.setValue(nomeBase);
                    lblNomeFile.setText("Analisi completata e salvata: " + nomeBase);
                } catch (Exception ex) {
                    System.err.println("[SERVER] Errore durante il salvataggio con testo: " + ex.getMessage());
                }
            }
            
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            
            System.out.println("Analisi file completata. Parole trovate: " + risultato.size());
        });

        taskAnalisi.setOnFailed(e -> {
            progressBar.setVisible(false);
            lblNomeFile.setText("Errore durante la lettura!");
            System.err.println("Errore FileManager: " + taskAnalisi.getException());
        });

        new Thread(taskAnalisi).start();
    }
}