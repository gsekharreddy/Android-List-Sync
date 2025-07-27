package com.example.listsync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

	// Firebase Authentication
	private FirebaseAuth mAuth;

	// UI Components
	private EditText editTextEmail, editTextPassword;
	private Button buttonLogin, buttonRegister;
	private ProgressBar progressBar;
	private LinearLayout loginFormContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// --- Boilerplate EdgeToEdge Code ---
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_login);
		// Ensure your root layout in activity_login.xml has android:id="@+id/main"
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});
		// --- End of Boilerplate ---

		// Initialize Firebase Auth
		mAuth = FirebaseAuth.getInstance();

		// Initialize UI components
		initializeViews();

		// Set up animations
		setupAnimations();

		// Set up click listeners for the buttons
		setupClickListeners();
	}

	@Override
	public void onStart() {
		super.onStart();
		// Check if user is signed in (non-null) and update UI accordingly.
		FirebaseUser currentUser = mAuth.getCurrentUser();
		if (currentUser != null) {
			// If user is already logged in, go directly to MainActivity
			navigateToMainActivity();
		}
	}

	private void initializeViews() {
		editTextEmail = findViewById(R.id.editTextEmail);
		editTextPassword = findViewById(R.id.editTextPassword);
		buttonLogin = findViewById(R.id.buttonLogin);
		buttonRegister = findViewById(R.id.buttonRegister);
		progressBar = findViewById(R.id.progressBar); // Add a ProgressBar to your XML
		loginFormContainer = findViewById(R.id.login_form_container); // Add this ID to the layout holding the form
	}

	private void setupAnimations() {
		// Animate the entire form sliding up when the activity starts
		Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
		loginFormContainer.startAnimation(slideUp);
	}

	private void setupClickListeners() {
		Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);

		buttonLogin.setOnClickListener(v -> {
			v.startAnimation(bounce);
			loginUser();
		});

		buttonRegister.setOnClickListener(v -> {
			v.startAnimation(bounce);
			registerUser();
		});
	}

	private void registerUser() {
		String email = editTextEmail.getText().toString().trim();
		String password = editTextPassword.getText().toString().trim();

		if (!validateInput(email, password)) return;

		showLoading(true);
		mAuth.createUserWithEmailAndPassword(email, password)
				.addOnCompleteListener(this, task -> {
					showLoading(false);
					if (task.isSuccessful()) {
						Toast.makeText(LoginActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
						navigateToMainActivity();
					} else {
						Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
					}
				});
	}

	private void loginUser() {
		String email = editTextEmail.getText().toString().trim();
		String password = editTextPassword.getText().toString().trim();

		if (!validateInput(email, password)) return;

		showLoading(true);
		mAuth.signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(this, task -> {
					showLoading(false);
					if (task.isSuccessful()) {
						Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
						navigateToMainActivity();
					} else {
						Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
					}
				});
	}

	private boolean validateInput(String email, String password) {
		if (email.isEmpty()) {
			editTextEmail.setError("Email is required");
			editTextEmail.requestFocus();
			return false;
		}
		if (password.isEmpty()) {
			editTextPassword.setError("Password is required");
			editTextPassword.requestFocus();
			return false;
		}
		// Basic check for password length
		if (password.length() < 6) {
			editTextPassword.setError("Password should be at least 6 characters");
			editTextPassword.requestFocus();
			return false;
		}
		return true;
	}

	private void showLoading(boolean isLoading) {
		if (isLoading) {
			progressBar.setVisibility(View.VISIBLE);
			buttonLogin.setEnabled(false);
			buttonRegister.setEnabled(false);
		} else {
			progressBar.setVisibility(View.GONE);
			buttonLogin.setEnabled(true);
			buttonRegister.setEnabled(true);
		}
	}

	private void navigateToMainActivity() {
		Intent intent = new Intent(LoginActivity.this, MainActivity.class);
		// Clear the activity stack so the user can't go back to the login screen
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		// Apply a custom animation for the transition
		overridePendingTransition(R.anim.slide_up_enter, R.anim.fade_out); // fade_out is a built-in anim
		finish();
	}
}
