package at.meroff.ce.ue.server;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import at.jku.ce.bay.api.*;
import at.jku.ce.bay.utils.CEBayHelper;
import at.meroff.ce.ue.api.SeederFile;


/**
 * Created by fragner on 11.01.17.
 * Klasse des Seeder Manager. Diese Klasse verwaltet die Seeds
 */
public class ActorSeeder extends UntypedActor {

    /**
     * Datei die vom Seeder angeboten wird
     */
    SeederFile file;
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof SeederFile) {
            // Datei veröffentlichen
            file = (SeederFile) message;
            ActorSelection CEBayRef = getContext().actorSelection(CEBayHelper.GetRegistryActorRef());
            CEBayRef.tell(new Publish(((SeederFile) message).getFilename(),((SeederFile) message).getHash(),CEBayHelper.GetRemoteActorRef(getSelf())),getSelf());
        } else if (message instanceof GetFile) {
            // Downloadanfrage von Clients

            // Prüfung ob die angefragte Datei von diesem Seeder gehostet wird
            if (((GetFile) message).name().equals(file.getFilename())) {
                getSender().tell(new FileRetrieved(file.getData()),getSelf());
            } else {
                getSender().tell(new FileNotFound(file.getFilename()),getSelf());
            }
        } else if (message instanceof GetStatus) {
            // Statusanfragen von Clients oder CEBay
            getSender().tell(new StatusRetrieved(),getSelf());
        } else {
            System.out.println(message.getClass().toString());
            unhandled(message);
        }
    }
}
