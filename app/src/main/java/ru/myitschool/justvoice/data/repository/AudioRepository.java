package ru.myitschool.justvoice.data.repository;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionResponse;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionStatusResponse;

public class AudioRepository {

    private final ApiService apiService;
    private final Context context;
    private static volatile AudioRepository instance;

    private AudioRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
    }

    public static AudioRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AudioRepository.class) {
                if (instance == null) {
                    instance = new AudioRepository(context);
                }
            }
        }
        return instance;
    }

    public void transcribeAudio(Uri audioUri, TranscriptionCallback callback) {
        try {
            File audioFile = getFileFromUri(audioUri);
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("audio/*"),
                    audioFile
            );
            MultipartBody.Part body = MultipartBody.Part.createFormData(
                    "file",
                    audioFile.getName(),
                    requestFile
            );

            apiService.transcribeAudio(body).enqueue(new Callback<TranscriptionStatusResponse>() {
                @Override
                public void onResponse(Call<TranscriptionStatusResponse> call,
                                       Response<TranscriptionStatusResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Transcription failed");
                    }
                }

                @Override
                public void onFailure(Call<TranscriptionStatusResponse> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void listTranscriptions(TranscriptionListCallback callback) {
        apiService.listTranscriptions().enqueue(new Callback<List<TranscriptionStatusResponse>>() {
            @Override
            public void onResponse(Call<List<TranscriptionStatusResponse>> call,
                                   Response<List<TranscriptionStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get transcriptions");
                }
            }

            @Override
            public void onFailure(Call<List<TranscriptionStatusResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getTranscription(int taskId, TranscriptionDetailCallback callback) {
        apiService.getTranscription(taskId).enqueue(new Callback<TranscriptionResponse>() {
            @Override
            public void onResponse(Call<TranscriptionResponse> call,
                                   Response<TranscriptionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Transcription not found");
                }
            }

            @Override
            public void onFailure(Call<TranscriptionResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private File getFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("audio", ".tmp", context.getCacheDir());
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

    public interface TranscriptionCallback {
        void onSuccess(TranscriptionStatusResponse response);
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
}