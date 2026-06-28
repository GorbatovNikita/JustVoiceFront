package ru.myitschool.justvoice.data.local;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import ru.myitschool.justvoice.data.remote.RetrofitClient;

public class SessionManager {

    private static SessionManager instance;
    private final Application application;
    private final Handler handler;
    private Runnable expiryCheckRunnable;
    private SessionExpiryListener listener;

    public interface SessionExpiryListener {
        void onSessionExpired();
    }

    private SessionManager(Application application) {
        this.application = application;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static SessionManager getInstance(Application application) {
        if (instance == null) {
            instance = new SessionManager(application);
        }
        return instance;
    }

    public void setExpiryListener(SessionExpiryListener listener) {
        this.listener = listener;
    }

    public void startSessionCheck() {
        stopSessionCheck();

        expiryCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!RetrofitClient.isAuthenticated(application)) {
                    if (listener != null) {
                        listener.onSessionExpired();
                    }
                } else {
                    long remaining = RetrofitClient.getRemainingTokenTime();
                    handler.postDelayed(this, 30000);
                }
            }
        };

        handler.post(expiryCheckRunnable);
    }

    public void stopSessionCheck() {
        if (expiryCheckRunnable != null) {
            handler.removeCallbacks(expiryCheckRunnable);
        }
    }
}