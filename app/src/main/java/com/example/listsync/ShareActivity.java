package com.example.listsync;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShareActivity extends AppCompatActivity {

	private EditText etShareCode, etSharePin;
	private Button btnCreateShare, btnJoinShare;
	private FirebaseFirestore db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);

		db = FirebaseFirestore.getInstance();
		etShareCode = findViewById(R.id.et_share_code);
		etSharePin = findViewById(R.id.et_share_pin);
		btnCreateShare = findViewById(R.id.btn_create_share);
		btnJoinShare = findViewById(R.id.btn_join_share);

		btnCreateShare.setOnClickListener(v -> createShareSession());
		btnJoinShare.setOnClickListener(v -> joinShareSession());
	}

	private void createShareSession() {
		// This is a placeholder for your actual task list
		// In the real app, you'd pass this from MainActivity
		ArrayList<Map<String, Object>> tasksToShare = new ArrayList<>();
		// Example:
		// Map<String, Object> task1 = new HashMap<>();
		// task1.put("taskText", "My first shared task");
		// tasksToShare.add(task1);

		String pin = "1234"; // In real app, you'd get this from a dialog
		String shareCode = generateShareCode();

		// Hash the PIN securely using jBCrypt
		String hashedPin = BCrypt.hashpw(pin, BCrypt.gensalt());

		Map<String, Object> sessionData = new HashMap<>();
		sessionData.put("hashedPin", hashedPin);
		sessionData.put("tasks", tasksToShare);
		sessionData.put("createdAt", System.currentTimeMillis());

		db.collection("sharing_sessions").document(shareCode)
				.set(sessionData)
				.addOnSuccessListener(aVoid -> {
					// Show the user the code and PIN
					new AlertDialog.Builder(this)
							.setTitle("Share Session Created!")
							.setMessage("Share this code and PIN with your friend:\n\nCode: " + shareCode + "\nPIN: " + pin)
							.setPositiveButton("OK", null)
							.show();
				})
				.addOnFailureListener(e -> Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show());
	}

	private void joinShareSession() {
		String shareCode = etShareCode.getText().toString().trim();
		String pin = etSharePin.getText().toString().trim();

		if (shareCode.isEmpty() || pin.isEmpty()) {
			Toast.makeText(this, "Please enter both code and PIN", Toast.LENGTH_SHORT).show();
			return;
		}

		db.collection("sharing_sessions").document(shareCode).get()
				.addOnSuccessListener(documentSnapshot -> {
					if (documentSnapshot.exists()) {
						String storedHashedPin = documentSnapshot.getString("hashedPin");
						// Verify the entered PIN against the stored hash
						if (BCrypt.checkpw(pin, storedHashedPin)) {
							// PIN is correct, load the tasks
							ArrayList<HashMap<String, Object>> tasks = (ArrayList<HashMap<String, Object>>) documentSnapshot.get("tasks");
							Toast.makeText(this, "Success! " + tasks.size() + " tasks synced.", Toast.LENGTH_LONG).show();
							// Here you would typically send the data back to MainActivity
						} else {
							Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
						}
					} else {
						Toast.makeText(this, "Share code not found.", Toast.LENGTH_SHORT).show();
					}
				})
				.addOnFailureListener(e -> Toast.makeText(this, "Error finding session.", Toast.LENGTH_SHORT).show());
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
