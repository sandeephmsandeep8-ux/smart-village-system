package com.smartvillage.villager

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.smartvillage.villager.adapters.ComplaintAdapter
import com.smartvillage.villager.databinding.ActivityDashboardBinding
import com.smartvillage.villager.models.Complaint
import com.smartvillage.villager.network.RetrofitClient
import com.smartvillage.villager.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var complaintAdapter: ComplaintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val userName = SharedPrefsManager.getUserName() ?: "Villager"
        val village = SharedPrefsManager.getVillage() ?: ""
        binding.tvWelcome.text = "Welcome, $userName!"
        binding.tvVillageName.text = village

        setupRecyclerView()
        setupQuickActions()
        setupBottomNavigation()
        loadRecentComplaints()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("complaint_id", complaint.id)
            startActivity(intent)
        }
        binding.rvRecentComplaints.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = complaintAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupQuickActions() {
        binding.cardReportProblem.setOnClickListener {
            startActivity(Intent(this, ReportComplaintActivity::class.java))
        }
        binding.cardMyReports.setOnClickListener {
            startActivity(Intent(this, MyComplaintsActivity::class.java))
        }
        binding.cardTrackStatus.setOnClickListener {
            startActivity(Intent(this, MyComplaintsActivity::class.java))
        }
        binding.cardProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_reports -> {
                    startActivity(Intent(this, MyComplaintsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadRecentComplaints() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getComplaints()
                if (response.isSuccessful) {
                    val complaints = response.body() ?: emptyList()
                    val recent = complaints.takeLast(3).reversed()
                    if (recent.isEmpty()) {
                        binding.tvNoComplaints.visibility = View.VISIBLE
                        binding.rvRecentComplaints.visibility = View.GONE
                    } else {
                        binding.tvNoComplaints.visibility = View.GONE
                        binding.rvRecentComplaints.visibility = View.VISIBLE
                        complaintAdapter.submitList(recent)
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
        binding.bottomNav.selectedItemId = R.id.nav_home
        loadRecentComplaints()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
