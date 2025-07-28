package com.example.listsync;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.example.listsync.adapter.TaskAdapter;
import com.example.listsync.model.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

	private static final String TAG = "MainActivity";

	// UI Components
	private DrawerLayout drawerLayout;
	private RecyclerView recyclerView;
	private FloatingActionButton fab;
	private FloatingActionButton fabDeleteLocal;
	private TextView emptyView;
	private SwipeRefreshLayout swipeRefreshLayout;
	private TaskAdapter adapter;
	private List<TaskItem> taskList; // Master list of all tasks from Firestore
	private List<TaskItem> filteredTaskList; // List that is displayed (for search results)

	// Firebase
	private FirebaseAuth mAuth;
	private FirebaseFirestore db;
	private FirebaseUser currentUser;
	private ListenerRegistration firestoreListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mAuth = FirebaseAuth.getInstance();
		db = FirebaseFirestore.getInstance();
		currentUser = mAuth.getCurrentUser();

		if (currentUser == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		initializeViews();
		setupToolbarAndDrawer();
		setupFab();
		setupSwipeToRefresh();
		setupItemTouchHelper();
	}

	@Override
	protected void onStart() {
		super.onStart();
		listenForTaskChanges();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (firestoreListener != null) {
			firestoreListener.remove();
		}
	}

	private void initializeViews() {
		drawerLayout = findViewById(R.id.drawer_layout);
		swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
		recyclerView = findViewById(R.id.recyclerView);
		emptyView = findViewById(R.id.empty_view);
		fab = findViewById(R.id.fab);
		fabDeleteLocal = findViewById(R.id.fab_delete_local);

		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		taskList = new ArrayList<>();
		filteredTaskList = new ArrayList<>();
		adapter = new TaskAdapter(filteredTaskList, this); // Adapter now uses the filtered list
		recyclerView.setAdapter(adapter);
	}

	private void setupToolbarAndDrawer() {
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		NavigationView navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawerLayout.addDrawerListener(toggle);
		toggle.syncState();

		updateNavHeader();
	}

	private void updateNavHeader() {
		NavigationView navigationView = findViewById(R.id.nav_view);
		View headerView = navigationView.getHeaderView(0);
		TextView navUserEmail = headerView.findViewById(R.id.nav_user_email);
		ImageView navProfileImage = headerView.findViewById(R.id.nav_profile_image);

		if (currentUser != null) {
			navUserEmail.setText(currentUser.getEmail());
			if (currentUser.getPhotoUrl() != null) {
				Glide.with(this).load(currentUser.getPhotoUrl()).into(navProfileImage);
			}
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.nav_logout) {
			mAuth.signOut();
			startActivity(new Intent(MainActivity.this, LoginActivity.class));
			finish();
		} else if (id == R.id.nav_toggle_theme) {
			Toast.makeText(this, "Theme toggle coming soon!", Toast.LENGTH_SHORT).show();
		}

		drawerLayout.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	public void onBackPressed() {
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			drawerLayout.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	private void setupFab() {
		fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddTaskActivity.class)));
		fabDeleteLocal.setOnClickListener(v -> showDeleteLocalDataConfirmationDialog());
	}

	private void showDeleteLocalDataConfirmationDialog() {
		new AlertDialog.Builder(this)
				.setTitle("Clear Local Data")
				.setMessage("Are you sure you want to clear all tasks from this device? This will not affect your data on the server.")
				.setPositiveButton("Clear", (dialog, which) -> {
					if (taskList != null && adapter != null) {
						taskList.clear();
						filterTasks(""); // Refresh the filtered list to be empty
						Toast.makeText(this, "Local data cleared.", Toast.LENGTH_SHORT).show();
					}
				})

				.setNegativeButton("Cancel", null)

				.setIcon(R.drawable.ic_delete)

				.show();
	}

	private void setupSwipeToRefresh() {
		swipeRefreshLayout.setOnRefreshListener(() -> {
			Toast.makeText(this, "Tasks are up to date!", Toast.LENGTH_SHORT).show();
			swipeRefreshLayout.setRefreshing(false);
		});
	}

	private void setupItemTouchHelper() {
		ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
				TaskItem deletedTask = filteredTaskList.get(position); // Get from the displayed list

				// Remove from both lists to maintain consistency
				filteredTaskList.remove(position);
				taskList.remove(deletedTask);
				adapter.notifyItemRemoved(position);

				Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
						.setAction("Undo", v -> {
							// Add back to the master list and re-filter
							taskList.add(deletedTask);
							filterTasks(""); // Re-filter to get correct position and sorting
						})
						.addCallback(new Snackbar.Callback() {
							@Override
							public void onDismissed(Snackbar transientBottomBar, int event) {
								if (event != DISMISS_EVENT_ACTION) {
									deleteTaskPermanently(deletedTask);
								}
							}
						}).show();
			}

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
		db.collection("tasks").document(task.getDocumentId())
				.delete()
				.addOnSuccessListener(aVoid -> {
					Log.d(TAG, "Task deleted successfully from Firestore.");
					if (task.getImageUrl() != null && !task.getImageUrl().isEmpty()) {
						deleteImageFromCloudinary(task.getImageUrl());
					}
				})
				.addOnFailureListener(e -> Log.w(TAG, "Error deleting task from Firestore", e));
	}

	private void deleteImageFromCloudinary(String imageUrl) {
		String publicId = imageUrl.substring(imageUrl.lastIndexOf('/') + 1, imageUrl.lastIndexOf('.'));
		new Thread(() -> {
			try {
				MediaManager.get().getCloudinary().uploader().destroy(publicId, null);
				Log.d(TAG, "Image deleted from Cloudinary: " + publicId);
			} catch (IOException e) {
				Log.e(TAG, "Error deleting image from Cloudinary", e);
			}
		}).start();
	}

	private void listenForTaskChanges() {
		swipeRefreshLayout.setRefreshing(true);
		Query query = db.collection("tasks")
				.whereEqualTo("userId", currentUser.getUid());

		firestoreListener = query.addSnapshotListener((snapshots, e) -> {
			swipeRefreshLayout.setRefreshing(false);
			if (e != null) {
				Toast.makeText(MainActivity.this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
			}

			if (snapshots != null) {
				taskList.clear();
				for (DocumentSnapshot doc : snapshots.getDocuments()) {
					TaskItem task = doc.toObject(TaskItem.class);
					if (task != null) {
						task.setDocumentId(doc.getId());
						taskList.add(task);
					}
				}
				filterTasks(""); // Initial sort and display
			}
		});
	}

	private void filterTasks(String query) {
		filteredTaskList.clear();
		List<TaskItem> completed = new ArrayList<>();
		List<TaskItem> incomplete = new ArrayList<>();

		for (TaskItem task : taskList) {
			if (task.getTaskText().toLowerCase().contains(query.toLowerCase())) {
				if (task.isCompleted()) {
					completed.add(task);
				} else {
					incomplete.add(task);
				}
			}
		}

		// Sort both lists by timestamp (newest first)
		Collections.sort(incomplete, (t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));
		Collections.sort(completed, (t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

		// Add incomplete tasks first, then completed tasks
		filteredTaskList.addAll(incomplete);
		filteredTaskList.addAll(completed);

		adapter.notifyDataSetChanged();
		updateEmptyView();
	}

	private void updateEmptyView() {
		if (filteredTaskList.isEmpty()) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
		}
	}

	// --- NEW: Inflate the menu with the search icon ---
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchItem.getActionView();

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				filterTasks(newText); // Filter the list as the user types
				return true;
			}
		});
		return true;
	}
}
