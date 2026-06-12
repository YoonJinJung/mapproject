package edu.skku.map.personalproject.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.model.FeedResponse
import edu.skku.map.personalproject.data.repository.AsteroidRepository
import edu.skku.map.personalproject.databinding.ActivityDashboardBinding
import edu.skku.map.personalproject.ui.list.AsteroidListActivity
import edu.skku.map.personalproject.ui.watchlist.WatchlistActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var repository: AsteroidRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AsteroidRepository(this)

        setSupportActionBar(binding.toolbar)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        binding.tvDate.text = today

        binding.btnViewAsteroids.setOnClickListener {
            startActivity(Intent(this, AsteroidListActivity::class.java))
        }
        binding.btnViewWatchlist.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }
        binding.btnRetry.setOnClickListener { fetchData() }

        fetchData()
    }

    private fun fetchData() {
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val feed = repository.fetchFeed()
                setLoadingState(false)
                showSummary(feed)
            } catch (e: Exception) {
                setLoadingState(false)
                showError(e.message ?: getString(R.string.error_network))
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.cardSummary.visibility = View.GONE
        binding.groupError.visibility = View.GONE
    }

    private fun showSummary(feed: FeedResponse) {
        binding.cardSummary.visibility = View.VISIBLE
        binding.tvTotalCount.text = feed.summary.totalCount.toString()
        binding.tvHazardousCount.text = feed.summary.hazardousCount.toString()

        val hazardColor = if (feed.summary.hazardousCount > 0)
            getColor(R.color.color_hazardous)
        else
            getColor(R.color.color_safe)
        binding.tvHazardousCount.setTextColor(hazardColor)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.groupError.visibility = View.VISIBLE
    }
}
