package com.tugalsan.api.file.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated //NOT TESTET WHATSOEVER
public class TS_FileSync {

    public static void mirror(String src, String dst) throws Exception {
        new TS_FileSync().sync(src, dst);
        new TS_FileSync().clean(src, dst);
    }

    public static void clean(String fromPath, String toPath) throws Exception {
        File fromFile = new File(fromPath);
        File toFile = new File(toPath);
        for (File file : fromFile.listFiles()) {
            String relativePath = file.getAbsolutePath().substring(fromFile.getAbsolutePath().length());
            File destFile = new File(toFile.getAbsolutePath() + relativePath);
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

    public static void sync(String fromPath, String toPath) throws Exception {
        File fromFile = new File(fromPath);
        File toFile = new File(toPath);
        for (File file : fromFile.listFiles()) {
            String relativePath = file.getAbsolutePath().substring(fromFile.getAbsolutePath().length());
            File destFile = new File(toFile.getAbsolutePath() + relativePath);
            if (file.isFile()) {
                syncFile(file, destFile);
            } else {
                if (!file.getName().startsWith(".")) {
                    destFile.mkdirs();
                    sync(file.getAbsolutePath(), destFile.getAbsolutePath());
                }
            }
        }
    }

    private static void syncFile(File file, File toFile) throws Exception {
        if (file.getName().startsWith(".")) {
            return;
        }
        try {
            if (TS_FileUtils.getChecksumLng(file.toPath()).get() == TS_FileUtils.getChecksumLng(toFile.toPath()).get()) {
                return;
            }
        } catch (Exception e) {
        }
        System.out.println(file + " -- sync --> " + toFile);
        InputStream in = new FileInputStream(file);
        OutputStream out = new FileOutputStream(toFile);
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        in.close();
        out.close();
    }
}
