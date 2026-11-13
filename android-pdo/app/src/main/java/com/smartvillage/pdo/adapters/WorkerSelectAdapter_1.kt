package com.smartvillage.pdo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartvillage.pdo.databinding.ItemWorkerSelectBinding
import com.smartvillage.pdo.models.Worker

class WorkerSelectAdapter(
    private var workers: List<Worker>,
    private val onWorkerSelected: (Worker) -> Unit
) : RecyclerView.Adapter<WorkerSelectAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1

    inner class ViewHolder(private val binding: ItemWorkerSelectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(worker: Worker, position: Int) {
            binding.tvWorkerName.text = worker.fullName
            binding.tvWorkerVillage.text = worker.villageName ?: "N/A"
            binding.tvWorkerInitial.text = worker.fullName.firstOrNull()?.toString()?.uppercase() ?: "W"

            binding.root.isSelected = position == selectedPosition

            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onWorkerSelected(worker)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkerSelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(workers[position], position)
    }

    override fun getItemCount() = workers.size

    fun updateData(newWorkers: List<Worker>) {
        workers = newWorkers
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
