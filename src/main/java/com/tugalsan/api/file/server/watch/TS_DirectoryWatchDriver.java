package com.tugalsan.api.file.server.watch;

import module com.tugalsan.api.file;
import module com.tugalsan.api.log;
import module com.tugalsan.api.function;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class TS_DirectoryWatchDriver {

    final private static TS_Log d = TS_Log.of(TS_DirectoryWatchDriver.class);
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    private void register(Path dir, TS_FileWatchUtils.Triggers... triggers) throws IOException {
        var key = dir.register(watcher, TS_FileWatchUtils.cast(triggers));
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

    private void registerAll(Path start, TS_FileWatchUtils.Triggers... triggers) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir, triggers);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private TS_DirectoryWatchDriver(Path dir, TGS_FuncMTU_In1<Path> forFile, boolean recursive, TS_FileWatchUtils.Triggers... triggers) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap();
        this.recursive = recursive;

        if (recursive) {
            d.ci("constructor.recursive", "Scanning %s ...\n".formatted(dir));
            registerAll(dir, triggers);
            d.ci("constructor.recursive", "Done.");
        } else {
            register(dir, triggers);
        }
        processEvents(forFile);
    }

    @Deprecated //DOUBLE NOTIFY? AND PATH AS FILENAME?
    public static TS_DirectoryWatchDriver of(Path dir, TGS_FuncMTU_In1<Path> forFile, TS_FileWatchUtils.Triggers... triggers) {
        return TGS_FuncMTCUtils.call(() -> new TS_DirectoryWatchDriver(dir, forFile, false, triggers));
    }

    @Deprecated //DOUBLE NOTIFY? AND PATH AS FILENAME?
    public static TS_DirectoryWatchDriver ofRecursive(Path dir, TGS_FuncMTU_In1<Path> forFile, TS_FileWatchUtils.Triggers... triggers) {
        return TGS_FuncMTCUtils.call(() -> new TS_DirectoryWatchDriver(dir, forFile, true, triggers));
    }

    @Deprecated //PATH AS FILENAME?
    public static TS_DirectoryWatchDriver ofFile(Path file, TGS_FuncMTU exe) {
        return TS_DirectoryWatchDriver.of(file.getParent(), forFile -> {
            if (forFile.equals(file)) {
                exe.run();
            }
        });
    }

    private void processEvents(TGS_FuncMTU_In1<Path> forFile) {
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

            key.pollEvents().forEach(event -> {
                var kind = event.kind();
                if (kind != StandardWatchEventKinds.OVERFLOW) {
                    WatchEvent<Path> ev = cast(event);
                    var name = ev.context();
                    var child = dir.resolve(name);
                    forFile.run(child);
                    if (recursive && (kind == StandardWatchEventKinds.ENTRY_CREATE)) {
                        TGS_FuncMTCUtils.run(() -> {
                            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                                registerAll(child);
                            }
                        }, e -> d.ce("processEvents", e));
                    }
                }
            });
            if (!key.reset()) {// reset key and remove from set if directory no longer accessible
                keys.remove(key);
                if (keys.isEmpty()) {// all directories are inaccessible
                    break;
                }
            }
        }
    }
}
