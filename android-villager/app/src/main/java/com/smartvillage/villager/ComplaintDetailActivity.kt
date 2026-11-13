package com.smartvillage.villager

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.smartvillage.villager.databinding.ActivityComplaintDetailBinding
import com.smartvillage.villager.models.Complaint
import com.smartvillage.villager.models.FeedbackRequest
import com.smartvillage.villager.network.RetrofitClient
import kotlinx.coroutines.launch

class ComplaintDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintDetailBinding
    private var complaintId: Int = -1
    private var currentComplaint: Complaint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Complaint Detail"
            setDisplayHomeAsUpEnabled(true)
        }

        complaintId = intent.getIntExtra("complaint_id", -1)
        if (complaintId == -1) {
            Snackbar.make(binding.root, "Invalid complaint", Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }

        loadComplaintDetail()

        binding.btnSubmitFeedback.setOnClickListener {
            submitFeedback()
        }
    }

    private fun loadComplaintDetail() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getComplaintById(complaintId)
                if (response.isSuccessful) {
                    val complaint = response.body()
                    if (complaint != null) {
                        currentComplaint = complaint
                        populateUI(complaint)
                    }
                } else {
                    Snackbar.make(binding.root, "Failed to load complaint details", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Network error: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun populateUI(complaint: Complaint) {
        binding.tvTitle.text = complaint.title
        binding.tvCategory.text = complaint.category
        binding.tvDescription.text = complaint.description
        binding.tvLocation.text = if (!complaint.location.isNullOrEmpty()) complaint.location else "Not specified"
        binding.tvVillage.text = complaint.villageName ?: "Unknown"
        binding.tvCreatedAt.text = formatDate(complaint.createdAt)
        binding.tvUpdatedAt.text = formatDate(complaint.updatedAt)

        // Status chip
        binding.tvStatus.text = complaint.status
        val (statusColor, statusBg) = getStatusColors(complaint.status)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, statusColor))
        binding.tvStatus.setBackgroundColor(ContextCompat.getColor(this, statusBg))

        // Load image
        if (!complaint.imageUrl.isNullOrEmpty()) {
            binding.ivComplaintImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(complaint.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.ivComplaintImage)
        } else {
            binding.ivComplaintImage.visibility = View.GONE
        }

        // Show feedback section only if resolved
        if (complaint.status.equals("Resolved", ignoreCase = true)) {
            binding.cardFeedback.visibility = View.VISIBLE
        } else {
            binding.cardFeedback.visibility = View.GONE
        }
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
            val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun submitFeedback() {
        val rating = binding.ratingBar.rating.toInt()
        val comment = binding.etFeedbackComment.text.toString().trim()

        if (rating == 0) {
            Snackbar.make(binding.root, "Please provide a rating", Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitFeedback.isEnabled = false
        lifecycleScope.launch {
            try {
                val request = FeedbackRequest(complaintId, rating, comment)
                val response = RetrofitClient.instance.submitFeedback(request)
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, "Feedback submitted. Thank you!", Snackbar.LENGTH_LONG).show()
                    binding.cardFeedback.visibility = View.GONE
                } else {
                    Snackbar.make(binding.root, "Failed to submit feedback", Snackbar.LENGTH_SHORT).show()
                    binding.btnSubmitFeedback.isEnabled = true
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                binding.btnSubmitFeedback.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
