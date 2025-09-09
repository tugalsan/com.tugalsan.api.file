package com.tugalsan.api.file.server;

import com.tugalsan.api.function.client.maythrowexceptions.unchecked.TGS_FuncMTU_In1;
import com.tugalsan.api.file.server.watch.TS_DirectoryWatchDriver;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.thread.server.sync.TS_ThreadSyncWait;
import com.tugalsan.api.thread.server.sync.TS_ThreadSyncTrigger;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.thread.server.async.run.TS_ThreadAsyncRun;
import com.tugalsan.api.time.client.TGS_Time;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import com.tugalsan.api.function.client.maythrowexceptions.unchecked.TGS_FuncMTU;
import com.tugalsan.api.function.client.maythrowexceptions.checked.TGS_FuncMTCUtils;
import java.util.function.Supplier;

public class TS_FileWatchUtils {

    private TS_FileWatchUtils() {

    }

    final private static Supplier<TS_Log> d = StableValue.supplier(() -> TS_Log.of(TS_FileWatchUtils.class));

    public static enum Triggers {
        CREATE, MODIFY, DELETE
    }

    public static boolean file(TS_ThreadSyncTrigger killTrigger, Path targetFile, TGS_FuncMTU exe, int maxSeconds, Triggers... types) {
        var targetFileName = TS_FileUtils.getNameFull(targetFile);
        AtomicReference<TGS_Time> lastProcessedFile_lastModified = new AtomicReference();
        return directory(killTrigger, targetFile.getParent(), filename -> {
            if (!targetFileName.equals(filename)) {
                d.get().ci("file", "INFO:skipped", "filenames not same", targetFile, filename);
                return;
            }
            d.get().ci("file", "filenames same", targetFile, filename);
            var totalSeconds = 0;
            var gapSeconds = 10;
            while (TS_FileUtils.isFileLocked(targetFile)) {
                d.get().cr("file", "file lock detected ", "waiting...", targetFile);
                TS_ThreadSyncWait.seconds("file", killTrigger, gapSeconds);
                totalSeconds += gapSeconds;
                if (totalSeconds > maxSeconds) {
                    d.get().cr("file", "file lock detected ", "totalSeconds > maxSeconds", "failed...", targetFile);
                    return;
                }
            }
            var lastModified = TS_FileUtils.getTimeLastModified(targetFile);
            if (lastModified == null) {
                d.get().ce("file", "cannot fetch lastModified", "skipping...", targetFile);
                return;
            }
            if (lastModified.equals(lastProcessedFile_lastModified.get())) {
                d.get().ce("file", "lastProcessedFile detected", "skipping...");
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
            d.get().ci("watch", "diretory not found", directory);
            return false;
        }
        TS_DirectoryWatchDriver.ofRecursive(directory, file, types);
        return true;
    }

    public static boolean directory(TS_ThreadSyncTrigger killTrigger, Path directory, TGS_FuncMTU_In1<String> filename, Triggers... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.get().ci("watch", "diretory not found", directory);
            return false;
        }
        TS_ThreadAsyncRun.now(killTrigger.newChild(d.get().className).newChild("directory"), kt -> {
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
                                d.get().ci("directory", "new", directoryBuffer.value1);
                                continue;
                            }
                            var oneSecondAgo = TGS_Time.ofSecondsAgo(1);
                            {//SKIP IF DOUBLE NOTIFY
                                if (oneSecondAgo.hasSmallerTimeThanOrEqual(directoryBuffer.value0)) {
                                    d.get().ci("directory", "skipped", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                                    continue;
                                }
                            }
                            {//NOTIFY
                                directoryBuffer.value0 = oneSecondAgo.incrementSecond(1);
                                d.get().ci("directory", "passed", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                                filename.run(TS_FileUtils.getNameFull(detectedFile));
                            }
                        }
                        key.reset();
                    }
                }
            }, e -> d.get().ce("directory", directory, e.getMessage(), "SKIP THIS ERROR ON RE-DEPLOY"));
        });
        return true;
    }
    private static final TGS_Tuple2<TGS_Time, Path> directoryBuffer = TGS_Tuple2.of();
}
