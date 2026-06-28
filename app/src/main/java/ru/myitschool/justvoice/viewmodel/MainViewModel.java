package ru.myitschool.justvoice.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionResponse;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionStatusResponse;
import ru.myitschool.justvoice.data.repository.TranscriptionRepository;

public class MainViewModel extends AndroidViewModel {

    private final TranscriptionRepository transcriptionRepository;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<TranscriptionStatusResponse> uploadResult;
    private final MutableLiveData<TranscriptionResponse> transcriptionDetail;

    public MainViewModel(Application application) {
        super(application);
        transcriptionRepository = TranscriptionRepository.getInstance(application);
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        uploadResult = new MutableLiveData<>();
        transcriptionDetail = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<TranscriptionStatusResponse> getUploadResult() {
        return uploadResult;
    }

    public MutableLiveData<TranscriptionResponse> getTranscriptionDetail() {
        return transcriptionDetail;
    }

    public void transcribeAudio(Uri audioUri, long userId) {
        isLoading.setValue(true);

        transcriptionRepository.transcribeAudio(audioUri, userId,
                new TranscriptionRepository.TranscriptionCallback() {
                    @Override
                    public void onSuccess(long localTaskId, TranscriptionStatusResponse response) {
                        isLoading.postValue(false);
                        uploadResult.postValue(response);
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.postValue(false);
                        errorMessage.postValue(error);
                    }
                });
    }

    public void loadTranscriptions(long userId) {
        transcriptionRepository.listTranscriptions(userId,
                new TranscriptionRepository.TranscriptionListCallback() {
                    @Override
                    public void onSuccess(List<TranscriptionStatusResponse> transcriptions) {

                    }

                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                    }
                });
    }

    public void loadTranscriptionDetail(String serverId) {
        if (serverId == null) return;

        isLoading.setValue(true);

        try {
            int taskId = Integer.parseInt(serverId);
            transcriptionRepository.getTranscription(taskId,
                    new TranscriptionRepository.TranscriptionDetailCallback() {
                        @Override
                        public void onSuccess(TranscriptionResponse transcription) {
                            isLoading.postValue(false);
                            transcriptionDetail.postValue(transcription);
                        }

                        @Override
                        public void onError(String error) {
                            isLoading.postValue(false);
                            errorMessage.postValue(error);
                        }
                    });
        } catch (NumberFormatException e) {
            isLoading.postValue(false);
            errorMessage.postValue("Invalid task ID");
        }
    }

    public LiveData<List<TranscriptionTask>> getLocalTasks(long userId) {
        return transcriptionRepository.getLocalTasks(userId);
    }

    public void deleteTask(TranscriptionTask task) {
        transcriptionRepository.deleteLocalTask(task);
    }
}