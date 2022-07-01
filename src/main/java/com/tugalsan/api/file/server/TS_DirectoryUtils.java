package com.tugalsan.api.file.server;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import com.tugalsan.api.list.client.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.os.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;

public class TS_DirectoryUtils {

    final private static TS_Log d = TS_Log.of(TS_DirectoryUtils.class.getSimpleName());

    public static Path rename(Path path, CharSequence newFolderName) {
        return TS_FileUtils.rename(path, newFolderName);
    }

    public static void flattenDirectory(Path sourceFolder) {
        d.ci("flattenDirectory", "sourceFolder", sourceFolder);
        var subDirs = subDirectories(sourceFolder, false, false);
        d.ci("flattenDirectory", "subDirs.size()", subDirs.size());
        subDirs.parallelStream().forEach(subDir -> {
            d.ci("flattenDirectory", "subDirs", subDirs);
            var subFiles = subFiles(subDir, null, false, true);
            d.ci("flattenDirectory", "subFiles.size()", subFiles.size());
            subFiles.parallelStream().forEach(subFile -> {
                TS_FileUtils.moveToFolder(subFile, sourceFolder);
            });
            TS_DirectoryUtils.deleteDirectoryIfExists(subDir);
        });
    }

    public static Path moveDirectory(Path sourceFolder, Path asDestFolder) {
        if (Objects.equals(sourceFolder.toAbsolutePath().toString(), asDestFolder.toAbsolutePath().toString())) {
            return asDestFolder;
        }

        if (!isExistDirectory(asDestFolder) || isEmptyDirectory(asDestFolder)) {
            TS_FileUtils.moveAsFile(sourceFolder, asDestFolder);
            return asDestFolder;
        }

        var strSubDirs = subDirectories2(sourceFolder, false, true);//DO NOT CHANGE THE ORDER
        var strSubFiles = subFiles2(sourceFolder, null, false, true);

        var strSource = sourceFolder.toAbsolutePath().toString();
        var strDest = asDestFolder.toAbsolutePath().toString();

        strSubDirs.parallelStream().forEach(strSubDir -> {
            var strDestDir = strSubDir.replace(strSource, strDest);
            createDirectoriesIfNotExists(Path.of(strDestDir));
        });
        strSubFiles.parallelStream().forEach(strSubFile -> {
            d.ci("strSubFile", strSubFile);
            var strDestFile = strSubFile.replace(strSource, strDest);
            d.ci("strDestFile", strDestFile);
            var strDestFolder = Path.of(strDestFile).getParent();
            d.ci("strDestFolder", strDestFolder);
            TS_FileUtils.moveToFolder(Path.of(strSubFile), strDestFolder);
        });

        return asDestFolder;
    }

    public static void copyDirectory(Path sourceFolder, Path asDestFolder, boolean overwrite) {
        d.cr("copyDirectory.i", sourceFolder, asDestFolder, overwrite);
        var dstDirPrefix = asDestFolder.toAbsolutePath().toString();
        subDirectories(sourceFolder, false, false).parallelStream().forEach(srcDir -> {
            var dstSubDir = Path.of(dstDirPrefix, srcDir.getFileName().toString());
            copyDirectory(srcDir, dstSubDir, overwrite);
        });
        copyFiles(sourceFolder, asDestFolder, overwrite);
    }

    public static void copyFiles(Path sourceFolder, Path destFolder, boolean overwrite) {
        d.cr("copyFiles.i", sourceFolder, destFolder, overwrite);
        createDirectoriesIfNotExists(destFolder);
        var dstFilePrefix = destFolder.toAbsolutePath().toString();
        subFiles(sourceFolder, null, false, false).parallelStream().forEach(srcFile -> {
            var dstFile = Path.of(dstFilePrefix, srcFile.getFileName().toString());
            d.cr("copyFiles.f", srcFile, dstFile, overwrite);
            TS_FileUtils.copyFile(srcFile, dstFile, overwrite);
        });
    }

    public static void deleteSubDirectories(Path parentDirectory) {
        subDirectories(parentDirectory, false, false).parallelStream().forEach(subDir -> {
            d.cr("deleteSubDirectories", "by deleteDirectoryIfExists", subDir);
            deleteDirectoryIfExists(subDir);
        });
    }

