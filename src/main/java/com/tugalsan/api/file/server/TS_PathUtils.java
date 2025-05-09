package com.tugalsan.api.file.server;

import com.tugalsan.api.charset.client.TGS_CharSetCast;
import com.tugalsan.api.function.client.maythrowexceptions.checked.TGS_FuncMTCUtils;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.union.client.TGS_UnionExcuse;

public class TS_PathUtils {

    private TS_PathUtils() {

    }

    final private static TS_Log d = TS_Log.of(TS_PathUtils.class);

    public static Path getPathHomeDesktop() {
        return getPathHome().resolve("Desktop");
    }

    public static Path getPathHome() {
        return Path.of(System.getProperty("user.home"));
    }

    //DONT TOUCH IT, I MAY USED IT BEFORE
    @Deprecated
    public static Path getPathCurrent() {
        return Path.of(System.getProperty("user.dir"));
    }

    public static Path getPathCurrent_nio() {
        return FileSystems.getDefault().getPath("").toAbsolutePath();
    }

    public static Path getPathCurrent_nio(String child, String... more) {
        return FileSystems.getDefault().getPath(child, more).toAbsolutePath();
    }

    public static List<Path> toPaths(String list, String delimiter) {
        return TGS_StreamUtils.toLst(
                Arrays.stream(list.split(delimiter)).map(split -> Path.of(split))
        );
    }

    public static Path getParent(Path path) {
        return path.getParent();
    }

    public static boolean isPathable(CharSequence fileOrDirectory) {
        return TGS_FuncMTCUtils.call(() -> {
            var path = fileOrDirectory.toString();
            var isURL = path.contains("://");
            if (isURL && !TGS_CharSetCast.current().toLowerCase(path).startsWith("file:")) {
                return false;
            }
            return true;
        }, e -> false);
    }

    public static TGS_UnionExcuse<Path> toPath(CharSequence fileOrDirectory) {
        return TGS_FuncMTCUtils.call(() -> {
            var path = fileOrDirectory.toString();
            var isURL = path.contains("://");
            if (isURL && !TGS_CharSetCast.current().toLowerCase(path).startsWith("file:")) {
                d.ci("toPathAndError", "PATH ONLY SUPPORTS FILE://", fileOrDirectory);
                return TGS_UnionExcuse.ofExcuse(d.className, "toPath",
                        "PATH ONLY SUPPORTS FILE://, fileOrDirectory:{" + fileOrDirectory + "]"
                );
            }
            return TGS_UnionExcuse.of(isURL ? Path.of(URI.create(path)) : Path.of(path));
        }, e -> {
            d.ci("toPathAndError", e);
            return TGS_UnionExcuse.ofExcuse(e);
        });
    }

    public static TGS_UnionExcuse<Path> of(String path) {
        return TGS_FuncMTCUtils.call(() -> {
            if (TGS_StringUtils.cmn().isNullOrEmpty(path)) {
                return TGS_UnionExcuse.ofExcuse(d.className, "of", "TGS_StringUtils.cmn().isNullOrEmpty(path)");
            }
            return TGS_UnionExcuse.of(Path.of(path));
        }, e -> {
            return TGS_UnionExcuse.ofExcuse(e);
        });
    }

    public static Path toPath(Class c) {
        return TGS_FuncMTCUtils.call(() -> {
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
        return TGS_FuncMTCUtils.call(() -> {
            return from_childFullPath.substring(to_parentPath.length() + 1);
        }, exception -> {
            return null;
        });
    }

    public static String getDriveLetter(Path path) {
        return path.getRoot().toString();
    }

    public static boolean contains(List<Path> sources, Path searchFor) {
        return sources.stream().anyMatch(src -> equals(src, searchFor));
    }

    public static boolean equals(Path src1, Path src2) {
        return src1.toAbsolutePath().toString().equals(src2.toAbsolutePath().toString());
    }
}
