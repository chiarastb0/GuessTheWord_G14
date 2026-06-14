package guesstheword_server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Classe principale (Main) che avvia l'applicazione Server con interfaccia grafica JavaFX.
 * Inizializza la prima schermata di autenticazione per l'amministratore.
 * 
 */
public class GuessTheWord_Server extends Application {
    
    /**
     * Metodo di partenza di JavaFX che prepara e mostra la finestra principale.
     * * @param stage La finestra principale (Stage) fornita dal framework JavaFX.
     * @throws Exception Se si verificano errori nel caricamento o nel parsing del file FXML.
     */
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/guesstheword_server/view/ServerLoginView.fxml"));
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.setTitle("GTW Server - Autenticazione");
        stage.show();
    }

    /**
     * Metodo main standard di Java che lancia l'applicazione.
     * * @param args Gli argomenti passati da riga di comando all'avvio.
     */
    public static void main(String[] args) {
        launch(args);   
    }
    
}
