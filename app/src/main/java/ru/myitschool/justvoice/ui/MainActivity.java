package ru.myitschool.justvoice.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.repository.AuthRepository;
import ru.myitschool.justvoice.data.repository.TranscriptionRepository;
import ru.myitschool.justvoice.ui.adapter.TranscriptionAdapter;
import ru.myitschool.justvoice.ui.callback.SwipeToDeleteCallback;
import ru.myitschool.justvoice.ui.screens.AuthActivity;
import ru.myitschool.justvoice.ui.screens.SearchActivity;
import ru.myitschool.justvoice.ui.screens.SpeakersActivity;
import ru.myitschool.justvoice.ui.screens.TranscriptionDetailActivity;
import ru.myitschool.justvoice.viewmodel.MainViewModel;
import ru.myitschool.justvoice.ui.screens.ProfileActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private ImageButton btnRecord;
    private ImageButton btnRefresh;
    private ImageButton btnProfile;
    private ImageButton btnLogout;
    private ImageButton btnSearch;
    private ImageButton btnSpeakers;
    private TextView tvStatus;
    private TextView tvTimer;
    private ProgressBar progressBar;
    private RecyclerView rvTranscriptions;
    private TextView tvNoTranscriptions;

    private TranscriptionAdapter transcriptionAdapter;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;

    private MainViewModel viewModel;
    private long currentUserId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!RetrofitClient.isAuthenticated(this)) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupViewModel();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RetrofitClient.isAuthenticated(this)) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        refreshTranscriptions();
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btn_record);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnProfile = findViewById(R.id.btn_profile);
        btnSpeakers = findViewById(R.id.btn_speakers);
        tvStatus = findViewById(R.id.tv_status);
        tvTimer = findViewById(R.id.tv_timer);
        progressBar = findViewById(R.id.progress_bar);
        rvTranscriptions = findViewById(R.id.rv_transcriptions);
        tvNoTranscriptions = findViewById(R.id.tv_no_transcriptions);
        btnLogout = findViewById(R.id.btn_logout);
        btnSearch = findViewById(R.id.btn_search);

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        progressBar.setVisibility(View.VISIBLE);

                        AuthRepository authRepo = AuthRepository.getInstance(this);
                        authRepo.logout(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, AuthActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        btnRecord.setOnClickListener(v -> toggleRecording());
        btnRefresh.setOnClickListener(v -> refreshTranscriptions());
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        btnSpeakers.setOnClickListener(v -> startActivity(new Intent(this, SpeakersActivity.class)));


        tvTimer.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        transcriptionAdapter = new TranscriptionAdapter(
                new ArrayList<>(),
                task -> {
                    if (task.getServerId() != null) {
                        Intent intent = new Intent(this, TranscriptionDetailActivity.class);
                        intent.putExtra("task_id", task.getServerId());
                        intent.putExtra("task_status", task.getStatus());
                        intent.putExtra("task_filename", task.getOriginalFilename());
                        startActivity(intent);
                    }
                },
                task -> showTaskOptions(task)
        );

        rvTranscriptions.setLayoutManager(new LinearLayoutManager(this));
        rvTranscriptions.setAdapter(transcriptionAdapter);

        SwipeToDeleteCallback swipeHandler = new SwipeToDeleteCallback(position -> {
            TranscriptionTask task = transcriptionAdapter.getTasks().get(position);
            showDeleteDialog(task);
            transcriptionAdapter.notifyItemChanged(position);
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHandler);
        itemTouchHelper.attachToRecyclerView(rvTranscriptions);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getUploadResult().observe(this, response -> {
            if (response != null) {
                tvStatus.setText("Processing...");
                Toast.makeText(this, "Uploaded!", Toast.LENGTH_SHORT).show();
                refreshTranscriptions();
                new Handler().postDelayed(() -> refreshTranscriptions(), 3000);
                new Handler().postDelayed(() -> refreshTranscriptions(), 6000);
            }
        });

        viewModel.getLocalTasks(currentUserId).observe(this, tasks -> {
            if (tasks != null) {
                transcriptionAdapter.setTasks(tasks);
                if (tasks.isEmpty()) {
                    tvNoTranscriptions.setVisibility(View.VISIBLE);
                    rvTranscriptions.setVisibility(View.GONE);
                } else {
                    tvNoTranscriptions.setVisibility(View.GONE);
                    rvTranscriptions.setVisibility(View.VISIBLE);
                }
            }
        });

        refreshTranscriptions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSIONS);
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/recording_" +
                System.currentTimeMillis() + ".m4a";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(192000);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            btnRecord.setBackgroundResource(R.drawable.bg_record_button_active);
            tvStatus.setText("Recording...");
            tvTimer.setVisibility(View.VISIBLE);
            startTimer();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mediaRecorder.reset();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioSamplingRate(16000);
                mediaRecorder.setAudioEncodingBitRate(32000);
                mediaRecorder.setAudioChannels(1);
                mediaRecorder.setOutputFile(audioFilePath);
                mediaRecorder.prepare();
                mediaRecorder.start();

                isRecording = true;
                btnRecord.setBackgroundResource(R.drawable.bg_record_button_active);
                tvStatus.setText("Recording...");
                tvTimer.setVisibility(View.VISIBLE);
                startTimer();
            } catch (IOException e2) {
                e2.printStackTrace();
                Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }

        isRecording = false;
        btnRecord.setBackgroundResource(R.drawable.bg_record_button);
        tvStatus.setText("Uploading...");
        tvTimer.setVisibility(View.GONE);
        stopTimer();

        File audioFile = new File(audioFilePath);
        Log.d("Recording", "File: " + audioFilePath + " Size: " + audioFile.length() + " bytes");

        if (audioFile.length() < 1000) {
            Toast.makeText(this, "Recording too short!", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Ready to record");
            return;
        }

        uploadRecording();
    }

    private void uploadRecording() {
        if (audioFilePath != null) {
            File audioFile = new File(audioFilePath);
            if (audioFile.exists() && audioFile.length() > 0) {
                Uri audioUri = Uri.fromFile(audioFile);
                viewModel.transcribeAudio(audioUri, currentUserId);
            } else {
                Toast.makeText(this, "Audio file is empty", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Ready to record");
            }
        }
    }

    private void refreshTranscriptions() {
        if (RetrofitClient.isAuthenticated(this)) {
            viewModel.loadTranscriptions(currentUserId);
        }
    }

    private void showTaskOptions(TranscriptionTask task) {
        List<String> options = new ArrayList<>();
        options.add("Rename");

        if ("failed".equalsIgnoreCase(task.getStatus())) {
            options.add("Retry");
        }

        if ("completed".equalsIgnoreCase(task.getStatus())) {
            options.add("Re-transcribe");
        }

        new AlertDialog.Builder(this)
                .setTitle(task.getEffectiveDisplayName())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selected = options.get(which);
                    if (selected.equals("Rename")) {
                        showRenameDialog(task);
                    } else if (selected.equals("Delete")) {
                        showDeleteDialog(task);
                    } else if (selected.equals("Retry")) {
                        showRetryDialog(task);
                    } else if (selected.equals("Re-transcribe")) {
                        showRetranscribeDialog(task);
                    }
                })
                .show();
    }

    private void showRetranscribeDialog(TranscriptionTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Re-transcribe")
                .setMessage("This will re-process the audio. Existing transcription will be replaced.")
                .setPositiveButton("Re-transcribe", (dialog, which) -> {
                    if (task.getServerId() != null) {
                        TranscriptionRepository.getInstance(this)
                                .retryTranscription(task.getServerId(), new TranscriptionRepository.RetryCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(MainActivity.this, "Re-transcription started!", Toast.LENGTH_SHORT).show();
                                        refreshTranscriptions();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameDialog(TranscriptionTask task) {
        EditText input = new EditText(this);
        input.setText(task.getEffectiveDisplayName());
        input.setHint("Enter new name");

        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && task.getServerId() != null) {
                        TranscriptionRepository.getInstance(this)
                                .renameTranscription(task.getServerId(), newName, new TranscriptionRepository.RenameCallback() {
                                    @Override
                                    public void onSuccess(String newName) {
                                        Toast.makeText(MainActivity.this, "Renamed!", Toast.LENGTH_SHORT).show();
                                        refreshTranscriptions();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(TranscriptionTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this transcription?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (task.getServerId() != null) {
                        TranscriptionRepository.getInstance(this)
                                .deleteTranscription(task.getServerId(), new TranscriptionRepository.DeleteCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(MainActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                                        refreshTranscriptions();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRetryDialog(TranscriptionTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Retry Transcription")
                .setMessage("Send this file for transcription again?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (task.getServerId() != null) {
                        TranscriptionRepository.getInstance(this)
                                .retryTranscription(task.getServerId(), new TranscriptionRepository.RetryCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(MainActivity.this, "Retry started!", Toast.LENGTH_SHORT).show();
                                        refreshTranscriptions();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission required for recording", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) stopRecording();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        stopTimer();
    }
}