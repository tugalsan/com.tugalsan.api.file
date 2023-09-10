package com.tugalsan.api.file.server;

import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Objects;

@Deprecated //NOT TESTET WHATSOEVER
public class TS_FileSync {

    public static void mirror(String src, String dst) throws Exception {
        new TS_FileSync().sync(src, dst);
        new TS_FileSync().clean(src, dst);
    }

    public static void clean(String fromPath, String toPath) throws Exception {
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

    private static void cleanFile(File file, File toFile) throws Exception {
        if (file.getName().startsWith(".")) {
            return;
        }
        if (!toFile.exists()) {
            System.out.println(" delete --> " + file);
            file.delete();
        }
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
        return TGS_UnSafe.call(() -> {
            if (file.getName().startsWith(".")) {
                return true;
            }
            if (Objects.equals(TS_FileUtils.getChecksumLng(file.toPath()).get(), TS_FileUtils.getChecksumLng(toFile.toPath()).get())) {
                return true;
            }
            System.out.println(file + " -- sync --> " + toFile);
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
