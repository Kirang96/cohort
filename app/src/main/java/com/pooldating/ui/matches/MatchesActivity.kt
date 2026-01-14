package com.pooldating.ui.matches

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pooldating.R
import com.pooldating.utils.Result
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.pooldating.ui.login.LoginActivity

class MatchesActivity : AppCompatActivity() {

    private val viewModel: MatchesViewModel by viewModels()
    private lateinit var adapter: MatchesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var rvMatches: RecyclerView
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_matches)

        val poolId = intent.getStringExtra("POOL_ID")
        if (poolId == null) {
            Toast.makeText(this, "Error: Pool ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar = findViewById(R.id.progressBarMatches)
        rvMatches = findViewById(R.id.rvMatches)
        tvTitle = findViewById(R.id.tvMatchesTitle)
        
        adapter = MatchesAdapter()
        rvMatches.layoutManager = LinearLayoutManager(this)
        rvMatches.adapter = adapter

        viewModel.loadMatches(poolId)
        
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.matches.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    rvMatches.visibility = View.GONE
                }
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    rvMatches.visibility = View.VISIBLE
                    adapter.setMatches(result.data)
                    
                    if (result.data.isEmpty()) {
                        Toast.makeText(this, "No matches found yet.", Toast.LENGTH_SHORT).show()
                    }
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error loading matches: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
