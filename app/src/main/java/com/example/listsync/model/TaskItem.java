package com.example.listsync.model;

import com.google.firebase.firestore.Exclude;

public class TaskItem {
	// This field will hold the unique ID of the Firestore document
	@Exclude // This tells Firestore not to try and save this field back to the DB
	private String documentId;

	private String taskText;
	private String imageUrl;
	private Long timestamp;
	private String userId;

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

	// --- Setter ---
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
}
