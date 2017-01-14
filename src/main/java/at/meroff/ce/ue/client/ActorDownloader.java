package at.meroff.ce.ue.client;

import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import at.jku.ce.bay.api.*;
import at.jku.ce.bay.utils.CEBayHelper;
import scala.concurrent.duration.Duration;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by fragner on 14.01.17.
 *
 * <p>Download Actor für gleichzeitigen Download von mehreren Dateien</p>
 */
public class ActorDownloader extends UntypedActor {

    // Alternative zum Erzuegen von Aktoren anstatt von Initialize
    static Props props(final Path downloadPath) {
        return Props.create(new Creator<ActorDownloader>() {
            private static final long serialVersionUID = 1L;

            @Override
            public ActorDownloader create() throws Exception {
                return new ActorDownloader(downloadPath);
            }
        });
    }

    /**
     * Timeout für Dateien die keine aktiven Seeder haben
     */
    private final Cancellable tick = getContext().system().scheduler().schedule(
            Duration.create(90, TimeUnit.SECONDS),
            Duration.create(90, TimeUnit.SECONDS),
            getSelf(), "tick", getContext().dispatcher(), null);

    /**
     * Link zur CEBay
     */
    private ActorSelection CEBayRef = getContext().actorSelection(CEBayHelper.GetRegistryActorRef());

    /**
     * Dateiname der Datei die gleaden wird
     */
    private String filename;

    /**
     * gibt an ob bei einer Anfrage eine Anwort gekommen ist
     */
    private boolean foundSeeder = false;

    /**
     * Datei unter der der Download gespeichert werden soll
     */
    final Path downloadPath;

    /**
     * Konstruktor in Verbindung mit Props
     * @param downloadPath Pfad zum Downloadverzeichnis
     */
    public ActorDownloader(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof GetFile) {
            // Downloadanfrage
            filename = ((GetFile) message).name();
            CEBayRef.tell(new FindFile(filename), getSelf());
        } else if (message instanceof SeederFound) {
            // Wenn Seeder gefunden wurden wird an alle ein Anfrage gesendet
            Iterator<String> iterator = ((SeederFound) message).seeder().iterator();
            while (iterator.hasNext()) {
                String seeder = iterator.next();
                ActorSelection seederRef = getContext().actorSelection(seeder);
                seederRef.tell(new GetStatus(), getSelf());
            }

        } else if (message instanceof StatusRetrieved) {
            // Eine Antwort auf eine Anfrage ist eingetroffen
            if (!foundSeeder) {
                foundSeeder = true;
                // Downloadanfrage an Seeder schicken
                getSender().tell(new GetFile(filename), getSelf());
                getContext().parent().tell(new ActorClientManager.UpdateStatus("downloading"),getSelf());
            }
        } else if (message instanceof FileRetrieved) {
            // Datei Download abgeschlossen
            tick.cancel(); // Stoppen des 'Unreachable' Timers

            // Speichern der Datei
            byte[] data = ((FileRetrieved) message).data();
            Path filepath = Paths.get(downloadPath.toString(), filename);
            FileOutputStream fos = new FileOutputStream(filepath.toFile());
            fos.write(data);
            fos.close();
            getContext().parent().tell(new ActorClientManager.UpdateStatus("finished"),getSelf());
        }  else if (message instanceof FileNotFound || (message.equals("tick") && !foundSeeder)) {
            getContext().parent().tell(new ActorClientManager.UpdateStatus("failed"), getSelf());
        }
    }
}
