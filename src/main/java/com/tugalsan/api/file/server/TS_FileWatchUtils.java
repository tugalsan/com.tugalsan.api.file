package com.tugalsan.api.file.server;

import com.tugalsan.api.executable.client.TGS_Executable;
import com.tugalsan.api.executable.client.TGS_ExecutableType1;
import com.tugalsan.api.file.server.watch.TS_DirectoryWatchDriver;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.pack.client.TGS_Pack2;
import com.tugalsan.api.thread.server.TS_ThreadRun;
import com.tugalsan.api.time.client.TGS_Time;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.stream.IntStream;

public class TS_FileWatchUtils {

    final private static TS_Log d = TS_Log.of(TS_FileWatchUtils.class);

    public static enum Types {
        CREATE, MODIFY, DELETE
    }

    public static void file(Path targetFile, TGS_Executable exe, Types... types) {
        directory(targetFile.getParent(), file -> {
            if (targetFile.equals(file)) {
                exe.execute();
            }
        }, types);
    }

    public static WatchEvent.Kind<Path>[] cast(Types... types) {
        WatchEvent.Kind<Path>[] kinds = new WatchEvent.Kind[types.length == 0 ? 3 : types.length];
        if (types.length == 0) {
            kinds[0] = StandardWatchEventKinds.ENTRY_CREATE;
            kinds[1] = StandardWatchEventKinds.ENTRY_MODIFY;
            kinds[2] = StandardWatchEventKinds.ENTRY_DELETE;
        } else {
            IntStream.range(0, types.length).forEachOrdered(i -> {
                if (types[i] == Types.CREATE) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_CREATE;
                    return;
                }
                if (types[i] == Types.MODIFY) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_MODIFY;
                    return;
                }
                if (types[i] == Types.DELETE) {
                    kinds[i] = StandardWatchEventKinds.ENTRY_DELETE;
                    return;
                }
            });
        }
        return kinds;
    }

    public static void directoryRecursive(Path directory, TGS_ExecutableType1<Path> file, Types... types) {
        TS_DirectoryWatchDriver.ofRecursive(directory, file, types);
    }

    public static void directory(Path directory, TGS_ExecutableType1<Path> file, Types... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ce("watch", "diretory not found", directory);
        }
        TS_ThreadRun.now(() -> {
            TGS_UnSafe.execute(() -> {
                try ( var watchService = FileSystems.getDefault().newWatchService()) {
                    directory.register(watchService, cast(types));
                    while (true) {
                        var key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            var detectedFile = (Path) event.context();
                            if (buffer.isEmpty() || !detectedFile.equals(buffer.value1)) {
                                buffer.value0 = TGS_Time.of();
                                buffer.value1 = detectedFile;
                                file.execute(detectedFile);
                                d.ci("watchModify", "new", buffer.value1);
                                continue;
                            }
                            var oneSecondAgo = TGS_Time.ofSecondsAgo(1);
                            if (oneSecondAgo.hasSmallerTimeThanOrEqual(buffer.value0)) {
                                d.ci("watchModify", "hasSmallerTimeThanOrEqual", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", buffer.value0);
                                continue;
                            }
                            buffer.value0 = oneSecondAgo.incrementSecond(1);
                            d.ci("watchModify", "passed", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", buffer.value0);
                            file.execute(detectedFile);
                        }
                        key.reset();
                    }
                }
            });
        });
    }
    private static TGS_Pack2<TGS_Time, Path> buffer = TGS_Pack2.of();

}
