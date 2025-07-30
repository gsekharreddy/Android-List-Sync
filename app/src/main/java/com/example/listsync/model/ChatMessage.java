package com.example.listsync.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatMessage {
	private String text;
	private String senderName;
	private long timestamp;

	// Required empty constructor for Firestore
	public ChatMessage() {}

	public ChatMessage(String text, String senderName) {
		this.text = text;
		this.senderName = senderName;
		this.timestamp = new Date().getTime();
	}

	// --- Getters ---
	public String getText() {
		return text;
	}

	public String getSenderName() {
		return senderName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	// --- Setters (NEW) ---
	public void setText(String text) {
		this.text = text;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	// Helper method to format the timestamp
	public String getFormattedTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
		return sdf.format(new Date(timestamp));
	}

	// Helper method to convert this object to a Map for Firestore
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("text", text);
		map.put("senderName", senderName);
		map.put("timestamp", timestamp);
		return map;
	}
}
