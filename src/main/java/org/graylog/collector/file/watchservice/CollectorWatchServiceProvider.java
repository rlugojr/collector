package org.graylog.collector.file.watchservice;

import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;

public class CollectorWatchServiceProvider implements Provider<CollectorWatchService> {
    private static final int DEFAULT_INTERVAL = 2000;
    private static final int DEFAULT_EVENT_QUEUE_SIZE = 4096;

    private final Config config;

    @Inject
    public CollectorWatchServiceProvider(Config config) {
        this.config = config;
    }

    @Override
    public CollectorWatchService get() {
        if (config.hasPath("file-watch-service")) {
            final Config watchService = config.getConfig("file-watch-service");

            switch (watchService.getString("type")) {
                case "commons-io":
                    final long interval = watchService.hasPath("interval") ? watchService.getDuration("interval", TimeUnit.MILLISECONDS) : DEFAULT_INTERVAL;
                    final int queueSize = watchService.hasPath("event-queue-size") ? watchService.getInt("event-queue-size") : DEFAULT_EVENT_QUEUE_SIZE;

                    return new ApacheCommonsWatchService(interval, queueSize);
                default:
                    return getDefault();
            }
        }

        return getDefault();
    }

    private CollectorWatchService getDefault() {
        try {
            return new JvmWatchService(FileSystems.getDefault().newWatchService());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create WatchService");
        }
    }
}