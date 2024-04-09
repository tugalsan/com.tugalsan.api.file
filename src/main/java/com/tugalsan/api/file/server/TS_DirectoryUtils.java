package com.tugalsan.api.file.server;

import com.tugalsan.api.charset.client.TGS_CharSetCast;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import com.tugalsan.api.list.client.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.os.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.union.client.TGS_Union;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;

public class TS_DirectoryUtils {

    final private static TS_Log d = TS_Log.of(TS_DirectoryUtils.class);

    public static String getName(Path path) {
        //EASY WAY
        var name = path.getFileName();
        if (name != null) {
            return name.toString();
        }
        //HARD WAY
        var pathStr = path.toString();
        //TRIM
        while (pathStr.endsWith(File.separator)) {
            pathStr = pathStr.substring(0, pathStr.length() - File.separator.length());
        }
        //PEEK
        var idx = pathStr.lastIndexOf(File.separator);
        if (idx == -1) {
            return null;
        }
        //RETURN
        return pathStr.substring(idx + 1);
    }

    public static TGS_Union<Boolean> rename(Path path, CharSequence newFolderName) {
        return TS_FileUtils.rename(path, newFolderName);
    }

    public static TGS_Union<Boolean> flattenDirectory(Path sourceFolder, boolean parallel) {
        d.ci("flattenDirectory", "sourceFolder", sourceFolder);
        var u_subDirs = subDirectories(sourceFolder, false, false);
        if (u_subDirs.isExcuse()) {
            return TGS_Union.ofExcuse(u_subDirs.excuse());
        }
        var subDirs = u_subDirs.value();
        d.ci("flattensubDirsDirectory", "subDirs.size()", subDirs.size());
        try {
            (parallel ? subDirs.parallelStream() : subDirs.stream()).forEach(subDir -> {
                d.ci("flattenDirectory", "subDirs", subDirs);
                var u_subFiles = subFiles(subDir, null, false, true);
                if (u_subFiles.isExcuse()) {
                    throw new FlattenDirectoryException(subDir, u_subFiles.excuse());
                }
                var subFiles = u_subDirs.value();
                d.ci("flattenDirectory", "subFiles.size()", subFiles.size());
                (parallel ? subFiles.parallelStream() : subFiles.stream()).forEach(subFile -> {
                    TS_FileUtils.moveToFolder(subFile, sourceFolder, true);
                });
                TS_DirectoryUtils.deleteDirectoryIfExists(subDir);
            });
            return TGS_Union.of(true);
        } catch (FlattenDirectoryException e) {
            return TGS_Union.ofExcuse(e);
        }
    }

    private static class FlattenDirectoryException extends RuntimeException {

        public FlattenDirectoryException(Path path, Throwable cause) {
            super(path.toString(), cause);
        }
    };

    public static TGS_Union<Boolean> moveDirectory(Path sourceFolder, Path asDestFolder, boolean overwrite, boolean parallel) {
        if (Objects.equals(sourceFolder.toAbsolutePath().toString(), asDestFolder.toAbsolutePath().toString())) {
            return TGS_Union.of(true);
        }

        if (!isExistDirectory(asDestFolder) || isEmptyDirectory(asDestFolder).orElse(false)) {
            return TS_FileUtils.moveAs(sourceFolder, asDestFolder, overwrite);
        }

        var u_strSubDirs = subDirectories2(sourceFolder, false, true);//DO NOT CHANGE THE ORDER
        if (u_strSubDirs.isExcuse()) {
            return TGS_Union.ofExcuse(u_strSubDirs.excuse());
        }
        var strSubDirs = u_strSubDirs.value();

        var u_strSubFiles = subFiles2(sourceFolder, null, false, true);
        if (u_strSubFiles.isExcuse()) {
            return TGS_Union.ofExcuse(u_strSubFiles.excuse());
        }
        var strSubFiles = u_strSubFiles.value();

        var strSource = sourceFolder.toAbsolutePath().toString();
        var strDest = asDestFolder.toAbsolutePath().toString();

        (parallel ? strSubDirs.parallelStream() : strSubDirs.stream()).forEach(strSubDir -> {
            var strDestDir = strSubDir.replace(strSource, strDest);
            createDirectoriesIfNotExists(Path.of(strDestDir));
        });
        (parallel ? strSubFiles.parallelStream() : strSubFiles.stream()).forEach(strSubFile -> {
            d.ci("strSubFile", strSubFile);
            var strDestFile = strSubFile.replace(strSource, strDest);
            d.ci("strDestFile", strDestFile);
            var strDestFolder = Path.of(strDestFile).getParent();
            d.ci("strDestFolder", strDestFolder);
            TS_FileUtils.moveToFolder(Path.of(strSubFile), strDestFolder, overwrite);
        });
        return TGS_Union.of(true);
    }

