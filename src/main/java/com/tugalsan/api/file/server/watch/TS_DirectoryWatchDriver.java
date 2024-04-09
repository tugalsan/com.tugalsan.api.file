package com.tugalsan.api.file.server.watch;

import com.tugalsan.api.runnable.client.TGS_Runnable;
import com.tugalsan.api.runnable.client.TGS_RunnableType1;
import com.tugalsan.api.file.server.TS_FileWatchUtils;
import com.tugalsan.api.file.server.TS_FileWatchUtils.Triggers;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.union.client.TGS_Union;
import com.tugalsan.api.union.server.TS_UnionUtils;
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

    private void register(Path dir, Triggers... triggers) throws IOException {
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

    private void registerAll(Path start, Triggers... triggers) {
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    register(dir, triggers);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            d.ce("registerAll", start, ex.getMessage());
        }
    }

    private TS_DirectoryWatchDriver(Path dir, TGS_RunnableType1<Path> forFile, boolean recursive, TS_FileWatchUtils.Triggers... triggers) throws IOException {
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
    public static TGS_Union<TS_DirectoryWatchDriver> of(Path dir, TGS_RunnableType1<Path> forFile, TS_FileWatchUtils.Triggers... triggers) {
        try {
            return TGS_Union.of(new TS_DirectoryWatchDriver(dir, forFile, false, triggers));
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    @Deprecated //DOUBLE NOTIFY? AND PATH AS FILENAME?
    public static TGS_Union<TS_DirectoryWatchDriver> ofRecursive(Path dir, TGS_RunnableType1<Path> forFile, TS_FileWatchUtils.Triggers... triggers) {
        try {
            return TGS_Union.of(new TS_DirectoryWatchDriver(dir, forFile, true, triggers));
        } catch (IOException ex) {
            return TGS_Union.ofExcuse(ex);
        }
    }

    @Deprecated //PATH AS FILENAME?
    public static TGS_Union<TS_DirectoryWatchDriver> ofFile(Path file, TGS_Runnable exe) {
        return TS_DirectoryWatchDriver.of(file.getParent(), forFile -> {
            if (forFile.equals(file)) {
                exe.run();
            }
        });
    }

    private void processEvents(TGS_RunnableType1<Path> forFile) {
        while (true) {
            WatchKey key;
            try {
                key = watcher.take();//WAIT SIGNAL
            } catch (InterruptedException x) {
                TS_UnionUtils.throwAsRuntimeExceptionIfInterruptedException(x);
                return;
            }

            var dir = keys.get(key);
            if (dir == null) {
                d.ce("WatchKey not recognized!!");
                continue;
            }

            key.pollEvents().forEach(event -> {
                var kind = event.kind();
                if (!(kind == OVERFLOW)) {
                    WatchEvent<Path> ev = cast(event);
                    var name = ev.context();
                    var child = dir.resolve(name);
                    forFile.run(child);
                    if (recursive && (kind == ENTRY_CREATE)) {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
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
