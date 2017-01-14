package at.meroff.ce.ue.api;

import at.jku.ce.bay.utils.CEBayHelper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * Created by fragner on 14.01.17.
 * Klasse die bereitzustellen Dateien repräsentiert
 */
public class SeederFile implements Serializable {
    /**
     * Pfad zur Datei
     */
    private Path path;

    /**
     * Dateiname
     */
    private String filename;

    /**
     * Hash der Datei
     */
    private String hash;

    /**
     * Actor Ref als String
     */
    private String seederRef;

    /**
     * Konstruktor ohne Actor Ref
     * @param path Pfad zur Datei
     */
    public SeederFile(Path path) {
        this(path,"");
    }

    /**
     * Konstruktor mit Actor Ref
     * @param path Pfad zur Datei
     * @param seederRef Actor Ref
     */
    public SeederFile(Path path, String seederRef) {
        this.path = path;
        this.seederRef = seederRef;
        this.filename = path.toFile().getName();
        try {
            hash = CEBayHelper.GetHash(path.toFile());
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Getter für Pfad
     * @return Pfad zur Datei
     */
    public Path getPath() {
        return path;
    }

    /**
     * Getter für Dateinamen
     * @return Dateiname
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Getter für Hashwert der Datei
     * @return Hashwert
     */
    public String getHash() {
        return hash;
    }

    /**
     * Getter für Daten als Byte[]
     * @return Daten
     * @throws IOException Fehler beim Einlesen der Datei
     */
    public byte[] getData() throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Setter für Actor Ref
     * @param seederRef String Abbildung der Actor Ref
     */
    public void setSeederRef(String seederRef) {
        this.seederRef = seederRef;
    }

    /**
     * Getter für Actor Ref String
     * @return Actor Ref
     */
    public String getSeederRef() {
        return seederRef;
    }

    @Override
    public String toString() {
        return "path=" + path +
                ", filename='" + filename + '\'' +
                ", seederRef='" + seederRef + '\'' +
                '}';
    }
}
