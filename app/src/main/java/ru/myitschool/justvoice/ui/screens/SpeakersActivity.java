package ru.myitschool.justvoice.ui.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.ApiService;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.dto.Speaker;
import ru.myitschool.justvoice.ui.adapter.SpeakerAdapter;

public class SpeakersActivity extends AppCompatActivity {

    private RecyclerView rvSpeakers;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private SpeakerAdapter adapter;
    private TextView tvNoSpeakers;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speakers);

        apiService = RetrofitClient.getApiService();

        initViews();
        loadSpeakers();
    }

    private void initViews() {
        rvSpeakers = findViewById(R.id.rv_speakers);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
        tvNoSpeakers = findViewById(R.id.tv_no_speakers);

        btnBack.setOnClickListener(v -> finish());

        adapter = new SpeakerAdapter(new ArrayList<>(), new SpeakerAdapter.OnSpeakerAction() {
            @Override
            public void onRename(Speaker speaker) {
                showRenameDialog(speaker);
            }

            @Override
            public void onDelete(Speaker speaker) {
                showDeleteDialog(speaker);
            }
        });

        rvSpeakers.setLayoutManager(new LinearLayoutManager(this));
        rvSpeakers.setAdapter(adapter);
    }

    private void loadSpeakers() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getSpeakers().enqueue(new Callback<List<Speaker>>() {
            @Override
            public void onResponse(Call<List<Speaker>> call, Response<List<Speaker>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<Speaker> speakers = response.body();

                    if (speakers.isEmpty()) {
                        tvNoSpeakers.setVisibility(View.VISIBLE);
                        rvSpeakers.setVisibility(View.GONE);
                    } else {
                        tvNoSpeakers.setVisibility(View.GONE);
                        rvSpeakers.setVisibility(View.VISIBLE);
                        adapter.setSpeakers(speakers);
                    }
                } else {
                    tvNoSpeakers.setVisibility(View.VISIBLE);
                    rvSpeakers.setVisibility(View.GONE);
                    Toast.makeText(SpeakersActivity.this, "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Speaker>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvNoSpeakers.setVisibility(View.VISIBLE);
                rvSpeakers.setVisibility(View.GONE);
                Toast.makeText(SpeakersActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRenameDialog(Speaker speaker) {
        EditText input = new EditText(this);
        input.setText(speaker.getName());

        new AlertDialog.Builder(this)
                .setTitle("Rename Speaker")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        renameSpeaker(speaker, newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(Speaker speaker) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Speaker")
                .setMessage("Remove \"" + speaker.getName() + "\"? All their segments will be unlinked.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteSpeaker(speaker);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameSpeaker(Speaker speaker, String newName) {
        apiService.renameSpeaker(speaker.getId(), newName).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SpeakersActivity.this, "Renamed!", Toast.LENGTH_SHORT).show();
                    loadSpeakers();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(SpeakersActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSpeaker(Speaker speaker) {
        apiService.deleteSpeaker(speaker.getId()).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SpeakersActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                    loadSpeakers();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(SpeakersActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}