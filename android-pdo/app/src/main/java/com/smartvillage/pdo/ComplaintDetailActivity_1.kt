package com.smartvillage.pdo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.smartvillage.pdo.databinding.ActivityComplaintDetailBinding
import com.smartvillage.pdo.models.AssignRequest
import com.smartvillage.pdo.models.ComplaintDetail
import com.smartvillage.pdo.models.MessageResponse
import com.smartvillage.pdo.models.RejectRequest
import com.smartvillage.pdo.models.Worker
import com.smartvillage.pdo.network.RetrofitClient
import com.smartvillage.pdo.utils.SharedPrefsManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintDetailBinding
    private lateinit var prefsManager: SharedPrefsManager

    private var complaintId: Int = -1
    private var currentComplaint: ComplaintDetail? = null
    private var workersList: List<Worker> = emptyList()
    private var selectedWorkerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintDetailBinding.inflate(layoutInflater)
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
        loadComplaintDetail()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Complaint Detail"
    }

    private fun loadComplaintDetail() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollContent.visibility = View.GONE

        RetrofitClient.getInstance().getComplaintDetail(complaintId)
            .enqueue(object : Callback<ComplaintDetail> {
                override fun onResponse(
                    call: Call<ComplaintDetail>,
                    response: Response<ComplaintDetail>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        response.body()?.let { complaint ->
                            currentComplaint = complaint
                            displayComplaintDetail(complaint)
                            binding.scrollContent.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Failed to load complaint",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ComplaintDetail>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ComplaintDetailActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun displayComplaintDetail(complaint: ComplaintDetail) {
        binding.tvTitle.text = complaint.title
        binding.tvCategory.text = complaint.category
        binding.tvDescription.text = complaint.description
        binding.tvReportedBy.text = complaint.reportedBy ?: "Unknown"
        binding.tvVillage.text = complaint.villageName ?: "Unknown"
        binding.tvLocation.text = complaint.location ?: "Not specified"
        binding.tvCreatedAt.text = formatDate(complaint.createdAt)

        // Status badge
        binding.tvStatus.text = complaint.status.replace("_", " ")
        val (bgColor, textColor) = getStatusColors(complaint.status)
        binding.tvStatus.setBackgroundColor(bgColor)
        binding.tvStatus.setTextColor(textColor)

        // Load image
        if (!complaint.imageUrl.isNullOrEmpty()) {
            binding.ivComplaintImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(complaint.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(binding.ivComplaintImage)
        } else {
            binding.ivComplaintImage.visibility = View.GONE
        }

        // Show/hide action sections based on status
        setupActionSection(complaint)
    }

    private fun setupActionSection(complaint: ComplaintDetail) {
        // Hide all action sections first
        binding.sectionPendingActions.visibility = View.GONE
        binding.sectionAssignWorker.visibility = View.GONE
        binding.sectionInProgress.visibility = View.GONE
        binding.sectionResolved.visibility = View.GONE

        when (complaint.status.uppercase()) {
            "PENDING" -> {
                binding.sectionPendingActions.visibility = View.VISIBLE
                binding.btnAccept.setOnClickListener { acceptComplaint() }
                binding.btnReject.setOnClickListener { showRejectDialog() }
            }
            "ACCEPTED" -> {
                binding.sectionAssignWorker.visibility = View.VISIBLE
                loadWorkersForAssignment()
            }
            "IN_PROGRESS" -> {
                binding.sectionInProgress.visibility = View.VISIBLE
                binding.tvAssignedWorkerName.text = complaint.assignedWorker ?: "Unknown Worker"
                binding.btnVerifyComplete.setOnClickListener { resolveComplaint() }
            }
            "RESOLVED" -> {
                binding.sectionResolved.visibility = View.VISIBLE
                binding.tvResolvedAt.text = "Resolved on: ${formatDate(complaint.updatedAt)}"
            }
        }
    }

    private fun acceptComplaint() {
        setButtonsEnabled(false)
        RetrofitClient.getInstance().acceptComplaint(complaintId)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {
                    setButtonsEnabled(true)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Complaint accepted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadComplaintDetail()
                    } else {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Failed to accept complaint",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    setButtonsEnabled(true)
                    Toast.makeText(
                        this@ComplaintDetailActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showRejectDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null)
        val etReason = dialogView.findViewById<EditText>(R.id.etRejectReason)

        AlertDialog.Builder(this)
            .setTitle("Reject Complaint")
            .setMessage("Please provide a reason for rejection:")
            .setView(dialogView)
            .setPositiveButton("Reject") { _, _ ->
                val reason = etReason.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
                } else {
                    rejectComplaint(reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectComplaint(reason: String) {
        setButtonsEnabled(false)
        val request = RejectRequest(reason)
        RetrofitClient.getInstance().rejectComplaint(complaintId, request)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {
                    setButtonsEnabled(true)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Complaint rejected",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Failed to reject complaint",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    setButtonsEnabled(true)
                    Toast.makeText(
                        this@ComplaintDetailActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadWorkersForAssignment() {
        binding.progressAssign.visibility = View.VISIBLE
        RetrofitClient.getInstance().getWorkers().enqueue(object : Callback<List<Worker>> {
            override fun onResponse(call: Call<List<Worker>>, response: Response<List<Worker>>) {
                binding.progressAssign.visibility = View.GONE
                if (response.isSuccessful) {
                    workersList = response.body() ?: emptyList()
                    if (workersList.isEmpty()) {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "No workers available",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    val workerNames =
                        workersList.map { "${it.fullName} (${it.villageName ?: "N/A"})" }
                    val spinnerAdapter = ArrayAdapter(
                        this@ComplaintDetailActivity,
                        android.R.layout.simple_spinner_item,
                        workerNames
                    )
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerWorkers.adapter = spinnerAdapter
                    selectedWorkerId = workersList[0].id

                    binding.spinnerWorkers.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {
                                selectedWorkerId = workersList[position].id
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }

                    binding.btnAssignWorker.setOnClickListener {
                        if (selectedWorkerId != -1) {
                            assignWorker(selectedWorkerId)
                        }
                    }

                    binding.btnOpenAssignScreen.setOnClickListener {
                        val intent = Intent(
                            this@ComplaintDetailActivity,
                            AssignWorkerActivity::class.java
                        )
                        intent.putExtra("COMPLAINT_ID", complaintId)
                        startActivity(intent)
                    }
                }
            }

            override fun onFailure(call: Call<List<Worker>>, t: Throwable) {
                binding.progressAssign.visibility = View.GONE
                Toast.makeText(
                    this@ComplaintDetailActivity,
                    "Failed to load workers",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun assignWorker(workerId: Int) {
        setButtonsEnabled(false)
        val request = AssignRequest(workerId)
        RetrofitClient.getInstance().assignComplaint(complaintId, request)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {
                    setButtonsEnabled(true)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Worker assigned successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadComplaintDetail()
                    } else {
                        Toast.makeText(
                            this@ComplaintDetailActivity,
                            "Failed to assign worker",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    setButtonsEnabled(true)
                    Toast.makeText(
                        this@ComplaintDetailActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun resolveComplaint() {
        AlertDialog.Builder(this)
            .setTitle("Mark as Resolved")
            .setMessage("Are you sure this complaint has been resolved?")
            .setPositiveButton("Yes, Resolve") { _, _ ->
                setButtonsEnabled(false)
                RetrofitClient.getInstance().resolveComplaint(complaintId)
                    .enqueue(object : Callback<MessageResponse> {
                        override fun onResponse(
                            call: Call<MessageResponse>,
                            response: Response<MessageResponse>
                        ) {
                            setButtonsEnabled(true)
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@ComplaintDetailActivity,
                                    "Complaint marked as resolved",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadComplaintDetail()
                            } else {
                                Toast.makeText(
                                    this@ComplaintDetailActivity,
                                    "Failed to resolve complaint",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                            setButtonsEnabled(true)
                            Toast.makeText(
                                this@ComplaintDetailActivity,
                                "Network error: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnAccept.isEnabled = enabled
        binding.btnReject.isEnabled = enabled
        binding.btnAssignWorker.isEnabled = enabled
        binding.btnVerifyComplete.isEnabled = enabled
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getStatusColors(status: String): Pair<Int, Int> {
        return when (status.uppercase()) {
            "PENDING" -> Pair(
                getColor(R.color.status_pending_bg),
                getColor(R.color.status_pending_text)
            )
            "ACCEPTED" -> Pair(
                getColor(R.color.status_accepted_bg),
                getColor(R.color.status_accepted_text)
            )
            "IN_PROGRESS" -> Pair(
                getColor(R.color.status_inprogress_bg),
                getColor(R.color.status_inprogress_text)
            )
            "RESOLVED" -> Pair(
                getColor(R.color.status_resolved_bg),
                getColor(R.color.status_resolved_text)
            )
            "REJECTED" -> Pair(
                getColor(R.color.status_rejected_bg),
                getColor(R.color.status_rejected_text)
            )
            else -> Pair(getColor(R.color.grey_200), getColor(R.color.grey_700))
        }
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
        if (complaintId != -1) loadComplaintDetail()
    }
}
