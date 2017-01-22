package at.meroff.ce.ue.server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import org.jboss.netty.channel.ChannelException;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * Created by fragner on 12.01.17.
 *
 * <p>Die Klasse ServerApp stellt ein Command Line Interface für die Klasse ActorSeederManager zur Verfügung.
 * Aufgaben des Interfaces</p>
 * <ul>
 *     <li>Command Line Input für das Menü</li>
 *     <li>Bereitstellen des Actor Systems</li>
 *     <li>Erstellen des Seeder Managers</li>
 *     <li>Übergabe von Umgebungsparametern an den Manager</li>
 *     <li>Pfad zum Verzeichnis mit den bereitzustellenden Dateien</li>
 *     <li>Ausgabe des Status aller durch den Manager verwalteten Seeds</li>
 *     <li>Abfrage der Dateien die von der CEBay bereitgestellt werden</li>
 * </ul>
 */
public class ServerApp {

    /**
     * Variable für den Zugriff auf Command Line Eingaben
     */
    private final static Scanner cmdInput = new Scanner(System.in);

    /**
     * Standardverzeichnis für bereitzustellende Dateien
     */
    private final static String DEFAULT_FILE_DIRECTORY = "./files";

    /**
     * Standardname für das Actor System
     */
    private final static String SYSTEM_NAME = "seeder139System";

    /**
     * Standard Timeout für synchrone Abfragen
     */
    private final static Timeout DEFAULT_TIMEOUT = new Timeout(Duration.create(5, "seconds"));

    /**
     * Pfad zum Verzeichnis der bereitzustellenden Dateien
     */
    private Path fileDirectory = null;

    /**
     * Actor System für die Anwendung
     */
    private ActorSystem serverActorSystem;

    /**
     * Actor Referenz für den Seed Manager
     */
    private ActorRef manager;

    /**
     * Methode zum Initialisieren der Anwendung.
     * Es wird die Methode {@link #init(Path) init} aufgerufen. Als Parameter wird das Standardverzeichnis
     * für bereitzustellende Dateien übergeben.
     * @return Status der Durchführung
     */
     public boolean init() {
        return init(Paths.get(DEFAULT_FILE_DIRECTORY));

    }

    /**
     * Methode zum Initialisieren der Anwendung
     * <ul>
     * <li>Überprüfung des Verzeichnis</li>
     * <li>Starten des Actor Systems</li>
     * <li>Starten des Management Actors</li>
     * </ul>
     * @param fileDirectory Pfad zum Datenverzeichnis
     * @return Status der Durchführung
     */
    public boolean init(Path fileDirectory) {
        try {
            checkDataDirectory(fileDirectory);
            startActorSystem();
            startManager();
            return true;
        } catch (IOException e) {
            System.out.println("Datenverzeichnis konnte nicht erzeugt werden!");
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
     * Starten des Actor Systems
     * @throws Exception Fehler beim Starten des Actor Systems
     */
    private void startActorSystem() throws Exception {
        serverActorSystem = ActorSystem.create(SYSTEM_NAME, ConfigFactory.load("server"));
        System.out.println("Actor System gestartet...");
    }


    /**
     * Stoppen des Actor Systems und aller Aktoren
     */
    private void stopActorSystem() {
        Future<Terminated> terminate = serverActorSystem.terminate();
        try {
            Await.result(terminate, Duration.create(5, "seconds"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starten des Management Actors. Dem Actor wird beim Starten der Verzeichnispfad übergeben.
     * @throws Exception Fehler beim Starten des Management Actors
     */
    private void startManager() throws Exception {
        manager = serverActorSystem.actorOf(Props.create(ActorSeederManager.class), "manage139Actor");
        try {
            Future<Object> ask = Patterns.ask(manager, new ActorSeederManager.Initialize(fileDirectory), DEFAULT_TIMEOUT);
            Await.result(ask, DEFAULT_TIMEOUT.duration());
        } catch (TimeoutException e) {
            System.out.println("Timeout: Management Actor konnte nicht gestartet werden");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Actor Manager gestartet...");
    }

    /**
     * Überprüfen des Datenverzeichnisses. Falls es nicht existiert wird versucht es zu erzeugen.
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
     * Ausgabe des Command Line Interfaces
     */
    public void commandLineInterface() {

        System.out.print("get status [s] | exit[x]: ");
        String input = cmdInput.next();

        while (!input.equals("x")) {
            if (input.equals("s")) {
                showStatus();
            }

            System.out.print("get status [s] | exit[x]: ");
            input = cmdInput.next();
        }

        // Stoppen des Actor Systems
        stopActorSystem();

    }

    /**
     * Anzeigen der aktuell bereitgestellten Dateien
     */
    private void showStatus() {
        try {
            Future<Object> ask = Patterns.ask(manager, new ActorSeederManager.SeederGetStatus(), DEFAULT_TIMEOUT);
            Object ret = Await.result(ask, DEFAULT_TIMEOUT.duration());
            if (ret instanceof ActorSeederManager.SeederStatusRetrieved) {
                System.out.println(ret.toString());
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout: Seeder Status konnte nicht abgerufen werden!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Starten aller Komponenten des Seeders und des Command Line Interfaces
     * @param args Parameter von der Kommandozeile
     */
    public static void main(String[] args) {
        ServerApp serverApp = new ServerApp();
        if (serverApp.init()) {
            serverApp.commandLineInterface();
        } else {
            System.exit(0);
        }
    }

}
