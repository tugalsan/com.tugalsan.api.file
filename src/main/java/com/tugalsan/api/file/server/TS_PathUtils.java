package com.tugalsan.api.file.server;

import com.tugalsan.api.charset.client.TGS_CharSetCast;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.union.client.TGS_Union;

public class TS_PathUtils {

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

    public static TGS_Union<Path> toPathOrError(CharSequence fileOrDirectory) {
        var path = fileOrDirectory.toString();
        var isURL = path.contains("://");
        if (isURL && !TGS_CharSetCast.toLocaleLowerCase(path).startsWith("file:")) {
            d.ci("toPathAndError", "PATH ONLY SUPPORTS FILE://", fileOrDirectory);
            return TGS_Union.ofThrowable(d.className, "toPathAndError",
                    "PATH ONLY SUPPORTS FILE://, fileOrDirectory:{" + fileOrDirectory + "]"
            );
        }
        return TGS_Union.of(isURL ? Path.of(URI.create(path)) : Path.of(path));
    }

    public static TGS_Union<Path> of(String path) {
        if (TGS_StringUtils.isNullOrEmpty(path)) {
            return TGS_Union.ofEmpty();
        }
        try {
            return TGS_Union.of(Path.of(path));
        } catch (FileSystemNotFoundException | SecurityException | IllegalArgumentException e) {
            return TGS_Union.ofThrowable(e);
        }
    }

    public static TGS_Union<Path> of(URL u) {
        try {
            return TGS_Union.of(Path.of(u.toURI()));
        } catch (URISyntaxException ex) {
            return TGS_Union.ofThrowable(ex);
        }
    }

    public static TGS_Union<Path> of(Class c) {
        return of(c.getProtectionDomain().getCodeSource().getLocation());
    }

    //GOODIES
    public static String combinePath(String parentPath, String parentPathDependentChildPath) {
        if (parentPath == null || parentPath.equals("")) {
            return parentPathDependentChildPath;
        }
        return parentPath.endsWith(File.separator) ? parentPath + parentPathDependentChildPath : parentPath + File.separator + parentPathDependentChildPath;
    }

    public static TGS_Union<String> substract(String from_childFullPath, String to_parentPath) {
        try {
            return TGS_Union.of(from_childFullPath.substring(to_parentPath.length() + 1));
        } catch (IndexOutOfBoundsException e) {
            return TGS_Union.ofThrowable(e);
        }
    }

    public static TGS_Union<String> getDriveLetter(Path path) {
        var root = path.getRoot();
        if (root == null) {
            return TGS_Union.ofEmpty();
        }
        return TGS_Union.of(path.toString());
    }

    public static boolean contains(List<Path> sources, Path searchFor) {
        return sources.stream().filter(src -> equals(src, searchFor)).findAny().isPresent();
    }

    public static boolean equals(Path src1, Path src2) {
        return src1.toAbsolutePath().toString().equals(src2.toAbsolutePath().toString());
    }
}
