package com.smartvillage.pdo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartvillage.pdo.databinding.ActivityProfileBinding
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)

        setupToolbar()
        loadProfileData()
        setupLogoutButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
    }

    private fun loadProfileData() {
        val name = prefsManager.getName() ?: "PDO Officer"
        val role = prefsManager.getRole() ?: "PDO"
        val village = prefsManager.getVillage() ?: "Smart Village"
        val userId = prefsManager.getUserId()

        binding.tvProfileName.text = name
        binding.tvProfileRole.text = "Role: $role"
        binding.tvProfileVillage.text = "Village: $village"
        binding.tvProfileId.text = "Officer ID: #$userId"
        binding.tvAvatarInitial.text = name.firstOrNull()?.toString()?.uppercase() ?: "P"
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showLogoutConfirmDialog() {
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
        prefsManager.clearAll()
        RetrofitClient.resetInstance()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
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
