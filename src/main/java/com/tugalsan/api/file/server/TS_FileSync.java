package com.tugalsan.api.file.server;

import com.tugalsan.api.list.client.TGS_ListUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.union.client.TGS_UnionExcuseVoid;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Deprecated //NOT TESTET WHATSOEVER
public class TS_FileSync {

    final private static TS_Log d = TS_Log.of(TS_FileSync.class);

    public static void mirror(String src, String dst) {
        TS_FileSync.sync(src, dst);
        TS_FileSync.clean(src, dst);
    }

    public static void clean(String fromPath, String toPath) {
        var fromFile = new File(fromPath);
        var toFile = new File(toPath);
        for (var file : fromFile.listFiles()) {
            var relativePath = file.getAbsolutePath().substring(fromFile.getAbsolutePath().length());
            var destFile = new File(toFile.getAbsolutePath() + relativePath);
            if (file.isFile()) {
                cleanFile(file, destFile);
            } else {
                if (!file.getName().startsWith(".")) {
                    clean(file.getAbsolutePath(), destFile.getAbsolutePath());
                    if (!destFile.exists()) {
                        file.delete();
                    }
                }
            }
        }
    }

    private static void cleanFile(File file, File toFile) {
        if (file.getName().startsWith(".")) {
            return;
        }
        if (!toFile.exists()) {
            System.out.println(" delete --> " + file);
            file.delete();
        }
    }

    public static List<TGS_UnionExcuseVoid> sync(String fromPath, String toPath) {
        List<TGS_UnionExcuseVoid> results = TGS_ListUtils.of();
        var fromFile = new File(fromPath);
        var toFile = new File(toPath);
        for (var file : fromFile.listFiles()) {
            var relativePath = file.getAbsolutePath().substring(fromFile.getAbsolutePath().length());
            var destFile = new File(toFile.getAbsolutePath() + relativePath);
            if (file.isFile()) {
                results.add(syncFile(file, destFile));
            } else {
                if (!file.getName().startsWith(".")) {
                    destFile.mkdirs();
                    sync(file.getAbsolutePath(), destFile.getAbsolutePath())
                            .stream().forEachOrdered(u -> results.add(u));
                }
            }
        }
        return results;
    }

    private static TGS_UnionExcuseVoid syncFile(File from, File to) {
        if (from.getName().startsWith(".")) {
            return TGS_UnionExcuseVoid.ofExcuse(d.className, "syncFile", "file starts with '.'");
        }
        var chksumFrom = TS_FileUtils.getChecksumLng(from.toPath()).orElse(-1L);
        if (chksumFrom != -1) {
            var chksumTo = TS_FileUtils.getChecksumLng(to.toPath()).orElse(-1L);
            if (chksumTo != -1) {
                if (Objects.equals(chksumFrom, chksumTo)) {
                    return TGS_UnionExcuseVoid.ofVoid();
                }
            }
        }
        d.ci("syncFile", from, to);
        try (var in = new FileInputStream(from); var out = new FileOutputStream(to);) {
            var buffer = new byte[1024];
            var len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            return TGS_UnionExcuseVoid.ofVoid();
        } catch (IOException ex) {
            return TGS_UnionExcuseVoid.ofExcuse(ex);
        }
    }
}
