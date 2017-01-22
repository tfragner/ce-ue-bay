package at.meroff.ce.ue.minitest;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import at.jku.ce.bay.api.*;
import at.jku.ce.bay.utils.CEBayHelper;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Created by fragner on 20.01.17.
 */
public class MiniTest {

    private static final ActorSystem actorSystem = ActorSystem.create("MiniTest", ConfigFactory.load("minitest"));
    private static final ActorSelection CEBayRef = actorSystem.actorSelection(CEBayHelper.GetRegistryActorRef());
    private final static Timeout DEFAULT_TIMEOUT = new Timeout(Duration.create(1, "seconds"));
    private final static Timeout DEFAULT_TIMEOUT2 = new Timeout(Duration.create(20, "seconds"));


    public static void main(String[] args) {
        Future<Object> ceabfrage = Patterns.ask(CEBayRef, new GetFileNames() , DEFAULT_TIMEOUT);
        FilesFound ceObject = null;
        try {
            ceObject = (FilesFound) Await.result(ceabfrage, DEFAULT_TIMEOUT.duration());
        } catch (Exception e) {
            System.out.println("CEBay konnte nicht abgefragt werden");
            System.exit(0);
        }

        for (String filename : ceObject.fileNames()) {
            System.out.println("Abfrage für Datei: " + filename);
            Future<Object> ask = Patterns.ask(CEBayRef, new FindFile(filename) , DEFAULT_TIMEOUT);

            Object foundObject;
            try {
                foundObject = Await.result(ask, DEFAULT_TIMEOUT.duration());
                if (foundObject instanceof SeederFound) {
                    SeederFound o1 = (SeederFound) foundObject;
                    for (String s : o1.seeder()) {

                        ActorSelection seeder = actorSystem.actorSelection(s);
                        Future<Object> ask2 = Patterns.ask(seeder, new GetStatus(), DEFAULT_TIMEOUT);
                        try {
                            Object foundObject2 = Await.result(ask2,DEFAULT_TIMEOUT.duration());
                            if (foundObject2 instanceof StatusRetrieved) {
//                                System.out.println("Status bekommen");
                                Future<Object> ask3 = Patterns.ask(seeder, new GetFile(filename), DEFAULT_TIMEOUT2);
                                Object foundObject3 = Await.result(ask3,DEFAULT_TIMEOUT2.duration());
                                if (foundObject3 instanceof FileRetrieved) {
                                    System.out.print(" - " + s + ": ");
                                    System.out.println(((FileRetrieved) foundObject3).data().toString());
                                } else {
//                                    System.out.println("keine Datei bekommen");
                                }
                            } else {
//                                System.out.println("kein Status bekommen");
                            }
                        } catch (Exception e) {
//                            System.out.println("Es wurde kein Status zurückgesendet");
                        }
                    }
                } else {
                    System.out.println(foundObject.getClass().toString());
                }
            } catch (Exception e) {
                System.out.println("Es konnten keine Seeder gefunden werden");
            }

            System.out.println();
        }




    }

}
