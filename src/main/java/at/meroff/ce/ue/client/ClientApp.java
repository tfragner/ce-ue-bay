package at.meroff.ce.ue.client;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import at.jku.ce.bay.api.FilesFound;
import at.jku.ce.bay.api.GetFile;
import at.jku.ce.bay.api.GetFileNames;
import at.jku.ce.bay.utils.CEBayHelper;
import com.typesafe.config.ConfigFactory;
import org.jboss.netty.channel.ChannelException;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * Created by fragner on 14.01.17.
 */
public class ClientApp {

    /**
     * Variable für den Zugriff auf Command Line Eingaben
     */
    private final static Scanner cmdInput = new Scanner(System.in);

    /**
     * Standardverzeichnis für Downloads
     */
    private final static String DEFAULT_FILE_DIRECTORY = "./downloads";

    /**
     * Standardname für das Actor System
     */
    private final static String SYSTEM_NAME = "client139System";

    /**
     * Standard Timeout für synchrone Abfragen
     */
    private final static Timeout DEFAULT_TIMEOUT = new Timeout(Duration.create(5, "seconds"));

    /**
     * Pfad zum Downloadverzeichnis
     */
    private Path fileDirectory = null;

    /**
     * Actor System für die Anwendung
     */
    private ActorSystem clientActorSystem;
    private ActorSelection CEBayRef;

    /**
     * Actor Referenz für den Download Manager
     */
    ActorRef manager;

    /**
     Methode zum Initialisieren der Anwendung.
     * Es wird die Methode {@link #init(Path) init} aufgerufen. Als Parameter wird das Standardverzeichnis
     * für Downloads übergeben
     * @return Status der Durchführung
     */
    public boolean init() {
        return init(Paths.get(DEFAULT_FILE_DIRECTORY));

    }

    /**
     * Methode zum Initialisieren der Anwendung
     * <ul>
     * <li>Überprüfen des Verzeichnis</li>
     * <li>Starten des Actor Systems</li>
     * <li>Starten des Download Managers</li>
     * </ul>
     * @param fileDirectory Pfad zum Downloadverzeichnis
     * @return Status der Durchführung
     */
    public boolean init(Path fileDirectory) {
        try {
            checkDataDirectory(fileDirectory);
            startActorSystem();
            startManager();
            return true;
        } catch (IOException e) {
            System.out.println("Downloadverzeichnis konnte nicht erzeugt werden!");
            System.out.println("Error: " + e.toString());
        } catch (ChannelException e) {
            System.out.println("Das Actor System konnte nicht gestartet werden!");
            System.out.println("Error: " + e.toString());
        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
        }
        return false;
    }

    /**
     * Starten des Actorsystem und Erzeugen einer Referenz zur CEBay
     * @throws Exception Fehler bei Starten des Actor Systems
     */
    private void startActorSystem() throws Exception {
        clientActorSystem = ActorSystem.create(SYSTEM_NAME, ConfigFactory.load("client"));
        CEBayRef = clientActorSystem.actorSelection(CEBayHelper.GetRegistryActorRef());
        System.out.println("Actor System gestartet...");
    }

    /**
     * Stoppen des Actor Sytems und aller Aktoren
     */
    private void stopActorSystem() {
        Future<Terminated> terminate = clientActorSystem.terminate();
        try {
            Await.result(terminate, Duration.create(5, "seconds"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starten des Download Management Actors. Dem Actor wird beim Starten der Verzeichnispfad übergeben.
     * @throws Exception Fehler beim Starten des Management Actors
     */
    private void startManager() throws Exception {
        manager = clientActorSystem.actorOf(Props.create(ActorClientManager.class), "manage139Actor");
        try {
            Future<Object> ask = Patterns.ask(manager, new ActorClientManager.Initialize(fileDirectory), DEFAULT_TIMEOUT);
            Await.result(ask, DEFAULT_TIMEOUT.duration());
        } catch (TimeoutException e) {
            System.out.println("Timeout: Management Actor konnte nicht gestartet werden");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Actor Manager gestartet...");
    }

    /**
     *  Überprüfen des Datenverzeichnisses. Falls es nicht existiert wird versucht es zu erzeugen.
     * @param fileDirectory Pfad zum Datenverzeichnis
     * @throws IOException Fehler beim Prüfen bzw. Erstellen des Datenverzeichnisses
     */
    private void checkDataDirectory(Path fileDirectory) throws IOException {
        if (!Files.isDirectory(fileDirectory) && !Files.isRegularFile(fileDirectory))
            Files.createDirectory(fileDirectory);
        this.fileDirectory = fileDirectory;
        System.out.println("Datenverzeichnis: " + fileDirectory.toAbsolutePath());

    }

    /**
     * Shows the main menu for the application (command line)
     */
    private void commandLineInterface() {

        System.out.print("print list [p] | get status [s] | exit[x]: ");
        String input = cmdInput.next();

        while (!input.equals("x")) {
            if (input.equals("p")) {
                printFileList();
            } else if (input.equals("s")) {
                showStatus();
            }

            System.out.print("print list [p] | get status [s] | exit[x]: ");
            input = cmdInput.next();
        }

        // Stoppen des Actor Systems
        stopActorSystem();

    }

    /**
     * Ausgabe der CEBay Dateiliste und Auswahl von Dateien zum Download
     */
    private void printFileList() {
        // Liste der CEBay Dateien
        List<String> fileList = new ArrayList<>();

        try {
            // Abfrage aller Dateien die auf der CEBay gelistet sind
            Future<Object> ask = Patterns.ask(CEBayRef, new GetFileNames(), DEFAULT_TIMEOUT);
            Object ret = Await.result(ask, DEFAULT_TIMEOUT.duration());
            if (ret instanceof FilesFound) {
                int counter = 0;
                for (String s : ((FilesFound) ret).fileNames()) {
                    System.out.println(counter + "\t" + s);
                    fileList.add(s); // Liste befüllen
                    counter++;
                }
            }
            System.out.print("download file [#] | download all files [a] | return[x]: ");
            String input = cmdInput.next();

            while (!input.equals("x")) {
                if (input.equals("a")) {
                    for (String s : fileList) {
                        manager.tell(new GetFile(s), ActorRef.noSender());
                    }
                    return;
                } else {
                    // check for valid number
                    try {
                        if (Integer.parseInt(input) >= 0 && Integer.parseInt(input) < fileList.size()) {
                            manager.tell(new GetFile(fileList.get(Integer.parseInt(input))), ActorRef.noSender());
                        }
                        return;
                    } catch(NumberFormatException | NullPointerException e) {
                        System.out.println("Beim Download ist etwas schief gegagngen");
                    }


                }

                System.out.print("select file [#] | return[x]: ");
                input = cmdInput.next();
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout: Cannot retrieve seeder status.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Anzeigen der Downloads inkl. Status
     */
    private void showStatus() {
        try {
            Future<Object> ask = Patterns.ask(manager, new ActorClientManager.downloaderGetStatus(), DEFAULT_TIMEOUT);
            Object ret = Await.result(ask, DEFAULT_TIMEOUT.duration());
            if (ret instanceof ActorClientManager.downloaderStatusRetrieved) {
                System.out.println(ret.toString());
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout: Downloadstatus konnte nicht abgefragt werden");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientApp clientApp = new ClientApp();
        if (clientApp.init()) {
            clientApp.commandLineInterface();
        } else {
            System.exit(0);
        }
    }



}
