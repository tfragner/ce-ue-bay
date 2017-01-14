package at.meroff.ce.ue.helper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by fragner on 07.01.17.
 */
public class CommonHelpers {
    public static boolean checkFile(Path path) {
        return path != null && Files.exists(path) && Files.isRegularFile(path);
    }

    public static boolean checkDirectory(Path path) {
        return path != null && Files.exists(path) && Files.isDirectory(path);
    }
}
