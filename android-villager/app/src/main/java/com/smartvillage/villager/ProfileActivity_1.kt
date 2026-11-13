package com.smartvillage.villager

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartvillage.villager.databinding.ActivityProfileBinding
import com.smartvillage.villager.utils.SharedPrefsManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "My Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        populateProfile()

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun populateProfile() {
        val name = SharedPrefsManager.getUserName() ?: "Unknown User"
        val village = SharedPrefsManager.getVillage() ?: "Unknown Village"
        val mobile = SharedPrefsManager.getMobile() ?: "Not provided"

        binding.tvUserName.text = name
        binding.tvVillage.text = village
        binding.tvMobile.text = mobile

        // Show first letter as avatar
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        binding.tvAvatarInitial.text = initial
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        SharedPrefsManager.clearAll()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
