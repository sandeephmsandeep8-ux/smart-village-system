package com.smartvillage.villager

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.smartvillage.villager.databinding.ActivityRegisterBinding
import com.smartvillage.villager.models.RegisterRequest
import com.smartvillage.villager.network.RetrofitClient
import com.smartvillage.villager.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = getString(R.string.register)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val mobile = binding.etMobile.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val village = binding.etVillage.text.toString().trim()

            if (!validateInputs(fullName, mobile, email, username, password, confirmPassword, village)) return@setOnClickListener

            performRegister(fullName, mobile, email, username, password, village)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(
        fullName: String, mobile: String, email: String,
        username: String, password: String, confirmPassword: String, village: String
    ): Boolean {
        var isValid = true

        if (fullName.isEmpty()) { binding.tilFullName.error = "Full name is required"; isValid = false }
        else binding.tilFullName.error = null

        if (mobile.isEmpty()) { binding.tilMobile.error = "Mobile number is required"; isValid = false }
        else if (mobile.length < 10) { binding.tilMobile.error = "Enter a valid mobile number"; isValid = false }
        else binding.tilMobile.error = null

        if (email.isEmpty()) { binding.tilEmail.error = "Email is required"; isValid = false }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilEmail.error = "Enter a valid email"; isValid = false }
        else binding.tilEmail.error = null

        if (username.isEmpty()) { binding.tilUsername.error = "Username is required"; isValid = false }
        else binding.tilUsername.error = null

        if (password.isEmpty()) { binding.tilPassword.error = "Password is required"; isValid = false }
        else if (password.length < 6) { binding.tilPassword.error = "Password must be at least 6 characters"; isValid = false }
        else binding.tilPassword.error = null

        if (confirmPassword != password) { binding.tilConfirmPassword.error = "Passwords do not match"; isValid = false }
        else binding.tilConfirmPassword.error = null

        if (village.isEmpty()) { binding.tilVillage.error = "Village name is required"; isValid = false }
        else binding.tilVillage.error = null

        return isValid
    }

    private fun performRegister(
        fullName: String, mobile: String, email: String,
        username: String, password: String, village: String
    ) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val request = RegisterRequest(fullName, mobile, email, username, password, village)
                val response = RetrofitClient.instance.register(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        SharedPrefsManager.saveToken(body.token)
                        SharedPrefsManager.saveUser(body.userId, body.name, body.role, body.village)
                        SharedPrefsManager.saveMobile(mobile)
                        Snackbar.make(binding.root, "Registration successful!", Snackbar.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "Email or username already exists"
                        400 -> "Invalid registration details"
                        else -> "Registration failed. Please try again."
                    }
                    Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Network error: ${e.localizedMessage ?: "Please check your connection"}", Snackbar.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "" else getString(R.string.register)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
