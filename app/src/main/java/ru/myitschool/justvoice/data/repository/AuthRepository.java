package ru.myitschool.justvoice.data.repository;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.data.local.AppDatabase;
import ru.myitschool.justvoice.data.local.dao.UserDao;
import ru.myitschool.justvoice.data.local.entity.User;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.TranscriptionTaskDao;
import ru.myitschool.justvoice.data.remote.dto.Token;
import ru.myitschool.justvoice.data.remote.dto.UserCreate;

public class AuthRepository {

    private final ApiService apiService;
    private final UserDao userDao;
    private final TranscriptionTaskDao taskDao;
    private final Context context;
    private final ExecutorService executorService;
    private static volatile AuthRepository instance;

    private AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        RetrofitClient.init(this.context);
        this.apiService = RetrofitClient.getApiService();
        AppDatabase database = AppDatabase.getInstance(this.context);
        this.userDao = database.userDao();
        this.taskDao = database.transcriptionTaskDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static AuthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(context);
                }
            }
        }
        return instance;
    }

    public void login(String email, String password, AuthCallback callback) {
        Log.d("AuthRepo", "Login attempt: " + email);

        apiService.login(email, password).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    RetrofitClient.setAuthToken(context, token);

                    executorService.execute(() -> {
                        clearLocalDatabase();
                    });

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(response.body());
                    });
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Login failed: " + response.code());
                    });
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Network error: " + t.getMessage());
                });
            }
        });
    }

    public void register(String firstName, String lastName, String email,
                         String password, AuthCallback callback) {
        UserCreate userCreate = new UserCreate(firstName, lastName, email, password);

        apiService.register(userCreate).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();
                    String refreshToken = response.body().getRefreshToken();
                    RetrofitClient.setAuthToken(context, accessToken, refreshToken);

                    executorService.execute(() -> {
                        clearLocalDatabase();
                        User localUser = new User(firstName, lastName, email);
                        userDao.insert(localUser);
                    });

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(response.body());
                    });
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onError("Registration failed: " + response.code());
                    });
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onError("Network error: " + t.getMessage());
                });
            }
        });
    }

    public void logout(LogoutCallback callback) {
        apiService.logout().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                executorService.execute(() -> {
                    clearLocalDatabase();
                    RetrofitClient.clearAuthToken(context);
                });

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onSuccess();
                });
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                executorService.execute(() -> {
                    clearLocalDatabase();
                    RetrofitClient.clearAuthToken(context);
                });

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onSuccess();
                });
            }
        });
    }

    private void clearLocalDatabase() {
        try {
            taskDao.deleteAll();
            userDao.deleteAll();
            Log.d("AuthRepo", "Local database cleared");
        } catch (Exception e) {
            Log.e("AuthRepo", "Error clearing database: " + e.getMessage());
        }
    }

    public boolean isLoggedIn() {
        return RetrofitClient.isAuthenticated(context);
    }

    public interface AuthCallback {
        void onSuccess(Token token);
        void onError(String error);
    }

    public interface LogoutCallback {
        void onSuccess();
    }
}