package com.tugalsan.api.file.server;

import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;
import java.nio.file.*;
import java.util.*;

public class TS_RootUtils {

    public static long getUsableSpaceInBytes(Path root) {
        return TGS_UnSafe.compile(() -> {
            var store = Files.getFileStore(root);
            return store.getUsableSpace();
        });
    }

    public static long getTotalSpaceInBytes(Path root) {
        return TGS_UnSafe.compile(() -> {
            var store = Files.getFileStore(root);
            return store.getTotalSpace();
        });
    }

    public static List<Path> getRoots() {
        return TGS_StreamUtils.toList(
                TGS_StreamUtils.of(
                        FileSystems.getDefault().getRootDirectories()
                )
        );
    }
}
