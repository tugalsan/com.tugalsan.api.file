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
import com.tugalsan.api.union.client.TGS_UnionExcuse;
import com.tugalsan.api.union.client.TGS_UnionExcuseVoid;
import com.tugalsan.api.unsafe.client.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class TS_FileUtils {

    final private static TS_Log d = TS_Log.of(TS_FileUtils.class);

    public static TGS_UnionExcuse<Boolean> hasSameContent(Path file1, Path file2, boolean abuseMemory) {
        return TGS_UnSafe.call(() -> {
            if (abuseMemory) {
                try (var randomAccessFile1 = new RandomAccessFile(file1.toFile(), "r");) {
                    try (var randomAccessFile2 = new RandomAccessFile(file2.toFile(), "r")) {
                        var ch1 = randomAccessFile1.getChannel();
                        var ch2 = randomAccessFile2.getChannel();
                        if (ch1.size() != ch2.size()) {
                            return TGS_UnionExcuse.of(false);
                        }
                        var size = ch1.size();
                        var m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size);
                        var m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size);
                        return TGS_UnionExcuse.of(m1.equals(m2));
                    }
                }
            } else {
                return TGS_UnionExcuse.of(Files.mismatch(file2, file2) == -1L);
            }
        }, e -> TGS_UnionExcuse.ofExcuse(e));
    }

    public static FileTime toFileTime(TGS_Time time) {
        return FileTime.fromMillis(time.toDateMillis());
    }

    public static TGS_Time toTime(FileTime fileTime) {
        return TGS_Time.ofMillis(fileTime.toMillis());
    }

    public static long getFileSizeInBytes(Path file) {
        return TGS_UnSafe.call(() -> Files.size(file));
    }

    public static Path setTimeLastModified(Path path, TGS_Time time) {
        return TGS_UnSafe.call(() -> {
            Files.setAttribute(path, "lastModifiedTime", toFileTime(time));
            return path;
        });
    }

    public static Path setTimeAccessTime(Path path, TGS_Time time) {
        return TGS_UnSafe.call(() -> {
            Files.setAttribute(path, "lastAccessTime", toFileTime(time));
            return path;
        });
    }

    public static Path setTimeCreationTime(Path path, TGS_Time time) {
        return TGS_UnSafe.call(() -> {
            Files.setAttribute(path, "creationTime", toFileTime(time));
            return path;
        });
    }

    public static TGS_Time getTimeLastModified(Path path) {
        return TGS_UnSafe.call(() -> {
            return TGS_Time.ofMillis(Files.getLastModifiedTime(path).toMillis());
//            return TGS_Time.ofMillis(Files
//                    .readAttributes(path, BasicFileAttributes.class)
//                    .lastModifiedTime()
//                    .toMillis()
//            );
        }, e -> null);//POSSIBLY ACCESS DENIED EXCEPTION
    }

    public static TGS_Time getTimeLastAccessTime(Path path) {
        return TGS_UnSafe.call(() -> {
            return TGS_Time.ofMillis(Files
                    .readAttributes(path, BasicFileAttributes.class)
                    .lastAccessTime()
                    .toMillis()
            );
        }, e -> null);//POSSIBLY ACCESS DENIED EXCEPTION
    }

    public static TGS_Time getTimeCreationTime(Path path) {
        return TGS_UnSafe.call(() -> {
            return TGS_Time.ofMillis(Files
                    .readAttributes(path, BasicFileAttributes.class)
                    .creationTime()
                    .toMillis()
            );
        }, e -> null);//POSSIBLY ACCESS DENIED EXCEPTION
    }

    public static byte[] read(Path source) {
        return TGS_UnSafe.call(() -> Files.readAllBytes(source));
    }

    public static Path write(byte[] source, Path dest, boolean append) {
        return TGS_UnSafe.call(() -> Files.write(dest, source, StandardOpenOption.CREATE, append ? StandardOpenOption.APPEND : StandardOpenOption.WRITE));
    }

    public static boolean isFileReadable(Path file) {
        return Files.isReadable(file);
    }

    public static boolean isFileWritable(Path file) {
        return Files.isWritable(file);
    }

    public static boolean isFileLocked(Path file) {
        if (!isExistFile(file)) {
            return false;
        }
        return TGS_UnSafe.call(() -> {
            try (var  _ = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
                return false;
            }
        }, e -> true);
    }

    public static boolean isExistFile(Path file) {
        return file != null && !Files.isDirectory(file) && Files.exists(file);
    }

    public static boolean createFileIfNotExists(Path file) {
        return isExistFile(file) || createFile(file);
    }

    public static TGS_UnionExcuse<Path> createFileTemp() {
        return createFileTemp(".tmp");
    }

    public static TGS_UnionExcuse<Path> createFileTemp(String suffix) {
        return TGS_UnSafe.call(() -> {
            var file = File.createTempFile(null, suffix);
            file.deleteOnExit();
            return TGS_UnionExcuse.of(file.toPath());
        }, e -> TGS_UnionExcuse.ofExcuse(e));
    }

    public static boolean createFile(Path file) {
        return TGS_UnSafe.call(() -> {
            TS_DirectoryUtils.createDirectoriesIfNotExists(file.getParent());
            Files.createFile(file);
            return true;
        }, exception -> {
            d.ce("createFile", file, exception);
            return false;
        });
    }

    public static boolean isEmptyFile(Path file) {
        return getFileSizeInBytes(file) == 0L;
    }

    public static TGS_UnionExcuseVoid deleteFileIfExists(Path file) {
        return TGS_UnSafe.call(() -> {
            if (!isExistFile(file)) {
                return TGS_UnionExcuseVoid.ofVoid();
            }
            if (isExistFile(file)) {
                Files.delete(file);
            }
            return TGS_UnionExcuseVoid.ofVoid();
        }, e -> {
            return TGS_UnionExcuseVoid.ofExcuse(e);
        });
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

    public static Path moveAs(Path sourceFile, Path asDestFile, boolean overwrite) {
        return TGS_UnSafe.call(() -> {
            d.ci("moveAs", "sourceFile", sourceFile, "asDestFile", asDestFile);
            if (Objects.equals(sourceFile.toAbsolutePath().toString(), asDestFile.toAbsolutePath().toString())) {
                return asDestFile;
            }
            TS_DirectoryUtils.createDirectoriesIfNotExists(asDestFile.getParent());
            if (!overwrite && isExistFile(asDestFile)) {
                return null;
            }
            TGS_UnSafe.run(() -> Files.move(sourceFile, asDestFile, StandardCopyOption.REPLACE_EXISTING), e -> d.ct("moveAs", e));
            return asDestFile;
        }, e -> {
            e.printStackTrace();
            return null;
        });
    }

    public static Path moveToFolder(Path sourceFile, Path destFolder, boolean overwrite) {
        d.ci("moveToFolder", "sourceFile", sourceFile, "destFolder", destFolder);
        var asDestFile = destFolder.resolve(sourceFile.getFileName());
        return moveAs(sourceFile, asDestFile, overwrite);
    }

    public static Path copyToFolder(Path sourceFile, Path destFolder, boolean overwrite) {
        d.ci("copyToFolder", "sourceFile", sourceFile, "destFolder", destFolder);
        var asDestFile = destFolder.resolve(sourceFile.getFileName());
        return copyAs(sourceFile, asDestFile, overwrite);
    }

    public static Path copyAs(Path sourceFile, Path asDestFile, boolean overwrite) {
        return TGS_UnSafe.call(() -> {
            d.ci("copyAs", "sourceFile", sourceFile, "asDestFile", asDestFile);
            if (Objects.equals(sourceFile.toAbsolutePath().toString(), asDestFile.toAbsolutePath().toString())) {
                return asDestFile;
            }
            TS_DirectoryUtils.createDirectoriesIfNotExists(asDestFile.getParent());
            if (!overwrite && isExistFile(asDestFile)) {
                return null;
            }
            TGS_UnSafe.run(() -> Files.copy(sourceFile, asDestFile, StandardCopyOption.REPLACE_EXISTING), e -> d.ce("copyAs", e));
            if (!isExistFile(asDestFile)) {
                return null;
            }
            return asDestFile;
        }, e -> {
            e.printStackTrace();
            return null;
        });
    }

    public static Path copyAsAssure(Path source, Path dest, boolean overwrite) {
        var path = copyAs(source, dest, overwrite);
        if (!isExistFile(dest)) {
            TGS_UnSafe.thrw(d.className, "copyAsAssure", "!isExistFile(dest):" + dest);
        }
        return path;
    }

    @SuppressWarnings("empty-statement")
    public static TGS_UnionExcuse<Long> getChecksumLng(Path file) {
        return TGS_UnSafe.call(() -> {
            try (var in = new CheckedInputStream(Files.newInputStream(file), new CRC32())) {
                var bytes = new byte[1024];
                while (in.read(bytes) >= 0)
			;
                return TGS_UnionExcuse.of(in.getChecksum().getValue());
            }
        }, e -> {
            return TGS_UnionExcuse.ofExcuse(e);
        });
    }

    public static TGS_UnionExcuse<String> getChecksumHex(Path file) {
        return TGS_UnSafe.call(() -> {
            var bytes = Files.readAllBytes(file);
            var hash = MessageDigest.getInstance("MD5").digest(bytes);
            return TGS_UnionExcuse.of(DatatypeConverter.printHexBinary(hash));
        }, e -> TGS_UnionExcuse.ofExcuse(e));//POSSIBLY ACCESS DENIED EXCEPTION
    }

    public static Path rename(Path source, CharSequence newFileName) {
        return moveAs(source, source.resolveSibling(newFileName.toString()), false);
    }

    public static Path imitateNameType(Path src, String newType) {
        var type = getNameType(src);
        var strSrc = src.toString();
        var strDst = strSrc.substring(0, strSrc.length() - type.length()) + newType;
        return Path.of(strDst);
    }

    public static TGS_UnionExcuse<String> mime(Path urlFile) {
        String typeByFileNameMap = TGS_UnSafe.call(() -> {
            var type = URLConnection.getFileNameMap().getContentTypeFor(getNameFull(urlFile)).replace(";charset=UTF-8", "");
            if (TGS_StringUtils.cmn().isPresent(type) && type.length() < 5) {
                return type;
            } else {
                return null;
            }
        }, e -> null);
        if (typeByFileNameMap != null) {
            return TGS_UnionExcuse.of(typeByFileNameMap);
        }
        var typeByURLConnection = TGS_UnSafe.call(() -> {
            var url = urlFile.toUri().toURL();
            return url.openConnection().getContentType().replace(";charset=UTF-8", "");
        }, e -> null);
        if (typeByURLConnection == null) {
            return TGS_UnionExcuse.ofExcuse(d.className, "mime", "Cannot detect type for " + urlFile);
        }
        return TGS_UnionExcuse.of(typeByURLConnection);
    }
}
