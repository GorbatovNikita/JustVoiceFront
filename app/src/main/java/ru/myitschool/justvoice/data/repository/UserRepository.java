package ru.myitschool.justvoice.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.dto.UserProfile;
import ru.myitschool.justvoice.data.remote.dto.UserUpdate;

public class UserRepository {

    private final ApiService apiService;
    private final Context context;
    private final Handler mainHandler;
    private static volatile UserRepository instance;

    private UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static UserRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository(context);
                }
            }
        }
        return instance;
    }

    public void getProfile(ProfileCallback callback) {
        if (!RetrofitClient.isAuthenticated(context)) {
            mainHandler.post(() -> callback.onError("Not authenticated"));
            return;
        }

        apiService.getCurrentUser().enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else if (response.code() == 401) {
                    RetrofitClient.clearAuthToken(context);
                    mainHandler.post(() -> callback.onError("Session expired"));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to load profile"));
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    public void updateProfile(String firstName, String lastName, UpdateProfileCallback callback) {
        if (!RetrofitClient.isAuthenticated(context)) {
            mainHandler.post(() -> callback.onError("Not authenticated"));
            return;
        }

        if (firstName == null || firstName.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("First name is required"));
            return;
        }

        UserUpdate update = new UserUpdate(firstName.trim(),
                lastName != null ? lastName.trim() : "");

        apiService.updateCurrentUser(update).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else if (response.code() == 401) {
                    RetrofitClient.clearAuthToken(context);
                    mainHandler.post(() -> callback.onError("Session expired"));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to update profile"));
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    public interface ProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(String error);
    }

    public interface UpdateProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(String error);
    }
}