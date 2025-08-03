package rom_editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileHandler {

    public byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public void saveFile(String filePath, byte[] data) throws IOException {
        try (OutputStream os = new FileOutputStream(filePath)) {
            os.write(data);
        }
    }

    public boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
}