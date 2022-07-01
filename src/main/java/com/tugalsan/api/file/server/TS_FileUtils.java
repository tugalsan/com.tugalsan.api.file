package com.tugalsan.api.file.server;

import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import javax.xml.bind.*;
import com.tugalsan.api.time.client.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;

public class TS_FileUtils {

    final private static TS_Log d = TS_Log.of(TS_FileUtils.class.getSimpleName());

    public static FileTime toFileTime(TGS_Time time) {
        return FileTime.fromMillis(time.toDateMillis());
    }

    public static TGS_Time toTime(FileTime fileTime) {
        return TGS_Time.of(fileTime.toMillis());
    }

    public static long getFileSizeInBytes(Path file) {
        return TGS_UnSafe.compile(() -> Files.size(file));
    }

    public static Path setTimeLastModified(Path path, TGS_Time time) {
        return TGS_UnSafe.compile(() -> {
            Files.setAttribute(path, "lastModifiedTime", toFileTime(time));
            return path;
        });
    }

    public static Path setTimeAccessTime(Path path, TGS_Time time) {
        return TGS_UnSafe.compile(() -> {
            Files.setAttribute(path, "lastAccessTime", toFileTime(time));
            return path;
        });
    }

    public static Path setTimeCreationTime(Path path, TGS_Time time) {
        return TGS_UnSafe.compile(() -> {
            Files.setAttribute(path, "creationTime", toFileTime(time));
            return path;
        });
    }

    public static TGS_Time getTimeLastModified(Path path) {
        return TGS_UnSafe.compile(() -> {
            return TGS_Time.of(
                    Files
                            .readAttributes(path, BasicFileAttributes.class)
                            .lastModifiedTime()
                            .toMillis()
            );
        });
    }

    public static TGS_Time getTimeLastAccessTime(Path path) {
        return TGS_UnSafe.compile(() -> {
            return TGS_Time.of(
                    new Date(
                            Files
                                    .readAttributes(path, BasicFileAttributes.class)
                                    .lastAccessTime()
                                    .toMillis()
                    )
            );
        });
    }

    public static TGS_Time getTimeCreationTime(Path path) {
        return TGS_UnSafe.compile(() -> {
            return TGS_Time.of(
                    new Date(
                            Files
                                    .readAttributes(path, BasicFileAttributes.class)
                                    .creationTime()
                                    .toMillis()
                    )
            );
        });
    }

    public static byte[] read(Path source) {
        return TGS_UnSafe.compile(() -> Files.readAllBytes(source));
    }

    public static Path write(byte[] source, Path dest, boolean append) {
        return TGS_UnSafe.compile(() -> Files.write(dest, source, StandardOpenOption.CREATE, append ? StandardOpenOption.APPEND : StandardOpenOption.WRITE));
    }

    public static boolean isFileReadable(Path file) {
        return Files.isReadable(file);
    }

    public static boolean isFileWritable(Path file) {
        return Files.isWritable(file);
    }

    public static boolean isExistFile(Path file) {
        return file != null && !Files.isDirectory(file) && Files.exists(file);
    }

    public static boolean createFile(Path file) {
        return TGS_UnSafe.compile(() -> {
            TS_DirectoryUtils.createDirectoriesIfNotExists(file.getParent());
            Files.createFile(file);
            return true;
        }, exception -> {
            exception.printStackTrace();
            return false;
        });
    }

    public static boolean isEmptyFile(Path file) {
        return getFileSizeInBytes(file) == 0L;
    }

    public static boolean deleteFileIfExists(Path file) {
        return deleteFileIfExists(file, true);
    }

    public static boolean deleteFileIfExists(Path file, boolean printStackTrace) {
        return TGS_UnSafe.compile(() -> {
            if (!isExistFile(file)) {
                return true;
            }
            Files.deleteIfExists(file);
            return true;
        }, exception -> {
            if (printStackTrace) {
                exception.printStackTrace();
            }
            return false;
        });
    }

    public static String getFullPath(Path path) {
        return path.toAbsolutePath().toString();
    }

    public static String getNameFull(Path path) {
        return path.getFileName().toString();
    }

    public static List<String> getNameFull(List<Path> paths) {
        return TGS_StreamUtils.toList(
                paths.stream().map(path -> path.getFileName().toString())
        );
    }

    public static List<String> getNameFull2(List<String> paths) {
        return getNameFull(TGS_StreamUtils.toList(
                paths.stream()
                        .map(str -> Path.of(str))
        ));
    }

    public static String getNameLabel(Path path) {
        var fullName = getNameFull(path);
        var i = fullName.lastIndexOf('.');
        if (i == 0) {
            return "";
        }
        if (i == -1) {
            return fullName;
        }
        return fullName.substring(0, i);
    }

    public static String getNameType(Path path) {
        var fullName = getNameFull(path);
        var i = fullName.lastIndexOf('.');
        if (i == 0) {
            return fullName.substring(i + 1);
        }
        if (i == -1) {
            return "";
        }
        return fullName.substring(i + 1);
    }

    public static Path moveAsFile(Path sourceFile, Path asDestFile) {
        return TGS_UnSafe.compile(() -> {
            d.ci("moveAsFile", "sourceFile", sourceFile, "asDestFile", asDestFile);
            if (Objects.equals(sourceFile.toAbsolutePath().toString(), asDestFile.toAbsolutePath().toString())) {
                return asDestFile;
            }
            TS_DirectoryUtils.createDirectoriesIfNotExists(asDestFile.getParent());
            deleteFileIfExists(asDestFile);
            Files.move(sourceFile, asDestFile, StandardCopyOption.REPLACE_EXISTING);
            return asDestFile;
        });
    }

    public static Path moveToFolder(Path sourceFile, Path destFolder) {
        d.ci("moveToFolder", "sourceFile", sourceFile, "destFolder", destFolder);
        var asDestFile = destFolder.resolve(sourceFile.getFileName());
        moveAsFile(sourceFile, asDestFile);
        return asDestFile;
    }

    public static Path copyFile(Path source, Path dest, boolean overwrite) {
        return TGS_UnSafe.compile(() -> {
            if (!isExistFile(dest)) {
                Files.copy(source, dest);
                return dest;
            }
            if (overwrite) {
                deleteFileIfExists(dest);
                Files.copy(source, dest);
            }
            return dest;
        });
    }

    public static Path copyFileAssure(Path source, Path dest, boolean overwrite) {
        var path = copyFile(source, dest, overwrite);
        if (!isExistFile(dest)) {
            TGS_UnSafe.catchMeIfUCan(d.className, "copyFileAssure", "!isExistFile(dest):" + dest);
        }
        return path;
    }

    public static String getChecksumHex(Path file) {
        return TGS_UnSafe.compile(() -> {
            var bytes = Files.readAllBytes(file);
            var hash = MessageDigest.getInstance("MD5").digest(bytes);
            return DatatypeConverter.printHexBinary(hash);
        });
    }

    public static Path rename(Path source, CharSequence newFileName) {
        return moveAsFile(source, source.resolveSibling(newFileName.toString()));
    }

    public static Path imitateNameType(Path src, String newType) {
        var type = getNameType(src);
        var strSrc = src.toString();
        var strDst = strSrc.substring(0, strSrc.length() - type.length()) + newType;
        return Path.of(strDst);
    }
}
