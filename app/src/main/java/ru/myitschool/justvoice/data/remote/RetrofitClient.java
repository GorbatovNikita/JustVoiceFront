package ru.myitschool.justvoice.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.myitschool.justvoice.data.remote.dto.Token;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.3.149:8000/";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String TOKEN_EXPIRY_KEY = "token_expiry";

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static String authToken = null;
    private static String refreshToken = null;
    private static long tokenExpiry = 0;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        authToken = prefs.getString(TOKEN_KEY, null);
        refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null);
        tokenExpiry = prefs.getLong(TOKEN_EXPIRY_KEY, 0);
    }

    public static void setAuthToken(Context context, String accessToken, String refreshToken) {
        authToken = accessToken;
        RetrofitClient.refreshToken = refreshToken;

        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.decode(parts[1], Base64.DEFAULT));
                JSONObject jsonObject = new JSONObject(payload);
                tokenExpiry = jsonObject.optLong("exp", 0) * 1000;
            }
        } catch (Exception e) {
            tokenExpiry = System.currentTimeMillis() + 1800000;
        }

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(TOKEN_KEY, accessToken)
                    .putString(REFRESH_TOKEN_KEY, refreshToken)
                    .putLong(TOKEN_EXPIRY_KEY, tokenExpiry)
                    .apply();
        }

        retrofit = null;
        apiService = null;
    }

    public static void setAuthToken(Context context, String accessToken) {
        setAuthToken(context, accessToken, refreshToken);
    }

    public static void clearAuthToken(Context context) {
        authToken = null;
        refreshToken = null;
        tokenExpiry = 0;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .remove(TOKEN_KEY)
                    .remove(REFRESH_TOKEN_KEY)
                    .remove(TOKEN_EXPIRY_KEY)
                    .apply();
        }
        retrofit = null;
        apiService = null;
    }

    public static boolean isAuthenticated(Context context) {
        String token = getAuthToken(context);
        if (token == null || token.isEmpty()) return false;
        return true;
    }

    public static String getAuthToken(Context context) {
        if (authToken == null && context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            authToken = prefs.getString(TOKEN_KEY, null);
            refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null);
            tokenExpiry = prefs.getLong(TOKEN_EXPIRY_KEY, 0);
        }
        return authToken;
    }

    public static long getRemainingTokenTime() {
        if (tokenExpiry == 0) return 0;
        return Math.max(0, tokenExpiry - System.currentTimeMillis());
    }

    private static synchronized String refreshTokenSync() {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService service = retrofit.create(ApiService.class);

            Map<String, String> body = new HashMap<>();
            body.put("refresh_token", refreshToken);

            retrofit2.Response<Token> response = service.refreshToken(body).execute();

            if (response.isSuccessful() && response.body() != null) {
                String newAccessToken = response.body().getAccessToken();
                String newRefreshToken = response.body().getRefreshToken();
                setAuthToken(appContext, newAccessToken, newRefreshToken != null ? newRefreshToken : refreshToken);
                return newAccessToken;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();

                        if (authToken != null && !authToken.isEmpty()) {
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + authToken)
                                    .build();
                            okhttp3.Response response = chain.proceed(newRequest);

                            if (response.code() == 401 && refreshToken != null) {
                                response.close();

                                String newToken = refreshTokenSync();
                                if (newToken != null) {
                                    Request retryRequest = originalRequest.newBuilder()
                                            .header("Authorization", "Bearer " + newToken)
                                            .build();
                                    return chain.proceed(retryRequest);
                                }
                            }

                            return response;
                        }

                        return chain.proceed(originalRequest);
                    })
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(ApiService.class);
        }
        return apiService;
    }
}