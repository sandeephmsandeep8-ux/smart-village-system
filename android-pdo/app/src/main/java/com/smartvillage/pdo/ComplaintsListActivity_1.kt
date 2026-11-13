package com.smartvillage.pdo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartvillage.pdo.adapters.ComplaintPdoAdapter
import com.smartvillage.pdo.databinding.ActivityComplaintsListBinding
import com.smartvillage.pdo.models.Complaint
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComplaintsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintsListBinding
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var complaintAdapter: ComplaintPdoAdapter

    private var allComplaints: List<Complaint> = emptyList()
    private var currentFilter: String = "ALL"
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupSearchView()
        loadComplaints()

        // Handle tab selection from intent
        val tab = intent.getIntExtra("TAB", 0)
        if (tab > 0) {
            binding.tabLayout.getTabAt(tab)?.select()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Complaints"
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintPdoAdapter(emptyList()) { complaint ->
            val intent = Intent(this, ComplaintDetailActivity::class.java)
            intent.putExtra("COMPLAINT_ID", complaint.id)
            startActivity(intent)
        }
        binding.rvComplaints.apply {
            layoutManager = LinearLayoutManager(this@ComplaintsListActivity)
            adapter = complaintAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("In Progress"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Resolved"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))

        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "PENDING"
                    1 -> "IN_PROGRESS"
                    2 -> "RESOLVED"
                    else -> "ALL"
                }
                applyFilters()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Default: All tab selected (index 3)
        binding.tabLayout.getTabAt(3)?.select()
        currentFilter = "ALL"
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query?.trim() ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.trim() ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun applyFilters() {
        var filtered = allComplaints

        if (currentFilter != "ALL") {
            filtered = filtered.filter { it.status.equals(currentFilter, ignoreCase = true) }
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { complaint ->
                complaint.title.contains(searchQuery, ignoreCase = true) ||
                        complaint.description.contains(searchQuery, ignoreCase = true) ||
                        (complaint.villageName?.contains(searchQuery, ignoreCase = true) == true) ||
                        complaint.category.contains(searchQuery, ignoreCase = true)
            }
        }

        complaintAdapter.updateData(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadComplaints() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        RetrofitClient.getInstance().getComplaints().enqueue(object : Callback<List<Complaint>> {
            override fun onResponse(
                call: Call<List<Complaint>>,
                response: Response<List<Complaint>>
            ) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    allComplaints = response.body() ?: emptyList()
                    applyFilters()
                } else {
                    Toast.makeText(
                        this@ComplaintsListActivity,
                        "Failed to load complaints",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Complaint>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ComplaintsListActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
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
        loadComplaints()
    }
}
