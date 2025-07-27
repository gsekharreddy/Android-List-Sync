package com.example.listsync;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cloudinary.android.MediaManager;
import com.example.listsync.adapter.TaskAdapter;
import com.example.listsync.model.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	// Firebase
	private FirebaseAuth mAuth;
	private FirebaseFirestore db;
	private FirebaseUser currentUser;

	// UI Components
	private RecyclerView recyclerView;
	private FloatingActionButton fab;
	private TextView emptyView;
	private SwipeRefreshLayout swipeRefreshLayout;
	private TaskAdapter adapter;
	private List<TaskItem> taskList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Note: EdgeToEdge is removed as it can interfere with SwipeRefreshLayout
		setContentView(R.layout.activity_main);

		// Firebase Init
		mAuth = FirebaseAuth.getInstance();
		db = FirebaseFirestore.getInstance();
		currentUser = mAuth.getCurrentUser();

		if (currentUser == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		initializeViews();
		setupFab();
		setupSwipeToRefresh();
		setupItemTouchHelper(); // Setup for swipe-to-delete
		listenForTaskChanges();
	}

	private void initializeViews() {
		swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
		recyclerView = findViewById(R.id.recyclerView);
		emptyView = findViewById(R.id.empty_view);
		fab = findViewById(R.id.fab);

		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		taskList = new ArrayList<>();
		adapter = new TaskAdapter(taskList, this);
		recyclerView.setAdapter(adapter);
	}

	private void setupFab() {
		fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddTaskActivity.class)));
	}

	// --- NEW: Pull to Refresh Logic ---
	private void setupSwipeToRefresh() {
		swipeRefreshLayout.setOnRefreshListener(() -> {
			// The listener already re-fetches data in real-time.
			// We can just show a toast and stop the refreshing animation.
			Toast.makeText(this, "Tasks are up to date!", Toast.LENGTH_SHORT).show();
			swipeRefreshLayout.setRefreshing(false);
		});
	}

	// --- NEW: Swipe to Delete & Undo Logic ---
	private void setupItemTouchHelper() {
		ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
				TaskItem deletedTask = taskList.get(position);

				// Step 1: Remove from list and update UI
				taskList.remove(position);
				adapter.notifyItemRemoved(position);

				// Step 2: Show Snackbar with Undo option
				Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
						.setAction("Undo", v -> {
							// User clicked Undo, add item back
							taskList.add(position, deletedTask);
							adapter.notifyItemInserted(position);
						})
						.addCallback(new Snackbar.Callback() {
							@Override
							public void onDismissed(Snackbar transientBottomBar, int event) {
								// If Snackbar is dismissed for any reason other than clicking UNDO,
								// permanently delete the task.
								if (event != DISMISS_EVENT_ACTION) {
									deleteTaskPermanently(deletedTask);
								}
							}
						}).show();
			}

			// Optional: Add a nice background color and icon when swiping
			@Override
			public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
						.addBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red))
						.addActionIcon(R.drawable.ic_delete)
						.create()
						.decorate();
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
		};

		new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
	}

	private void deleteTaskPermanently(TaskItem task) {
		// Delete from Firestore
		db.collection("tasks").document(task.getDocumentId())
				.delete()
				.addOnSuccessListener(aVoid -> {
					Log.d(TAG, "Task deleted successfully from Firestore.");
					// If task had an image, delete it from Cloudinary
					if (task.getImageUrl() != null && !task.getImageUrl().isEmpty()) {
						deleteImageFromCloudinary(task.getImageUrl());
					}
				})
				.addOnFailureListener(e -> {
					Log.w(TAG, "Error deleting task from Firestore", e);
					// If deletion fails, you might want to add the item back to the list to prevent inconsistency
				});
	}

	private void deleteImageFromCloudinary(String imageUrl) {
		// Cloudinary public ID is the last part of the URL before the file extension
		String publicId = imageUrl.substring(imageUrl.lastIndexOf('/') + 1, imageUrl.lastIndexOf('.'));

		// Deletion needs to run on a background thread
		new Thread(() -> {
			try {
				MediaManager.get().uploader().destroy(publicId, null);
				Log.d(TAG, "Image deleted from Cloudinary: " + publicId);
			} catch (IOException e) {
				Log.e(TAG, "Error deleting image from Cloudinary", e);
			}
		}).start();
	}

	private void listenForTaskChanges() {
		swipeRefreshLayout.setRefreshing(true); // Show loading indicator
		db.collection("tasks")
				.whereEqualTo("userId", currentUser.getUid())
				.orderBy("timestamp", Query.Direction.DESCENDING)
				.addSnapshotListener((snapshots, e) -> {
					swipeRefreshLayout.setRefreshing(false); // Hide loading indicator
					if (e != null) {
						Toast.makeText(MainActivity.this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
						return;
					}

					if (snapshots != null) {
						taskList.clear();
						for (DocumentSnapshot doc : snapshots.getDocuments()) {
							TaskItem task = doc.toObject(TaskItem.class);
							if (task != null) {
								// IMPORTANT: Set the document ID on the task object
								task.setDocumentId(doc.getId());
								taskList.add(task);
							}
						}
						adapter.notifyDataSetChanged();
						updateEmptyView();
					}
				});
	}

	private void updateEmptyView() {
		if (taskList.isEmpty()) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
		}
	}
}
