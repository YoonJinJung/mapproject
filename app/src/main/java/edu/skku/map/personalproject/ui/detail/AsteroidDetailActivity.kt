package edu.skku.map.personalproject.ui.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.model.Asteroid
import edu.skku.map.personalproject.data.repository.AsteroidRepository
import edu.skku.map.personalproject.databinding.ActivityAsteroidDetailBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AsteroidDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASTEROID_JSON = "extra_asteroid_json"
    }

    private lateinit var binding: ActivityAsteroidDetailBinding
    private lateinit var repository: AsteroidRepository
    private lateinit var asteroid: Asteroid
    private var isInWatchlist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsteroidDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GSON으로 JSON 역직렬화
        val json = intent.getStringExtra(EXTRA_ASTEROID_JSON)
        if (json == null) { finish(); return }
        asteroid = try {
            Gson().fromJson(json, Asteroid::class.java) ?: run { finish(); return }
        } catch (e: Exception) {
            finish(); return
        }

        repository = AsteroidRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.asteroid_detail_title)

        bindData()
        checkWatchlistStatus()

        binding.btnWatchlist.setOnClickListener { toggleWatchlist() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun bindData() {
        binding.tvDetailName.text = asteroid.name

        if (asteroid.isPotentiallyHazardous) {
            binding.tvHazardStatus.text = getString(R.string.status_hazardous)
            binding.tvHazardStatus.setBackgroundColor(getColor(R.color.color_hazardous))
            binding.tvHazardStatus.setTextColor(getColor(R.color.white))
        } else {
            binding.tvHazardStatus.text = getString(R.string.status_safe)
            binding.tvHazardStatus.setBackgroundColor(getColor(R.color.color_safe))
            binding.tvHazardStatus.setTextColor(getColor(R.color.color_on_primary))
        }

        val fmtDec = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 3 }
        val fmtInt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }

        binding.tvDiameter.text =
            "${fmtDec.format(asteroid.estimatedDiameterMinKm)} – ${fmtDec.format(asteroid.estimatedDiameterMaxKm)} km"

        val velocityKmh = asteroid.relativeVelocityKmh.toDoubleOrNull() ?: 0.0
        binding.tvVelocity.text = "${fmtInt.format(velocityKmh)} km/h"

        val distanceKm = asteroid.missDistanceKm.toDoubleOrNull() ?: 0.0
        binding.tvDistance.text = "${fmtInt.format(distanceKm)} km"

        binding.tvApproachDate.text = asteroid.closeApproachDate
        binding.tvOrbitingBody.text = asteroid.orbitingBody
        binding.tvMagnitude.text = asteroid.absoluteMagnitude?.toString() ?: "N/A"
    }

    private fun checkWatchlistStatus() {
        lifecycleScope.launch {
            try {
                isInWatchlist = repository.isInWatchlist(asteroid.id)
            } catch (_: Exception) {}
            updateWatchlistButton()
        }
    }

    private fun toggleWatchlist() {
        lifecycleScope.launch {
            try {
                if (isInWatchlist) {
                    repository.removeFromWatchlist(asteroid.id)
                    isInWatchlist = false
                    Toast.makeText(
                        this@AsteroidDetailActivity,
                        getString(R.string.removed_from_watchlist),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    repository.addToWatchlist(asteroid)
                    isInWatchlist = true
                    Toast.makeText(
                        this@AsteroidDetailActivity,
                        getString(R.string.added_to_watchlist),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                updateWatchlistButton()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AsteroidDetailActivity,
                    "Failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateWatchlistButton() {
        if (isInWatchlist) {
            binding.btnWatchlist.text = getString(R.string.btn_remove_watchlist)
            binding.btnWatchlist.setBackgroundColor(getColor(R.color.color_hazardous))
        } else {
            binding.btnWatchlist.text = getString(R.string.btn_add_watchlist)
            binding.btnWatchlist.setBackgroundColor(getColor(R.color.color_primary))
        }
    }
}
