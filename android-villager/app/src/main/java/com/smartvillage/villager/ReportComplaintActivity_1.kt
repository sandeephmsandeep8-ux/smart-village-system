package com.smartvillage.villager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.smartvillage.villager.databinding.ActivityReportComplaintBinding
import com.smartvillage.villager.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportComplaintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportComplaintBinding
    private var selectedCategory: String = ""
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val categories = listOf(
        "Water Problem" to "💧",
        "Road Damage" to "🛣️",
        "Street Light" to "💡",
        "Garbage Collection" to "🗑️",
        "Tree Fallen" to "🌳",
        "Other" to "📋"
    )

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                binding.ivImagePreview.visibility = View.VISIBLE
                Glide.with(this).load(it).into(binding.ivImagePreview)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = cameraImageUri
            selectedImageUri?.let {
                binding.ivImagePreview.visibility = View.VISIBLE
                Glide.with(this).load(it).into(binding.ivImagePreview)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else Snackbar.make(binding.root, "Camera permission denied", Snackbar.LENGTH_SHORT).show()
    }

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openGallery() else Snackbar.make(binding.root, "Storage permission denied", Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportComplaintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Report Problem"
            setDisplayHomeAsUpEnabled(true)
        }

        setupCategoryGrid()

        binding.btnUploadPhoto.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnSubmit.setOnClickListener {
            submitComplaint()
        }
    }

    private fun setupCategoryGrid() {
        categories.forEach { (name, emoji) ->
            val chip = Chip(this).apply {
                text = "$emoji $name"
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(this@ReportComplaintActivity, R.color.chip_background_selector)
                setTextColor(ContextCompat.getColorStateList(this@ReportComplaintActivity, R.color.chip_text_selector))
                setOnClickListener {
                    selectedCategory = name
                    updateCategorySelection(this)
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun updateCategorySelection(selectedChip: Chip) {
        for (i in 0 until binding.chipGroupCategory.childCount) {
            val chip = binding.chipGroupCategory.getChildAt(i) as? Chip
            chip?.isChecked = (chip == selectedChip)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Add Photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkStoragePermission()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(cacheDir, "IMG_${timeStamp}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun submitComplaint() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (selectedCategory.isEmpty()) {
            Snackbar.make(binding.root, "Please select a category", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        }
        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            return
        }
        binding.tilTitle.error = null
        binding.tilDescription.error = null

        setLoading(true)
        lifecycleScope.launch {
            try {
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val catBody = selectedCategory.toRequestBody("text/plain".toMediaTypeOrNull())
                val locBody = location.toRequestBody("text/plain".toMediaTypeOrNull())

                var imagePart: MultipartBody.Part? = null
                selectedImageUri?.let { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    val file = File(cacheDir, "upload_image.jpg")
                    file.outputStream().use { output -> inputStream?.copyTo(output) }
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)
                }

                val response = RetrofitClient.instance.submitComplaint(titleBody, descBody, catBody, locBody, imagePart)
                if (response.isSuccessful) {
                    Toast.makeText(this@ReportComplaintActivity, "Complaint submitted successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Snackbar.make(binding.root, "Failed to submit complaint. Try again.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !loading
        binding.btnSubmit.text = if (loading) "" else getString(R.string.submit_report)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
