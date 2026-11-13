package com.smartvillage.worker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartvillage.worker.adapters.TaskAdapter
import com.smartvillage.worker.databinding.ActivityDashboardBinding
import com.smartvillage.worker.models.Assignment
import com.smartvillage.worker.models.WorkerDashboard
import com.smartvillage.worker.network.RetrofitClient
import com.smartvillage.worker.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = (application as SmartVillageWorkerApp).prefsManager

        setupToolbar()
        setupWelcomeCard()
        setupRecyclerView()
        setupQuickActions()
        setupBottomNav()
        loadDashboard()
        loadRecentTasks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.worker_dashboard)
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))

        binding.ivNotification.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWelcomeCard() {
        val name = prefsManager.getName() ?: "Worker"
        val village = prefsManager.getVillage() ?: "Smart Village"
        binding.tvWorkerName.text = "Hello, $name!"
        binding.tvWorkerRole.text = getString(R.string.panchayat_worker)
        binding.tvWorkerVillage.text = village
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { assignment ->
            val intent = Intent(this, TaskDetailActivity::class.java)
            intent.putExtra("assignment_id", assignment.id)
            startActivity(intent)
        }
        binding.rvRecentTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupQuickActions() {
        binding.cardMyTasks.setOnClickListener {
            startActivity(Intent(this, MyTasksActivity::class.java))
        }
        binding.cardUpdateStatus.setOnClickListener {
            startActivity(Intent(this, UpdateStatusActivity::class.java))
        }
        binding.cardUploadPhotos.setOnClickListener {
            startActivity(Intent(this, MyTasksActivity::class.java))
        }
        binding.cardCompletedWork.setOnClickListener {
            val intent = Intent(this, MyTasksActivity::class.java)
            intent.putExtra("tab", 2)
            startActivity(intent)
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_tasks -> {
                    startActivity(Intent(this, MyTasksActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().getWorkerDashboard()
                if (response.isSuccessful) {
                    response.body()?.let { updateStats(it) }
                }
            } catch (e: Exception) {
                // Show cached/default data
            }
        }
    }

    private fun updateStats(dashboard: WorkerDashboard) {
        binding.tvAssignedCount.text = dashboard.assignedCount.toString()
        binding.tvInProgressCount.text = dashboard.inProgressCount.toString()
        binding.tvCompletedCount.text = dashboard.completedCount.toString()
    }

    private fun loadRecentTasks() {
        binding.progressRecentTasks.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().getAssignments()
                binding.progressRecentTasks.visibility = View.GONE
                if (response.isSuccessful) {
                    val tasks = response.body() ?: emptyList()
                    val recentTasks = tasks.take(5)
                    if (recentTasks.isEmpty()) {
                        binding.tvNoRecentTasks.visibility = View.VISIBLE
                        binding.rvRecentTasks.visibility = View.GONE
                    } else {
                        binding.tvNoRecentTasks.visibility = View.GONE
                        binding.rvRecentTasks.visibility = View.VISIBLE
                        taskAdapter.submitList(recentTasks)
                    }
                }
            } catch (e: Exception) {
                binding.progressRecentTasks.visibility = View.GONE
                binding.tvNoRecentTasks.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
        loadRecentTasks()
    }
}
