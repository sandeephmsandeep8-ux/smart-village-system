package com.smartvillage.pdo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartvillage.pdo.databinding.ActivityLoginBinding
import com.smartvillage.pdo.models.AuthResponse
import com.smartvillage.pdo.models.LoginRequest
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            return false
        }
        binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        binding.tilPassword.error = null

        return true
    }

    private fun performLogin(email: String, password: String) {
        setLoading(true)

        val request = LoginRequest(email, password)
        RetrofitClient.getInstance().login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                setLoading(false)
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        if (authResponse.role != "PDO" && authResponse.role != "pdo" && authResponse.role != "OFFICER") {
                            Toast.makeText(
                                this@LoginActivity,
                                "Access denied. PDO Officer credentials only.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }
                        prefsManager.saveToken(authResponse.token)
                        prefsManager.saveUserId(authResponse.userId)
                        prefsManager.saveName(authResponse.name)
                        prefsManager.saveRole(authResponse.role)
                        RetrofitClient.resetInstance()
                        RetrofitClient.init(prefsManager)

                        navigateToDashboard()
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> "Access denied. PDO credentials required."
                        404 -> "Account not found"
                        else -> "Login failed. Please try again."
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