    public static TGS_Union<Boolean> copyDirectory(Path sourceFolder, Path asDestFolder) {
        return copyDirectory(sourceFolder, asDestFolder, true, false, null, true);
    }

    public static TGS_Union<Boolean> copyDirectory(Path sourceFolder, Path asDestFolder, boolean overwrite, boolean parallel,
            TGS_ValidatorType1<Path> filter, boolean skipIfSameSizeAndDateAndTime) {
        d.cr("copyDirectory.i", sourceFolder, asDestFolder, overwrite);
        var dstDirPrefix = asDestFolder.toAbsolutePath().toString();
        var u_subDirectories = subDirectories(sourceFolder, false, false);
        if (u_subDirectories.isExcuse()) {
            return TGS_Union.ofExcuse(u_subDirectories.excuse());
        }
        var subDirectories = u_subDirectories.value();
        (parallel ? subDirectories.parallelStream() : subDirectories.stream()).forEach(srcDir -> {
            var dstSubDir = Path.of(dstDirPrefix, srcDir.getFileName().toString());
            copyDirectory(srcDir, dstSubDir, overwrite, parallel, filter, skipIfSameSizeAndDateAndTime);
        });
        return copyFiles(sourceFolder, asDestFolder, overwrite, parallel, filter, skipIfSameSizeAndDateAndTime);
    }

    public static TGS_Union<Boolean> copyFiles(Path sourceFolder, Path destFolder) {
        return copyFiles(sourceFolder, destFolder, true, false, null, true);
    }

    public static TGS_Union<Boolean> copyFiles(Path sourceFolder, Path destFolder, boolean overwrite, boolean parallel,
            TGS_ValidatorType1<Path> filter, boolean skipIfSameSizeAndDateAndTime) {
        d.cr("copyFiles.i", sourceFolder, destFolder, overwrite);
        var u_createDirectoriesIfNotExists = createDirectoriesIfNotExists(destFolder);
        if (u_createDirectoriesIfNotExists.isExcuse()) {
            return TGS_Union.ofExcuse(u_createDirectoriesIfNotExists.excuse());
        }

        var dstFilePrefix = destFolder.toAbsolutePath().toString();
        var u_subFiles = subFiles(sourceFolder, null, false, false);
        if (u_subFiles.isExcuse()) {
            return TGS_Union.ofExcuse(u_subFiles.excuse());
        }
        var subFiles = u_subFiles.value();
        try {
            (parallel ? subFiles.parallelStream() : subFiles.stream()).forEach(srcFile -> {
                if (filter != null) {
                    var valid = filter.validate(srcFile);
                    if (!valid) {
                        return;
                    }
                }
                var dstFile = Path.of(dstFilePrefix, srcFile.getFileName().toString());
                if (TS_FileUtils.isExistFile(dstFile)) {
                    if (!overwrite) {
                        return;
                    }
                    if (skipIfSameSizeAndDateAndTime) {
                        var srcSize = TS_FileUtils.getFileSizeInBytes(srcFile);
                        var dstSize = TS_FileUtils.getFileSizeInBytes(dstFile);
                        var sameSize = srcSize == dstSize;
                        var srcDateAndTime = TS_FileUtils.getTimeLastModified(srcFile);
                        var dstDateAndTime = TS_FileUtils.getTimeLastModified(dstFile);
                        var sameDateAnd = srcDateAndTime.equals(dstDateAndTime);
                        if (sameSize && sameDateAnd) {
                            return;
                        }
                    }
                }
                d.cr("copyFiles.f", srcFile, dstFile, overwrite);
                var u_copyAs = TS_FileUtils.copyAs(srcFile, dstFile, overwrite);
                if (u_copyAs.isExcuse()) {
                    throw new CopyFilesException(dstFile, u_copyAs.excuse());
                }
            });
            return TGS_Union.of(true);
        } catch (CopyFilesException e) {
            return TGS_Union.ofExcuse(e);
        }
    }

    private static class CopyFilesException extends RuntimeException {

        public CopyFilesException(Path path, Throwable cause) {
            super(path.toString(), cause);
        }
    };

    public static TGS_Union<Boolean> deleteSubDirectories(Path parentDirectory, boolean parallel) {
        var u_subDirectories = subDirectories(parentDirectory, false, false);
        if (u_subDirectories.isExcuse()) {
            return TGS_Union.ofExcuse(u_subDirectories.excuse());
        }
        var subDirectories = u_subDirectories.value();
        (parallel ? subDirectories.parallelStream() : subDirectories.stream()).forEach(subDir -> {
            d.cr("deleteSubDirectories", "by deleteDirectoryIfExists", subDir);
            deleteDirectoryIfExists(subDir);
        });
        return TGS_Union.of(true);
    }

