package com.example.listsync;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShareActivity extends AppCompatActivity {

	private EditText etShareCode, etSharePin, etSessionName; // Added session name field
	private Button btnCreateShare, btnJoinShare;
	private FirebaseFirestore db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);

		db = FirebaseFirestore.getInstance();
		etShareCode = findViewById(R.id.et_share_code);
		etSharePin = findViewById(R.id.et_share_pin);
		etSessionName = findViewById(R.id.et_session_name); // Find the new EditText
		btnCreateShare = findViewById(R.id.btn_create_share);
		btnJoinShare = findViewById(R.id.btn_join_share);

		btnCreateShare.setOnClickListener(v -> showCreatePinDialog());
		btnJoinShare.setOnClickListener(v -> joinShareSession());
	}

	private void showCreatePinDialog() {
		String sessionName = etSessionName.getText().toString().trim();
		if (sessionName.isEmpty()) {
			etSessionName.setError("Your name is required");
			return;
		}

		final EditText pinInput = new EditText(this);
		pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
		pinInput.setHint("Enter a 4-digit PIN");

		new AlertDialog.Builder(this)
				.setTitle("Create a PIN")
				.setMessage("Create a simple 4-digit PIN for your friend to join.")
				.setView(pinInput)
				.setPositiveButton("Create", (dialog, which) -> {
					String pin = pinInput.getText().toString().trim();
					if (pin.length() == 4) {
						createShareSession(pin, sessionName);
					} else {
						Toast.makeText(this, "PIN must be 4 digits.", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Cancel", null)
				.show();
	}

	private void createShareSession(String pin, String sessionName) {
		String shareCode = generateShareCode();
		String hashedPin = BCrypt.hashpw(pin, BCrypt.gensalt());

		Map<String, Object> sessionData = new HashMap<>();
		sessionData.put("hashedPin", hashedPin);
		sessionData.put("messages", new ArrayList<>()); // Start with an empty message list
		sessionData.put("createdAt", System.currentTimeMillis());

		db.collection("sharing_sessions").document(shareCode)
				.set(sessionData)
				.addOnSuccessListener(aVoid -> {
					new AlertDialog.Builder(this)
							.setTitle("Share Session Created!")
							.setMessage("Share this code and PIN with your friend:\n\nCode: " + shareCode + "\nPIN: " + pin)
							.setPositiveButton("OK", (dialog, which) -> {
								openShareTextActivity(shareCode, sessionName);
							})
							.show();
				})
				.addOnFailureListener(e -> Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show());
	}

	private void joinShareSession() {
		String shareCode = etShareCode.getText().toString().trim();
		String pin = etSharePin.getText().toString().trim();
		String sessionName = etSessionName.getText().toString().trim();

		if (shareCode.isEmpty() || pin.isEmpty() || sessionName.isEmpty()) {
			Toast.makeText(this, "Please enter your name, code, and PIN", Toast.LENGTH_SHORT).show();
			return;
		}

		db.collection("sharing_sessions").document(shareCode).get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String storedHashedPin = documentSnapshot.getString("hashedPin");
						if (BCrypt.checkpw(pin, storedHashedPin)) {
							Toast.makeText(this, "Success! Joining session...", Toast.LENGTH_SHORT).show();
							openShareTextActivity(shareCode, sessionName);
						} else {
							Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(this, "Share code not found.", Toast.LENGTH_SHORT).show();
					}
				})
				.addOnFailureListener(e -> Toast.makeText(this, "Error finding session.", Toast.LENGTH_SHORT).show());
	}

	private void openShareTextActivity(String shareCode, String sessionName) {
		Intent intent = new Intent(this, ShareTextActivity.class);
		intent.putExtra("SHARE_CODE", shareCode);
		intent.putExtra("SESSION_NAME", sessionName);
		startActivity(intent);
	}

	private String generateShareCode() {
		String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder code = new StringBuilder();
		Random rnd = new Random();
		while (code.length() < 6) {
			int index = (int) (rnd.nextFloat() * chars.length());
			code.append(chars.charAt(index));
		}
		return code.toString();
	}
}
