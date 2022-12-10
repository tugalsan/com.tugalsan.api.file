package com.tugalsan.api.file.server;

import com.tugalsan.api.executable.client.TGS_ExecutableType1;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class TS_DirectoryWatch {

    public static TS_Log d = TS_Log.of(TS_DirectoryWatch.class);
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    private void register(Path dir) throws IOException {
        var key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
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

    private void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private TS_DirectoryWatch(Path dir, TGS_ExecutableType1<Path> forFile, boolean recursive) throws IOException {
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

    public static TS_DirectoryWatch of(Path dir, TGS_ExecutableType1<Path> forFile) {
        return TGS_UnSafe.compile(() -> new TS_DirectoryWatch(dir, forFile, false));
    }

    public static TS_DirectoryWatch ofRecursive(Path dir, TGS_ExecutableType1<Path> forFile) {
        return TGS_UnSafe.compile(() -> new TS_DirectoryWatch(dir, forFile, true));
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
                if (child.toFile().length() != 0L) {
                    forFile.execute(child);
                }
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
