package com.example.listsync;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

	private static final String TAG = "AddTaskActivity";

	// Firebase Services
	private FirebaseAuth mAuth;
	private FirebaseFirestore db;
	private FirebaseUser currentUser;

	// UI Components
	private EditText etTaskText;
	private ImageView imagePreview;
	private Button btnAddImage, btnSaveTask;
	private ProgressBar progressBar;

	private Uri selectedImageUri;

	private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			uri -> {
				if (uri != null) {
					selectedImageUri = uri;
					imagePreview.setImageURI(selectedImageUri);
					imagePreview.setVisibility(View.VISIBLE);
					btnAddImage.setText("Change Image");
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_add_task);
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
			return insets;
		});

		// Initialize Firebase (No Storage needed)
		mAuth = FirebaseAuth.getInstance();
		db = FirebaseFirestore.getInstance();
		currentUser = mAuth.getCurrentUser();

		if (currentUser == null) {
			finish();
			return;
		}

		initializeViews();
		setupClickListeners();
	}

	private void initializeViews() {
		etTaskText = findViewById(R.id.etTaskText);
		imagePreview = findViewById(R.id.imagePreview);
		btnAddImage = findViewById(R.id.btnAddImage);
		btnSaveTask = findViewById(R.id.btnSaveTask);
		progressBar = findViewById(R.id.progressBar);
	}

	private void setupClickListeners() {
		btnAddImage.setOnClickListener(v -> mGetContent.launch("image/*"));
		btnSaveTask.setOnClickListener(v -> {
			Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
			v.startAnimation(bounce);
			saveTask();
		});
	}

	private void saveTask() {
		String taskText = etTaskText.getText().toString().trim();

		if (taskText.isEmpty()) {
			etTaskText.setError("Task description cannot be empty!");
			etTaskText.requestFocus();
			return;
		}

		showLoading(true);

		if (selectedImageUri != null) {
			// Scenario 1: Task with an image -> Upload to Cloudinary
			uploadToCloudinaryAndSave(taskText);
		} else {
			// Scenario 2: Task without an image -> Save directly to Firestore
			saveTaskToFirestore(taskText, null);
		}
	}

	private void uploadToCloudinaryAndSave(final String taskText) {
		Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
		MediaManager.get().upload(selectedImageUri)
				.callback(new UploadCallback() {
					@Override
					public void onStart(String requestId) {
						Log.d(TAG, "Cloudinary upload started.");
					}

					@Override
					public void onProgress(String requestId, long bytes, long totalBytes) {
						// You can add progress logic here if needed
					}

					@Override
					public void onSuccess(String requestId, Map resultData) {
						// Image uploaded successfully, get the secure URL
						String imageUrl = (String) resultData.get("secure_url");
						Log.d(TAG, "Cloudinary upload successful: " + imageUrl);
						// Now, save the task with the image URL to Firestore
						saveTaskToFirestore(taskText, imageUrl);
					}

					@Override
					public void onError(String requestId, ErrorInfo error) {
						Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
						Toast.makeText(AddTaskActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
						showLoading(false);
					}

					@Override
					public void onReschedule(String requestId, ErrorInfo error) {
						Log.w(TAG, "Cloudinary upload rescheduled: " + error.getDescription());
					}
				})
				.dispatch(); // This starts the upload
	}

	private void saveTaskToFirestore(String taskText, String imageUrl) {
		Map<String, Object> taskData = new HashMap<>();
		taskData.put("taskText", taskText);
		taskData.put("imageUrl", imageUrl); // This will be null if no image was added
		taskData.put("timestamp", System.currentTimeMillis());
		taskData.put("userId", currentUser.getUid());

		db.collection("tasks")
				.add(taskData)
				.addOnSuccessListener(documentReference -> {
					Toast.makeText(AddTaskActivity.this, "Task saved!", Toast.LENGTH_SHORT).show();
					showLoading(false);
					finish(); // Go back to the main screen
				})
				.addOnFailureListener(e -> {
					Toast.makeText(AddTaskActivity.this, "Failed to save task: " + e.getMessage(), Toast.LENGTH_LONG).show();
					showLoading(false);
				});
	}

	private void showLoading(boolean isLoading) {
		if (isLoading) {
			progressBar.setVisibility(View.VISIBLE);
			btnSaveTask.setEnabled(false);
			btnAddImage.setEnabled(false);
		} else {
			progressBar.setVisibility(View.GONE);
			btnSaveTask.setEnabled(true);
			btnAddImage.setEnabled(true);
		}
	}
}
