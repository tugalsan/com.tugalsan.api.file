package com.tugalsan.api.file.server;

import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import javax.xml.bind.*;
import com.tugalsan.api.time.client.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.union.client.TGS_Union;
import com.tugalsan.api.union.client.TGS_UnionUtils;
import java.io.IOException;
import java.net.URLConnection;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class TS_FileUtils {

    final private static TS_Log d = TS_Log.of(TS_FileUtils.class);

    public static FileTime toFileTime(TGS_Time time) {
        return FileTime.fromMillis(time.toDateMillis());
    }

    public static TGS_Time toTime(FileTime fileTime) {
        return TGS_Time.ofMillis(fileTime.toMillis());
    }

    public static TGS_Union<Long> getFileSizeInBytes(Path file) {
        try {
            return TGS_Union.of(Files.size(file));
        } catch (IOException | SecurityException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> setTimeLastModified(Path path, TGS_Time time) {
        try {
            Files.setAttribute(path, "lastModifiedTime", toFileTime(time));
            return TGS_Union.of(true);
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException | SecurityException | ClassCastException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> setTimeAccessTime(Path path, TGS_Time time) {
        try {
            Files.setAttribute(path, "lastAccessTime", toFileTime(time));
            return TGS_Union.of(true);
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException | SecurityException | ClassCastException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> setTimeCreationTime(Path path, TGS_Time time) {
        try {
            Files.setAttribute(path, "creationTime", toFileTime(time));
            return TGS_Union.of(true);
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException | SecurityException | ClassCastException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<TGS_Time> getTimeLastModified(Path path) {
        try {
            return TGS_Union.of(
                    TGS_Time.ofMillis(
                            Files
                                    .readAttributes(path, BasicFileAttributes.class)
                                    .lastModifiedTime()
                                    .toMillis()
                    )
            );
        } catch (IOException | SecurityException | UnsupportedOperationException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<TGS_Time> getTimeLastAccessTime(Path path) {
        try {
            return TGS_Union.of(
                    TGS_Time.ofMillis(
                            Files
                                    .readAttributes(path, BasicFileAttributes.class)
                                    .lastAccessTime()
                                    .toMillis()
                    )
            );
        } catch (IOException | SecurityException | UnsupportedOperationException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<TGS_Time> getTimeCreationTime(Path path) {
        try {
            return TGS_Union.of(
                    TGS_Time.ofMillis(
                            Files
                                    .readAttributes(path, BasicFileAttributes.class)
                                    .creationTime()
                                    .toMillis()
                    )
            );
        } catch (IOException | SecurityException | UnsupportedOperationException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<byte[]> read(Path source) {
        try {
            return TGS_Union.of(Files.readAllBytes(source));
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> write(byte[] source, Path dest, boolean append) {
        try {
            return TGS_Union.of(
                    Files.write(
                            dest,
                            source,
                            StandardOpenOption.CREATE,
                            append ? StandardOpenOption.APPEND : StandardOpenOption.WRITE
                    ) != null
            );
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
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

    public static TGS_Union<Boolean> createFileIfNotExists(Path file) {
        if (isExistFile(file)) {
            return TGS_Union.of(true);
        }
        return createFile(file);
    }

    public static TGS_Union<Boolean> createFile(Path file) {
        try {
            TS_DirectoryUtils.createDirectoriesIfNotExists(file.getParent());
            Files.createFile(file);
            return TGS_Union.of(true);
        } catch (IOException | UnsupportedOperationException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> isEmptyFile(Path file) {
        var u = getFileSizeInBytes(file);
        if (u.isEmpty()) {
            return TGS_Union.ofExcuse(u.excuse());
        }
        return TGS_Union.of(u.value() == 0L);
    }

    public static TGS_Union<Boolean> deleteFileIfExists(Path file) {
        return deleteFileIfExists(file, true);
    }

    public static TGS_Union<Boolean> deleteFileIfExists(Path file, boolean printStackTrace) {
        try {
            if (!isExistFile(file)) {
                return TGS_Union.of(true);
            }
            Files.deleteIfExists(file);
            return TGS_Union.of(!isExistFile(file));
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static String getFullPath(Path path) {
        return path.toAbsolutePath().toString();
    }

    public static String getNameFull(Path path) {
        return path.getFileName().toString();
    }

    public static List<String> getNameFull(List<Path> paths) {
        return TGS_StreamUtils.toLst(
                paths.stream().map(path -> path.getFileName().toString())
        );
    }

    public static List<String> getNameFull2(List<String> paths) {
        return getNameFull(TGS_StreamUtils.toLst(
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

    public static TGS_Union<Boolean> moveAs(Path sourceFile, Path asDestFile, boolean overwrite) {
        try {
            d.ci("moveAs", "sourceFile", sourceFile, "asDestFile", asDestFile);
            if (Objects.equals(sourceFile.toAbsolutePath().toString(), asDestFile.toAbsolutePath().toString())) {
                return TGS_Union.of(true);
            }
            TS_DirectoryUtils.createDirectoriesIfNotExists(asDestFile.getParent());
            if (!overwrite && isExistFile(asDestFile)) {
                return TGS_Union.of(false);
            }
            return TGS_Union.of(
                    Files.move(
                            sourceFile,
                            asDestFile,
                            StandardCopyOption.REPLACE_EXISTING
                    ) != null
            );
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> moveToFolder(Path sourceFile, Path destFolder, boolean overwrite) {
        d.ci("moveToFolder", "sourceFile", sourceFile, "destFolder", destFolder);
        var asDestFile = destFolder.resolve(sourceFile.getFileName());
        return moveAs(sourceFile, asDestFile, overwrite);
    }

    public static TGS_Union<Boolean> copyToFolder(Path sourceFile, Path destFolder, boolean overwrite) {
        d.ci("copyToFolder", "sourceFile", sourceFile, "destFolder", destFolder);
        var asDestFile = destFolder.resolve(sourceFile.getFileName());
        return copyAs(sourceFile, asDestFile, overwrite);
    }

    public static TGS_Union<Boolean> copyAs(Path sourceFile, Path asDestFile, boolean overwrite) {
        d.ci("copyAs", "sourceFile", sourceFile, "asDestFile", asDestFile);
        if (Objects.equals(sourceFile.toAbsolutePath().toString(), asDestFile.toAbsolutePath().toString())) {
            return TGS_Union.of(true);
        }
        TS_DirectoryUtils.createDirectoriesIfNotExists(asDestFile.getParent());
        if (!overwrite && isExistFile(asDestFile)) {
            return TGS_Union.ofEmpty_NullPointerException();
        }
        try {
            Files.copy(sourceFile, asDestFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
        if (!isExistFile(asDestFile)) {
            return TGS_Union.of(false);
        }
        return TGS_Union.of(true);
    }

    @SuppressWarnings("empty-statement")
    public static TGS_Union<Long> getChecksumLng(Path file) {
        try (var in = new CheckedInputStream(Files.newInputStream(file), new CRC32())) {
            var bytes = new byte[1024];
            while (in.read(bytes) >= 0)
			;
            return TGS_Union.of(in.getChecksum().getValue());
        } catch (IOException e) {
            return TGS_Union.ofExcuse(e);
        }
    }

    public static TGS_Union<String> getChecksumHex(Path file) {
        try {
            var bytes = Files.readAllBytes(file);
            var hash = MessageDigest.getInstance("MD5").digest(bytes);
            return TGS_Union.of(DatatypeConverter.printHexBinary(hash));
        } catch (IOException | NoSuchAlgorithmException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> rename(Path source, CharSequence newFileName) {
        return moveAs(
                source,
                source.resolveSibling(newFileName.toString()),
                false
        );
    }

    public static Path imitateNameType(Path src, String newType) {
        var type = getNameType(src);
        var strSrc = src.toString();
        var strDst = strSrc.substring(0, strSrc.length() - type.length()) + newType;
        return Path.of(strDst);
    }

    public static String mime(Path img) {
        var typ = URLConnection.getFileNameMap().getContentTypeFor(getNameFull(img));
        if (TGS_StringUtils.isPresent(typ) && typ.length() < 5) {
            return typ.replace(";charset=UTF-8", "");
        }
        try {
            var url = img.toUri().toURL();
            return url.openConnection().getContentType()
                    .replace(";charset=UTF-8", "");
        } catch (IOException ex) {
            return typ;
        }
    }

}
