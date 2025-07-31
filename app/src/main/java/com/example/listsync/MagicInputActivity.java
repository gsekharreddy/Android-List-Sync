package com.example.listsync;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MagicInputActivity extends AppCompatActivity {

	private static final String TAG = "MagicInputActivity";
	private EditText etMagicInput;
	private Button btnCreateTaskAi;
	private ProgressBar progressBar;

	private FirebaseFirestore db;
	private FirebaseUser currentUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_magic_input);

		db = FirebaseFirestore.getInstance();
		currentUser = FirebaseAuth.getInstance().getCurrentUser();

		etMagicInput = findViewById(R.id.et_magic_input);
		btnCreateTaskAi = findViewById(R.id.btn_create_task_ai);
		progressBar = findViewById(R.id.progressBar);

		btnCreateTaskAi.setOnClickListener(v -> {
			String inputText = etMagicInput.getText().toString().trim();
			if (!inputText.isEmpty()) {
				createTaskWithAi(inputText);
			}
		});
	}

	private void createTaskWithAi(String inputText) {
		showLoading(true);

		String apiKey = BuildConfig.GEMINI_API_KEY;

		if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.isEmpty()) {
			Toast.makeText(this, "Gemini API Key not found. Please add it to local.properties.", Toast.LENGTH_LONG).show();
			showLoading(false);
			return;
		}

		GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
		GenerativeModelFutures model = GenerativeModelFutures.from(gm);

		String prompt = "Analyze the following text and extract the main task description. " +
				"Return the response as a simple JSON object with one key: 'taskText'. " +
				"For example, if the input is 'remind me to call the bank tomorrow', " +
				"the output should be {\"taskText\": \"Call the bank\"}. " +
				"Here is the text: " + inputText;

		Content content = new Content.Builder().addText(prompt).build();
		Executor executor = Executors.newSingleThreadExecutor();

		ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
		Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
			@Override
			public void onSuccess(GenerateContentResponse result) {
				String resultText = result.getText();
				Log.d(TAG, "AI Response: " + resultText);
				try {
					String cleanJson = resultText.replace("```json", "").replace("```", "").trim();
					JSONObject jsonObject = new JSONObject(cleanJson);
					String taskText = jsonObject.getString("taskText");
					saveTaskToFirestore(taskText);
				} catch (JSONException e) {
					Log.e(TAG, "JSON parsing error", e);
					runOnUiThread(() -> {
						Toast.makeText(MagicInputActivity.this, "AI response was unclear. Please try rephrasing.", Toast.LENGTH_LONG).show();
						showLoading(false);
					});
				}
			}

			@Override
			public void onFailure(Throwable t) {
				// UPDATED: Log the specific error message from the AI service
				Log.e(TAG, "AI Error: " + t.getMessage(), t);
				runOnUiThread(() -> {
					Toast.makeText(MagicInputActivity.this, "Failed to connect to AI service. Check Logcat for details.", Toast.LENGTH_LONG).show();
					showLoading(false);
				});
			}
		}, executor);
	}

	private void saveTaskToFirestore(String taskText) {
		Map<String, Object> taskData = new HashMap<>();
		taskData.put("taskText", taskText);
		taskData.put("imageUrl", null);
		taskData.put("timestamp", System.currentTimeMillis());
		taskData.put("userId", currentUser.getUid());
		taskData.put("completed", false);

		db.collection("tasks")
				.add(taskData)
				.addOnSuccessListener(documentReference -> {
					runOnUiThread(() -> {
						Toast.makeText(MagicInputActivity.this, "AI Task created!", Toast.LENGTH_SHORT).show();
						finish();
					});
				})
				.addOnFailureListener(e -> {
					runOnUiThread(() -> {
						Toast.makeText(MagicInputActivity.this, "Failed to save task.", Toast.LENGTH_SHORT).show();
						showLoading(false);
					});
				});
	}

	private void showLoading(boolean isLoading) {
		progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
		btnCreateTaskAi.setEnabled(!isLoading);
	}
}
