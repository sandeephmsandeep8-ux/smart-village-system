package com.smartvillage.worker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartvillage.worker.databinding.ActivityLoginBinding
import com.smartvillage.worker.models.LoginRequest
import com.smartvillage.worker.network.RetrofitClient
import com.smartvillage.worker.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = (application as SmartVillageWorkerApp).prefsManager

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            }

            binding.tilEmail.error = null
            binding.tilPassword.error = null
            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance()
                    .login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Verify role is WORKER
                        if (body.role.uppercase() != "WORKER") {
                            setLoading(false)
                            Toast.makeText(
                                this@LoginActivity,
                                "Access denied. Worker credentials required.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        prefsManager.saveLoginData(
                            token = body.token,
                            userId = body.userId,
                            name = body.name,
                            role = body.role
                        )
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        setLoading(false)
                        Toast.makeText(this@LoginActivity, "Login failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    setLoading(false)
                    val errMsg = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> "Access denied for this role"
                        else -> "Login failed (${response.code()})"
                    }
                    Toast.makeText(this@LoginActivity, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Network error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.login)
    }
}
