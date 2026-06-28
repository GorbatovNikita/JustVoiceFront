package ru.myitschool.justvoice.ui.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.RetrofitClient;
import ru.myitschool.justvoice.data.remote.dto.UserProfile;
import ru.myitschool.justvoice.data.repository.AuthRepository;
import ru.myitschool.justvoice.data.repository.UserRepository;


public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail;
    private TextView tvFirstName;
    private TextView tvLastName;
    private EditText etFirstName;
    private EditText etLastName;
    private Button btnEdit;
    private Button btnSave;
    private Button btnCancel;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private View viewMode;
    private View editMode;

    private UserRepository userRepository;
    private AuthRepository authRepository;
    private UserProfile currentProfile;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = UserRepository.getInstance(this);
        authRepository = AuthRepository.getInstance(this);

        initViews();
        loadProfile();
    }

    private void initViews() {
        tvEmail = findViewById(R.id.tv_email);
        tvFirstName = findViewById(R.id.tv_first_name);
        tvLastName = findViewById(R.id.tv_last_name);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        viewMode = findViewById(R.id.view_mode);
        editMode = findViewById(R.id.edit_mode);

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> switchToEditMode());
        btnCancel.setOnClickListener(v -> switchToViewMode());
        btnSave.setOnClickListener(v -> saveProfile());

        switchToViewMode();
    }

    private void switchToEditMode() {
        isEditMode = true;
        viewMode.setVisibility(View.GONE);
        editMode.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);

        if (currentProfile != null) {
            etFirstName.setText(currentProfile.getFirstName());
            etLastName.setText(currentProfile.getLastName());
        }
    }

    private void switchToViewMode() {
        isEditMode = false;
        viewMode.setVisibility(View.VISIBLE);
        editMode.setVisibility(View.GONE);
        btnEdit.setVisibility(View.VISIBLE);
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.getProfile(new UserRepository.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                progressBar.setVisibility(View.GONE);
                currentProfile = profile;
                displayProfile(profile);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);

                if (error != null && error.equals("Session expired")) {
                    RetrofitClient.clearAuthToken(ProfileActivity.this);
                    Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayProfile(UserProfile profile) {
        tvEmail.setText(profile.getEmail());
        tvFirstName.setText(profile.getFirstName());
        tvLastName.setText(profile.getLastName() != null && !profile.getLastName().isEmpty()
                ? profile.getLastName() : "Not set");
    }

    private void saveProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty()) {
            Toast.makeText(this, "First name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);

        userRepository.updateProfile(firstName, lastName, new UserRepository.UpdateProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnCancel.setEnabled(true);
                currentProfile = profile;
                displayProfile(profile);
                switchToViewMode();
                Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                btnCancel.setEnabled(true);

                if (error != null && error.equals("Session expired")) {
                    RetrofitClient.clearAuthToken(ProfileActivity.this);
                    Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}