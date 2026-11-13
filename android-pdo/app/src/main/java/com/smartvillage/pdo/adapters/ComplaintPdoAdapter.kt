package com.smartvillage.pdo.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartvillage.pdo.R
import com.smartvillage.pdo.databinding.ItemComplaintPdoBinding
import com.smartvillage.pdo.models.Complaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ComplaintPdoAdapter(
    private var complaints: List<Complaint>,
    private val onItemClick: (Complaint) -> Unit
) : RecyclerView.Adapter<ComplaintPdoAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemComplaintPdoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(complaint: Complaint) {
            binding.tvComplaintTitle.text = complaint.title
            binding.tvComplaintVillage.text = complaint.villageName ?: "Unknown Village"
            binding.tvTimeAgo.text = getTimeAgo(complaint.createdAt)
            binding.tvCategory.text = complaint.category

            // Status chip
            binding.chipStatus.text = complaint.status.replace("_", " ")
            val (chipBg, chipText) = getStatusColors(complaint, binding.root.context)
            binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(chipBg)
            binding.chipStatus.setTextColor(chipText)

            // Thumbnail image
            if (!complaint.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.ivThumbnail.context)
                    .load(complaint.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(complaint) }
            binding.btnView.setOnClickListener { onItemClick(complaint) }
            binding.btnAssignQuick.setOnClickListener { onItemClick(complaint) }
        }

        private fun getTimeAgo(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                val diff = Date().time - date.time
                when {
                    diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                    diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                    diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                    diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
                    else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                dateStr
            }
        }

        private fun getStatusColors(
            complaint: Complaint,
            context: android.content.Context
        ): Pair<Int, Int> {
            return when (complaint.status.uppercase()) {
                "PENDING" -> Pair(
                    ContextCompat.getColor(context, R.color.status_pending_bg),
                    ContextCompat.getColor(context, R.color.status_pending_text)
                )
                "ACCEPTED" -> Pair(
                    ContextCompat.getColor(context, R.color.status_accepted_bg),
                    ContextCompat.getColor(context, R.color.status_accepted_text)
                )
                "IN_PROGRESS" -> Pair(
                    ContextCompat.getColor(context, R.color.status_inprogress_bg),
                    ContextCompat.getColor(context, R.color.status_inprogress_text)
                )
                "RESOLVED" -> Pair(
                    ContextCompat.getColor(context, R.color.status_resolved_bg),
                    ContextCompat.getColor(context, R.color.status_resolved_text)
                )
                "REJECTED" -> Pair(
                    ContextCompat.getColor(context, R.color.status_rejected_bg),
                    ContextCompat.getColor(context, R.color.status_rejected_text)
                )
                else -> Pair(
                    ContextCompat.getColor(context, R.color.grey_200),
                    ContextCompat.getColor(context, R.color.grey_700)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComplaintPdoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(complaints[position])
    }

    override fun getItemCount() = complaints.size

    fun updateData(newComplaints: List<Complaint>) {
        complaints = newComplaints
        notifyDataSetChanged()
    }
}
