package ch.so.agi.sodata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.util.FileSystemUtils;

public class CleanupInputStreamResource extends InputStreamResource {
    public CleanupInputStreamResource(File file) throws FileNotFoundException {
        super(new FileInputStream(file) {
            @Override
            public void close() throws IOException {
                super.close();
                FileSystemUtils.deleteRecursively(file.getParentFile());
            }
        });
    }
}
