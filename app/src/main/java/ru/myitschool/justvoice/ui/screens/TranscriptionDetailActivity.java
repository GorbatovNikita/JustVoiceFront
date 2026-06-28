package ru.myitschool.justvoice.ui.screens;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionResponse;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionSegment;
import ru.myitschool.justvoice.ui.adapter.SegmentAdapter;
import ru.myitschool.justvoice.viewmodel.MainViewModel;

public class TranscriptionDetailActivity extends AppCompatActivity {

    private TextView tvFilename;
    private TextView tvStatus;
    private TextView tvLanguage;
    private TextView tvDuration;
    private ProgressBar progressBar;
    private RecyclerView rvSegments;
    private TextView tvNoSegments;

    private ImageButton btnCopy;
    private String fullTranscriptionText = "";

    private ImageButton btnPlay;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    private SegmentAdapter segmentAdapter;
    private MainViewModel viewModel;
    private ApiService apiService;

    private String taskId;
    private String taskStatus;
    private String taskFilename;

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBar;
    private boolean isPlaying = false;
    private String currentAudioPath;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcription_detail);

        taskId = getIntent().getStringExtra("task_id");
        taskStatus = getIntent().getStringExtra("task_status");
        taskFilename = getIntent().getStringExtra("task_filename");

        handler = new Handler();
        executorService = Executors.newSingleThreadExecutor();
        apiService = RetrofitClient.getApiService();

        initViews();
        setupRecyclerView();
        setupPlayer();
        setupViewModel();

        if (taskId != null) {
            loadTranscription();
            downloadAudioFromServer();
        }
    }

    private void initViews() {
        tvFilename = findViewById(R.id.tv_filename);
        tvStatus = findViewById(R.id.tv_status);
        tvLanguage = findViewById(R.id.tv_language);
        tvDuration = findViewById(R.id.tv_duration);
        progressBar = findViewById(R.id.progress_bar);
        rvSegments = findViewById(R.id.rv_segments);
        tvNoSegments = findViewById(R.id.tv_no_segments);

        btnPlay = findViewById(R.id.btn_play);
        seekBar = findViewById(R.id.seek_bar);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        btnCopy = findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> copyToClipboard());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPlay.setOnClickListener(v -> togglePlayback());
    }

    private void setupRecyclerView() {
        segmentAdapter = new SegmentAdapter(new ArrayList<>(), segment -> {
            if (mediaPlayer != null) {
                int seekToMs = (int) (segment.getStartTime() * 1000);
                mediaPlayer.seekTo(seekToMs);
                seekBar.setProgress(seekToMs);
                tvCurrentTime.setText(formatTime(seekToMs));

                if (!isPlaying) {
                    playAudio();
                }
            }
        });

        rvSegments.setLayoutManager(new LinearLayoutManager(this));
        rvSegments.setAdapter(segmentAdapter);
    }

    private void copyToClipboard() {
        if (fullTranscriptionText.isEmpty()) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip =
                android.content.ClipData.newPlainText("Transcription", fullTranscriptionText);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void setupPlayer() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    tvCurrentTime.setText(formatTime(currentPosition));
                }
                handler.postDelayed(this, 100);
            }
        };
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getTranscriptionDetail().observe(this, detail -> {
            if (detail != null) {
                displayTranscription(detail);
            }
        });
    }

    private void loadTranscription() {
        try {
            int id = Integer.parseInt(taskId);
            viewModel.loadTranscriptionDetail(taskId);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAudioFromServer() {
        try {
            int id = Integer.parseInt(taskId);

            progressBar.setVisibility(View.VISIBLE);

            apiService.downloadAudioFile(id).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        executorService.execute(() -> {
                            try {
                                File audioFile = new File(getCacheDir(), "audio_" + taskId + ".mp3");
                                FileOutputStream fos = new FileOutputStream(audioFile);
                                InputStream inputStream = response.body().byteStream();

                                byte[] buffer = new byte[4096];
                                int bytesRead;

                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }

                                fos.close();
                                inputStream.close();

                                currentAudioPath = audioFile.getAbsolutePath();

                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnPlay.setVisibility(View.VISIBLE);
                                    seekBar.setVisibility(View.VISIBLE);
                                    tvCurrentTime.setVisibility(View.VISIBLE);
                                    tvTotalTime.setVisibility(View.VISIBLE);
                                });
                            } catch (IOException e) {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(TranscriptionDetailActivity.this,
                                            "Failed to download audio", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(TranscriptionDetailActivity.this,
                                    "Audio not available", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TranscriptionDetailActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayTranscription(TranscriptionResponse response) {
        String name = response.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = response.getOriginalFilename();
        }
        tvFilename.setText(name != null ? name : "Unknown");

        tvStatus.setText(response.getStatus() != null ? response.getStatus().toUpperCase() : "PENDING");
        tvLanguage.setText(response.getLanguage() != null ?
                response.getLanguage().toUpperCase() : "Unknown");

        if (response.getDuration() != null && response.getDuration() > 0) {
            int totalSeconds = (int) Math.round(response.getDuration());
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
            tvDuration.setVisibility(View.VISIBLE);
        } else {
            tvDuration.setVisibility(View.GONE);
        }

        if (response.getSegments() != null && !response.getSegments().isEmpty()) {
            segmentAdapter.setSegments(response.getSegments());
            rvSegments.setVisibility(View.VISIBLE);
            tvNoSegments.setVisibility(View.GONE);

            StringBuilder sb = new StringBuilder();
            sb.append("Title: ").append(response.getDisplayName()).append("\n");

            if (response.getTopic() != null && !response.getTopic().isEmpty()) {
                sb.append("Topic:  ").append(response.getTopic().toUpperCase()).append("\n");
            }

            if (response.getLanguage() != null) {
                sb.append("Language: ").append(response.getLanguage().toUpperCase()).append("\n");
            }

            sb.append("\n");

            for (TranscriptionSegment seg : response.getSegments()) {
                sb.append("- ").append(seg.getText().trim()).append("\n\n");
            }

            fullTranscriptionText = sb.toString().trim();

            btnCopy.setVisibility(View.VISIBLE);
        } else {
            rvSegments.setVisibility(View.GONE);
            tvNoSegments.setVisibility(View.VISIBLE);
            tvNoSegments.setText("No segments yet. Processing may take a moment.");
            btnCopy.setVisibility(View.GONE);
        }

        switch (response.getStatus() != null ? response.getStatus().toLowerCase() : "") {
            case "completed":
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                break;
            case "processing":
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                break;
            case "failed":
                tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                break;
            default:
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
                break;
        }
    }

    private void togglePlayback() {
        if (mediaPlayer == null) {
            initAudioPlayer();
        }

        if (mediaPlayer != null) {
            if (isPlaying) {
                pauseAudio();
            } else {
                playAudio();
            }
        }
    }

    private void initAudioPlayer() {
        if (currentAudioPath == null) {
            Toast.makeText(this, "Audio not yet downloaded", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(currentAudioPath);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(currentAudioPath);
            mediaPlayer.prepare();

            seekBar.setMax(mediaPlayer.getDuration());
            tvTotalTime.setText(formatTimeMinutes(mediaPlayer.getDuration()));

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlay.setImageResource(R.drawable.ic_play);
                seekBar.setProgress(0);
                tvCurrentTime.setText("0:00");
                handler.removeCallbacks(updateSeekBar);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
                return true;
            });

            playAudio();

        } catch (IOException e) {
            Toast.makeText(this, "Cannot play: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            mediaPlayer = null;
        }
    }

    private void playAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlay.setImageResource(R.drawable.ic_pause);
            handler.post(updateSeekBar);
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlay.setImageResource(R.drawable.ic_play);
            handler.removeCallbacks(updateSeekBar);
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private String formatTimeMinutes(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
        executorService.shutdown();
    }
}