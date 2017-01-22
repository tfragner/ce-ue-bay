package at.meroff.ce.ue.api;

import java.io.Serializable;

/**
 * Created by fragner on 14.01.17.
 * Klasse die bereitzustellen Dateien repräsentiert
 */
public class UploadFile implements Serializable {
    /**
     * Dateiname unter dem das Byte[] gespeichert werden soll
     */
    String filename;

    /**
     * Daten als Byte Array
     */
    byte[] data;

    /**
     * Standard Konstruktor
     * @param filename Dateiname
     * @param data Daten der Datei als Byte Array
     */
    public UploadFile(String filename, byte[] data) {
        this.filename = filename;
        this.data = data;
    }

    /**
     * Getter für den Dateinamen
     * @return Dateiname
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Getter für die Daten
     * @return Byte Array der Daten
     */
    public byte[] getData() {
        return data;
    }
}
