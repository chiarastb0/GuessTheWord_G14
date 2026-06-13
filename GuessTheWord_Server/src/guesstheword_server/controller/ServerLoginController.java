/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guesstheword_server.controller;

/**
 *
 * @author admin
 */
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

public class ServerLoginController implements Initializable {

    @FXML private TextField txtAdminUser;
    @FXML private PasswordField txtAdminPass;
    @FXML private Label lblErroreAdmin;

    private UtenteDAO utenteDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Istanziamo direttamente il DAO locale (siamo sul server, nessuna socket!)
        this.utenteDAO = new UtenteDAO();
    }

    @FXML
    void gestisciLoginAdmin(ActionEvent event) {
        String user = txtAdminUser.getText().trim();
        String pass = txtAdminPass.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            lblErroreAdmin.setText("⚠️ Compila tutti i campi!");
            return;
        }

        try {
            // Interroghiamo direttamente il DB locale
            Optional<Utente> utenteLoggato = utenteDAO.login(user, pass);

            if (utenteLoggato.isPresent()) {
                Utente admin = utenteLoggato.get();

                // Controllo di sicurezza: verifichiamo che sia DAVVERO un ADMIN
                if (admin.getRuolo().equalsIgnoreCase("ADMIN")) {
                    lblErroreAdmin.setText("Accesso autorizzato. Avvio dashboard...");
                    
                    // Carichiamo la console principale del server (es. ServerDashboardView)
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/guesstheword_server/view/AdminDashboard.fxml"));
                    Parent dashboardRoot = loader.load();
                    
                    // Recuperiamo lo stage attuale e cambiamo scena
                    Stage stage = (Stage) txtAdminUser.getScene().getWindow();
                    Scene scenaDashboard = new Scene(dashboardRoot);
                    stage.setScene(scenaDashboard);
                    stage.setTitle("Guess The Word - Console Amministrazione Server");
                    stage.centerOnScreen();
                    stage.show();
                    
                } else {
                    // Se un normale player tenta di loggarsi dall'eseguibile server, lo blocchiamo!
                    lblErroreAdmin.setText("Errore: Questo account non ha i privilegi di Admin.");
                }
            } else {
                lblErroreAdmin.setText("❌ Username o Password errati.");
            }
            
        } catch (Exception e) {
            System.err.println("[SERVER LOGIN] Eccezione durante l'autenticazione: " + e.getMessage());
            e.printStackTrace();
            lblErroreAdmin.setText("Errore critico di connessione al Database.");
        }
    }
}