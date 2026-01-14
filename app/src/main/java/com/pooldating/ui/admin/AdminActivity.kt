package com.pooldating.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.pooldating.R

class AdminActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel

    private lateinit var spinnerCity: Spinner
    private lateinit var btnLoadPool: Button
    private lateinit var tvPoolStatus: TextView
    private lateinit var tvCounts: TextView
    private lateinit var tvBufferCounts: TextView
    private lateinit var btnAddMale: Button
    private lateinit var btnAddFemale: Button
    private lateinit var btnAdd5Male: Button
    private lateinit var btnAdd5Female: Button
    private lateinit var etTargetPoolId: EditText

    private lateinit var progressBar: ProgressBar
    
    // Predefined city list - Kochi-only for initial launch
    private val cities = listOf("Kochi")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Initialize Views
        spinnerCity = findViewById(R.id.spinnerCity)
        btnLoadPool = findViewById(R.id.btnLoadPool)
        tvPoolStatus = findViewById(R.id.tvPoolStatus)
        tvCounts = findViewById(R.id.tvCounts)
        tvBufferCounts = findViewById(R.id.tvBufferCounts)
        btnAddMale = findViewById(R.id.btnAddMale)
        btnAddFemale = findViewById(R.id.btnAddFemale)
        btnAdd5Male = findViewById(R.id.btnAdd5Male)
        btnAdd5Female = findViewById(R.id.btnAdd5Female)
        
        // FF Controls
        etTargetPoolId = findViewById(R.id.etTargetPoolId)

        
        progressBar = findViewById(R.id.progressBar)
        
        // ... (Initialization continues)

        // Initialize ViewModel
        val factory = AdminViewModelFactory()
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[AdminViewModel::class.java]

        // Setup Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter
        
        // Wrapper to get Pool ID (Input or Loaded)
        fun resolvePoolId(): String? {
            val inputId = etTargetPoolId.text.toString().trim()
            if (inputId.isNotEmpty()) return inputId
            return viewModel.pool.value?.pool_id
        }

        // Set Click Listeners
        btnLoadPool.setOnClickListener {
            val city = spinnerCity.selectedItem?.toString() ?: ""
            if (city.isNotEmpty()) {
                viewModel.loadPoolInfo(city)
            }
        }
        
        // Add User Buttons
        btnAddMale.setOnClickListener {
            val city = spinnerCity.selectedItem?.toString() ?: ""
            if (city.isNotEmpty()) viewModel.addDummyUser(city, "male")
        }
        
        btnAddFemale.setOnClickListener {
            val city = spinnerCity.selectedItem?.toString() ?: ""
            if (city.isNotEmpty()) viewModel.addDummyUser(city, "female")
        }
        
        btnAdd5Male.setOnClickListener {
             val city = spinnerCity.selectedItem?.toString() ?: ""
            if (city.isNotEmpty()) viewModel.addDummyUsers(city, "male", 5)
        }
        
        btnAdd5Female.setOnClickListener {
             val city = spinnerCity.selectedItem?.toString() ?: ""
            if (city.isNotEmpty()) viewModel.addDummyUsers(city, "female", 5)
        }


        // Initialize FF Buttons
        val btnFFJoin = findViewById<Button>(R.id.btnFFJoin)
        val btnFFEnd = findViewById<Button>(R.id.btnFFEnd)
        val btnTriggerChecks = findViewById<Button>(R.id.btnTriggerChecks)

        btnFFJoin.setOnClickListener {
             val poolId = resolvePoolId()
             if (poolId != null) {
                 Toast.makeText(this, "Targeting Pool: $poolId", Toast.LENGTH_SHORT).show()
                 viewModel.fastForwardPool(poolId, "validating")
             }
             else Toast.makeText(this, "No Pool ID (Load a pool or enter ID)", Toast.LENGTH_SHORT).show()
        }
        
        btnFFEnd.setOnClickListener {
             val poolId = resolvePoolId()
             if (poolId != null) {
                Toast.makeText(this, "Targeting Pool: $poolId", Toast.LENGTH_SHORT).show()
                viewModel.fastForwardPool(poolId, "completed")
             }
             else Toast.makeText(this, "No Pool ID (Load a pool or enter ID)", Toast.LENGTH_SHORT).show()
        }
        
        btnTriggerChecks.setOnClickListener {
             viewModel.triggerLifecycleChecks()
        }



        // Observers
        try {
            viewModel.pool.observe(this, Observer { pool ->
                if (pool != null) {
                    tvPoolStatus.text = "Status: ${pool.status} (ID: ${pool.pool_id?.take(8) ?: "N/A"})"
                    tvCounts.text = "Pool: ${pool.male_count ?: 0}M / ${pool.female_count ?: 0}F"
                    tvBufferCounts.text = "Buffer: ${pool.buffer_m_count ?: 0}M / ${pool.buffer_f_count ?: 0}F"
                    
                    // Update Timeline UI
                    updateTimelineUI(pool)
                } else {
                    tvPoolStatus.text = "Status: No active pool found for this city"
                    tvCounts.text = "Pool: 0M / 0F"
                    tvBufferCounts.text = "Buffer: 0M / 0F"
                    resetTimelineUI()
                }
            })
    
            viewModel.isLoading.observe(this, Observer { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                // Disable buttons while loading to prevent spam
                btnAddMale.isEnabled = !isLoading
                btnAddFemale.isEnabled = !isLoading
                btnAdd5Male.isEnabled = !isLoading
                if (::btnAdd5Female.isInitialized) btnAdd5Female.isEnabled = !isLoading
            })
    
            viewModel.message.observe(this, Observer { msg ->
                if (msg.isNotEmpty()) {
                      Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            })
            
            // Iteration 8: Inspect result observer
            viewModel.inspectResult.observe(this, Observer { result ->
                if (result.isNotEmpty()) {
                    findViewById<TextView>(R.id.tvInspectResult).text = result
                }
            })
        } catch (e: Exception) {
             android.util.Log.e("AdminActivity", "Observer Error", e)
             Toast.makeText(this, "Observer Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        // Iteration 8: Admin Inspection Buttons
        findViewById<Button>(R.id.btnInspectMe).setOnClickListener {
            viewModel.inspectCurrentUser()
        }
        
        findViewById<Button>(R.id.btnInspectPool).setOnClickListener {
            val pool = viewModel.pool.value
            if (pool?.pool_id != null) {
                viewModel.inspectPoolById(pool.pool_id!!)
            } else {
                Toast.makeText(this, "Load a pool first!", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnInspectCredits).setOnClickListener {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                viewModel.inspectCredits(uid)
            } else {
                Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show()
            }
        }
        
        // NEW: Inspect specific user targets
        findViewById<Button>(R.id.btnInspectTargetUser).setOnClickListener {
            val targetUid = findViewById<EditText>(R.id.etTargetUserId).text.toString().trim()
            if (targetUid.isNotBlank()) {
                viewModel.inspectUser(targetUid)
            } else {
                Toast.makeText(this, "Enter a User ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnInspectTargetCredits).setOnClickListener {
            val targetUid = findViewById<EditText>(R.id.etTargetUserId).text.toString().trim()
            if (targetUid.isNotBlank()) {
                viewModel.inspectCredits(targetUid)
            } else {
                Toast.makeText(this, "Enter a User ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnVerifyInvariants).setOnClickListener {
            viewModel.verifyInvariants()
        }
        
        // Bootstrap Admin (one-time setup)
        findViewById<Button>(R.id.btnBootstrapAdmin).setOnClickListener {
            viewModel.bootstrapAdmin()
        }
    }
    
    private fun updateTimelineUI(pool: com.pooldating.data.model.Pool) {
        val tvPhaseJoining = findViewById<TextView>(R.id.tvPhaseJoining)
        val tvPhaseMatching = findViewById<TextView>(R.id.tvPhaseMatching)
        val progressTimeline = findViewById<android.widget.ProgressBar>(R.id.progressTimeline)
        val tvTimeRemaining = findViewById<TextView>(R.id.tvTimeRemaining)
        val tvMatchDeadline = findViewById<TextView>(R.id.tvMatchDeadline)
        
        val isJoining = pool.status == "joining"
        val isMatching = pool.status in listOf("validating", "matching")
        
        // Update phase indicators
        tvPhaseJoining.setBackgroundColor(if (isJoining) 0xFFE3F2FD.toInt() else 0xFFF5F5F5.toInt())
        tvPhaseJoining.setTextColor(if (isJoining) 0xFF1976D2.toInt() else 0xFF757575.toInt())
        
        tvPhaseMatching.setBackgroundColor(if (isMatching) 0xFFFCE4EC.toInt() else 0xFFF5F5F5.toInt())
        tvPhaseMatching.setTextColor(if (isMatching) 0xFFC2185B.toInt() else 0xFF757575.toInt())
        
        // Calculate progress
        val now = System.currentTimeMillis()
        val createdAt = pool.created_at?.toDate()?.time ?: now
        val joinDeadline = pool.join_deadline?.toDate()?.time ?: (now + 5 * 24 * 60 * 60 * 1000L)
        val matchDeadline = pool.match_deadline?.toDate()?.time ?: (joinDeadline + 2 * 24 * 60 * 60 * 1000L)
        
        val totalDuration = matchDeadline - createdAt
        val elapsed = now - createdAt
        val progress = ((elapsed.toFloat() / totalDuration) * 100).coerceIn(0f, 100f).toInt()
        progressTimeline.progress = progress
        
        // Format deadlines
        val dateFormat = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
        val joinDeadlineStr = dateFormat.format(java.util.Date(joinDeadline))
        val matchDeadlineStr = dateFormat.format(java.util.Date(matchDeadline))
        
        // Calculate time remaining
        val joinRemaining = joinDeadline - now
        val matchRemaining = matchDeadline - now
        
        val joinRemainingStr = if (joinRemaining > 0) {
            val days = joinRemaining / (24 * 60 * 60 * 1000)
            val hours = (joinRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            "${days}d ${hours}h left"
        } else "Closed"
        
        val matchRemainingStr = if (matchRemaining > 0) {
            val days = matchRemaining / (24 * 60 * 60 * 1000)
            val hours = (matchRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            "${days}d ${hours}h left"
        } else "Ended"
        
        tvTimeRemaining.text = "⏰ Join: $joinDeadlineStr ($joinRemainingStr)"
        tvMatchDeadline.text = "🎯 Match: $matchDeadlineStr ($matchRemainingStr)"
    }
    
    private fun resetTimelineUI() {
        val tvPhaseJoining = findViewById<TextView>(R.id.tvPhaseJoining)
        val tvPhaseMatching = findViewById<TextView>(R.id.tvPhaseMatching)
        val progressTimeline = findViewById<android.widget.ProgressBar>(R.id.progressTimeline)
        val tvTimeRemaining = findViewById<TextView>(R.id.tvTimeRemaining)
        val tvMatchDeadline = findViewById<TextView>(R.id.tvMatchDeadline)
        
        tvPhaseJoining.setBackgroundColor(0xFFF5F5F5.toInt())
        tvPhaseJoining.setTextColor(0xFF757575.toInt())
        tvPhaseMatching.setBackgroundColor(0xFFF5F5F5.toInt())
        tvPhaseMatching.setTextColor(0xFF757575.toInt())
        progressTimeline.progress = 0
        tvTimeRemaining.text = "⏰ Join deadline: --"
        tvMatchDeadline.text = "🎯 Match deadline: --"
    }
}
