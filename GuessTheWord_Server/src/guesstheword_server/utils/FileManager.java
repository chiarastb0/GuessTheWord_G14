package guesstheword_server.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.concurrent.Task;

/**
 * Classe di utilità per la gestione dei file di testo e la serializzazione.
 * Si occupa di leggere i documenti, contare le frequenze delle parole in modo asincrono
 * e salvare/caricare i dati elaborati in formato binario.
 * 
 */
public class FileManager {

    /**
     * Legge un file di testo in background e calcola la frequenza assoluta delle singole parole.
     * Utilizza un Task di JavaFX per garantire l'esecuzione asincrona e non bloccare l'interfaccia grafica.
     * * @param percorsoFile Il percorso del file di testo (.txt) da analizzare.
     * @return Un Task che, una volta completato, restituirà una mappa contenente le parole e la loro frequenza.
     */
    public static Task<Map<String, Long>> analizzaDocumentoTask(String percorsoFile) {
        
        return new Task<Map<String, Long>>() {
            @Override
            protected Map<String, Long> call() throws Exception {
                Path path = Paths.get(percorsoFile);

                // Utilizzo delle Stream API per un'elaborazione funzionale ed efficiente del testo
                return Files.lines(path)
                        // 1. Divide ogni riga in parole singole (ignorando la punteggiatura)
                        .flatMap(linea -> Arrays.stream(linea.split("\\W+")))
                        // 2. Rimuove gli spazi vuoti e le parole troppo corte (es. articoli, preposizioni)
                        .filter(parola -> parola.length() > 3)
                        // 3. Converte tutto in minuscolo per uniformare il conteggio
                        .map(String::toLowerCase)
                        // 4. Raggruppa le parole uguali e le conta
                        .collect(Collectors.groupingBy(parola -> parola, Collectors.counting()));
            }
        };
    }
    
    /**
     * Salva sia la mappa delle frequenze che il testo integrale in un unico file binario (.dat).
     * Questo permette di ricaricare le sfide in futuro senza dover rianalizzare il file di testo.
     * * @param dizionario La mappa contenente le parole e le relative frequenze.
     * @param testoIntegrale Il contenuto testuale grezzo originale.
     * @param percorsoSalvataggio Il percorso e il nome del file binario di destinazione.
     * @throws IOException Se si verifica un errore durante la scrittura del file.
     */
    public static void salvaDizionarioETesto(Map<String, Long> dizionario, String testoIntegrale, String percorsoSalvataggio) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(percorsoSalvataggio))) {
            oos.writeObject(dizionario);      // Primo oggetto scritto nel flusso
            oos.writeObject(testoIntegrale);  // Secondo oggetto scritto nel flusso
        }
    }
    
    /**
     * Carica dal file binario sia la mappa delle frequenze che il testo completo.
     * I dati vengono estratti nello stesso esatto ordine in cui sono stati scritti.
     * * @param percorsoSalvataggio Il percorso del file binario (.dat) da caricare.
     * @return Un array di Object dove l'indice 0 contiene la Mappa e l'indice 1 contiene il Testo.
     * @throws IOException Se si verifica un errore di lettura del file.
     * @throws ClassNotFoundException Se le classi serializzate non corrispondono a quelle attuali.
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