package com.example.listsync.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.listsync.R;
import com.example.listsync.TaskDetailActivity;
import com.example.listsync.model.TaskItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

	private final List<TaskItem> taskList;
	private final Context context;
	private int lastPosition = -1;

	public TaskAdapter(List<TaskItem> taskList, Context context) {
		this.taskList = taskList;
		this.context = context;
	}

	@NonNull
	@Override
	public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
		return new TaskViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
		TaskItem currentTask = taskList.get(position);

		holder.taskTextView.setText(currentTask.getTaskText());
		holder.completedCheckbox.setChecked(currentTask.isCompleted());

		// Apply visual change based on completion
		if (currentTask.isCompleted()) {
			holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			holder.itemView.setAlpha(0.7f);
		} else {
			holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
			holder.itemView.setAlpha(1.0f);
		}

		if (currentTask.getImageUrl() != null && !currentTask.getImageUrl().isEmpty()) {
			holder.taskImagePreview.setVisibility(View.VISIBLE);
			Glide.with(context).load(currentTask.getImageUrl()).into(holder.taskImagePreview);
		} else {
			holder.taskImagePreview.setVisibility(View.GONE);
		}

		setAnimation(holder.itemView, position);

		holder.itemView.setOnClickListener(v -> {
			Intent intent = new Intent(context, TaskDetailActivity.class);
			intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, currentTask.getDocumentId());
			intent.putExtra(TaskDetailActivity.EXTRA_TASK_TEXT, currentTask.getTaskText());
			intent.putExtra(TaskDetailActivity.EXTRA_IMAGE_URL, currentTask.getImageUrl());
			context.startActivity(intent);
		});

		holder.completedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
			// Update Firestore when checkbox is toggled
			FirebaseFirestore.getInstance().collection("tasks").document(currentTask.getDocumentId())
					.update("completed", isChecked);
		});
	}

	private void setAnimation(View viewToAnimate, int position) {
		if (position > lastPosition) {
			Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
			viewToAnimate.startAnimation(animation);
			lastPosition = position;
		}
	}

	@Override
	public int getItemCount() {
		return taskList.size();
	}

	public static class TaskViewHolder extends RecyclerView.ViewHolder {
		public TextView taskTextView;
		public ImageView taskImagePreview;
		public CheckBox completedCheckbox;

		public TaskViewHolder(@NonNull View itemView) {
			super(itemView);
			taskTextView = itemView.findViewById(R.id.file_name_text_view);
			taskImagePreview = itemView.findViewById(R.id.file_image_preview);
			completedCheckbox = itemView.findViewById(R.id.checkbox_completed);
		}
	}
}
