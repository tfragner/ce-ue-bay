package at.jku.ce.bay.app;

import akka.actor.ActorRef;
import at.jku.ce.bay.utils.CEBayHelper;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class App {

    public static void main(String[] args) {

        //liefert den für 'Publish' benötigten Hashwert
        try {
            CEBayHelper.GetHash(new File(""));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        //Liefert die Referenz zum CEBay Actor, der vom CE Inst. bereitgestellt wird
        CEBayHelper.GetRegistryActorRef();

        //Wird für die Umwandlung einer ActorRef in die zu versendende Stringrepräsentation benötigt
        CEBayHelper.GetRemoteActorRef(ActorRef.noSender());

    }

}
