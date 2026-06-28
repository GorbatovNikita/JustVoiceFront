package ru.myitschool.justvoice.data.repository;

import androidx.lifecycle.LiveData;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ru.myitschool.justvoice.data.local.entity.BaseEntity;
import ru.myitschool.justvoice.data.remote.SyncManager;


public abstract class BaseRepository<T extends BaseEntity> {

    protected final SyncManager syncManager;
    protected final ExecutorService executorService;

    public BaseRepository(SyncManager syncManager) {
        this.syncManager = syncManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public abstract LiveData<Integer> getUnsyncedCount();

    public abstract void syncUnsynced();

    protected void runOnBackgroundThread(Runnable runnable) {
        executorService.execute(runnable);
    }

    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}