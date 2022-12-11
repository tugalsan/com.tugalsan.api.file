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

    public static boolean file(Path targetFile, TGS_Executable exe, Types... types) {
        return directory(targetFile.getParent(), file -> {
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

    @Deprecated //Not working well
    public static boolean directoryRecursive(Path directory, TGS_ExecutableType1<Path> file, Types... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ce("watch", "diretory not found", directory);
            return false;
        }
        TS_DirectoryWatchDriver.ofRecursive(directory, file, types);
        return true;
    }

    public static boolean directory(Path directory, TGS_ExecutableType1<Path> file, Types... types) {
        if (!TS_DirectoryUtils.isExistDirectory(directory)) {
            d.ce("watch", "diretory not found", directory);
            return false;
        }
        TS_ThreadRun.now(() -> {
            TGS_UnSafe.execute(() -> {
                try ( var watchService = FileSystems.getDefault().newWatchService()) {
                    directory.register(watchService, cast(types));
                    while (true) {
                        var key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            var detectedFile = (Path) event.context();
                            if (directoryBuffer.isEmpty() || !detectedFile.equals(directoryBuffer.value1)) {
                                directoryBuffer.value0 = TGS_Time.of();
                                directoryBuffer.value1 = detectedFile;
                                file.execute(detectedFile);
                                d.ci("watchModify", "new", directoryBuffer.value1);
                                continue;
                            }
                            var oneSecondAgo = TGS_Time.ofSecondsAgo(1);
                            if (oneSecondAgo.hasSmallerTimeThanOrEqual(directoryBuffer.value0)) {
                                d.ci("watchModify", "hasSmallerTimeThanOrEqual", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                                continue;
                            }
                            directoryBuffer.value0 = oneSecondAgo.incrementSecond(1);
                            d.ci("watchModify", "passed", "oneSecondAgo", oneSecondAgo.toString_timeOnly(), "last", directoryBuffer.value0);
                            file.execute(detectedFile);
                        }
                        key.reset();
                    }
                }
            });
        });
        return true;
    }
    private static TGS_Pack2<TGS_Time, Path> directoryBuffer = TGS_Pack2.of();
}
