package ru.myitschool.justvoice.data.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.myitschool.justvoice.data.local.AppDatabase;
import ru.myitschool.justvoice.data.local.dao.UserDao;
import ru.myitschool.justvoice.data.local.entity.BaseEntity;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.data.local.entity.User;


public class SyncManager {

    private static SyncManager instance;
    private final UserDao userDao;
    private final TranscriptionTaskDao taskDao;
    private final ApiService apiService;
    private final Context context;
    private final ExecutorService executorService;
    private final MutableLiveData<Boolean> isSyncing;
    private final MutableLiveData<String> syncError;
    private final MutableLiveData<Integer> pendingSyncCount;

    private SyncManager(Context context) {
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        this.userDao = database.userDao();
        this.taskDao = database.transcriptionTaskDao();
        this.apiService = RetrofitClient.getApiService();
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.isSyncing = new MutableLiveData<>(false);
        this.syncError = new MutableLiveData<>();
        this.pendingSyncCount = new MutableLiveData<>(0);
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    public LiveData<String> getSyncError() {
        return syncError;
    }

    public LiveData<Integer> getPendingSyncCount() {
        return pendingSyncCount;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void syncAllUnsyncedData() {
        if (!isNetworkAvailable()) {
            syncError.postValue("No network connection available");
            return;
        }

        isSyncing.postValue(true);

        executorService.execute(() -> {
            List<User> unsyncedUsers = userDao.getUnsyncedUsersSync(
                    BaseEntity.SyncStatus.SYNCED.getValue());
            for (User user : unsyncedUsers) {
                syncUser(user);
            }

            List<TranscriptionTask> unsyncedTasks = taskDao.getUnsyncedTasksSync(
                    BaseEntity.SyncStatus.SYNCED.getValue());
            for (TranscriptionTask task : unsyncedTasks) {
                syncTranscriptionTask(task);
            }

            updatePendingCount();
            isSyncing.postValue(false);
        });
    }

    public void syncUser(User user) {
        if (!isNetworkAvailable()) {
            return;
        }

        executorService.execute(() -> {
            BaseEntity.SyncStatus status = user.getSyncStatus();

            switch (status) {
                case UNSYNCED:
                    updateUserSyncStatus(user, BaseEntity.SyncStatus.SYNCED, user.getServerId());
                    break;

                case PENDING_UPDATE:
                    updateUserSyncStatus(user, BaseEntity.SyncStatus.SYNCED, user.getServerId());
                    break;

                case PENDING_DELETE:
                    userDao.deleteById(user.getId());
                    break;
            }

            updatePendingCount();
        });
    }

    public void syncTranscriptionTask(TranscriptionTask task) {
        if (!isNetworkAvailable()) {
            return;
        }

        executorService.execute(() -> {
            BaseEntity.SyncStatus status = task.getSyncStatus();

            switch (status) {
                case UNSYNCED:
                    updateTaskSyncStatus(task, BaseEntity.SyncStatus.SYNCED, task.getServerId());
                    break;

                case PENDING_UPDATE:
                    updateTaskSyncStatus(task, BaseEntity.SyncStatus.SYNCED, task.getServerId());
                    break;

                case PENDING_DELETE:
                    taskDao.deleteById(task.getId());
                    break;
            }

            updatePendingCount();
        });
    }

    private void updateUserSyncStatus(User user, BaseEntity.SyncStatus status, String serverId) {
        user.setSyncStatus(status);
        user.setServerId(serverId);
        user.setUpdatedAt(System.currentTimeMillis());
        userDao.update(user);
    }

    private void updateTaskSyncStatus(TranscriptionTask task, BaseEntity.SyncStatus status, String serverId) {
        task.setSyncStatus(status);
        task.setServerId(serverId);
        task.setUpdatedAt(System.currentTimeMillis());
        taskDao.update(task);
    }

    private void updatePendingCount() {
        executorService.execute(() -> {
            int userCount = userDao.getUnsyncedCountSync(BaseEntity.SyncStatus.SYNCED.getValue());
            int taskCount = taskDao.getUnsyncedCountSync(BaseEntity.SyncStatus.SYNCED.getValue());
            pendingSyncCount.postValue(userCount + taskCount);
        });
    }

    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}