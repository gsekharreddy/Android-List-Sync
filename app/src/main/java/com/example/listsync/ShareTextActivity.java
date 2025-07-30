package com.example.listsync;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.listsync.model.ChatMessage;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

public class ShareTextActivity extends AppCompatActivity {

	private static final String TAG = "ShareTextActivity"; // Added TAG for logging
	private EditText etTextToShare;
	private Button btnShareText;
	private TextView tvSharedContent, tvShareCode;
	private ScrollView scrollView;

	private FirebaseFirestore db;
	private String shareCode;
	private String sessionName;
	private ListenerRegistration sessionListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share_text);

		db = FirebaseFirestore.getInstance();
		shareCode = getIntent().getStringExtra("SHARE_CODE");
		sessionName = getIntent().getStringExtra("SESSION_NAME");

		if (shareCode == null || shareCode.isEmpty() || sessionName == null || sessionName.isEmpty()) {
			Toast.makeText(this, "Error: Invalid share session.", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		etTextToShare = findViewById(R.id.et_text_to_share);
		btnShareText = findViewById(R.id.btn_share_text);
		tvSharedContent = findViewById(R.id.tv_shared_content);
		tvShareCode = findViewById(R.id.tv_share_code);
		scrollView = findViewById(R.id.scroll_view);

		tvShareCode.setText("Code: " + shareCode);

		btnShareText.setOnClickListener(v -> {
			String text = etTextToShare.getText().toString().trim();
			if (!text.isEmpty()) {
				sendMessage(text);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		startListeningForMessages();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (sessionListener != null) {
			sessionListener.remove();
		}
	}

	private void startListeningForMessages() {
		sessionListener = db.collection("sharing_sessions").document(shareCode)
				.addSnapshotListener((snapshot, e) -> {
					if (e != null) {
						Log.w(TAG, "Listen failed.", e); // Log the full error
						Toast.makeText(this, "Listen failed.", Toast.LENGTH_SHORT).show();
						return;
					}

					if (snapshot != null && snapshot.exists()) {
						List<Map<String, Object>> messages = (List<Map<String, Object>>) snapshot.get("messages");
						if (messages != null) {
							displayMessages(messages);
						}
					} else {
						Toast.makeText(this, "Share session has ended.", Toast.LENGTH_SHORT).show();
						finish();
					}
				});
	}

	private void displayMessages(List<Map<String, Object>> messages) {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		for (Map<String, Object> msgMap : messages) {
			ChatMessage msg = new ChatMessage();
			msg.setText((String) msgMap.get("text"));
			msg.setSenderName((String) msgMap.get("senderName"));
			Object timestampObj = msgMap.get("timestamp");
			if (timestampObj instanceof Long) {
				msg.setTimestamp((Long) timestampObj);
			}

			String header = msg.getSenderName() + " (" + msg.getFormattedTime() + ")\n";
			String body = msg.getText() + "\n\n";

			builder.append(header);
			builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), builder.length() - header.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			builder.append(body);
		}
		tvSharedContent.setText(builder);
		scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
	}

	private void sendMessage(String text) {
		ChatMessage message = new ChatMessage(text, sessionName);
		db.collection("sharing_sessions").document(shareCode)
				.update("messages", FieldValue.arrayUnion(message.toMap()))
				.addOnSuccessListener(aVoid -> etTextToShare.setText(""))
				.addOnFailureListener(e -> {
					// --- UPDATED: Add detailed logging ---
					Log.e(TAG, "Failed to send message to Firestore", e); // This will print the full error to Logcat
					Toast.makeText(this, "Failed to send message.", Toast.LENGTH_LONG).show();
				});
	}
}