    public static void deleteSubDirectories_withBat(Path parentDirectory) {
        d.cr("deleteSubDirectories", "by bat", parentDirectory);
        var batCode = new StringJoiner("\n");
        batCode.add(TS_PathUtils.getDriveLetter(parentDirectory) + ":");
        batCode.add("cd " + parentDirectory.toAbsolutePath().toString());
        batCode.add("FOR /d /r . %%d IN (backdrops) DO @IF EXIST \"%%d\" rd /s /q \"%%d\"");
        d.cr("deleteSubDirectories", "batCode", batCode);
        TS_OsProcess.ofCode(batCode.toString(), TS_OsProcess.CodeType.BAT);
    }

    public static TGS_Union<Boolean> deleteSubFiles(Path parentDirectory, String fileNameMatcher, boolean parallel) {
        var u_subFiles = subFiles(parentDirectory, fileNameMatcher, false, false);
        if (u_subFiles.isExcuse()) {
            return TGS_Union.ofExcuse(u_subFiles.excuse());
        }
        var subFiles = u_subFiles.value();
        (parallel ? subFiles.parallelStream() : subFiles.stream()).forEach(subFile -> {
            TS_FileUtils.deleteFileIfExists(subFile);
        });
        return TGS_Union.of(true);
    }

    public static TGS_Union<Boolean> deleteDirectoryIfExists(Path path) {
        return deleteDirectoryIfExists(path, false);
    }

