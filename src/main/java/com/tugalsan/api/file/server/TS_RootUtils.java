package com.tugalsan.api.file.server;

import com.tugalsan.api.stream.client.*;
import java.nio.file.*;
import java.util.*;

public class TS_RootUtils {

    public static long getUsableSpaceInBytes(Path root) {
        try {
            var store = Files.getFileStore(root);
            return store.getUsableSpace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long getTotalSpaceInBytes(Path root) {
        try {
            var store = Files.getFileStore(root);
            return store.getTotalSpace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Path> getRoots() {
        return TGS_StreamUtils.toList(
                TGS_StreamUtils.of(
                        FileSystems.getDefault().getRootDirectories()
                )
        );
    }
}
