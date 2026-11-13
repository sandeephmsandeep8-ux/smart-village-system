package com.smartvillage.worker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smartvillage.worker.databinding.ActivityTaskDetailBinding
import com.smartvillage.worker.models.AssignmentDetail
import com.smartvillage.worker.models.StatusUpdateRequest
import com.smartvillage.worker.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private var assignmentId: Int = -1
    private var currentDetail: AssignmentDetail? = null
    private var selectedPhotoUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedPhotoUri = result.data?.data
                selectedPhotoUri?.let { uri ->
                    binding.ivCompletionPhoto.visibility = View.VISIBLE
                    Glide.with(this).load(uri).into(binding.ivCompletionPhoto)
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedPhotoUri?.let { uri ->
                    binding.ivCompletionPhoto.visibility = View.VISIBLE
                    Glide.with(this).load(uri).into(binding.ivCompletionPhoto)
                }
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.values.all { it }) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission required to upload photo", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assignmentId = intent.getIntExtra("assignment_id", -1)
        if (assignmentId == -1) {
            Toast.makeText(this, "Invalid task", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        loadTaskDetail()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.task_detail)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))
        binding.toolbar.navigationIcon?.setTint(getColor(android.R.color.white))
    }

    private fun loadTaskDetail() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().getAssignmentDetail(assignmentId)
                showLoading(false)
                if (response.isSuccessful) {
                    currentDetail = response.body()
                    currentDetail?.let { populateUI(it) }
                } else {
                    Toast.makeText(this@TaskDetailActivity, "Failed to load task", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@TaskDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateUI(detail: AssignmentDetail) {
        // Complaint image
        if (!detail.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(detail.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(binding.ivComplaintImage)
        }

        // Basic info
        binding.tvTitle.text = detail.title
        binding.tvCategory.text = detail.category
        binding.tvDescription.text = detail.description
        binding.tvLocation.text = detail.location ?: "N/A"
        binding.tvVillage.text = detail.villageName ?: "N/A"
        binding.tvReportedBy.text = detail.reportedBy ?: "N/A"
        binding.tvAssignedDate.text = formatDate(detail.assignedAt)

        // Status badge
        binding.tvCurrentStatus.text = formatStatus(detail.status)
        binding.tvCurrentStatus.setBackgroundColor(getStatusColor(detail.status))

        // Show action section based on status
        when (detail.status.uppercase()) {
            "ASSIGNED" -> {
                binding.sectionAccept.visibility = View.VISIBLE
                binding.sectionUpdate.visibility = View.GONE
                binding.sectionCompleted.visibility = View.GONE
                setupAcceptButton()
            }
            "IN_PROGRESS" -> {
                binding.sectionAccept.visibility = View.GONE
                binding.sectionUpdate.visibility = View.VISIBLE
                binding.sectionCompleted.visibility = View.GONE
                setupUpdateSection()
            }
            "COMPLETED" -> {
                binding.sectionAccept.visibility = View.GONE
                binding.sectionUpdate.visibility = View.GONE
                binding.sectionCompleted.visibility = View.VISIBLE
                populateCompletedSection(detail)
            }
            else -> {
                binding.sectionAccept.visibility = View.GONE
                binding.sectionUpdate.visibility = View.GONE
                binding.sectionCompleted.visibility = View.GONE
            }
        }
    }

    private fun setupAcceptButton() {
        binding.btnAcceptWork.setOnClickListener {
            acceptWork()
        }
    }

    private fun acceptWork() {
        binding.btnAcceptWork.isEnabled = false
        binding.btnAcceptWork.text = "Accepting..."
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().acceptAssignment(assignmentId)
                if (response.isSuccessful) {
                    Toast.makeText(this@TaskDetailActivity, "Work accepted! Status updated to In Progress.", Toast.LENGTH_LONG).show()
                    loadTaskDetail()
                } else {
                    binding.btnAcceptWork.isEnabled = true
                    binding.btnAcceptWork.text = getString(R.string.accept_work)
                    Toast.makeText(this@TaskDetailActivity, "Failed to accept work", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnAcceptWork.isEnabled = true
                binding.btnAcceptWork.text = getString(R.string.accept_work)
                Toast.makeText(this@TaskDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUpdateSection() {
        binding.btnUploadPhoto.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnSubmitUpdate.setOnClickListener {
            val remarks = binding.etRemarks.text.toString().trim()
            submitStatusUpdate(remarks)
        }
    }

    private fun submitStatusUpdate(remarks: String) {
        binding.btnSubmitUpdate.isEnabled = false
        binding.btnSubmitUpdate.text = "Submitting..."
        lifecycleScope.launch {
            try {
                // Upload photo first if selected
                if (selectedPhotoUri != null) {
                    uploadPhoto()
                }

                // Submit status as COMPLETED
                val request = StatusUpdateRequest(status = "COMPLETED", remarks = remarks.ifEmpty { null })
                val response = RetrofitClient.getInstance().updateAssignmentStatus(assignmentId, request)
                if (response.isSuccessful) {
                    Toast.makeText(this@TaskDetailActivity, "Status updated to Completed!", Toast.LENGTH_LONG).show()
                    loadTaskDetail()
                } else {
                    binding.btnSubmitUpdate.isEnabled = true
                    binding.btnSubmitUpdate.text = getString(R.string.submit_update)
                    Toast.makeText(this@TaskDetailActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.btnSubmitUpdate.isEnabled = true
                binding.btnSubmitUpdate.text = getString(R.string.submit_update)
                Toast.makeText(this@TaskDetailActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadPhoto() {
        val uri = selectedPhotoUri ?: return
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val tempFile = File.createTempFile("completion_photo", ".jpg", cacheDir)
            tempFile.outputStream().use { out -> inputStream.copyTo(out) }

            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            RetrofitClient.getInstance().uploadCompletionPhoto(assignmentId, part)
            tempFile.delete()
        } catch (e: Exception) {
            // Photo upload failure is non-fatal
        }
    }

    private fun populateCompletedSection(detail: AssignmentDetail) {
        binding.tvCompletionRemarks.text = detail.completionRemarks ?: "No remarks provided"
        if (!detail.completionPhotoUrl.isNullOrEmpty()) {
            binding.ivCompletionResult.visibility = View.VISIBLE
            Glide.with(this)
                .load(detail.completionPhotoUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(binding.ivCompletionResult)
        } else {
            binding.ivCompletionResult.visibility = View.GONE
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            openImagePicker()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollContent.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun formatStatus(status: String): String {
        return when (status.uppercase()) {
            "ASSIGNED" -> "Assigned"
            "IN_PROGRESS" -> "In Progress"
            "COMPLETED" -> "Completed"
            else -> status
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status.uppercase()) {
            "ASSIGNED" -> getColor(R.color.status_assigned)
            "IN_PROGRESS" -> getColor(R.color.status_in_progress)
            "COMPLETED" -> getColor(R.color.status_completed)
            else -> getColor(R.color.primary_blue)
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
