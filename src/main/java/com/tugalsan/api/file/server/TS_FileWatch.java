package com.tugalsan.api.file.server;

import com.tugalsan.api.executable.client.TGS_ExecutableType1;
import java.nio.file.Path;

public class TS_FileWatch {

    public static TS_DirectoryWatch of(Path file, TGS_ExecutableType1<Path> exe) {
        return TS_DirectoryWatch.of(file, forFile -> {
            if (forFile.equals(file)) {
                exe.execute(file);
            }
        });
    }
}