    public static void deleteSubDirectories_withBat(Path parentDirectory) {
        d.cr("deleteSubDirectories", "by bat", parentDirectory);
        var batCode = new StringJoiner("\n");
        batCode.add(TS_PathUtils.getDriveLetter(parentDirectory) + ":");
        batCode.add("cd " + parentDirectory.toAbsolutePath().toString());
        batCode.add("FOR /d /r . %%d IN (backdrops) DO @IF EXIST \"%%d\" rd /s /q \"%%d\"");
        d.cr("deleteSubDirectories", "batCode", batCode);
        TS_RuntimeUtils.runConsoleBAT_readResult(batCode.toString());
    }

    public static void deleteSubFiles(Path parentDirectory, String fileNameMatcher) {
        subFiles(parentDirectory, fileNameMatcher, false, false).parallelStream().forEach(subFile -> TS_FileUtils.deleteFileIfExists(subFile));
    }

    public static boolean deleteDirectoryIfExists(Path path) {
        return deleteDirectoryIfExists(path, false);
    }

    public static boolean deleteDirectoryIfExists(Path path, boolean dontDeleteSelfDirectory) {
        try {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                try ( var entries = Files.newDirectoryStream(path)) {
                    for (var entry : entries) {
                        deleteDirectoryIfExists(entry, false);
                    }
                }
            }
            if (!dontDeleteSelfDirectory) {
                Files.delete(path);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Deprecated//MAY NOT BE WORKING ON WINDOWS SERVER 2008 R2
    public static void deleteDirectoryIfExists2(Path path, boolean dontDeleteSelfDirectory) {
        TGS_UnSafe.execute(() -> {
            if (!isExistDirectory(path)) {
                return;
            }
            var pathStr = path.toAbsolutePath().toString();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    return TGS_UnSafe.compile(() -> {
                        d.ci("visitFile", file);
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    });
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return TGS_UnSafe.compile(() -> {
                        d.ci("postVisitDirectory", dir);
                        if (dir.toAbsolutePath().toString().equalsIgnoreCase(pathStr)) {
                            return FileVisitResult.CONTINUE;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    });
                }
            });
            if (!dontDeleteSelfDirectory) {
                Files.deleteIfExists(path);
            }
        });
    }

    public static Path assureExists(Path path) {
        TS_DirectoryUtils.createDirectoriesIfNotExists(path);
        if (!TS_DirectoryUtils.isExistDirectory(path)) {
            TGS_UnSafe.catchMeIfUCan(d.className, "assureExists", "!TS_DirectoryUtils.isExistDirectory(path)");
        }
        return path;
    }

    public static boolean isExistDirectory(Path directory) {
        return directory != null && Files.isDirectory(directory) && Files.exists(directory);
    }

