package guesstheword_server.controller;

import guesstheword_server.db.PartitaDAO;
import guesstheword_server.db.RisultatoDAO;
import guesstheword_server.network.ServerManager;
import guesstheword_server.utils.FileManager;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller principale per l'interfaccia di amministrazione del Server.
 * Gestisce l'avvio del server di gioco, il caricamento e l'analisi dei file di testo,
 * la configurazione della difficoltà e la visualizzazione delle statistiche globali (vittorie e tempi).
 */
public class AdminDashboardController {

    @FXML private Label lblServerStatus;
    @FXML private Button btnStartServer;
    @FXML private Label lblTotalePartite;
    @FXML private Label lblNomeFile;
    @FXML private ProgressBar progressBar;
    
    // Tabelle e Colonne per il dizionario delle parole
    @FXML private TableView<Map.Entry<String, Long>> tabellaParole;
    @FXML private TableColumn<Map.Entry<String, Long>, String> colonnaParola;
    @FXML private TableColumn<Map.Entry<String, Long>, Long> colonnaFrequenza;
    
    @FXML private ComboBox<String> comboStoricoFile;
    @FXML private ComboBox<String> comboDifficolta;
    
    // Tabelle e Colonne per le statistiche globali
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

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX all'avvio della schermata.
     * Configura il comportamento delle tabelle, definisce le proprietà delle celle
     * e carica i dati iniziali dal database.
     */
    @FXML
    public void initialize() {
        // 1. Associazione delle colonne per la tabella delle parole estratte
        colonnaParola.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        colonnaFrequenza.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getValue()).asObject());
        
        // 2. Associazione delle colonne per la tabella delle vittorie
        colVittoriaUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));
        colVittoriaCount.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getValue()).asObject());

        // 3. Associazione delle colonne per la tabella dei tempi medi
        colTempoUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getKey()));
        colTempoMedia.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getValue()).asObject());
        
        // 4. Caricamento immediato delle statistiche dal database SQLite
        caricaStatisticheDatabase();
        
        // 5. Configurazione del selettore della difficoltà con Listener dinamico
        if (comboDifficolta != null) {
            comboDifficolta.getItems().addAll("Facile", "Media", "Difficile");
            comboDifficolta.setValue("Facile");
        
            // Aggiunge un "ascoltatore": se l'admin cambia difficoltà mentre il server è acceso, 
            // la modifica viene trasmessa in tempo reale al ServerManager.
            comboDifficolta.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && serverManager != null) {
                    serverManager.setDifficoltaCorrente(newValue);
                    System.out.println(">>> DIFFICOLTÀ AGGIORNATA IN CORSA A: " + newValue);
                }
            });
        }

        // 6. Creazione della cartella per i salvataggi se non esiste già
        if (!cartellaStorico.exists()) {
            cartellaStorico.mkdir();
        }
        
        // 7. Miglioramento dell'interfaccia: le colonne occupano tutto lo spazio disponibile senza lasciare margini vuoti
        tabellaVittorie.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabellaTempi.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabellaParole.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // 8. Popolamento del menù a tendina con i file precedentemente analizzati
        aggiornaTendinaStorico();
    }
    
    /**
     * Legge il contenuto della cartella di storico e popola il ComboBox 
     * con tutti i file .dat (analisi pregresse) disponibili.
     */
    private void aggiornaTendinaStorico() {
        comboStoricoFile.getItems().clear();
        File[] salvataggi = cartellaStorico.listFiles((dir, name) -> name.endsWith(".dat"));
        
        if (salvataggi != null) {
            for (File f : salvataggi) {
                comboStoricoFile.getItems().add(f.getName().replace(".dat", ""));
            }
        }
    }

    /**
     * Recupera e carica nell'interfaccia e nella memoria del Server 
     * un dizionario precedentemente elaborato selezionandolo dal menu a tendina.
     * * @param event L'evento scatenato dalla selezione nel ComboBox.
     */
    @FXML
    void caricaDaStorico(ActionEvent event) {
        String nomeSelezionato = comboStoricoFile.getValue();
        if (nomeSelezionato != null) {
            try {
                String percorso = cartellaStorico.getPath() + "/" + nomeSelezionato + ".dat";
                
                // FileManager restituisce un array Object[] con Mappa e Testo Completo
                Object[] dati = FileManager.caricaDizionarioETesto(percorso);
                
                @SuppressWarnings("unchecked")
                Map<String, Long> dizionarioCaricato = (Map<String, Long>) dati[0];
                String testoIntegrale = (String) dati[1];
                
                this.testoIntegraleCorrente = testoIntegrale; 
                
                // Aggiorna l'interfaccia utente con le parole appena lette dal file binario
                tabellaParole.getItems().clear();
                tabellaParole.getItems().addAll(dizionarioCaricato.entrySet());
                lblNomeFile.setText("Caricato dallo storico: " + nomeSelezionato);
                
                // Se il server è già in ascolto, gli passiamo subito i nuovi dati di gioco
                if (serverManager != null) {
                    serverManager.setDatiSfida(dizionarioCaricato, testoIntegrale);
                }
            } catch (Exception e) {
                System.err.println("[SERVER] Errore nel caricamento dallo storico: " + e.getMessage());
            }
        }
    }

    /**
     * Interroga il database tramite i DAO per calcolare e visualizzare 
     * il totale delle partite, le vittorie per utente e i tempi medi.
     */
    private void caricaStatisticheDatabase() {
        try {
            RisultatoDAO risDAO = new RisultatoDAO();
            PartitaDAO parDAO = new PartitaDAO();
            
            lblTotalePartite.setText(String.valueOf(parDAO.getNumeroPartiteDisputate()));
            tabellaVittorie.getItems().setAll(risDAO.getVittoriePerUtente());
            tabellaTempi.getItems().setAll(risDAO.getTempoMedioPerUtente());

        } catch (Exception e) {
            System.err.println("Errore caricamento statistiche: " + e.getMessage());
        }
    }
    
    /**
     * Aggiorna forzatamente le statistiche quando l'amministratore clicca sull'apposito pulsante.
     */
    @FXML
    void aggiornaStatisticheClick() {
        caricaStatisticheDatabase();
    }

    /**
     * Istanzia e avvia il motore principale del Server su un Thread asincrono.
     * Recupera le impostazioni dall'interfaccia (Dizionario, Difficoltà) e le inietta nel ServerManager.
     * * @param event L'evento scatenato dal click sul pulsante "Avvia Ascolto Socket".
     */
    @FXML
    void avviaServer(ActionEvent event) {
        if (serverManager == null) {
            serverManager = new ServerManager();
            
            // 1. Se c'è già una mappa caricata nella tabella, la passiamo al server nascente
            if (!tabellaParole.getItems().isEmpty()) {
                java.util.Map<String, Long> mappaCorrente = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, Long> entry : tabellaParole.getItems()) {
                    mappaCorrente.put(entry.getKey(), entry.getValue());
                }
                serverManager.setDatiSfida(mappaCorrente, testoIntegraleCorrente);
            }
            
            // 2. Applichiamo la difficoltà impostata nella tendina
            if (comboDifficolta != null && comboDifficolta.getValue() != null) {
                serverManager.setDifficoltaCorrente(comboDifficolta.getValue());
            }
            
            // 3. Lanciamo il Server su un Thread di background Daemon
            // (Il Daemon Thread si chiuderà automaticamente quando viene chiusa l'interfaccia JavaFX)
            Thread serverThread = new Thread(() -> {
                serverManager.start();
            });
            serverThread.setDaemon(true); 
            serverThread.start();

            // 4. Aggiorniamo la grafica per segnalare che il server è operativo
            lblServerStatus.setText("In Ascolto...");
            lblServerStatus.setTextFill(javafx.scene.paint.Color.GREEN);
            btnStartServer.setDisable(true); 
        }
    }

    /**
     * Apre una finestra di dialogo di sistema per permettere all'amministratore 
     * di cercare un file .txt dal proprio computer.
     */
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
            
            // Avviamo la logica di calcolo asincrona
            avviaAnalisiTestoAsincrona(fileSelezionato.getAbsolutePath());
        }
    }

    /**
     * Esegue il Task di analisi e conteggio delle parole in background tramite JavaFX Task.
     * Durante il calcolo, la barra di caricamento (ProgressBar) si aggiorna progressivamente.
     * Al termine, i risultati vengono salvati nel file `.dat` di storico e inviati al ServerManager.
     * * @param percorso Il percorso assoluto del file di testo da analizzare.
     */
    private void avviaAnalisiTestoAsincrona(String percorso) {
        Task<Map<String, Long>> taskAnalisi = FileManager.analizzaDocumentoTask(percorso);

        // Rendiamo visibile e colleghiamo la ProgressBar al Task asincrono
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(taskAnalisi.progressProperty());

        // Azioni da eseguire SOLO quando il Task finisce con successo
        taskAnalisi.setOnSucceeded(e -> {
            Map<String, Long> risultato = taskAnalisi.getValue();
            
            // Aggiorniamo la UI con i nuovi dati calcolati
            tabellaParole.getItems().clear();
            tabellaParole.getItems().addAll(risultato.entrySet());
            
            if (ultimoFileCaricato != null) {
                try {
                    String nomeBase = ultimoFileCaricato.getName().replace(".txt", "");
                    String percorsoSalvataggio = cartellaStorico.getPath() + "/" + nomeBase + ".dat";
                    
                    // Leggiamo fisicamente il testo integrale in formato stringa UTF-8
                    String testoIntegrale = new String(Files.readAllBytes(ultimoFileCaricato.toPath()), StandardCharsets.UTF_8);
                    
                    this.testoIntegraleCorrente = testoIntegrale; 
                    
                    // Salviamo il blocco di dati serializzato (Mappa + Testo) in un file .dat
                    FileManager.salvaDizionarioETesto(risultato, testoIntegrale, percorsoSalvataggio);
                    
                    // Inietto i nuovi dati calcolati direttamente nel motore del gioco
                    if (serverManager != null) {
                        serverManager.setDatiSfida(risultato, testoIntegrale);
                    }
                    
                    // Aggiungo il nuovo elemento alla tendina dello storico senza creare doppioni
                    if (!comboStoricoFile.getItems().contains(nomeBase)) {
                        comboStoricoFile.getItems().add(nomeBase);
                    }
                    comboStoricoFile.setValue(nomeBase);
                    lblNomeFile.setText("Analisi completata e salvata: " + nomeBase);
                    
                } catch (Exception ex) {
                    System.err.println("[SERVER] Errore durante il salvataggio con testo: " + ex.getMessage());
                }
            }
            
            // Operazione conclusa, nascondiamo la ProgressBar
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            
            System.out.println("Analisi file completata. Parole trovate: " + risultato.size());
        });

        // Azioni da eseguire in caso di rottura (es. file corrotto o protetto)
        taskAnalisi.setOnFailed(e -> {
            progressBar.setVisible(false);
            lblNomeFile.setText("Errore durante la lettura!");
            System.err.println("Errore FileManager: " + taskAnalisi.getException());
        });

        // Avvio effettivo del thread
        new Thread(taskAnalisi).start();
    }
}