package ru.myitschool.justvoice.data.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.data.local.AppDatabase;
import ru.myitschool.justvoice.data.local.entity.BaseEntity;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.SyncManager;
import ru.myitschool.justvoice.data.remote.TranscriptionTaskDao;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionResponse;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionStatusResponse;


public class TranscriptionRepository extends BaseRepository<TranscriptionTask> {

    private final TranscriptionTaskDao taskDao;
    private final ApiService apiService;
    private final Context context;
    private final ExecutorService dbExecutor;
    private final Handler mainHandler;
    private static volatile TranscriptionRepository instance;

    private TranscriptionRepository(Context context) {
        super(SyncManager.getInstance(context));
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        this.taskDao = database.transcriptionTaskDao();
        this.apiService = RetrofitClient.getApiService();
        this.context = context.getApplicationContext();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static TranscriptionRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TranscriptionRepository.class) {
                if (instance == null) {
                    instance = new TranscriptionRepository(context);
                }
            }
        }
        return instance;
    }

    public void transcribeAudio(Uri audioUri, long userId, TranscriptionCallback callback) {
        dbExecutor.execute(() -> {
            try {
                File originalFile = getFileFromUri(audioUri);
                File renamedFile = renameToProperExtension(originalFile);

                String mimeType = getMimeType(renamedFile.getName());

                RequestBody requestFile = RequestBody.create(
                        MediaType.parse(mimeType), renamedFile);

                MultipartBody.Part body = MultipartBody.Part.createFormData(
                        "file", renamedFile.getName(), requestFile);

                apiService.transcribeAudio(body).enqueue(new Callback<TranscriptionStatusResponse>() {
                    @Override
                    public void onResponse(Call<TranscriptionStatusResponse> call,
                                           Response<TranscriptionStatusResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TranscriptionStatusResponse serverResponse = response.body();

                            dbExecutor.execute(() -> {
                                TranscriptionTask existing = taskDao.getTaskByServerId(
                                        String.valueOf(serverResponse.getId()));

                                if (existing != null) {
                                    existing.setStatus(serverResponse.getStatus());
                                    existing.setLanguage(serverResponse.getLanguage());
                                    existing.setDuration(serverResponse.getDuration());
                                    existing.setSegmentsCount(serverResponse.getSegmentsCount());
                                    existing.setDisplayName(serverResponse.getDisplayName());
                                    existing.setTopic(serverResponse.getTopic());
                                    existing.setSyncStatus(BaseEntity.SyncStatus.SYNCED);
                                    taskDao.update(existing);
                                    mainHandler.post(() -> callback.onSuccess(existing.getId(), serverResponse));
                                } else {
                                    TranscriptionTask localTask = new TranscriptionTask(
                                            serverResponse.getOriginalFilename(),
                                            serverResponse.getStatus(),
                                            userId
                                    );
                                    localTask.setSyncStatus(BaseEntity.SyncStatus.SYNCED);
                                    localTask.setServerId(String.valueOf(serverResponse.getId()));
                                    localTask.setLanguage(serverResponse.getLanguage());
                                    localTask.setDuration(serverResponse.getDuration());
                                    localTask.setSegmentsCount(serverResponse.getSegmentsCount());
                                    localTask.setDisplayName(serverResponse.getDisplayName());
                                    localTask.setTopic(serverResponse.getTopic());

                                    long localId = taskDao.insert(localTask);
                                    mainHandler.post(() -> callback.onSuccess(localId, serverResponse));
                                }
                                originalFile.delete();
                            });
                        } else {
                            mainHandler.post(() -> callback.onError("Server error: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<TranscriptionStatusResponse> call, Throwable t) {
                        mainHandler.post(() -> callback.onError(t.getMessage()));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void listTranscriptions(long userId, TranscriptionListCallback callback) {
        apiService.listTranscriptions().enqueue(new Callback<List<TranscriptionStatusResponse>>() {
            @Override
            public void onResponse(Call<List<TranscriptionStatusResponse>> call,
                                   Response<List<TranscriptionStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dbExecutor.execute(() -> {
                        for (TranscriptionStatusResponse serverTask : response.body()) {
                            TranscriptionTask localTask = taskDao.getTaskByServerId(
                                    String.valueOf(serverTask.getId()));

                            if (localTask == null) {
                                localTask = new TranscriptionTask(
                                        serverTask.getOriginalFilename(),
                                        serverTask.getStatus(),
                                        userId
                                );
                                localTask.setServerId(String.valueOf(serverTask.getId()));
                            }

                            localTask.setStatus(serverTask.getStatus());
                            localTask.setLanguage(serverTask.getLanguage());
                            localTask.setDuration(serverTask.getDuration());
                            localTask.setSegmentsCount(serverTask.getSegmentsCount());
                            localTask.setDisplayName(serverTask.getDisplayName());
                            localTask.setTopic(serverTask.getTopic());  // ← ADD THIS LINE
                            localTask.setOriginalFilename(serverTask.getOriginalFilename());
                            localTask.setSyncStatus(BaseEntity.SyncStatus.SYNCED);

                            if (localTask.getId() == 0) {
                                taskDao.insert(localTask);
                            } else {
                                taskDao.update(localTask);
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(response.body()));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get transcriptions"));
                }
            }

            @Override
            public void onFailure(Call<List<TranscriptionStatusResponse>> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        });
    }

    public void getTranscription(int taskId, TranscriptionDetailCallback callback) {
        apiService.getTranscription(taskId).enqueue(new Callback<TranscriptionResponse>() {
            @Override
            public void onResponse(Call<TranscriptionResponse> call,
                                   Response<TranscriptionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TranscriptionResponse serverResponse = response.body();

                    dbExecutor.execute(() -> {
                        TranscriptionTask localTask = taskDao.getTaskByServerId(
                                String.valueOf(serverResponse.getId()));

                        if (localTask != null) {
                            localTask.setStatus(serverResponse.getStatus());
                            localTask.setLanguage(serverResponse.getLanguage());
                            localTask.setDuration(serverResponse.getDuration());
                            localTask.setDisplayName(serverResponse.getDisplayName());
                            localTask.setSegmentsCount(
                                    serverResponse.getSegments() != null ?
                                            serverResponse.getSegments().size() : 0);
                            taskDao.update(localTask);
                        }

                        mainHandler.post(() -> callback.onSuccess(serverResponse));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Transcription not found"));
                }
            }

            @Override
            public void onFailure(Call<TranscriptionResponse> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage()));
            }
        });
    }

    public void renameTranscription(String serverId, String newName, RenameCallback callback) {
        try {
            int taskId = Integer.parseInt(serverId);
            Map<String, String> nameData = new HashMap<>();
            nameData.put("display_name", newName);

            apiService.renameTranscription(taskId, nameData).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        dbExecutor.execute(() -> {
                            TranscriptionTask localTask = taskDao.getTaskByServerId(serverId);
                            if (localTask != null) {
                                localTask.setDisplayName(newName);
                                taskDao.update(localTask);
                            }
                            mainHandler.post(() -> callback.onSuccess(newName));
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to rename"));
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    mainHandler.post(() -> callback.onError(t.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            mainHandler.post(() -> callback.onError("Invalid task ID"));
        }
    }

    public void deleteTranscription(String serverId, DeleteCallback callback) {
        try {
            int taskId = Integer.parseInt(serverId);

            apiService.deleteTranscription(taskId).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        dbExecutor.execute(() -> {
                            TranscriptionTask localTask = taskDao.getTaskByServerId(serverId);
                            if (localTask != null) {
                                taskDao.delete(localTask);
                            }
                            mainHandler.post(() -> callback.onSuccess());
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to delete"));
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    mainHandler.post(() -> callback.onError(t.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            mainHandler.post(() -> callback.onError("Invalid task ID"));
        }
    }

    public void retryTranscription(String serverId, RetryCallback callback) {
        try {
            int taskId = Integer.parseInt(serverId);

            apiService.retryTranscription(taskId).enqueue(new Callback<TranscriptionStatusResponse>() {
                @Override
                public void onResponse(Call<TranscriptionStatusResponse> call,
                                       Response<TranscriptionStatusResponse> response) {
                    if (response.isSuccessful()) {
                        dbExecutor.execute(() -> {
                            TranscriptionTask localTask = taskDao.getTaskByServerId(serverId);
                            if (localTask != null) {
                                localTask.setStatus("pending");
                                localTask.setTopic(null);
                                taskDao.update(localTask);
                            }
                            mainHandler.post(() -> callback.onSuccess());
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Retry failed"));
                    }
                }

                @Override
                public void onFailure(Call<TranscriptionStatusResponse> call, Throwable t) {
                    mainHandler.post(() -> callback.onError(t.getMessage()));
                }
            });
        } catch (NumberFormatException e) {
            mainHandler.post(() -> callback.onError("Invalid task ID"));
        }
    }

    public interface RetryCallback {
        void onSuccess();
        void onError(String error);
    }

    private File renameToProperExtension(File originalFile) {
        String path = originalFile.getAbsolutePath();
        String newPath = path;

        if (path.endsWith(".tmp")) {
            newPath = path.replace(".tmp", ".mp3");
        } else if (!path.endsWith(".mp3") && !path.endsWith(".wav") &&
                !path.endsWith(".ogg") && !path.endsWith(".flac") &&
                !path.endsWith(".aac") && !path.endsWith(".m4a")) {
            newPath = path + ".mp3";
        }

        if (!path.equals(newPath)) {
            File renamedFile = new File(newPath);
            originalFile.renameTo(renamedFile);
            return renamedFile;
        }
        return originalFile;
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".ogg")) return "audio/ogg";
        if (fileName.endsWith(".flac")) return "audio/flac";
        if (fileName.endsWith(".aac")) return "audio/aac";
        if (fileName.endsWith(".m4a")) return "audio/mp4";
        return "audio/mpeg";
    }

    private File getFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("audio_", ".mp3", context.getCacheDir());
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    public LiveData<List<TranscriptionTask>> getLocalTasks(long userId) {
        return taskDao.getTasksByUserId(userId);
    }

    public void deleteLocalTask(TranscriptionTask task) {
        task.markForDeletion();
        dbExecutor.execute(() -> {
            taskDao.update(task);
            syncManager.syncTranscriptionTask(task);
        });
    }

    @Override
    public LiveData<Integer> getUnsyncedCount() {
        return taskDao.getUnsyncedCount(BaseEntity.SyncStatus.SYNCED.getValue());
    }

    @Override
    public void syncUnsynced() {
        dbExecutor.execute(() -> {
            List<TranscriptionTask> unsyncedTasks = taskDao.getUnsyncedTasksSync(
                    BaseEntity.SyncStatus.SYNCED.getValue());
            for (TranscriptionTask task : unsyncedTasks) {
                syncManager.syncTranscriptionTask(task);
            }
        });
    }

    public interface TranscriptionCallback {
        void onSuccess(long localTaskId, TranscriptionStatusResponse response);
        void onError(String error);
    }

    public interface TranscriptionListCallback {
        void onSuccess(List<TranscriptionStatusResponse> transcriptions);
        void onError(String error);
    }

    public interface TranscriptionDetailCallback {
        void onSuccess(TranscriptionResponse transcription);
        void onError(String error);
    }

    public interface RenameCallback {
        void onSuccess(String newName);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}