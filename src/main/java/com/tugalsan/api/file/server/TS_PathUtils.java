package com.tugalsan.api.file.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;

public class TS_PathUtils {

    final private static TS_Log d = TS_Log.of(TS_PathUtils.class.getSimpleName());

    public static Path getPathCurrent() {
        return Path.of(System.getProperty("user.dir"));
    }

    public static List<Path> toPaths(String list, String delimiter) {
        return TGS_StreamUtils.toList(
                Arrays.stream(list.split(delimiter)).map(split -> Path.of(split))
        );
    }

    public static Path getParent(Path path) {
        return path.getParent();
    }

    public static Path toPath(CharSequence fileOrDirectory) {
        try {
            var path = fileOrDirectory.toString();
            var isURL = path.contains("://");
            if (isURL && !path.toLowerCase(Locale.ROOT).startsWith("file:")) {
                d.ce("toPath", "PATH ONLY SUPPORTS FILE://", fileOrDirectory);
                return null;
            }
            return isURL ? Path.of(new URL(path).toURI()) : Path.of(path);
        } catch (Exception e) {
            d.ce("toPath", e.getMessage());
            return null;
        }
    }

    public static Path toPath(Class c) {
        try {
            var url = c.getProtectionDomain().getCodeSource().getLocation();
            return Path.of(url.toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //GOODIES
    public static String combinePath(String parentPath, String parentPathDependentChildPath) {
        if (parentPath == null || parentPath.equals("")) {
            return parentPathDependentChildPath;
        }
        return parentPath.endsWith(File.separator) ? parentPath + parentPathDependentChildPath : parentPath + File.separator + parentPathDependentChildPath;
    }

    public static String substract(String from_childFullPath, String to_parentPath) {
        try {
            return from_childFullPath.substring(to_parentPath.length() + 1);
        } catch (Exception e) {
        }
        return null;
    }

    public static String getDriveLetter(Path path) {
        return path.getRoot().toString();
    }
}
