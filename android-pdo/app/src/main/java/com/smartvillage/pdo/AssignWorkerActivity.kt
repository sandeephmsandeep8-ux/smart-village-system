package com.smartvillage.pdo

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartvillage.pdo.adapters.WorkerSelectAdapter
import com.smartvillage.pdo.databinding.ActivityAssignWorkerBinding
import com.smartvillage.pdo.models.AssignRequest
import com.smartvillage.pdo.models.MessageResponse
import com.smartvillage.pdo.models.Worker
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssignWorkerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssignWorkerBinding
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var workerAdapter: WorkerSelectAdapter

    private var complaintId: Int = -1
    private var selectedWorker: Worker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignWorkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)
        RetrofitClient.init(prefsManager)

        complaintId = intent.getIntExtra("COMPLAINT_ID", -1)
        if (complaintId == -1) {
            Toast.makeText(this, "Invalid complaint", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupConfirmButton()
        loadWorkers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Assign Worker"
    }

    private fun setupRecyclerView() {
        workerAdapter = WorkerSelectAdapter(emptyList()) { worker ->
            selectedWorker = worker
            binding.btnConfirmAssign.isEnabled = true
            binding.tvSelectedWorker.text = "Selected: ${worker.fullName}"
            binding.tvSelectedWorker.visibility = View.VISIBLE
        }
        binding.rvWorkers.apply {
            layoutManager = LinearLayoutManager(this@AssignWorkerActivity)
            adapter = workerAdapter
        }
    }

    private fun setupConfirmButton() {
        binding.btnConfirmAssign.isEnabled = false
        binding.btnConfirmAssign.setOnClickListener {
            selectedWorker?.let { worker ->
                assignWorker(worker.id)
            }
        }
    }

    private fun loadWorkers() {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance().getWorkers().enqueue(object : Callback<List<Worker>> {
            override fun onResponse(call: Call<List<Worker>>, response: Response<List<Worker>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val workers = response.body() ?: emptyList()
                    workerAdapter.updateData(workers)
                    if (workers.isEmpty()) {
                        binding.tvNoWorkers.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@AssignWorkerActivity,
                        "Failed to load workers",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Worker>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@AssignWorkerActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun assignWorker(workerId: Int) {
        binding.btnConfirmAssign.isEnabled = false
        val request = AssignRequest(workerId)
        RetrofitClient.getInstance().assignComplaint(complaintId, request)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {
                    binding.btnConfirmAssign.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AssignWorkerActivity,
                            "Worker assigned successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@AssignWorkerActivity,
                            "Failed to assign worker",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    binding.btnConfirmAssign.isEnabled = true
                    Toast.makeText(
                        this@AssignWorkerActivity,
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
}
