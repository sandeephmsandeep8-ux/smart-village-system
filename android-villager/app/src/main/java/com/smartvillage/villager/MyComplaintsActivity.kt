package com.smartvillage.villager

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.smartvillage.villager.adapters.ComplaintAdapter
import com.smartvillage.villager.databinding.ActivityMyComplaintsBinding
import com.smartvillage.villager.network.RetrofitClient
import kotlinx.coroutines.launch

class MyComplaintsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyComplaintsBinding
    private lateinit var complaintAdapter: ComplaintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyComplaintsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "My Reports"
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        loadComplaints()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("complaint_id", complaint.id)
            startActivity(intent)
        }
        binding.rvComplaints.apply {
            layoutManager = LinearLayoutManager(this@MyComplaintsActivity)
            adapter = complaintAdapter
        }
    }

    private fun loadComplaints() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoComplaints.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getComplaints()
                if (response.isSuccessful) {
                    val complaints = response.body() ?: emptyList()
                    if (complaints.isEmpty()) {
                        binding.tvNoComplaints.visibility = View.VISIBLE
                        binding.rvComplaints.visibility = View.GONE
                    } else {
                        binding.tvNoComplaints.visibility = View.GONE
                        binding.rvComplaints.visibility = View.VISIBLE
                        complaintAdapter.submitList(complaints.reversed())
                    }
                } else {
                    Snackbar.make(binding.root, "Failed to load complaints", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Network error: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadComplaints()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
