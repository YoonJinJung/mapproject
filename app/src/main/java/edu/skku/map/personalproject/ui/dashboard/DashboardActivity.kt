package edu.skku.map.personalproject.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.model.ApodResponse
import edu.skku.map.personalproject.data.model.FeedResponse
import edu.skku.map.personalproject.data.repository.AsteroidRepository
import edu.skku.map.personalproject.databinding.ActivityDashboardBinding
import edu.skku.map.personalproject.ui.list.AsteroidListActivity
import edu.skku.map.personalproject.ui.watchlist.WatchlistActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var repository: AsteroidRepository

    // APOD 고해상도 URL (클릭 시 브라우저로 열기)
    private var apodBrowserUrl: String = ""

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

        // APOD 카드 클릭 → 브라우저 열기 (Implicit Intent)
        binding.cardApod.setOnClickListener {
            openApodInBrowser()
        }

        fetchData()
    }

    private fun fetchData() {
        setLoadingState(true)

        lifecycleScope.launch {
            val (feedResult, apodResult) = coroutineScope {
                val feedDeferred = async { runCatching { repository.fetchFeed() } }
                val apodDeferred = async { runCatching { repository.fetchApod() } }
                Pair(feedDeferred.await(), apodDeferred.await())
            }

            setLoadingState(false)

            feedResult
                .onSuccess { showSummary(it) }
                .onFailure { showError(it.message ?: getString(R.string.error_network)) }

            apodResult.onSuccess { apod -> showApod(apod) }
        }
    }

    private suspend fun showApod(apod: ApodResponse) {
        // 브라우저에서 열 URL: hdurl(원본) 우선, 없으면 url
        apodBrowserUrl = apod.hdurl?.takeIf { it.isNotEmpty() } ?: apod.url

        binding.cardApod.visibility = View.VISIBLE
        binding.tvApodTitle.text = apod.title
        binding.tvApodExplanation.text = apod.explanation
        binding.tvApodCopyright.text = apod.copyright?.let { "© $it" } ?: ""

        if (apod.mediaType == "image" && apod.url.isNotEmpty()) {
            val bitmap = repository.loadApodBitmap(apod.url)
            binding.progressApod.visibility = View.GONE
            if (bitmap != null) {
                binding.ivApod.setImageBitmap(bitmap)
                binding.ivApod.visibility = View.VISIBLE
            } else {
                binding.tvApodNotice.text = getString(R.string.apod_load_error)
                binding.tvApodNotice.visibility = View.VISIBLE
            }
        } else {
            binding.progressApod.visibility = View.GONE
            binding.tvApodNotice.text = getString(R.string.apod_video_notice)
            binding.tvApodNotice.visibility = View.VISIBLE
        }
    }

    // ── Implicit Intent: 브라우저로 APOD 원본 이미지 열기 ─────────────────────
    private fun openApodInBrowser() {
        if (apodBrowserUrl.isEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apodBrowserUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.browser_error), Toast.LENGTH_SHORT).show()
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
            getColor(R.color.color_hazardous) else getColor(R.color.color_safe)
        binding.tvHazardousCount.setTextColor(hazardColor)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.groupError.visibility = View.VISIBLE
    }
}
