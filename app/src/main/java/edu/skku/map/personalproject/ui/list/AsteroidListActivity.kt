package edu.skku.map.personalproject.ui.list

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import edu.skku.map.personalproject.R
import edu.skku.map.personalproject.data.repository.AsteroidRepository
import edu.skku.map.personalproject.databinding.ActivityAsteroidListBinding
import edu.skku.map.personalproject.ui.detail.AsteroidDetailActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AsteroidListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsteroidListBinding
    private lateinit var repository: AsteroidRepository
    private lateinit var adapter: AsteroidAdapter

    private var selectedDate: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsteroidListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AsteroidRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.asteroid_list_title)

        setupRecyclerView()

        binding.tvSelectedDate.text = selectedDate
        binding.cardDateSelector.setOnClickListener { showDatePicker() }

        fetchData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        adapter = AsteroidAdapter { asteroid ->
            val intent = Intent(this, AsteroidDetailActivity::class.java).apply {
                putExtra(AsteroidDetailActivity.EXTRA_ASTEROID_JSON, Gson().toJson(asteroid))
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun showDatePicker() {
        val parts = selectedDate.split("-")
        val year  = parts[0].toInt()
        val month = parts[1].toInt() - 1  // Calendar.MONTH는 0-based
        val day   = parts[2].toInt()

        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
            binding.tvSelectedDate.text = selectedDate
            fetchData()
        }, year, month, day).show()
    }

    private fun fetchData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val feed = repository.fetchFeed(selectedDate)
                binding.progressBar.visibility = View.GONE
                if (feed.asteroids.isEmpty()) {
                    binding.tvEmpty.text = getString(R.string.list_empty)
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.submitList(feed.asteroids)
                    binding.recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.text = getString(R.string.error_network)
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(
                    this@AsteroidListActivity,
                    e.message ?: getString(R.string.error_network),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
