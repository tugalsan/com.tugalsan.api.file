package com.tugalsan.api.file.server;

import module com.tugalsan.api.function;
import module com.tugalsan.api.log;
import java.io.*;
import java.util.*;

@Deprecated //NOT TESTET WHATSOEVER
public class TS_FileSyncUtils {

    final private static TS_Log d = TS_Log.of(TS_FileSyncUtils.class);

    private TS_FileSyncUtils() {

    }

    public static void mirror(String src, String dst) {
        TS_FileSyncUtils.sync(src, dst);
        TS_FileSyncUtils.clean(src, dst);
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
                        destFile.delete();
                    }
                }
            }
        }
    }

    private static void cleanFile(File file, File toFile) {
        TGS_FuncMTCUtils.run(() -> {
            if (file.getName().startsWith(".")) {
                return;
            }
            if (!toFile.exists()) {
                d.cr(" delete --> " + file);
                file.delete();
            }
        });
    }

    public static boolean sync(String fromPath, String toPath) {
        var fromFile = new File(fromPath);
        var toFile = new File(toPath);
        var success = true;
        for (var file : fromFile.listFiles()) {
            var relativePath = file.getAbsolutePath().substring(fromFile.getAbsolutePath().length());
            var destFile = new File(toFile.getAbsolutePath() + relativePath);
            if (file.isFile()) {
                success = success && syncFile(file, destFile);
            } else {
                if (!file.getName().startsWith(".")) {
                    destFile.mkdirs();
                    success = success && sync(file.getAbsolutePath(), destFile.getAbsolutePath());
                }
            }
        }
        return success;
    }

    private static boolean syncFile(File file, File toFile) {
        return TGS_FuncMTCUtils.call(() -> {
            if (file.getName().startsWith(".")) {
                return true;
            }
            if (Objects.equals(TS_FileUtils.getChecksumLng(file.toPath()).value(), TS_FileUtils.getChecksumLng(toFile.toPath()).value())) {
                return true;
            }
            d.cr(file + " -- sync --> " + toFile);
            try (var in = new FileInputStream(file); var out = new FileOutputStream(toFile);) {
                var buffer = new byte[1024];
                var len = in.read(buffer);
                while (len != -1) {
                    out.write(buffer, 0, len);
                    len = in.read(buffer);
                }
                return true;
            }
        }, e -> false);
    }
}