    public static TGS_Union<Boolean> deleteDirectoryIfExists(Path path, boolean dontDeleteSelfDirectory) {
        try (var entries = Files.newDirectoryStream(path)) {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                for (var entry : entries) {
                    deleteDirectoryIfExists(entry, false);
                }
            }
            if (!dontDeleteSelfDirectory) {
                Files.delete(path);
            }
            return TGS_Union.of(true);
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    @Deprecated//MAY NOT BE WORKING ON WINDOWS SERVER 2008 R2
    public static TGS_Union<Boolean> deleteDirectoryIfExists2(Path path, boolean dontDeleteSelfDirectory) {
        try {
            if (!isExistDirectory(path)) {
                return TGS_Union.of(true);
            }
            var pathStr = path.toAbsolutePath().toString();
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    d.ci("visitFile", file);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    d.ci("postVisitDirectory", dir);
                    if (TGS_CharSetCast.equalsLocaleIgnoreCase(dir.toAbsolutePath().toString(), pathStr)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            if (!dontDeleteSelfDirectory) {
                Files.deleteIfExists(path);
            }
            return TGS_Union.of(true);
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static boolean isExistDirectory(Path directory) {
        return directory != null && Files.isDirectory(directory) && Files.exists(directory);
    }

    public static TGS_Union<Boolean> createDirectoriesIfNotExists(Path directory) {
        try {
            if (!isExistDirectory(directory)) {
                directory.toFile().mkdirs();
                //return Files.createDirectories(directory);//BUGGY
            }
            return TGS_Union.of(true);
        } catch (SecurityException e) {
            return TGS_Union.ofExcuse(e);
        }
    }

    public static TGS_Union<Boolean> isEmptyDirectory(Path directory) {
        return isEmptyDirectory(directory, false, false);
    }

    public static TGS_Union<Boolean> isEmptyDirectory(Path directory, boolean recursive, boolean parallel) {
        if (recursive) {
            if (!isExistDirectory(directory)) {
                return TGS_Union.of(false);
            }
            if (isEmptyDirectory(directory, false, false).orElse(false)) {
                return TGS_Union.of(true);
            }
            var u_subDirectories = subDirectories(directory, false, true);
            if (u_subDirectories.isExcuse()) {
                return TGS_Union.ofExcuse(u_subDirectories.excuse());
            }
            var subDirectories = u_subDirectories.value();
            var r = (parallel ? subDirectories.parallelStream() : subDirectories.stream())
                    .filter(p -> isEmptyDirectory(directory, false, false).orElse(false) == false)
                    .findAny();
            return TGS_Union.of(!r.isPresent());
        }
        createDirectoriesIfNotExists(directory);
        try (var dirStream = Files.newDirectoryStream(directory)) {
            return TGS_Union.of(!dirStream.iterator().hasNext());
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<Boolean> deleteDirectoryIfExistsIfEmpty(Path directory) {
        return deleteDirectoryIfExistsIfEmpty(directory, false);
    }

    public static TGS_Union<Boolean> deleteDirectoryIfExistsIfEmpty(Path directory, boolean recursive) {
        if (recursive) {
            if (!isEmptyDirectory(directory, true, false).orElse(false)) {
                return TGS_Union.of(false);
            }
            return deleteDirectoryIfExists(directory);
        }
        try {
            Files.deleteIfExists(directory);
            return TGS_Union.of(true);
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    public static TGS_Union<List<Path>> subFilesByType(Path path, String type) {
        return TS_DirectoryUtils.subFiles(path, "*." + type, false, false);
    }

    public static TGS_Union<List<Path>> subFilesNameStartsWith(Path path, String nameStartsWith) {
        return TS_DirectoryUtils.subFiles(path, nameStartsWith + "_*.*", false, false);
    }

    public static TGS_Union<List<Path>> subFiles(Path parentDirectory, CharSequence fileNameMatcher, boolean sorted, boolean recursive) {
        var u = subFiles2(parentDirectory, fileNameMatcher, sorted, recursive);
        if (u.isExcuse()) {
            return TGS_Union.ofExcuse(u.excuse());
        }
        return TGS_Union.of(
                TGS_StreamUtils.toLst(
                        u.value().stream()
                                .map(str -> Path.of(str))
                )
        );
    }

    //DONT TOUCH: ARRAYLIST<PATH> DOES NOT WORKING, DONT KNOW WHY!!
    public static TGS_Union<List<String>> subFiles2(Path parentDirectory, CharSequence fileNameMatcher, boolean sorted, boolean recursive) {
        createDirectoriesIfNotExists(parentDirectory);
        List<String> subFiles;
        if (fileNameMatcher == null) {
            if (recursive) {
                try {
                    subFiles = TGS_StreamUtils.toLst(
                            Files.walk(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p)).map(p -> p.toString())
                    );
                } catch (IOException ex) {
                    return TGS_Union.ofExcuse(ex);
                }
            } else {
                try {
                    subFiles = TGS_StreamUtils.toLst(
                            Files.list(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p)).map(p -> p.toString())
                    );
                } catch (IOException ex) {
                    return TGS_Union.ofExcuse(ex);
                }
            }
        } else {
            var fileNameMatcherStr = fileNameMatcher.toString();
            var matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + fileNameMatcherStr);//"glob:*.java" or glob:**/*.java;
            var matcherUP = FileSystems.getDefault().getPathMatcher("glob:**/" + TGS_CharSetCast.toLocaleUpperCase(fileNameMatcherStr));//"glob:*.java" or glob:**/*.java;
            var matcherDW = FileSystems.getDefault().getPathMatcher("glob:**/" + TGS_CharSetCast.toLocaleUpperCase(fileNameMatcherStr));//"glob:*.java" or glob:**/*.java;
            if (recursive) {
                try {
                    subFiles = TGS_StreamUtils.toLst(
                            Files.walk(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p) && (matcher.matches(p) || matcherUP.matches(p) || matcherDW.matches(p)))
                                    .map(p -> p.toString())
                    );
                } catch (IOException ex) {
                    return TGS_Union.ofExcuse(ex);
                }
            } else {
                try {
                    subFiles = TGS_StreamUtils.toLst(
                            Files.list(parentDirectory)
                                    .filter(p -> !Files.isDirectory(p) && (matcher.matches(p) || matcherUP.matches(p) || matcherDW.matches(p)))
                                    .map(p -> p.toString())
                    );
                } catch (IOException ex) {
                    return TGS_Union.ofExcuse(ex);
                }
            }
        }
        if (sorted) {
            subFiles = TGS_ListUtils.of(subFiles);
            Collections.sort(subFiles);
        }
        return TGS_Union.of(subFiles);
    }

    public static TGS_Union<List<String>> subDirectories2(Path parentDirectory, boolean sorted, boolean recursive) {
        var u = subDirectories(parentDirectory, sorted, recursive);
        if (u.isExcuse()) {
            return TGS_Union.ofExcuse(u.excuse());
        }
        return TGS_Union.of(
                TGS_StreamUtils.toLst(
                        u.value().stream()
                                .map(p -> p.toAbsolutePath().toString())
                )
        );
    }

    public static TGS_Union<List<Path>> subDirectories(Path parentDirectory, boolean sorted, boolean recursive) {
        createDirectoriesIfNotExists(parentDirectory);
        List<Path> subDirectories;
        if (recursive) {
            try {
                subDirectories = TGS_StreamUtils.toLst(
                        Files.walk(parentDirectory)
                                .filter(Files::isDirectory)
                );
            } catch (IOException ex) {
                return TGS_Union.ofExcuse(ex);
            }
        } else {
            try {
                subDirectories = TGS_StreamUtils.toLst(
                        Files.list(parentDirectory)
                                .filter(Files::isDirectory)
                );
            } catch (IOException ex) {
                return TGS_Union.ofExcuse(ex);
            }
        }
        subDirectories = TGS_ListUtils.of(subDirectories);
        if (sorted) {
            Collections.sort(subDirectories);
        }
        return TGS_Union.of(subDirectories);
    }

}
