package guesstheword_server.controller;

import guesstheword_server.db.UtenteDAO;
import guesstheword_server.model.Utente;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller per l'interfaccia di Login del Server.
 * Gestisce l'autenticazione dell'amministratore, verificando le credenziali sul database locale
 * e assicurandosi che l'utente abbia i privilegi di "ADMIN" prima di concedere l'accesso alla Dashboard.
 * 
 */
public class ServerLoginController implements Initializable {

    @FXML private TextField txtAdminUser;
    @FXML private PasswordField txtAdminPass;
    @FXML private Label lblErroreAdmin;

    private UtenteDAO utenteDAO;

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX al caricamento della vista.
     * Prepara gli oggetti necessari al controller, in questo caso istanziando il DAO per interrogare il DB.
     *
     * @param url La posizione usata per risolvere percorsi relativi per l'oggetto radice.
     * @param rb Le risorse usate per localizzare l'oggetto radice.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Istanziamo direttamente il DAO locale. Essendo sul Server, 
        // interroghiamo direttamente il database senza passare per i Socket.
        this.utenteDAO = new UtenteDAO();
    }

    /**
     * Gestisce l'evento scatenato dal click sul pulsante di Login.
     * Esegue la validazione dei campi, verifica l'identità e i permessi dell'utente, 
     * e in caso di successo sostituisce la scena corrente con la Dashboard di Amministrazione.
     *
     * @param event L'evento scatenato dall'interazione con l'interfaccia.
     */
    @FXML
    void gestisciLoginAdmin(ActionEvent event) {
        String user = txtAdminUser.getText().trim();
        String pass = txtAdminPass.getText().trim();

        // 1. Validazione base: impedisce l'invio di query vuote
        if (user.isEmpty() || pass.isEmpty()) {
            lblErroreAdmin.setText("Compila tutti i campi!");
            return;
        }

        try {
            // 2. Interroghiamo direttamente il DB locale per recuperare l'utente
            Optional<Utente> utenteLoggato = utenteDAO.login(user, pass);

            if (utenteLoggato.isPresent()) {
                Utente admin = utenteLoggato.get();

                // 3. Controllo di sicurezza critico: l'utente è DAVVERO un ADMIN?
                // Impedisce a un giocatore normale di loggarsi sul pannello di controllo del server.
                if (admin.getRuolo().equalsIgnoreCase("ADMIN")) {
                    lblErroreAdmin.setText("Accesso autorizzato. Avvio dashboard...");
                    
                    // 4. Caricamento dinamico della vista principale (Dashboard) dal file FXML
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_server/view/AdminDashboard.fxml"));
                    Parent dashboardRoot = loader.load();
                    
                    // 5. Cambio Scena (Context Switch): 
                    // Recuperiamo la finestra (Stage) attuale usando uno degli elementi grafici e le assegniamo la nuova Scena.
                    Stage stage = (Stage) txtAdminUser.getScene().getWindow();
                    Scene scenaDashboard = new Scene(dashboardRoot);
                    stage.setScene(scenaDashboard);
                    stage.setTitle("Guess The Word - Console Amministrazione Server");
                    stage.centerOnScreen(); // Ri-centra la finestra che potrebbe aver cambiato dimensioni
                    stage.show();
                    
                } else {
                    // Tentativo di accesso da parte di un utente senza permessi amministrativi
                    lblErroreAdmin.setText("Errore: Questo account non ha i privilegi di Admin.");
                }
            } else {
                // Le credenziali non corrispondono a nessun utente nel DB
                lblErroreAdmin.setText("Username o Password errati.");
            }
            
        } catch (Exception e) {
            // Gestione delle eccezioni per impedire il blocco totale dell'interfaccia in caso di errori SQL
            System.err.println("[SERVER LOGIN] Eccezione durante l'autenticazione: " + e.getMessage());
            e.printStackTrace();
            lblErroreAdmin.setText("Errore critico di connessione al Database.");
        }
    }
}