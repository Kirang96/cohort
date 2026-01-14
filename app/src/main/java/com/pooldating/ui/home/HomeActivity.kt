package com.pooldating.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.pooldating.R
import com.pooldating.data.model.Pool
import com.pooldating.ui.login.LoginActivity
import com.pooldating.ui.profile.EditProfileActivity
import com.pooldating.utils.Result
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    
    // Notification Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("HomeActivity", "Notification permission granted")
        } else {
            android.util.Log.w("HomeActivity", "Notification permission denied")
            Toast.makeText(this, "Notifications enabled only for matches/chats", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private lateinit var tvName: TextView
    private lateinit var tvCity: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnViewMatches: Button
    private lateinit var btnRunMatchmaking: Button

    // Pool UI
    // Pool UI (Top - City/Joining)
    private lateinit var cardPoolStatus: CardView
    private lateinit var tvPoolTitle: TextView
    private lateinit var tvPoolStatus: TextView
    private lateinit var tvMaleCount: TextView
    private lateinit var tvFemaleCount: TextView
    private lateinit var btnJoinPool: Button

    // Membership UI (Bottom - History/Status)
    private lateinit var cardMembershipPool: CardView
    private lateinit var tvPoolTitleMem: TextView
    private lateinit var tvPoolStatusMem: TextView
    private lateinit var tvMaleCountMem: TextView
    private lateinit var tvFemaleCountMem: TextView
    private lateinit var tvMembershipStatusMem: TextView
    private lateinit var btnViewMatchesMem: Button

    // Credit UI
    private lateinit var cardCredits: CardView
    private lateinit var tvCreditBalance: TextView
    private lateinit var btnAddCredits: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("HomeActivity", "onCreate started")
        Toast.makeText(this, "HOME: onCreate started", Toast.LENGTH_SHORT).show()
        
        try {
            setContentView(R.layout.activity_home)
            android.util.Log.d("HomeActivity", "setContentView done")
            
            // Initialize Google Sign In Client for logout
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).requestIdToken(getString(R.string.default_web_client_id)).build()
            googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

            // Init Views
            tvName = findViewById(R.id.tvName)
            tvCity = findViewById(R.id.tvCity)
            btnEdit = findViewById(R.id.btnEditProfile)
            btnLogout = findViewById(R.id.btnLogout)
            progressBar = findViewById(R.id.progressBar)
            android.util.Log.d("HomeActivity", "Basic views initialized")

            cardPoolStatus = findViewById(R.id.cardPoolStatus)
            tvPoolTitle = findViewById(R.id.tvPoolStatusTitle)
            tvPoolStatus = findViewById(R.id.tvPoolStatusText)
            tvMaleCount = findViewById(R.id.tvMaleCount)
            tvFemaleCount = findViewById(R.id.tvFemaleCount)
            btnJoinPool = findViewById(R.id.btnJoinPool)
            btnRunMatchmaking = findViewById(R.id.btnRunMatchmaking)

            // Membership Card Init
            cardMembershipPool = findViewById(R.id.cardMembershipPool)
            tvPoolTitleMem = findViewById(R.id.tvPoolTitle_Member)
            tvPoolStatusMem = findViewById(R.id.tvPoolStatusText_Member)
            tvMaleCountMem = findViewById(R.id.tvMaleCount_Member)
            tvFemaleCountMem = findViewById(R.id.tvFemaleCount_Member)
            tvMembershipStatusMem = findViewById(R.id.tvMembershipStatus_Member)
            btnViewMatchesMem = findViewById(R.id.btnViewMatches_Member)

            android.util.Log.d("HomeActivity", "Pool views initialized")

            // Credit Views
            cardCredits = findViewById(R.id.cardCredits)
            tvCreditBalance = findViewById(R.id.tvCreditBalance)
            btnAddCredits = findViewById(R.id.btnAddCredits)
            android.util.Log.d("HomeActivity", "Credit views initialized")

            // Click Listeners
            btnEdit.setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }

            findViewById<Button>(R.id.btnMyChats).setOnClickListener {
                startActivity(Intent(this, com.pooldating.ui.chat.ChatsListActivity::class.java))
            }
            
            // Admin Tools (available when logged in)
            findViewById<Button>(R.id.btnAdminTools).setOnClickListener {
                startActivity(Intent(this, com.pooldating.ui.admin.AdminActivity::class.java))
            }

            btnLogout.setOnClickListener {
                Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
                // Unregister FCM token to prevent notifications for this account
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    com.google.firebase.functions.FirebaseFunctions.getInstance()
                        .getHttpsCallable("unregisterDeviceToken")
                        .call(hashMapOf("fcm_token" to token))
                        .addOnCompleteListener { 
                            // Proceed to logout regardless of success
                            googleSignInClient.signOut().addOnCompleteListener {
                                viewModel.signOut()
                            }
                        }
                }.addOnFailureListener {
                    // Fallback if token retrieval fails
                    googleSignInClient.signOut().addOnCompleteListener {
                        viewModel.signOut()
                    }
                }
            }

            btnJoinPool.setOnClickListener {
            // Optimistic UI: Disable immediately
            btnJoinPool.isEnabled = false
            btnJoinPool.text = "Joining..."
            
            val pool = viewModel.activePool.value
            if (pool != null) {
                // Check if user has enough credits (1 for males)
                viewModel.joinPool()
            }
        }

            btnAddCredits.setOnClickListener {
                viewModel.purchaseCredits(5).also {
                    Toast.makeText(this, "Purchasing 5 credits...", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnViewMatchesMem.setOnClickListener {
                 val pool = viewModel.membershipPool.value
                 if (pool != null) {
                     val intent = Intent(this, com.pooldating.ui.matches.MatchesActivity::class.java)
                     intent.putExtra("POOL_ID", pool.pool_id)
                     startActivity(intent)
                 }
            }
            
            btnRunMatchmaking.setOnClickListener {
                 viewModel.runMatchmaking()
                 Toast.makeText(this, "Matchmaking Triggered...", Toast.LENGTH_SHORT).show()
            }
            
            android.util.Log.d("HomeActivity", "Click listeners set")

            android.util.Log.d("HomeActivity", "Click listeners set")
 
            observeViewModel()
            
            // Observe missed activity summary
            viewModel.missedSummary.observe(this) { summary ->
                if (!summary.isNullOrEmpty()) {
                    if (summary.startsWith("Debug Error")) {
                         android.widget.Toast.makeText(this, summary, android.widget.Toast.LENGTH_LONG).show()
                    } else {
                         showLocalNotification("Welcome Back!", summary)
                    }
                    viewModel.onSummaryShown()
                }
            }
            
            android.util.Log.d("HomeActivity", "onCreate completed successfully")
            
            // ONE-TIME DATA MIGRATION: Backfill names in existing chats/matches
            // This runs automatically on first app launch and uses SharedPreferences to ensure it only runs once
            runOneTimeBackfill()
            
            // FCM: Register device token for push notifications
            registerFcmToken()
            
            // FCM: Request runtime permission (Android 13+)
            askNotificationPermission()
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "onCreate CRASHED", e)
            Toast.makeText(this, "Home init error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Register FCM token with backend for push notifications
     * NOTE: This is informational only. App functions correctly without notifications.
     */
    private fun registerFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                android.util.Log.d("HomeActivity", "FCM token: $token")
                
                // Register with backend
                com.google.firebase.functions.FirebaseFunctions.getInstance()
                    .getHttpsCallable("registerDeviceToken")
                    .call(hashMapOf("fcm_token" to token))
                    .addOnSuccessListener {
                        android.util.Log.d("HomeActivity", "FCM token registered with backend")
                    }
                    .addOnFailureListener { e ->
                        // Non-fatal - app works without notifications
                        android.util.Log.e("HomeActivity", "FCM registration failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                // Non-fatal - app works without notifications
                android.util.Log.e("HomeActivity", "Failed to get FCM token: ${e.message}")
            }
    }

    private fun askNotificationPermission() {
        // Android 13 (API 33) and above requires runtime permission for notifications
        if (Build.VERSION.SDK_INT >= 33) { // Build.VERSION_CODES.TIRAMISU
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                // Already granted
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun runOneTimeBackfill() {
        val prefs = getSharedPreferences("pool_dating_prefs", MODE_PRIVATE)
        
        // Backfill 1: Chat Names
        val hasRunChatBackfill = prefs.getBoolean("has_run_backfill_v1", false)
        if (!hasRunChatBackfill) {
            android.util.Log.d("HomeActivity", "Running one-time chat names backfill...")
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("backfillChatNames")
                .call(hashMapOf<String, Any>())
                .addOnSuccessListener {
                    android.util.Log.d("HomeActivity", "Chat names backfill completed")
                    prefs.edit().putBoolean("has_run_backfill_v1", true).apply()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("HomeActivity", "Chat names backfill failed: ${e.message}")
                }
        }
        
        // Backfill 2: Membership Status (fixes stale active/buffer memberships for completed pools)
        val hasRunMembershipBackfill = prefs.getBoolean("has_run_membership_backfill_v1", false)
        if (!hasRunMembershipBackfill) {
            android.util.Log.d("HomeActivity", "Running one-time membership status backfill...")
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("backfillMembershipStatus")
                .call(hashMapOf<String, Any>())
                .addOnSuccessListener {
                    android.util.Log.d("HomeActivity", "Membership status backfill completed")
                    prefs.edit().putBoolean("has_run_membership_backfill_v1", true).apply()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("HomeActivity", "Membership status backfill failed: ${e.message}")
                }
        }
    }

    private fun observeViewModel() {
        // User Data
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvName.text = "Name: ${user.name ?: "Not set"}"
                tvCity.text = "City: ${user.city ?: "Not set"}"
                
                // Hide credit card for females
                if (user.gender == "Female") {
                    cardCredits.visibility = View.GONE
                } else {
                    cardCredits.visibility = View.VISIBLE
                }
                
                // Restriction Banner
                val cardRestriction: CardView = findViewById(R.id.cardRestriction)
                val tvReason: TextView = findViewById(R.id.tvRestrictionReason)
                
                if (user.restriction_level != null && user.restriction_level != "none") {
                    cardRestriction.visibility = View.VISIBLE
                    tvReason.text = "Reason: ${user.restriction_reason ?: "Community Guidelines"}"
                } else {
                    cardRestriction.visibility = View.GONE
                }
            }
        }

        // ========================================
        // TOP CARD: City Pool (for joining)
        // ========================================
        // Shows if: activePool exists (a joining pool in user's city)
        viewModel.activePool.observe(this) { pool ->
            updatePoolCard(pool, viewModel.myMembership.value)
        }

        // Listen to membership changes to update button state
        viewModel.myMembership.observe(this) { membership ->
            updatePoolCard(viewModel.activePool.value, membership)
        }

        // ========================================
        // BOTTOM CARD: User's Pool (completed - for viewing matches)
        // ========================================
        // Shows if: membershipPool exists AND pool.status == "completed"
        viewModel.membershipPool.observe(this) { pool ->
            if (pool != null && pool.status == "completed") {
                cardMembershipPool.visibility = View.VISIBLE
                tvPoolTitleMem.text = "Previous Pool (${pool.city ?: "Unknown"})"
                tvPoolStatusMem.text = "Status: Completed"
                tvMaleCountMem.text = "Men: ${pool.male_count ?: 0}"
                tvFemaleCountMem.text = "Women: ${pool.female_count ?: 0}"
                btnViewMatchesMem.visibility = View.VISIBLE
                tvMembershipStatusMem.text = "View your matches!"
            } else {
                cardMembershipPool.visibility = View.GONE
            }
        }

        // Join State
        viewModel.joinState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                     btnJoinPool.isEnabled = false
                     btnJoinPool.text = "Joining..."
                }
                is Result.Success -> {
                    // User is now in buffer, awaiting
                    btnJoinPool.isEnabled = false
                    btnJoinPool.text = "Awaiting..."
                    Toast.makeText(this, "Added to waiting list!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    btnJoinPool.isEnabled = true
                    btnJoinPool.text = "Join Pool"
                    Toast.makeText(this, "Join Failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Sign Out
        viewModel.signOutState.observe(this) { signedOut ->
            if (signedOut) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        // Credit Balance
        viewModel.creditBalance.observe(this) { balance ->
            tvCreditBalance.text = balance.toString()
            android.util.Log.d("HomeActivity", "Credit balance updated: $balance")
            
            // Update Join button state for male users
            val user = viewModel.currentUser.value
            if (user?.gender == "Male" && balance < 1) {
                btnJoinPool.isEnabled = false
                btnJoinPool.text = "Join Pool (Need Credits)"
            } else if (viewModel.activePool.value?.status == "joining") {
                btnJoinPool.isEnabled = true
                btnJoinPool.text = "Join Pool"
            }
        }

        // Purchase State
        viewModel.purchaseState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    btnAddCredits.isEnabled = false
                    btnAddCredits.text = "Adding..."
                }
                is Result.Success -> {
                    btnAddCredits.isEnabled = true
                    btnAddCredits.text = "+ Add Credits"
                    Toast.makeText(this, "Added ${result.data.creditsAdded} credits!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    btnAddCredits.isEnabled = true
                    btnAddCredits.text = "+ Add Credits"
                    Toast.makeText(this, "Purchase failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                null -> { /* Initial state */ }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadUserData()
    }
    private fun updatePoolCard(pool: Pool?, membership: com.pooldating.data.model.PoolMembership?) {
        if (pool != null) {
            cardPoolStatus.visibility = View.VISIBLE
            tvPoolTitle.text = "Pool in ${pool.city ?: "Your City"}"
            tvPoolStatus.text = "Status: ${pool.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"}"

            // Show Total Count (Active + Buffer)
            val totalMale = (pool.male_count ?: 0) + (pool.buffer_m_count ?: 0)
            val totalFemale = (pool.female_count ?: 0) + (pool.buffer_f_count ?: 0)

            tvMaleCount.text = "Men: $totalMale"
            tvFemaleCount.text = "Women: $totalFemale"

            // Button Logic
            // Check if user is member of THIS pool
            val isMemberOfThisPool = membership != null && membership.pool_id == pool.pool_id
            // Check if user has ANY active/buffer membership (excludes completed)
            val isActiveOrBuffer = membership != null && (membership.status == "active" || membership.status == "buffer")

            if (isMemberOfThisPool && isActiveOrBuffer) {
                // In THIS pool (active or buffer)
                if (membership?.status == "buffer") {
                    btnJoinPool.isEnabled = false
                    btnJoinPool.text = "Awaiting..."
                } else {
                    btnJoinPool.isEnabled = false
                    btnJoinPool.text = "Joined ✓"
                }
                btnJoinPool.visibility = View.VISIBLE
            } else if (isActiveOrBuffer) {
                // In ANOTHER pool (active or buffer)
                btnJoinPool.isEnabled = false
                btnJoinPool.text = "In another pool"
                btnJoinPool.visibility = View.VISIBLE
            } else {
                // Not in any active pool (null or completed) -> CAN JOIN
                // Reset button to Join state
                btnJoinPool.isEnabled = pool.status == "joining"
                btnJoinPool.text = "Join Pool"
                btnJoinPool.visibility = View.VISIBLE
            }

            if (pool.status == "validating") btnRunMatchmaking.visibility = View.VISIBLE
            else btnRunMatchmaking.visibility = View.GONE
            
            // Update Timeline UI
            updatePoolTimeline(pool)

        } else {
            cardPoolStatus.visibility = View.GONE
        }
    }
    
    private fun updatePoolTimeline(pool: Pool) {
        val tvPhaseJoining = findViewById<TextView>(R.id.tvPhaseJoiningHome)
        val tvPhaseMatching = findViewById<TextView>(R.id.tvPhaseMatchingHome)
        val progressTimeline = findViewById<android.widget.ProgressBar>(R.id.progressTimelineHome)
        val tvTimeRemaining = findViewById<TextView>(R.id.tvTimeRemainingHome)
        
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
        
        // Calculate time remaining
        val remaining = if (isJoining) joinDeadline - now else matchDeadline - now
        val remainingStr = if (remaining > 0) {
            val days = remaining / (24 * 60 * 60 * 1000)
            val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            "${days}d ${hours}h left"
        } else "Ended"
        
        val phaseLabel = if (isJoining) "⏰ Joining ends" else "💘 Matching ends"
        tvTimeRemaining.text = "$phaseLabel: $remainingStr"
    }
    
    private fun showLocalNotification(title: String, message: String) {
        val channelId = "pool_dating_notifications"
        
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId,
                "Pool Dating Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
