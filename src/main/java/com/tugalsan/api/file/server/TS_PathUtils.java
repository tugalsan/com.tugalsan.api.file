package com.tugalsan.api.file.server;

import com.tugalsan.api.charset.client.TGS_CharSetCast;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.tuple.client.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;

public class TS_PathUtils {

    final private static TS_Log d = TS_Log.of(TS_PathUtils.class);

    public static Optional<String> getPath(Class clazz) {
        return TGS_UnSafe.call(() -> {
            return Optional.of(clazz
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath());
        }, e -> Optional.empty());
    }

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

    public static TGS_Tuple2<Path, Exception> toPathOrError(CharSequence fileOrDirectory) {
        return TGS_UnSafe.call(() -> {
            var path = fileOrDirectory.toString();
            var isURL = path.contains("://");
            if (isURL && !TGS_CharSetCast.toLocaleLowerCase(path).startsWith("file:")) {
                d.ci("toPathAndError", "PATH ONLY SUPPORTS FILE://", fileOrDirectory);
                return new TGS_Tuple2(null, TGS_UnSafe.toRuntimeException(d.className, "toPathAndError",
                        "PATH ONLY SUPPORTS FILE://, fileOrDirectory:{" + fileOrDirectory + "]"
                ));
            }
            return new TGS_Tuple2(isURL ? Path.of(new URL(path).toURI()) : Path.of(path), null);
        }, e -> {
            d.ci("toPathAndError", e);
            return new TGS_Tuple2(null, e);
        });
    }

    public static Path toPath(Class c) {
        return TGS_UnSafe.call(() -> {
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
        return TGS_UnSafe.call(() -> {
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
