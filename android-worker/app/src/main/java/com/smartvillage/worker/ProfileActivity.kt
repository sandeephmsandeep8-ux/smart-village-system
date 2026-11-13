package com.smartvillage.worker

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartvillage.worker.databinding.ActivityProfileBinding
import com.smartvillage.worker.network.RetrofitClient
import com.smartvillage.worker.utils.SharedPrefsManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = (application as SmartVillageWorkerApp).prefsManager

        setupToolbar()
        populateProfile()
        setupLogout()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.my_profile)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))
        binding.toolbar.navigationIcon?.setTint(getColor(android.R.color.white))
    }

    private fun populateProfile() {
        val name = prefsManager.getName() ?: "Panchayat Worker"
        val role = prefsManager.getRole() ?: "WORKER"
        val village = prefsManager.getVillage() ?: "Smart Village"
        val mobile = prefsManager.getMobile() ?: "N/A"
        val userId = prefsManager.getUserId()

        binding.tvProfileName.text = name
        binding.tvProfileRole.text = formatRole(role)
        binding.tvProfileVillage.text = village
        binding.tvProfileMobile.text = mobile
        binding.tvProfileId.text = "Worker ID: #$userId"

        // Avatar initials
        val initials = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
        binding.tvAvatarInitials.text = initials.ifEmpty { "W" }
    }

    private fun formatRole(role: String): String {
        return when (role.uppercase()) {
            "WORKER" -> "Panchayat Worker"
            "ADMIN" -> "Administrator"
            else -> role
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performLogout() {
        prefsManager.clearAll()
        RetrofitClient.resetInstance()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
