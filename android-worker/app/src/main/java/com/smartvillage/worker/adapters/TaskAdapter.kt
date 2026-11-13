package com.smartvillage.worker.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartvillage.worker.R
import com.smartvillage.worker.databinding.ItemTaskBinding
import com.smartvillage.worker.models.Assignment

class TaskAdapter(
    private val onItemClick: (Assignment) -> Unit
) : ListAdapter<Assignment, TaskAdapter.TaskViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Assignment>() {
            override fun areItemsTheSame(oldItem: Assignment, newItem: Assignment): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Assignment, newItem: Assignment): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(assignment: Assignment) {
            binding.tvTaskTitle.text = assignment.title
            binding.tvTaskVillage.text = assignment.villageName ?: "Smart Village"
            binding.tvTaskDate.text = formatDate(assignment.assignedAt)
            binding.tvTaskCategory.text = assignment.category

            // Status chip
            val status = assignment.status.uppercase()
            binding.chipStatus.text = formatStatus(status)
            val chipColorRes = when (status) {
                "ASSIGNED" -> R.color.status_assigned
                "IN_PROGRESS" -> R.color.status_in_progress
                "COMPLETED" -> R.color.status_completed
                else -> R.color.primary_blue
            }
            binding.chipStatus.setChipBackgroundColorResource(chipColorRes)

            // Thumbnail
            if (!assignment.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(assignment.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.ivTaskThumbnail)
            } else {
                binding.ivTaskThumbnail.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(assignment) }
        }

        private fun formatStatus(status: String): String {
            return when (status) {
                "ASSIGNED" -> "Assigned"
                "IN_PROGRESS" -> "In Progress"
                "COMPLETED" -> "Completed"
                else -> status
            }
        }

        private fun formatDate(dateStr: String): String {
            return try {
                val inputFormat = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    java.util.Locale.getDefault()
                )
                val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                if (date != null) outputFormat.format(date) else dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
