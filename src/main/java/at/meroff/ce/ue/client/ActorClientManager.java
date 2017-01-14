package at.meroff.ce.ue.client;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import at.jku.ce.bay.api.GetFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fragner on 14.01.17.
 *
 * <p>Der Download Manager verwaltet die Downloads</p>
 */
public class ActorClientManager extends UntypedActor {

    /**
     * Nachricht zum Initialisieren des Actors. Diese Nachricht muss den Pfad zu einem gültigen Verzeichnis
     * für die Downloads enthalten
     */
    public static class Initialize{
        /**
         * Verzeichnis zum Downloadverzeichnis
         */
        Path fileDirectory;

        /**
         * Standard Konstruktor für die Methode
         * @param fileDirectory Pfad zum Downloadverzeichnis
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
     * NAchricht zum Abrufen des Status
     */
    public static class downloaderGetStatus{}


    /**
     * Nachricht wird als Antwort auf eine Statusabfrage gesendet.
     */
    public static class downloaderStatusRetrieved{
        /**
         * HashMap mit Downloads und detaillierten Informationen
         */
        HashMap<ActorRef,HashMap<String,String>> status = new HashMap<>();

        /**
         * Standard Konstruktur für die Nachricht.
         * @param status HashMap mit Dateiname und zugehöriger Actor Ref
         */
        public downloaderStatusRetrieved(HashMap<ActorRef,HashMap<String,String>> status) {
            this.status = status;
        }

        /**
         * Ausgabe der Downloads
         * @return Status der aktuellen Seeds
         */
        @Override
        public String toString() {
            String ret = "";
            for (Map.Entry<ActorRef, HashMap<String,String>> entry : status.entrySet()) {
                HashMap<String,String> value = entry.getValue();
                for (Map.Entry<String, String> entry2 : value.entrySet()) {
                    Object value2 = entry2.getValue();
                    ret += value2 + "\t\t";
                }
                ret += "\n";
            }
            return ret;
        }
    }

    /**
     * Update Nachricht für Status von Downloads
     */
    static class UpdateStatus {
        /**
         * Status des Downloads
         */
        String status;

        /**
         * Standard Konstruktur für Status Updates
         * @param status
         */
        UpdateStatus(String status) {
            this.status = status;
        }

        /**
         * Getter für den Status
         * @return
         */
        String getStatus() {
            return status;
        }
    }

    /**
     * Pfad zum Downloadverzeichnis
     */
    private Path fileDirectory;

    /**
     * Informationen zu Downloads
     */
    private static final HashMap<ActorRef,HashMap<String,String>> downloadList = new HashMap<>();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Initialize) {

            fileDirectory = ((Initialize) message).getFileDirectory();
            getSender().tell("done", getSelf());
        } else if (message instanceof GetFile) {
            // Download von Datei
            GetFile m = (GetFile) message;
            String filename = m.name();

            // Startet Actor für den Download
            if (!getContext().child(filename).isDefined()) {
                ActorRef downloader = getContext()
                        .actorOf(ActorDownloader.props(fileDirectory), m.name());
                downloader.tell(m, getSelf());
                // Eintragen in der Downloadliste
                downloadList.put(downloader,new HashMap<>());
                downloadList.get(downloader).put("filename", filename);
                downloadList.get(downloader).put("status", "waiting");
            }
        } else if (message instanceof downloaderGetStatus) {
            // Info zu allen Downloads
            getSender().tell(new downloaderStatusRetrieved(downloadList),getSelf());
        } else if (message instanceof UpdateStatus) {
            // Update von Seedern
            downloadList.get(getSender()).replace("status",((UpdateStatus) message).getStatus());
            if (((UpdateStatus) message).getStatus().equals("finished") ||
                    ((UpdateStatus) message).getStatus().equals("failed")) {
                getSender().tell(PoisonPill.getInstance(),getSelf());
            }
        }

    }
}
