package com.example.listsync;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditTaskActivity extends AppCompatActivity {

	public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
	public static final String EXTRA_TASK_TEXT = "EXTRA_TASK_TEXT";
	public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
	private static final String TAG = "EditTaskActivity";

	private EditText etTaskText;
	private ImageView imagePreview;
	private Button btnAddImage, btnUpdateTask;
	private ProgressBar progressBar;

	private String taskId, initialTaskText, initialImageUrl;
	private Uri newImageUri;

	private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			uri -> {
				if (uri != null) {
					newImageUri = uri;
					imagePreview.setImageURI(newImageUri);
					imagePreview.setVisibility(View.VISIBLE);
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_task);

		if (!initializeViewsAndCheck()) {
			// If any view is not found, stop execution.
			return;
		}

		loadInitialData();
		setupClickListeners();
	}

	private boolean initializeViewsAndCheck() {
		etTaskText = findViewById(R.id.etTaskText);
		if (etTaskText == null) {
			Log.e(TAG, "FATAL ERROR: EditText with ID 'etTaskText' not found in layout. Check your XML.");
			finishWithError();
			return false;
		}

		imagePreview = findViewById(R.id.imagePreview);
		if (imagePreview == null) {
			Log.e(TAG, "FATAL ERROR: ImageView with ID 'imagePreview' not found in layout. Check your XML.");
			finishWithError();
			return false;
		}

		btnAddImage = findViewById(R.id.btnAddImage);
		if (btnAddImage == null) {
			Log.e(TAG, "FATAL ERROR: Button with ID 'btnAddImage' not found in layout. Check your XML.");
			finishWithError();
			return false;
		}

		btnUpdateTask = findViewById(R.id.btnUpdateTask);
		if (btnUpdateTask == null) {
			Log.e(TAG, "FATAL ERROR: Button with ID 'btnUpdateTask' not found in layout. Check your XML.");
			finishWithError();
			return false;
		}

		progressBar = findViewById(R.id.progressBar);
		if (progressBar == null) {
			Log.e(TAG, "FATAL ERROR: ProgressBar with ID 'progressBar' not found in layout. Check your XML.");
			finishWithError();
			return false;
		}

		return true;
	}

	private void finishWithError() {
		Toast.makeText(this, "A layout error occurred.", Toast.LENGTH_LONG).show();
		finish();
	}

	private void loadInitialData() {
		taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
		initialTaskText = getIntent().getStringExtra(EXTRA_TASK_TEXT);
		initialImageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

		// --- UPDATED: Add null checks to prevent crashes ---
		// The Task ID is essential. If it's missing, we can't do anything.
		if (taskId == null || taskId.isEmpty()) {
			Toast.makeText(this, "Error: Task ID is missing. Cannot edit.", Toast.LENGTH_LONG).show();
			Log.e(TAG, "FATAL ERROR: Task ID was not passed to EditTaskActivity.");
			finish(); // Exit the activity because it's in an invalid state.
			return;
		}

		// Check for null text before trying to set it.
		if (initialTaskText != null) {
			etTaskText.setText(initialTaskText);
		}

		if (initialImageUrl != null && !initialImageUrl.isEmpty()) {
			imagePreview.setVisibility(View.VISIBLE);
			Glide.with(this).load(initialImageUrl).into(imagePreview);
		}
	}

	private void setupClickListeners() {
		btnAddImage.setOnClickListener(v -> mGetContent.launch("image/*"));
		btnUpdateTask.setOnClickListener(v -> updateTask());
	}

	private void updateTask() {
		String updatedText = etTaskText.getText().toString().trim();
		if (updatedText.isEmpty()) {
			etTaskText.setError("Task text cannot be empty");
			return;
		}

		showLoading(true);

		if (newImageUri != null) {
			uploadNewImageAndUpdate(updatedText);
		} else {
			updateFirestore(updatedText, initialImageUrl);
		}
	}

	private void uploadNewImageAndUpdate(String text) {
		MediaManager.get().upload(newImageUri).callback(new UploadCallback() {
			@Override
			public void onSuccess(String requestId, Map resultData) {
				String newUrl = (String) resultData.get("secure_url");
				updateFirestore(text, newUrl);
			}
			@Override
			public void onError(String requestId, ErrorInfo error) {
				Toast.makeText(EditTaskActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
				showLoading(false);
			}
			@Override
			public void onStart(String requestId) {}
			@Override
			public void onProgress(String requestId, long bytes, long totalBytes) {}
			@Override
			public void onReschedule(String requestId, ErrorInfo error) {}
		}).dispatch();
	}

	private void updateFirestore(String text, String imageUrl) {
		Map<String, Object> updates = new HashMap<>();
		updates.put("taskText", text);
		updates.put("imageUrl", imageUrl);

		FirebaseFirestore.getInstance().collection("tasks").document(taskId)
				.update(updates)
				.addOnSuccessListener(aVoid -> {
					Toast.makeText(EditTaskActivity.this, "Task updated!", Toast.LENGTH_SHORT).show();
					finish();
				})
				.addOnFailureListener(e -> {
					Toast.makeText(EditTaskActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
					showLoading(false);
				});
	}

	private void showLoading(boolean isLoading) {
		progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
		btnUpdateTask.setEnabled(!isLoading);
		btnAddImage.setEnabled(!isLoading);
	}
}
