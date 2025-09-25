package com.tugalsan.api.file.server;

import module com.tugalsan.api.log;
import module com.tugalsan.api.thread;
import module com.tugalsan.api.tuple;
import module com.tugalsan.api.time;
import module com.tugalsan.api.function;
import com.tugalsan.api.file.server.watch.TS_DirectoryWatchDriver;
import java.nio.file.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

public class TS_FileWatchUtils {

    private TS_FileWatchUtils() {

    }

    final private static TS_Log d = TS_Log.of(TS_FileWatchUtils.class);

    public static enum Triggers {
        CREATE, MODIFY, DELETE
    }

    public static boolean file(TS_ThreadSyncTrigger killTrigger, Path targetFile, TGS_FuncMTU exe, int maxSeconds, Triggers... types) {
        var targetFileName = TS_FileUtils.getNameFull(targetFile);
        AtomicReference<TGS_Time> lastProcessedFile_lastModified = new AtomicReference();
        return directory(killTrigger, targetFile.getParent(), filename -> {
            if (!targetFileName.equals(filename)) {
                d.ci("file", "INFO:skipped", "filenames not same", targetFile, filename);
                return;
            }
            d.ci("file", "filenames same", targetFile, filename);
            var totalSeconds = 0;
            var gapSeconds = 10;
            while (TS_FileUtils.isFileLocked(targetFile)) {
                d.cr("file", "file lock detected ", "waiting...", targetFile);
                TS_ThreadSyncWait.seconds("file", killTrigger, gapSeconds);
                totalSeconds += gapSeconds;
                if (totalSeconds > maxSeconds) {
                    d.cr("file", "file lock detected ", "totalSeconds > maxSeconds", "failed...", targetFile);
                    return;
                }
            }
            var lastModified = TS_FileUtils.getTimeLastModified(targetFile);
            if (lastModified == null) {
                d.ce("file", "cannot fetch lastModified", "skipping...", targetFile);
                return;
            }
            if (lastModified.equals(lastProcessedFile_lastModified.get())) {
                d.ce("file", "lastProcessedFile detected", "skipping...");
                return;
            }
            lastProcessedFile_lastModified.set(lastModified);
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
    public static boolean directoryRecursive(Path directory, TGS_FuncMTU_In1<Path> file, Triggers... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ci("watch", "diretory not found", directory);
            return false;
        }
        TS_DirectoryWatchDriver.ofRecursive(directory, file, types);
        return true;
    }

    public static boolean directory(TS_ThreadSyncTrigger killTrigger, Path directory, TGS_FuncMTU_In1<String> filename, Triggers... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ci("watch", "diretory not found", directory);
            return false;
        }
        TS_ThreadAsyncRun.now(killTrigger.newChild(d.className()).newChild("directory"), kt -> {
            TGS_FuncMTCUtils.run(() -> {
                try (var watchService = FileSystems.getDefault().newWatchService()) {
                    directory.register(watchService, cast(types));
                    WatchKey key;
                    while (kt.hasNotTriggered() && (key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            var detectedFile = (Path) event.context();
                            if (directoryBuffer.isEmpty() || !detectedFile.equals(directoryBuffer.value1)) {//IF INIT
                                directoryBuffer.value0 = TGS_Time.of();
                                directoryBuffer.value1 = detectedFile;
                                filename.run(TS_FileUtils.getNameFull(detectedFile));
                                d.ci("directory", "new", directoryBuffer.value1);
                                continue;
                            }
                            var oneSecondAgo = TGS_Time.ofSecondsAgo(1);
                            {//SKIP IF DOUBLE NOTIFY
                                if (oneSecondAgo.hasSmallerTimeThanOrEqual(directoryBuffer.value0)) {
                                    d.ci("directory", "skipped", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                                    continue;
                                }
                            }
                            {//NOTIFY
                                directoryBuffer.value0 = oneSecondAgo.incrementSecond(1);
                                d.ci("directory", "passed", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                                filename.run(TS_FileUtils.getNameFull(detectedFile));
                            }
                        }
                        key.reset();
                    }
                }
            }, e -> d.ce("directory", directory, e.getMessage(), "SKIP THIS ERROR ON RE-DEPLOY"));
        });
        return true;
    }
    private static final TGS_Tuple2<TGS_Time, Path> directoryBuffer = TGS_Tuple2.of();
}
