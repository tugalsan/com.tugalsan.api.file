package com.tugalsan.api.file.server.watch;

import com.tugalsan.api.executable.client.TGS_Executable;
import com.tugalsan.api.executable.client.TGS_ExecutableType1;
import com.tugalsan.api.file.server.TS_FileWatchUtils;
import com.tugalsan.api.file.server.TS_FileWatchUtils.Types;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class TS_DirectoryWatchDriver {

    public static TS_Log d = TS_Log.of(TS_DirectoryWatchDriver.class);
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    private void register(Path dir, Types... types) throws IOException {
        var key = dir.register(watcher, TS_FileWatchUtils.cast(types));
        if (d.infoEnable) {
            var prev = keys.get(key);
            if (prev == null) {
                d.ci("register", "register: %s\n".formatted(dir));
            } else {
                if (!dir.equals(prev)) {
                    d.ci("register", "update: %s -> %s\n".formatted(prev, dir));
                }
            }
        }
        keys.put(key, dir);
    }

    private void registerAll(Path start, Types... types) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir, types);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private TS_DirectoryWatchDriver(Path dir, TGS_ExecutableType1<Path> forFile, boolean recursive, TS_FileWatchUtils.Types... types) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap();
        this.recursive = recursive;

        if (recursive) {
            d.ci("constructor.recursive", "Scanning %s ...\n".formatted(dir));
            registerAll(dir);
            d.ci("constructor.recursive", "Done.");
        } else {
            register(dir);
        }
        processEvents(forFile);
    }

    public static TS_DirectoryWatchDriver of(Path dir, TGS_ExecutableType1<Path> forFile, TS_FileWatchUtils.Types... types) {
        return TGS_UnSafe.compile(() -> new TS_DirectoryWatchDriver(dir, forFile, false, types));
    }

    public static TS_DirectoryWatchDriver ofRecursive(Path dir, TGS_ExecutableType1<Path> forFile, TS_FileWatchUtils.Types... types) {
        return TGS_UnSafe.compile(() -> new TS_DirectoryWatchDriver(dir, forFile, true, types));
    }

    public static TS_DirectoryWatchDriver ofFile(Path file, TGS_Executable exe) {
        return TS_DirectoryWatchDriver.of(file.getParent(), forFile -> {
            if (forFile.equals(file)) {
                exe.execute();
            }
        });
    }

    private void processEvents(TGS_ExecutableType1<Path> forFile) {
        while (true) {
            WatchKey key;
            try {
                key = watcher.take();//WAIT SIGNAL
            } catch (InterruptedException x) {
                return;
            }

            var dir = keys.get(key);
            if (dir == null) {
                d.ce("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                var kind = event.kind();
                if (kind == OVERFLOW) {
                    continue;
                }
                WatchEvent<Path> ev = cast(event);
                var name = ev.context();
                var child = dir.resolve(name);
                forFile.execute(child);
                if (recursive && (kind == ENTRY_CREATE)) {
                    TGS_UnSafe.execute(() -> {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    }, e -> d.ce("processEvents", e));
                }
            }
            if (!key.reset()) {// reset key and remove from set if directory no longer accessible
                keys.remove(key);
                if (keys.isEmpty()) {// all directories are inaccessible
                    break;
                }
            }
        }
    }
}
