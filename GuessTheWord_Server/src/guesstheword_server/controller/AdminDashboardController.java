/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.controller;

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
    
    private final File cartellaStorico = new File("storico_dizionari");
    private File ultimoFileCaricato;
    private ServerManager serverManager;
    
    // Variabile aggiunta per tenere in memoria il testo completo e passarlo al Server!
    private String testoIntegraleCorrente = ""; 

    @FXML
    public void initialize() {
        colonnaParola.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        colonnaFrequenza.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getValue()).asObject());
        caricaStatisticheDatabase();

        if (!cartellaStorico.exists()) {
            cartellaStorico.mkdir();
        }
        
        File[] salvataggi = cartellaStorico.listFiles((dir, name) -> name.endsWith(".dat"));
        if (salvataggi != null && salvataggi.length > 0) {
            try {
                Object[] dati = FileManager.caricaDizionarioETesto(salvataggi[0].getAbsolutePath());
                Map<String, Long> mappa = (Map<String, Long>) dati[0];
                String testo = (String) dati[1];
                
                this.testoIntegraleCorrente = testo; // Salviamo il testo in memoria
                
                tabellaParole.getItems().addAll(mappa.entrySet());
                lblNomeFile.setText("Ripristinato: " + salvataggi[0].getName().replace(".dat", ""));
                
                if (serverManager != null) {
                    serverManager.setDatiSfida(mappa, testo);
                }
            } catch (Exception e) {
                System.err.println("Errore ripristino inizializzazione: " + e.getMessage());
            }
        }
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
            
            if (!tabellaParole.getItems().isEmpty()) {
                java.util.Map<String, Long> mappaCorrente = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, Long> entry : tabellaParole.getItems()) {
                    mappaCorrente.put(entry.getKey(), entry.getValue());
                }
                // Usiamo il metodo aggiornato con la firma corretta!
                serverManager.setDatiSfida(mappaCorrente, testoIntegraleCorrente);
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