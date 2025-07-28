package com.example.listsync.model;

import com.google.firebase.firestore.Exclude;

public class TaskItem {
	@Exclude
	private String documentId;

	private String taskText;
	private String imageUrl;
	private Long timestamp;
	private String userId;
	private boolean completed; // NEW: To track completion status

	public TaskItem() {}

	// --- Getters ---
	public String getDocumentId() {
		return documentId;
	}

	public String getTaskText() {
		return taskText;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public String getUserId() {
		return userId;
	}

	public boolean isCompleted() { // NEW
		return completed;
	}

	// --- Setters ---
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public void setCompleted(boolean completed) { // NEW
		this.completed = completed;
	}
}
