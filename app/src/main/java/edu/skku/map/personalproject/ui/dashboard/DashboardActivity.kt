package edu.skku.map.personalproject.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
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
            // NeoWs + APOD 병렬 호출
            val (feedResult, apodResult) = coroutineScope {
                val feedDeferred = async { runCatching { repository.fetchFeed() } }
                val apodDeferred = async { runCatching { repository.fetchApod() } }
                Pair(feedDeferred.await(), apodDeferred.await())
            }

            setLoadingState(false)

            feedResult
                .onSuccess { showSummary(it) }
                .onFailure { showError(it.message ?: getString(R.string.error_network)) }

            apodResult.onSuccess { apod ->
                binding.cardApod.visibility = View.VISIBLE
                binding.tvApodTitle.text = apod.title
                binding.tvApodExplanation.text = apod.explanation
                binding.tvApodCopyright.text = apod.copyright?.let { "© $it" } ?: ""

                if (apod.mediaType == "image" && apod.url.isNotEmpty()) {
                    // 이미지 로드 (IO → Main)
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
                    // 영상 타입
                    binding.progressApod.visibility = View.GONE
                    binding.tvApodNotice.text = getString(R.string.apod_video_notice)
                    binding.tvApodNotice.visibility = View.VISIBLE
                }
            }
            // APOD 실패 시 카드 숨김 유지 (NeoWs가 주 기능이므로 에러 표시 안 함)
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
