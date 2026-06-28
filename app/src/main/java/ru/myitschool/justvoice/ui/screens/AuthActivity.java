package ru.myitschool.justvoice.ui.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.repository.AuthRepository;
import ru.myitschool.justvoice.ui.MainActivity;
import ru.myitschool.justvoice.viewmodel.AuthViewModel;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private EditText etFirstName;
    private EditText etLastName;
    private ImageButton btnSubmit;
    private TextView btnLoginTab;
    private TextView btnRegisterTab;
    private ProgressBar progressBar;
    private TextView tvError;

    private boolean isLoginMode = true;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (RetrofitClient.isAuthenticated(this)) {
            long remaining = RetrofitClient.getRemainingTokenTime();
            if (remaining > 60000) {
                goToMain();
                return;
            } else {
                AuthRepository.getInstance(this).logout(null);
            }
        }

        initViews();
        setupViewModel();
        updateUiMode();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        btnSubmit = findViewById(R.id.btn_submit);
        btnLoginTab = findViewById(R.id.btn_login_tab);
        btnRegisterTab = findViewById(R.id.btn_register_tab);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);

        btnSubmit.setOnClickListener(v -> {
            if (isLoginMode) {
                login();
            } else {
                register();
            }
        });

        btnLoginTab.setOnClickListener(v -> {
            isLoginMode = true;
            updateUiMode();
        });

        btnRegisterTab.setOnClickListener(v -> {
            isLoginMode = false;
            updateUiMode();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getLoginSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                goToMain();
            }
        });
    }

    private void updateUiMode() {
        tvError.setVisibility(View.GONE);

        if (isLoginMode) {
            etFirstName.setVisibility(View.GONE);
            etLastName.setVisibility(View.GONE);

            btnLoginTab.setTextColor(Color.WHITE);
            btnLoginTab.setBackgroundResource(R.drawable.bg_tab_selected);
            btnRegisterTab.setTextColor(Color.parseColor("#1d2d44"));
            btnRegisterTab.setBackgroundResource(R.drawable.bg_tab_unselected);
        } else {
            etFirstName.setVisibility(View.VISIBLE);
            etLastName.setVisibility(View.VISIBLE);

            btnRegisterTab.setTextColor(Color.WHITE);
            btnRegisterTab.setBackgroundResource(R.drawable.bg_tab_selected);
            btnLoginTab.setTextColor(Color.parseColor("#1d2d44"));
            btnLoginTab.setBackgroundResource(R.drawable.bg_tab_unselected);
        }
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        viewModel.login(email, password);
    }

    private void register() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        if (TextUtils.isEmpty(firstName)) {
            tvError.setText("First name is required");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        viewModel.register(firstName, lastName, email, password);
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            tvError.setText("Email is required");
            tvError.setVisibility(View.VISIBLE);
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvError.setText("Invalid email format");
            tvError.setVisibility(View.VISIBLE);
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            tvError.setText("Password is required");
            tvError.setVisibility(View.VISIBLE);
            return false;
        }

        if (password.length() < 10) {
            tvError.setText("Password must be at least 10 characters");
            tvError.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}