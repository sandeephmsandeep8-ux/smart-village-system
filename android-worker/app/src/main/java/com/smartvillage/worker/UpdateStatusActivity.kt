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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smartvillage.worker.databinding.ActivityUpdateStatusBinding
import com.smartvillage.worker.models.Assignment
import com.smartvillage.worker.models.StatusUpdateRequest
import com.smartvillage.worker.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UpdateStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateStatusBinding
    private var selectedPhotoUri: Uri? = null
    private var assignments: List<Assignment> = emptyList()
    private var selectedAssignmentId: Int = -1

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedPhotoUri = result.data?.data
                selectedPhotoUri?.let { uri ->
                    binding.ivPhotoPreview.visibility = View.VISIBLE
                    Glide.with(this).load(uri).into(binding.ivPhotoPreview)
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
        binding = ActivityUpdateStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupStatusSpinner()
        setupButtons()
        loadInProgressAssignments()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.update_status)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))
        binding.toolbar.navigationIcon?.setTint(getColor(android.R.color.white))
    }

    private fun setupStatusSpinner() {
        val statusOptions = arrayOf("IN_PROGRESS", "COMPLETED")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnUploadPhoto.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnSubmit.setOnClickListener {
            val selectedStatus = binding.spinnerStatus.selectedItem?.toString() ?: "IN_PROGRESS"
            val remarks = binding.etRemarks.text.toString().trim()

            if (selectedAssignmentId == -1) {
                Toast.makeText(this, "Please select a task first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitUpdate(selectedAssignmentId, selectedStatus, remarks)
        }
    }

    private fun loadInProgressAssignments() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().getAssignments()
                showLoading(false)
                if (response.isSuccessful) {
                    assignments = (response.body() ?: emptyList()).filter {
                        it.status.uppercase() == "IN_PROGRESS" || it.status.uppercase() == "ASSIGNED"
                    }
                    if (assignments.isEmpty()) {
                        binding.tvNoTasks.visibility = View.VISIBLE
                        binding.cardSelectTask.visibility = View.GONE
                        binding.cardUpdateForm.visibility = View.GONE
                    } else {
                        binding.tvNoTasks.visibility = View.GONE
                        binding.cardSelectTask.visibility = View.VISIBLE
                        binding.cardUpdateForm.visibility = View.VISIBLE
                        setupTaskSpinner()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@UpdateStatusActivity, "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupTaskSpinner() {
        val taskTitles = assignments.map { "${it.id}: ${it.title}" }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taskTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTask.adapter = adapter

        binding.spinnerTask.setSelection(0)
        selectedAssignmentId = assignments.firstOrNull()?.id ?: -1

        binding.spinnerTask.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedAssignmentId = assignments.getOrNull(position)?.id ?: -1
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun submitUpdate(assignmentId: Int, status: String, remarks: String) {
        setSubmitting(true)
        lifecycleScope.launch {
            try {
                // Upload photo if selected
                if (selectedPhotoUri != null) {
                    uploadPhoto(assignmentId)
                }

                val request = StatusUpdateRequest(
                    status = status,
                    remarks = remarks.ifEmpty { null }
                )
                val response = RetrofitClient.getInstance().updateAssignmentStatus(assignmentId, request)

                setSubmitting(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@UpdateStatusActivity, "Status updated successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@UpdateStatusActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                setSubmitting(false)
                Toast.makeText(this@UpdateStatusActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadPhoto(assignmentId: Int) {
        val uri = selectedPhotoUri ?: return
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val tempFile = File.createTempFile("status_photo", ".jpg", cacheDir)
            tempFile.outputStream().use { out -> inputStream.copyTo(out) }

            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            RetrofitClient.getInstance().uploadCompletionPhoto(assignmentId, part)
            tempFile.delete()
        } catch (e: Exception) {
            // Photo upload failure is non-fatal
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
    }

    private fun setSubmitting(submitting: Boolean) {
        binding.btnSubmit.isEnabled = !submitting
        binding.btnSubmit.text = if (submitting) "Submitting..." else getString(R.string.submit_update)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
