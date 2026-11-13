package com.smartvillage.worker

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.smartvillage.worker.adapters.TaskAdapter
import com.smartvillage.worker.databinding.ActivityMyTasksBinding
import com.smartvillage.worker.models.Assignment
import com.smartvillage.worker.network.RetrofitClient
import kotlinx.coroutines.launch

class MyTasksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyTasksBinding
    private lateinit var taskAdapter: TaskAdapter
    private var allAssignments: List<Assignment> = emptyList()

    // Tab indices
    companion object {
        const val TAB_ASSIGNED = 0
        const val TAB_IN_PROGRESS = 1
        const val TAB_COMPLETED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRecyclerView()
        loadAssignments()

        // Check if a specific tab was requested
        val tab = intent.getIntExtra("tab", TAB_ASSIGNED)
        binding.tabLayout.getTabAt(tab)?.select()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.my_tasks)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setTitleTextColor(getColor(android.R.color.white))
        binding.toolbar.navigationIcon?.setTint(getColor(android.R.color.white))
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_assigned))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_in_progress))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_completed))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAndDisplay(tab?.position ?: TAB_ASSIGNED)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { assignment ->
            val intent = Intent(this, TaskDetailActivity::class.java)
            intent.putExtra("assignment_id", assignment.id)
            startActivity(intent)
        }
        binding.rvTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MyTasksActivity)
        }
    }

    private fun loadAssignments() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance().getAssignments()
                showLoading(false)
                if (response.isSuccessful) {
                    allAssignments = response.body() ?: emptyList()
                    filterAndDisplay(binding.tabLayout.selectedTabPosition)
                } else {
                    showError("Failed to load tasks")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Network error: ${e.localizedMessage}")
            }
        }
    }

    private fun filterAndDisplay(tabIndex: Int) {
        val filtered = when (tabIndex) {
            TAB_ASSIGNED -> allAssignments.filter { it.status.uppercase() == "ASSIGNED" }
            TAB_IN_PROGRESS -> allAssignments.filter { it.status.uppercase() == "IN_PROGRESS" }
            TAB_COMPLETED -> allAssignments.filter { it.status.uppercase() == "COMPLETED" }
            else -> allAssignments
        }

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvTasks.visibility = View.GONE
            val emptyMsg = when (tabIndex) {
                TAB_ASSIGNED -> "No assigned tasks"
                TAB_IN_PROGRESS -> "No tasks in progress"
                TAB_COMPLETED -> "No completed tasks"
                else -> "No tasks found"
            }
            binding.tvEmptyState.text = emptyMsg
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvTasks.visibility = View.VISIBLE
            taskAdapter.submitList(filtered)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvTasks.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = msg
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        loadAssignments()
    }
}
