package com.example.listsync.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.listsync.R;
import com.example.listsync.model.TaskItem;

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

		// Use the correct getter: getTaskText()
		holder.taskTextView.setText(currentTask.getTaskText());

		// Check if there is an image URL
		if (currentTask.getImageUrl() != null && !currentTask.getImageUrl().isEmpty()) {
			// If yes, show the image preview and load the image with Glide
			holder.taskImagePreview.setVisibility(View.VISIBLE);
			holder.taskIcon.setVisibility(View.GONE);
			Glide.with(context)
					.load(currentTask.getImageUrl())
					.into(holder.taskImagePreview);
		} else {
			// If no, show the text icon and hide the image preview
			holder.taskImagePreview.setVisibility(View.GONE);
			holder.taskIcon.setVisibility(View.VISIBLE);
		}

		// Apply animation
		setAnimation(holder.itemView, position);
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
		// Renamed for clarity to match the new item_file.xml IDs
		public TextView taskTextView;
		public ImageView taskImagePreview;
		public ImageView taskIcon;

		public TaskViewHolder(@NonNull View itemView) {
			super(itemView);
			// Make sure these IDs match your item_file.xml
			taskTextView = itemView.findViewById(R.id.file_name_text_view);
			taskImagePreview = itemView.findViewById(R.id.file_image_preview);
			taskIcon = itemView.findViewById(R.id.file_icon_image_view);
		}
	}
}
