package ru.myitschool.justvoice.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import ru.myitschool.justvoice.data.remote.dto.Token;
import ru.myitschool.justvoice.data.repository.AuthRepository;


public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> loginSuccess;

    public AuthViewModel(Application application) {
        super(application);
        authRepository = AuthRepository.getInstance(application);
        isLoading = new MutableLiveData<>(false);
        errorMessage = new MutableLiveData<>();
        loginSuccess = new MutableLiveData<>();
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(Token token) {
                isLoading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    public void register(String firstName, String lastName, String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.register(firstName, lastName, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(Token token) {
                isLoading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
}