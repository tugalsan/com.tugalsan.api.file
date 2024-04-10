package com.tugalsan.api.file.server;

import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.union.client.TGS_UnionExcuse;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TS_RootUtils {

    public static TGS_UnionExcuse<Long> getUsableSpaceInBytes(Path root) {
        try {
            var store = Files.getFileStore(root);
            return TGS_UnionExcuse.of(store.getUsableSpace());
        } catch (IOException ex) {
            return TGS_UnionExcuse.ofExcuse(ex);
        }
    }

    public static TGS_UnionExcuse<Long> getTotalSpaceInBytes(Path root) {
        try {
            var store = Files.getFileStore(root);
            return TGS_UnionExcuse.of(store.getTotalSpace());
        } catch (IOException ex) {
            return TGS_UnionExcuse.ofExcuse(ex);
        }
    }

    public static List<Path> getRoots() {
        return TGS_StreamUtils.toLst(
                TGS_StreamUtils.of(
                        FileSystems.getDefault().getRootDirectories()
                )
        );
    }
}
