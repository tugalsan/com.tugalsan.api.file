package com.tugalsan.api.file.server;

import com.tugalsan.api.executable.client.*;
import java.nio.file.Path;

public class TS_FileWatch {

    public static TS_DirectoryWatch of(Path file, TGS_Executable exe) {
        return TS_DirectoryWatch.of(file.getParent(), forFile -> {
            if (forFile.equals(file)) {
                exe.execute();
            }
        });
    }
}
