package com.tugalsan.api.file.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;

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
        return TGS_UnSafe.compile(() -> {
            var path = fileOrDirectory.toString();
            var isURL = path.contains("://");
            if (isURL && !path.toLowerCase(Locale.ROOT).startsWith("file:")) {
                d.ce("toPath", "PATH ONLY SUPPORTS FILE://", fileOrDirectory);
                return null;
            }
            return isURL ? Path.of(new URL(path).toURI()) : Path.of(path);
        }, exception -> {
            d.ce("toPath", e.getMessage());
            return null;
        });
    }

    public static Path toPath(Class c) {
        return TGS_UnSafe.compile(() -> {
            var url = c.getProtectionDomain().getCodeSource().getLocation();
            return Path.of(url.toURI());
        });
    }

    //GOODIES
    public static String combinePath(String parentPath, String parentPathDependentChildPath) {
        if (parentPath == null || parentPath.equals("")) {
            return parentPathDependentChildPath;
        }
        return parentPath.endsWith(File.separator) ? parentPath + parentPathDependentChildPath : parentPath + File.separator + parentPathDependentChildPath;
    }

    public static String substract(String from_childFullPath, String to_parentPath) {
        return TGS_UnSafe.compile(() -> {
            return from_childFullPath.substring(to_parentPath.length() + 1);
        }, exception -> {
            return null;
        });
    }

    public static String getDriveLetter(Path path) {
        return path.getRoot().toString();
    }

    public static boolean contains(List<Path> sources, Path searchFor) {
        return sources.stream().filter(src -> equals(src, searchFor)).findAny().isPresent();
    }

    public static boolean equals(Path src1, Path src2) {
        return src1.toAbsolutePath().toString().equals(src2.toAbsolutePath().toString());
    }
}
