package com.tugalsan.api.file.server;

import com.tugalsan.api.function.client.maythrow.checkedexceptions.TGS_FuncMTCEUtils;
import com.tugalsan.api.stream.client.*;

import java.nio.file.*;
import java.util.*;

public class TS_RootUtils {

    public static long getUsableSpaceInBytes(Path root) {
        return TGS_FuncMTCEUtils.call(() -> {
            var store = Files.getFileStore(root);
            return store.getUsableSpace();
        });
    }

    public static long getTotalSpaceInBytes(Path root) {
        return TGS_FuncMTCEUtils.call(() -> {
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
