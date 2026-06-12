package edu.skku.map.personalproject.ui.watchlist

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.local.WatchlistEntity
import edu.skku.map.personalproject.data.repository.AsteroidRepository
import edu.skku.map.personalproject.databinding.ActivityWatchlistBinding
import kotlinx.coroutines.launch

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private lateinit var repository: AsteroidRepository
    private lateinit var adapter: WatchlistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AsteroidRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.watchlist_title)

        setupRecyclerView()
        observeWatchlist()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        adapter = WatchlistAdapter { entity -> deleteItem(entity) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeWatchlist() {
        lifecycleScope.launch {
            repository.getWatchlist().collect { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun deleteItem(entity: WatchlistEntity) {
        lifecycleScope.launch {
            try {
                repository.removeFromWatchlist(entity.id)
            } catch (_: Exception) {}
        }
    }
}
