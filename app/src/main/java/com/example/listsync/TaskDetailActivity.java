package com.example.listsync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class TaskDetailActivity extends AppCompatActivity {

	public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
	public static final String EXTRA_TASK_TEXT = "EXTRA_TASK_TEXT";
	public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";

	private String taskId, taskText, imageUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_task_detail);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		// Add null check for getSupportActionBar()
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowTitleEnabled(false);
		}


		// Get data passed from the adapter
		taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
		taskText = getIntent().getStringExtra(EXTRA_TASK_TEXT);
		imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

		// Find all the views from the layout
		ImageView detailImage = findViewById(R.id.detail_image);
		TextView detailText = findViewById(R.id.detail_text);
		Button btnCopyText = findViewById(R.id.btn_copy_text);
		Button btnDownloadImage = findViewById(R.id.btn_download_image);
		Button btnEditTask = findViewById(R.id.btn_edit_task);

		// --- NEW: Add null checks to prevent crashes ---
		if (detailImage == null || detailText == null || btnCopyText == null || btnDownloadImage == null || btnEditTask == null) {
			Toast.makeText(this, "Error: UI component mismatch in layout.", Toast.LENGTH_LONG).show();
			Log.e("TaskDetailActivity", "A view is null. Check your activity_task_detail.xml layout file for missing IDs.");
			finish(); // Close the activity safely
			return;
		}

		// Populate the views
		detailText.setText(taskText);

		if (imageUrl != null && !imageUrl.isEmpty()) {
			Glide.with(this).load(imageUrl).into(detailImage);
			btnDownloadImage.setVisibility(View.VISIBLE);
		} else {
			detailImage.setVisibility(View.GONE);
			btnDownloadImage.setVisibility(View.GONE);
		}

		// Set up the click listeners
		btnCopyText.setOnClickListener(v -> {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Task Text", taskText);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "Text copied!", Toast.LENGTH_SHORT).show();
		});

		btnDownloadImage.setOnClickListener(v -> {
			Toast.makeText(this, "Download coming soon!", Toast.LENGTH_SHORT).show();
		});

		// Set the click listener for the edit button
		btnEditTask.setOnClickListener(v -> {
			Intent intent = new Intent(TaskDetailActivity.this, EditTaskActivity.class);
			intent.putExtra(EditTaskActivity.EXTRA_TASK_ID, taskId);
			intent.putExtra(EditTaskActivity.EXTRA_TASK_TEXT, taskText);
			intent.putExtra(EditTaskActivity.EXTRA_IMAGE_URL, imageUrl);
			startActivity(intent);
		});
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
}
