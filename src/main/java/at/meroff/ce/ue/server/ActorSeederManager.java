package at.meroff.ce.ue.server;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import at.jku.ce.bay.utils.CEBayHelper;
import at.meroff.ce.ue.api.SeederFile;
import at.meroff.ce.ue.api.UploadFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fragner on 12.01.17.
 *
 * <p>Der Management Actor dient zur Verwaltung der bereitzustellenden Dateien. Beim Initialisieren benötigt
 * Der Actor einen Verzeichnispfad in dem sich die bereitzustellenden Dateien befinden</p>
 *
 * <p><b>Aufgaben</b></p>
 * <ul>
 *     <li>Initiales bereitstellen aller verfügbaren Dateien (Seeder Actor erstellen)</li>
 *     <li>Verarbeiten von Uploads und anschließendes Erstellen eines Seed Actors</li>
 * </ul>
 */
public class ActorSeederManager extends UntypedActor {

    /**
     * Nachricht zum Initialisieren des Actors. Diese Nachricht muss den Pfad zu einem gültigen Verzeichnis
     * für die bereitzustellenden Dateien enthalten
     */
    public static class Initialize{
        /**
         * Verzeichnis in dem sich die bereitzustellenden Dateien befinden
         */
        Path fileDirectory;

        /**
         * Standard Konstruktor für die Methode
         * @param fileDirectory Pfad zum Datenverzeichnis
         */
        public Initialize(Path fileDirectory) {
            this.fileDirectory = fileDirectory;
        }

        /**
         * Getter für das Datenverzeichnis
         * @return Pfad zum Datenverzeichnis
         */
        public Path getFileDirectory() {
            return fileDirectory;
        }
    }

    /**
     * Nachricht zum Abrufen des Status
     */
    public static class seederGetStatus{}

    /**
     * Nachricht wird als Antwort auf eine Statusabfrage gesendet.
     */
    public static class seederStatusRetrieved{
        /**
         * HashMap mit Status der Seeder
         */
        HashMap<String,SeederFile> status = new HashMap<>();

        /**
         * Standard Konstruktur für die Nachricht.
         * @param status HashMap mit Dateiname und zugehöriger Actor Ref
         */
        seederStatusRetrieved(HashMap<String, SeederFile> status) {
            this.status = status;
        }

        /**
         * Ausgabe der aktuell bereitgestellten Dateien und der dazugehörigen Referenz
         * @return Status der aktuellen Seeds
         */
        @Override
        public String toString() {
            String ret = "";
            for (Map.Entry<String, SeederFile> entry : status.entrySet())
            {
                ret += entry.getKey() + ": " + "\n";
                ret += "\t" + entry.getValue().toString() + "\n";
            }
            return ret;
        }
    }

    /**
     * Pfad zum Verzeichnis das die bereitzustellenden Dateien beinhaltet.
     */
    private Path fileDirectory;

    /**
     * Informationen über bereitgestellte Dateien
     */
    private static final HashMap<String, SeederFile> files = new HashMap<>();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Initialize) {
            // Initialisieren des Manager
            // Setzt den Pfad zu Dateiverzeichnis
            fileDirectory = ((Initialize) message).getFileDirectory();

            // Erzeugen eines ActorSeeder für jede Datei im Verzeichnis
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileDirectory)) {
                for (Path file : stream) {
                    SeederFile fileToSeed = new SeederFile(file);
                    // Seeder für Datei erstellen
                    getSelf().tell(fileToSeed, getSelf());
                    getSender().tell("done", getSelf());
                }
            } catch (IOException | DirectoryIteratorException x) {
                getSender().tell("failure", getSelf());
                x.printStackTrace();
            }
        } else if (message instanceof UploadFile) {
            // eine Datei soll hochgeladen werden
            Path newFile = Paths.get(fileDirectory.toString(), ((UploadFile) message).getFilename());
            try {
                FileOutputStream fos = new FileOutputStream(newFile.toFile());
                fos.write(((UploadFile) message).getData());
                fos.close();
                getSelf().tell(new SeederFile(newFile),getSelf());
                getSender().tell("done", getSelf());
            } catch (IOException e) {
                getSender().tell("failure", getSelf());
            }
        } else if (message instanceof SeederFile) {
            // Erzeugen eines Seeders
            ActorRef seeder = getContext().actorOf(Props.create(ActorSeeder.class), ((SeederFile) message).getFilename());
            ((SeederFile) message).setSeederRef(CEBayHelper.GetRemoteActorRef(seeder));
            files.put(((SeederFile) message).getFilename(), (SeederFile) message);
            seeder.tell(message, getSelf());
        } else if (message instanceof seederGetStatus) {
            getSender().tell(new seederStatusRetrieved(files),getSelf());
        }
    }
}
