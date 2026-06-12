package edu.skku.map.personalproject.ui.watchlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.local.WatchlistEntity
import edu.skku.map.personalproject.databinding.ItemWatchlistBinding
import java.text.NumberFormat
import java.util.Locale

class WatchlistAdapter(
    private val onDeleteClick: (WatchlistEntity) -> Unit
) : ListAdapter<WatchlistEntity, WatchlistAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWatchlistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWatchlistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: WatchlistEntity) {
            val ctx = binding.root.context

            binding.tvName.text = entity.name
            binding.tvApproachDate.text = entity.closeApproachDate

            val distanceKm = entity.missDistanceKm.toDoubleOrNull() ?: 0.0
            val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(distanceKm)
            binding.tvMissDistance.text = "$formatted km"

            if (entity.isPotentiallyHazardous) {
                binding.tvHazardBadge.visibility = View.VISIBLE
                binding.tvHazardBadge.text = ctx.getString(R.string.hazard_badge)
                binding.tvHazardBadge.setBackgroundColor(ctx.getColor(R.color.color_hazardous))
            } else {
                binding.tvHazardBadge.visibility = View.GONE
            }

            binding.btnDelete.setOnClickListener { onDeleteClick(entity) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WatchlistEntity>() {
        override fun areItemsTheSame(oldItem: WatchlistEntity, newItem: WatchlistEntity) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WatchlistEntity, newItem: WatchlistEntity) =
            oldItem == newItem
    }
}
