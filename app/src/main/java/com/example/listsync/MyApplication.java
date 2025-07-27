package com.example.listsync;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		// Initialize Firebase
		FirebaseApp.initializeApp(this);

		// Initialize Firebase App Check
		FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
		firebaseAppCheck.installAppCheckProviderFactory(
				PlayIntegrityAppCheckProviderFactory.getInstance());

		// Initialize Cloudinary MediaManager using secure credentials
		initCloudinary();
	}

	private void initCloudinary() {
		Map<String, String> config = new HashMap<>();
		// Access the secrets from the auto-generated BuildConfig class
		// This is the secure way to handle credentials.
		config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
		config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
		config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
		MediaManager.init(this, config);
	}
}
