package ru.myitschool.justvoice.ui.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;
import ru.myitschool.justvoice.ui.adapter.SearchAdapter;
import ru.myitschool.justvoice.utils.BoyerMooreSearch;
import ru.myitschool.justvoice.viewmodel.MainViewModel;


public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvResults;
    private TextView tvNoResults;
    private TextView tvResultsCount;
    private ProgressBar progressBar;
    private ImageButton btnBack;

    private SearchAdapter adapter;
    private MainViewModel viewModel;
    private List<TranscriptionTask> allTasks = new ArrayList<>();
    private long currentUserId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerView();
        setupViewModel();
        setupSearch();
        loadData();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);
        tvNoResults = findViewById(R.id.tv_no_results);
        tvResultsCount = findViewById(R.id.tv_results_count);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(new ArrayList<>(), result -> {
            Intent intent = new Intent(this, TranscriptionDetailActivity.class);
            intent.putExtra("task_id", result.getServerId());
            intent.putExtra("task_status", result.getStatus());
            intent.putExtra("task_filename", result.getOriginalFilename());
            startActivity(intent);
        });

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getLocalTasks(currentUserId).observe(this, tasks -> {
            if (tasks != null) {
                allTasks = tasks;
                performSearch(etSearch.getText().toString());
            }
        });
    }

    private void loadData() {
        viewModel.loadTranscriptions(currentUserId);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString());
            }
        });
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            adapter.setResults(new ArrayList<>());
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("Введите запрос для поиска");
            tvResultsCount.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            return;
        }

        String searchQuery = query.toLowerCase().trim();
        List<SearchAdapter.SearchResultItem> results = new ArrayList<>();

        for (TranscriptionTask task : allTasks) {
            String displayName = task.getDisplayName() != null ?
                    task.getDisplayName().toLowerCase() : "";
            String effectiveName = task.getEffectiveDisplayName() != null ?
                    task.getEffectiveDisplayName().toLowerCase() : "";
            String topic = task.getTopic() != null ?
                    task.getTopic().toLowerCase() : "";

            boolean found = false;
            String matchedIn = "";

            if (BoyerMooreSearch.search(effectiveName, searchQuery).size() > 0) {
                found = true;
                matchedIn = "название";
            }

            if (!found && BoyerMooreSearch.search(topic, searchQuery).size() > 0) {
                found = true;
                matchedIn = "тема: " + topic;
            }

            if (found) {
                results.add(new SearchAdapter.SearchResultItem(task, matchedIn, query));
            }
        }

        adapter.setResults(results);

        if (results.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("Ничего не найдено по запросу: " + query);
            tvResultsCount.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            tvResultsCount.setVisibility(View.VISIBLE);
            tvResultsCount.setText("Найдено: " + results.size());
            rvResults.setVisibility(View.VISIBLE);
        }
    }
}