    public static boolean createDirectoriesIfNotExists(Path directory) {
        try {
            if (!isExistDirectory(directory)) {
                directory.toFile().mkdirs();
                //return Files.createDirectories(directory);//BUGGY
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isEmptyDirectory(Path directory) {
        return isEmptyDirectory(directory, false);
    }

    public static boolean isEmptyDirectory(Path directory, boolean recursive) {
        return TGS_UnSafe.compile(() -> {
            if (recursive) {
                if (!isExistDirectory(directory)) {
                    return false;
                }
                if (isEmptyDirectory(directory, false)) {
                    return true;
                }
                var r = subDirectories(directory, false, true).parallelStream().filter(p -> isEmptyDirectory(directory, false) == false).findAny();
                return !r.isPresent();
            } else {
                assureExists(directory);
                try ( var dirStream = Files.newDirectoryStream(directory)) {
                    return !dirStream.iterator().hasNext();
                }
            }
        });
    }

    public static boolean deleteDirectoryIfExistsIfEmpty(Path directory) {
        return deleteDirectoryIfExistsIfEmpty(directory, false);
    }

    public static boolean deleteDirectoryIfExistsIfEmpty(Path directory, boolean recursive) {
        try {
            if (recursive) {
                if (!isEmptyDirectory(directory, true)) {
                    return false;
                }
                return deleteDirectoryIfExists(directory);
            }
            Files.deleteIfExists(directory);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Path> subFilesByType(Path path, String type) {
        d.ci("subFilesByType", path, "type", type);
        var files = TS_DirectoryUtils.subFiles(path, "*." + type, false, false);
        d.ci("subFilesByType", path, "size", files.size());
        return files;
    }

    public static List<Path> subFilesNameStartsWith(Path path, String nameStartsWith) {
        d.ci("getFiles_Zip", "path", path, "prefix", nameStartsWith);
        var files = TS_DirectoryUtils.subFiles(path, nameStartsWith + "_*.*", false, false);
        d.ci("getFiles_Zip", path, "size", files.size());
        return files;
    }

    public static List<Path> subFiles(Path parentDirectory, CharSequence fileNameMatcher, boolean sorted, boolean recursive) {
        return TGS_StreamUtils.toList(
                subFiles2(parentDirectory, fileNameMatcher, sorted, recursive)
                        .stream().map(str -> Path.of(str))
        );
    }

    //DONT TOUCH: ARRAYLIST<PATH> DOES NOT WORKING, DONT KNOW WHY!!
    public static List<String> subFiles2(Path parentDirectory, CharSequence fileNameMatcher, boolean sorted, boolean recursive) {
        return TGS_UnSafe.compile(() -> {
            assureExists(parentDirectory);
            List<String> subFiles;
            if (fileNameMatcher == null) {
                if (recursive) {
                    subFiles = TGS_StreamUtils.toList(
                            Files.walk(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p)).map(p -> p.toString())
                    );
                } else {
                    subFiles = TGS_StreamUtils.toList(
                            Files.list(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p)).map(p -> p.toString())
                    );
                }
            } else {
                var fileNameMatcherStr = fileNameMatcher.toString();
                var matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr);//"glob:*.java" or glob:**/*.java;
                var matcherUP = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr.toUpperCase(Locale.ROOT));//"glob:*.java" or glob:**/*.java;
                var matcherUP2 = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr.toUpperCase());//"glob:*.java" or glob:**/*.java;
                var matcherDW = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr.toLowerCase(Locale.ROOT));//"glob:*.java" or glob:**/*.java;
                var matcherDW2 = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr.toLowerCase());//"glob:*.java" or glob:**/*.java;
                if (recursive) {
                    subFiles = TGS_StreamUtils.toList(
                            Files.walk(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p) && (matcher.matches(p) || matcherUP.matches(p) || matcherDW.matches(p) || matcherUP2.matches(p) || matcherDW2.matches(p))).map(p -> p.toString())
                    );
                } else {
                    subFiles = TGS_StreamUtils.toList(
                            Files.list(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p) && (matcher.matches(p) || matcherUP.matches(p) || matcherDW.matches(p) || matcherUP2.matches(p) || matcherDW2.matches(p))).map(p -> p.toString())
                    );
                }
            }
            if (sorted) {
                subFiles = TGS_ListUtils.of(subFiles);
                Collections.sort(subFiles);
            }
            return subFiles;
        });
    }

    public static List<String> subDirectories2(Path parentDirectory, boolean sorted, boolean recursive) {
        return TGS_StreamUtils.toList(
                subDirectories(parentDirectory, sorted, recursive).stream()
                        .map(p -> p.toAbsolutePath().toString())
        );
    }

    public static List<Path> subDirectories(Path parentDirectory, boolean sorted, boolean recursive) {
        return TGS_UnSafe.compile(() -> {
            assureExists(parentDirectory);
            List<Path> subDirectories;
            if (recursive) {
                subDirectories = TGS_StreamUtils.toList(
                        Files.walk(parentDirectory)
                                .filter(Files::isDirectory)
                );
            } else {
                subDirectories = TGS_StreamUtils.toList(
                        Files.list(parentDirectory)
                                .filter(Files::isDirectory)
                );
            }
            subDirectories = TGS_ListUtils.of(subDirectories);
            if (sorted) {
                Collections.sort(subDirectories);
            }
            return subDirectories;
        });
    }

}
