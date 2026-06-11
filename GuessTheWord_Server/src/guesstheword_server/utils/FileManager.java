/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package guesstheword_server.utils;

/**
 *
 * @author angel
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class FileManager {

    /**
     * Legge un file di testo in background e calcola la frequenza delle parole.
     * Utilizza un Task di JavaFX per garantire l'esecuzione asincrona.
     */
    public static Task<Map<String, Long>> analizzaDocumentoTask(String percorsoFile) {
        
        return new Task<Map<String, Long>>() {
            @Override
            protected Map<String, Long> call() throws Exception {
                Path path = Paths.get(percorsoFile);

                // Stream API in una singola catena di operazioni
                return Files.lines(path)
                        // 1. Dividi ogni riga in parole singole (ignorando la punteggiatura)
                        .flatMap(linea -> Arrays.stream(linea.split("\\W+")))
                        // 2. Rimuovi gli spazi vuoti e le parole troppo corte (es. preposizioni)
                        .filter(parola -> parola.length() > 3)
                        // 3. Converti tutto in minuscolo per uniformare il conteggio
                        .map(String::toLowerCase)
                        // 4. Raggruppa le parole uguali e contale
                        .collect(Collectors.groupingBy(parola -> parola, Collectors.counting()));
            }
        };
    }
    
    /**
    * Salva sia la mappa delle frequenze che il testo integrale in un unico file binario.
    */
    public static void salvaDizionarioETesto(Map<String, Long> dizionario, String testoIntegrale, String percorsoSalvataggio) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(percorsoSalvataggio))) {
            oos.writeObject(dizionario);     // Primo oggetto scritto
            oos.writeObject(testoIntegrale);  // Secondo oggetto scritto
        }
    }
    
    /**
    * Carica dal file binario sia la mappa che il testo completo restituendoli come array di Object.
    */
   @SuppressWarnings("unchecked")
   public static Object[] caricaDizionarioETesto(String percorsoSalvataggio) throws IOException, ClassNotFoundException {
       try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(percorsoSalvataggio))) {
           Map<String, Long> mappa = (Map<String, Long>) ois.readObject();
           String testo = (String) ois.readObject();
           return new Object[]{mappa, testo};
       }
   }
}