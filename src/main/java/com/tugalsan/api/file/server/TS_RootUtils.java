package com.tugalsan.api.file.server;

import module com.tugalsan.api.function;
import module com.tugalsan.api.stream;
import java.nio.file.*;
import java.util.*;

public class TS_RootUtils {

    private TS_RootUtils() {

    }

    public static long getUsableSpaceInBytes(Path root) {
        return TGS_FuncMTCUtils.call(() -> {
            var store = Files.getFileStore(root);
            return store.getUsableSpace();
        });
    }

    public static long getTotalSpaceInBytes(Path root) {
        return TGS_FuncMTCUtils.call(() -> {
            var store = Files.getFileStore(root);
            return store.getTotalSpace();
        });
    }

    public static List<Path> getRoots() {
        return TGS_StreamUtils.toLst(
                TGS_StreamUtils.of(
                        FileSystems.getDefault().getRootDirectories()
                )
        );
    }
}
