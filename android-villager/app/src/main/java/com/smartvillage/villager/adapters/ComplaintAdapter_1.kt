package com.smartvillage.villager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartvillage.villager.R
import com.smartvillage.villager.databinding.ItemComplaintBinding
import com.smartvillage.villager.models.Complaint

class ComplaintAdapter(
    private val onItemClick: (Complaint) -> Unit
) : ListAdapter<Complaint, ComplaintAdapter.ComplaintViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val binding = ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComplaintViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ComplaintViewHolder(
        private val binding: ItemComplaintBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(complaint: Complaint) {
            binding.tvTitle.text = complaint.title
            binding.tvCategory.text = complaint.category
            binding.tvVillage.text = complaint.villageName ?: "Unknown Village"
            binding.tvDate.text = formatDate(complaint.createdAt)
            binding.tvStatus.text = complaint.status

            val (textColor, bgColor) = getStatusColors(complaint.status)
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColor))
            binding.tvStatus.setBackgroundColor(ContextCompat.getColor(context, bgColor))

            if (!complaint.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(complaint.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(complaint) }
        }

        private fun getStatusColors(status: String): Pair<Int, Int> {
            return when (status.lowercase()) {
                "pending" -> Pair(R.color.status_pending, R.color.status_pending_bg)
                "in progress" -> Pair(R.color.status_in_progress, R.color.status_in_progress_bg)
                "resolved" -> Pair(R.color.status_resolved, R.color.status_resolved_bg)
                "rejected" -> Pair(R.color.status_rejected, R.color.status_rejected_bg)
                else -> Pair(R.color.text_secondary, R.color.background)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val date = inputFormat.parse(dateString)
                if (date != null) outputFormat.format(date) else dateString
            } catch (e: Exception) {
                dateString
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Complaint>() {
        override fun areItemsTheSame(oldItem: Complaint, newItem: Complaint) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Complaint, newItem: Complaint) = oldItem == newItem
    }
}
