package com.example.listsync.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

	public String getText() {
		return text;
	}

	public String getSenderName() {
		return senderName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	// Helper method to format the timestamp into a readable string
	public String getFormattedTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
		return sdf.format(new Date(timestamp));
	}
}
