package com.tugalsan.api.file.server;

import com.tugalsan.api.runnable.client.TGS_Runnable;
import com.tugalsan.api.runnable.client.TGS_RunnableType1;
import com.tugalsan.api.file.server.watch.TS_DirectoryWatchDriver;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.thread.server.sync.TS_ThreadSyncTrigger;
import com.tugalsan.api.thread.server.async.TS_ThreadAsync;
import com.tugalsan.api.time.client.TGS_Time;
import com.tugalsan.api.union.server.TS_UnionUtils;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class TS_FileWatchUtils {

    final public static TS_Log d = TS_Log.of(TS_FileWatchUtils.class);

    public static enum Triggers {
        CREATE, MODIFY, DELETE
    }

    public static boolean file(TS_ThreadSyncTrigger killTrigger, Path targetFile, TGS_Runnable exe, Triggers... types) {
        var targetFileName = TS_FileUtils.getNameFull(targetFile);
        AtomicReference<TGS_Time> lastProcessedFile_lastModified = new AtomicReference();
        return directory(killTrigger, targetFile.getParent(), filename -> {
            if (!targetFileName.equals(filename)) {
                d.ci("file", "INFO:skipped", "filenames not same", targetFile, filename);
                return;
            }
            d.ci("file", "filenames same", targetFile, filename);
            var u = TS_FileUtils.getTimeLastModified(targetFile);
            if (u.isExcuse()) {
                d.ce("file", "cannot fetch lastModified", "skipping...", targetFile, u.excuse().getMessage());
                return;
            }
            var lastModified = u.value();
            if (lastModified.equals(lastProcessedFile_lastModified.get())) {
                d.ce("file", "lastProcessedFile detected", "skipping...");
                return;
            }
            lastProcessedFile_lastModified.set(u.value());
            exe.run();
        }, types);
    }

    public static WatchEvent.Kind<Path>[] cast(Triggers... types) {
        WatchEvent.Kind<Path>[] kinds = new WatchEvent.Kind[types.length == 0 ? 3 : types.length];
        if (types.length == 0) {
            kinds[0] = StandardWatchEventKinds.ENTRY_CREATE;
            kinds[1] = StandardWatchEventKinds.ENTRY_MODIFY;
            kinds[2] = StandardWatchEventKinds.ENTRY_DELETE;
        } else {
            IntStream.range(0, types.length).forEachOrdered(i -> {
                if (types[i] == Triggers.CREATE) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_CREATE;
                    return;
                }
                if (types[i] == Triggers.MODIFY) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_MODIFY;
                    return;
                }
                if (types[i] == Triggers.DELETE) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_DELETE;
                    return;
                }
            });
        }
        return kinds;
    }

    @Deprecated //DOUBLE NOTIFY? AND PATH AS FILENAME?
    public static boolean directoryRecursive(Path directory, TGS_RunnableType1<Path> file, Triggers... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ci("watch", "diretory not found", directory);
            return false;
        }
        TS_DirectoryWatchDriver.ofRecursive(directory, file, types);
        return true;
    }

    public static boolean directory(TS_ThreadSyncTrigger killTrigger, Path directory, TGS_RunnableType1<String> filename, Triggers... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ci("watch", "diretory not found", directory);
            return false;
        }
        TS_ThreadAsync.now(killTrigger, kt -> {
            try (var watchService = FileSystems.getDefault().newWatchService()) {
                directory.register(watchService, cast(types));
                WatchKey key;
                while (kt.hasNotTriggered() && (key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        var detectedFile = (Path) event.context();
                        if (directoryBuffer.isEmpty() || !detectedFile.equals(directoryBuffer.path)) {//IF INIT
                            directoryBuffer.time = TGS_Time.of();
                            directoryBuffer.path = detectedFile;
                            filename.run(TS_FileUtils.getNameFull(detectedFile));
                            d.ci("directory", "new", directoryBuffer.path);
                            continue;
                        }
                        var oneSecondAgo = TGS_Time.ofSecondsAgo(1);
                        {//SKIP IF DOUBLE NOTIFY
                            if (oneSecondAgo.hasSmallerTimeThanOrEqual(directoryBuffer.time)) {
                                d.ci("directory", "skipped", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.time);
                                continue;
                            }
                        }
                        {//NOTIFY
                            directoryBuffer.time = oneSecondAgo.incrementSecond(1);
                            d.ci("directory", "passed", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.time);
                            filename.run(TS_FileUtils.getNameFull(detectedFile));
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException | IOException ex) {
                TS_UnionUtils.throwAsRuntimeExceptionIfInterruptedException(ex);
                d.ce("directory.now", directory, ex.getMessage());
            }
        });
        return true;
    }
    private static final DirectoryBuffer directoryBuffer = new DirectoryBuffer();

    private static class DirectoryBuffer {

        public TGS_Time time;
        public Path path;

        public boolean isEmpty() {
            return time == null;
        }
    }
}
