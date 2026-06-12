package edu.skku.map.personalproject.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.model.Asteroid
import edu.skku.map.personalproject.databinding.ItemAsteroidBinding
import java.text.NumberFormat
import java.util.Locale

class AsteroidAdapter(
    private val onItemClick: (Asteroid) -> Unit
) : ListAdapter<Asteroid, AsteroidAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAsteroidBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAsteroidBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(asteroid: Asteroid) {
            val ctx = binding.root.context

            binding.tvName.text = asteroid.name
            binding.tvApproachDate.text = asteroid.closeApproachDate

            val distanceKm = asteroid.missDistanceKm.toDoubleOrNull() ?: 0.0
            val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 0
            }.format(distanceKm)
            binding.tvMissDistance.text = ctx.getString(R.string.label_distance) + ": $formatted km"

            if (asteroid.isPotentiallyHazardous) {
                binding.tvHazardBadge.visibility = View.VISIBLE
                binding.tvHazardBadge.text = ctx.getString(R.string.hazard_badge)
                binding.tvHazardBadge.setBackgroundColor(ctx.getColor(R.color.color_hazardous))
            } else {
                binding.tvHazardBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(asteroid) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Asteroid>() {
        override fun areItemsTheSame(oldItem: Asteroid, newItem: Asteroid) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Asteroid, newItem: Asteroid) =
            oldItem == newItem
    }
}
