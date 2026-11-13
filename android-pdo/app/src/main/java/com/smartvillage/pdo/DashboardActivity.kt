package com.smartvillage.pdo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartvillage.pdo.adapters.ComplaintPdoAdapter
import com.smartvillage.pdo.databinding.ActivityDashboardBinding
import com.smartvillage.pdo.models.Complaint
import com.smartvillage.pdo.models.PdoDashboard
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var complaintAdapter: ComplaintPdoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)

        setupToolbar()
        setupProfileCard()
        setupQuickActions()
        setupRecentComplaints()
        setupBottomNavigation()
        loadDashboardData()
        loadRecentComplaints()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.ivNotification.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProfileCard() {
        val name = prefsManager.getName() ?: "PDO Officer"
        val village = prefsManager.getVillage() ?: "Smart Village"
        binding.tvOfficerName.text = name
        binding.tvVillageName.text = village
    }

    private fun setupQuickActions() {
        binding.cardViewComplaints.setOnClickListener {
            startActivity(Intent(this, ComplaintsListActivity::class.java))
        }
        binding.cardAssignWork.setOnClickListener {
            val intent = Intent(this, ComplaintsListActivity::class.java)
            intent.putExtra("TAB", 1) // In Progress tab
            startActivity(intent)
        }
        binding.cardViewReports.setOnClickListener {
            Toast.makeText(this, "Reports coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.cardAnalytics.setOnClickListener {
            Toast.makeText(this, "Analytics coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecentComplaints() {
        complaintAdapter = ComplaintPdoAdapter(emptyList()) { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("COMPLAINT_ID", complaint.id)
            startActivity(intent)
        }
        binding.rvRecentComplaints.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = complaintAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_complaints -> {
                    startActivity(Intent(this, ComplaintsListActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun loadDashboardData() {
        binding.progressDashboard.visibility = View.VISIBLE
        RetrofitClient.getInstance().getDashboard().enqueue(object : Callback<PdoDashboard> {
            override fun onResponse(call: Call<PdoDashboard>, response: Response<PdoDashboard>) {
                binding.progressDashboard.visibility = View.GONE
                if (response.isSuccessful) {
                    response.body()?.let { dashboard ->
                        binding.tvPendingCount.text = dashboard.pendingCount.toString()
                        binding.tvInProgressCount.text = dashboard.inProgressCount.toString()
                        binding.tvResolvedCount.text = dashboard.resolvedCount.toString()
                    }
                } else {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Failed to load dashboard",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<PdoDashboard>, t: Throwable) {
                binding.progressDashboard.visibility = View.GONE
                Toast.makeText(
                    this@DashboardActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadRecentComplaints() {
        RetrofitClient.getInstance().getComplaints().enqueue(object : Callback<List<Complaint>> {
            override fun onResponse(
                call: Call<List<Complaint>>,
                response: Response<List<Complaint>>
            ) {
                if (response.isSuccessful) {
                    val complaints = response.body() ?: emptyList()
                    val recentComplaints = complaints.take(5)
                    complaintAdapter.updateData(recentComplaints)
                    binding.tvNoComplaints.visibility =
                        if (recentComplaints.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onFailure(call: Call<List<Complaint>>, t: Throwable) {
                binding.tvNoComplaints.visibility = View.VISIBLE
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
        loadRecentComplaints()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }
}
